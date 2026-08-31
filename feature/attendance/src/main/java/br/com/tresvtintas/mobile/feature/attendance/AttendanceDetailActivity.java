package br.com.tresvtintas.mobile.feature.attendance;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import br.com.tresvtintas.mobile.core.attendance.AttendanceDetailController;
import br.com.tresvtintas.mobile.core.attendance.AttendanceDetailState;
import br.com.tresvtintas.mobile.core.attendance.AttendanceException;
import br.com.tresvtintas.mobile.core.attendance.AttendanceFailureKind;
import br.com.tresvtintas.mobile.core.attendance.AttendanceReplyContent;
import br.com.tresvtintas.mobile.core.attendance.AttendanceReplyController;
import br.com.tresvtintas.mobile.core.attendance.AttendanceReplyState;
import br.com.tresvtintas.mobile.feature.attendance.databinding.AttendanceActivityDetailBinding;
import java.util.Optional;

public final class AttendanceDetailActivity extends AppCompatActivity {
    private static final String EXTRA_CONVERSATION_ID =
            "br.com.tresvtintas.mobile.attendance.CONVERSATION_ID";
    private static final String STATE_REPLY_KEY =
            "attendance.reply.key";
    private static final String STATE_REPLY_FINGERPRINT =
            "attendance.reply.fingerprint";
    private final AttendanceDetailController.Listener listener =
            this::render;
    private final AttendanceReplyController.Listener replyListener =
            this::renderReply;
    private AttendanceActivityDetailBinding binding;
    private AttendanceDetailRenderer renderer;
    private AttendanceReplyRenderer replyRenderer;
    private AttendanceManagementCoordinator managementCoordinator;
    private AttendanceRealtimeUiCoordinator realtime;
    private AttendanceReplyAttempt replyAttempt =
            new AttendanceReplyAttempt();
    private Optional<AttendanceDetailController> controller =
            Optional.empty();
    private Optional<AttendanceReplyController> replyController =
            Optional.empty();

    public static Intent intent(
            Context context,
            String conversationId) {
        return new Intent(context, AttendanceDetailActivity.class)
                .putExtra(EXTRA_CONVERSATION_ID, conversationId);
    }

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        AttendancePrivacy.protect(this);
        EdgeToEdge.enable(this);
        binding = AttendanceActivityDetailBinding.inflate(
                getLayoutInflater());
        AttendanceMessageAdapter adapter =
                new AttendanceMessageAdapter();
        renderer = new AttendanceDetailRenderer(binding, adapter);
        replyRenderer = new AttendanceReplyRenderer(binding);
        managementCoordinator = new AttendanceManagementCoordinator(
                this,
                binding,
                () -> controller.ifPresent(
                        AttendanceDetailController::refresh),
                state);
        realtime = new AttendanceRealtimeUiCoordinator(
                binding.attendanceDetailRealtimeStatus);
        if (state != null) {
            replyAttempt = AttendanceReplyAttempt.restored(
                    state.getString(STATE_REPLY_KEY),
                    state.getString(STATE_REPLY_FINGERPRINT));
        }
        replyRenderer.configure(false);
        setContentView(binding.getRoot());
        AttendanceInsets.applySystemBars(binding.getRoot());
        LinearLayoutManager messages =
                new LinearLayoutManager(
                        this,
                        LinearLayoutManager.VERTICAL,
                        true);
        messages.setStackFromEnd(true);
        binding.attendanceDetailMessages.setLayoutManager(messages);
        binding.attendanceDetailMessages.setAdapter(adapter);
        binding.attendanceDetailToolbar.setNavigationOnClickListener(
                ignored -> finish());
        binding.attendanceDetailRefresh.setOnClickListener(
                ignored -> controller.ifPresent(
                        AttendanceDetailController::refresh));
        binding.attendanceDetailRetry.setOnClickListener(
                ignored -> openConversation());
        binding.attendanceDetailLoadMore.setOnClickListener(
                ignored -> controller.ifPresent(
                        AttendanceDetailController::loadMore));
        binding.attendanceReplySend.setOnClickListener(
                ignored -> sendReply());
    }

    @Override
    protected void onStart() {
        super.onStart();
        Optional<AttendanceFeatureRuntime> runtime = runtime();
        Optional<String> conversationId = conversationId();
        if (runtime.isEmpty() || conversationId.isEmpty()) {
            render(AttendanceDetailState.error(
                    new AttendanceException(
                            AttendanceFailureKind.ACCESS_REVOKED,
                            "Attendance detail runtime is unavailable.")));
            return;
        }
        AttendanceFeatureRuntime value = runtime.orElseThrow();
        replyRenderer.configure(value.canReply());
        managementCoordinator.start(value);
        AttendanceDetailController next =
                new AttendanceDetailController(
                        value.repository(),
                        value.workerExecutor(),
                        ContextCompat.getMainExecutor(this));
        controller = Optional.of(next);
        next.subscribe(listener);
        if (value.canReply()) {
            AttendanceReplyController replies =
                    new AttendanceReplyController(
                            value.repository(),
                            value.workerExecutor(),
                            ContextCompat.getMainExecutor(this));
            replyController = Optional.of(replies);
            replies.subscribe(replyListener);
        }
        openSafely(next, conversationId.orElseThrow());
        realtime.start(
                value,
                () -> controller.ifPresent(
                        AttendanceDetailController::refresh));
    }

    @Override
    protected void onStop() {
        realtime.stop();
        managementCoordinator.stop();
        controller.ifPresent(value -> {
            value.unsubscribe(listener);
            value.close();
        });
        controller = Optional.empty();
        replyController.ifPresent(value -> {
            value.unsubscribe(replyListener);
            value.close();
        });
        replyController = Optional.empty();
        super.onStop();
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        state.putString(STATE_REPLY_KEY, replyAttempt.key());
        state.putString(
                STATE_REPLY_FINGERPRINT,
                replyAttempt.fingerprint());
        managementCoordinator.saveState(state);
        super.onSaveInstanceState(state);
    }

    private void openConversation() {
        if (controller.isEmpty()) {
            return;
        }
        conversationId().ifPresent(
                value -> openSafely(
                        controller.orElseThrow(),
                        value));
    }

    private void openSafely(
            AttendanceDetailController target,
            String conversationId) {
        try {
            target.open(conversationId);
        } catch (IllegalArgumentException failure) {
            render(AttendanceDetailState.error(
                    new AttendanceException(
                            AttendanceFailureKind.INVALID_REQUEST,
                            "Attendance conversation ID is invalid.",
                            failure)));
        }
    }

    private void render(AttendanceDetailState state) {
        renderer.render(state);
        state.snapshot().ifPresent(value ->
                managementCoordinator.onConversation(
                        value.conversation()));
    }

    private void sendReply() {
        if (replyController.isEmpty()) {
            return;
        }
        Optional<String> target = conversationId();
        if (target.isEmpty()) {
            return;
        }
        final String content;
        try {
            content = AttendanceReplyContent.normalize(
                    String.valueOf(
                            binding.attendanceReplyInput.getText()));
        } catch (IllegalArgumentException failure) {
            binding.attendanceReplyInputLayout.setError(
                    getString(R.string.attendance_reply_invalid));
            return;
        }
        binding.attendanceReplyInputLayout.setError(null);
        replyController.orElseThrow().send(
                target.orElseThrow(),
                content,
                replyAttempt.keyFor(
                        target.orElseThrow(),
                        content));
    }

    private void renderReply(AttendanceReplyState state) {
        replyRenderer.render(state);
        if (state.phase() == AttendanceReplyState.Phase.SUCCESS) {
            binding.attendanceReplyInput.setText("");
            replyAttempt.reset();
            controller.ifPresent(AttendanceDetailController::refresh);
        } else if (state.phase() == AttendanceReplyState.Phase.ERROR
                && state.failure().orElse(null)
                        == AttendanceFailureKind
                                .IDEMPOTENCY_KEY_REUSED) {
            replyAttempt.reset();
        }
    }

    private Optional<String> conversationId() {
        return Optional.ofNullable(
                        getIntent().getStringExtra(
                                EXTRA_CONVERSATION_ID))
                .filter(value -> !value.isBlank());
    }

    private Optional<AttendanceFeatureRuntime> runtime() {
        return getApplication()
                        instanceof AttendanceRuntimeProvider provider
                ? provider.attendanceRuntime()
                : Optional.empty();
    }
}
