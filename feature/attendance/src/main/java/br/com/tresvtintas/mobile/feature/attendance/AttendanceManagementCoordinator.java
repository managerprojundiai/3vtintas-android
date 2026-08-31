package br.com.tresvtintas.mobile.feature.attendance;

import android.content.Context;
import android.os.Bundle;
import androidx.core.content.ContextCompat;
import br.com.tresvtintas.mobile.core.attendance.AttendanceConversation;
import br.com.tresvtintas.mobile.core.attendance.AttendanceFailureKind;
import br.com.tresvtintas.mobile.core.attendance.AttendanceManagementController;
import br.com.tresvtintas.mobile.core.attendance.AttendanceManagementSelection;
import br.com.tresvtintas.mobile.core.attendance.AttendanceManagementState;
import br.com.tresvtintas.mobile.feature.attendance.databinding.AttendanceActivityDetailBinding;
import java.util.Optional;

final class AttendanceManagementCoordinator {
    private static final String STATE_KEY = "attendance.management.key";
    private static final String STATE_FINGERPRINT =
            "attendance.management.fingerprint";

    private final Context context;
    private final AttendanceManagementDialog dialog;
    private final AttendanceManagementRenderer renderer;
    private final Runnable refreshConversation;
    private final AttendanceManagementController.Listener listener =
            this::render;
    private AttendanceManagementAttempt attempt;
    private Optional<AttendanceManagementController> controller =
            Optional.empty();
    private Optional<AttendanceConversation> conversation =
            Optional.empty();
    private Optional<AttendanceManagementState.Snapshot> snapshot =
            Optional.empty();
    private AttendanceManagementState.Phase phase =
            AttendanceManagementState.Phase.IDLE;
    private boolean allowed;
    private String openedRevision = "";

    AttendanceManagementCoordinator(
            Context context,
            AttendanceActivityDetailBinding binding,
            Runnable refreshConversation,
            Bundle savedState) {
        this.context = context;
        this.refreshConversation = refreshConversation;
        dialog = new AttendanceManagementDialog(context);
        renderer = new AttendanceManagementRenderer(binding);
        attempt = savedState == null
                ? new AttendanceManagementAttempt()
                : AttendanceManagementAttempt.restored(
                        savedState.getString(STATE_KEY),
                        savedState.getString(STATE_FINGERPRINT));
        renderer.configure(false);
        binding.attendanceManagementOpen.setOnClickListener(
                ignored -> openDialog());
        binding.attendanceManagementRetry.setOnClickListener(
                ignored -> retry());
    }

    void start(AttendanceFeatureRuntime runtime) {
        stopController();
        allowed = runtime.canManage();
        renderer.configure(allowed);
        if (!allowed) {
            return;
        }
        AttendanceManagementController next =
                new AttendanceManagementController(
                        runtime.repository(),
                        runtime.workerExecutor(),
                        ContextCompat.getMainExecutor(context));
        controller = Optional.of(next);
        next.subscribe(listener);
        conversation.ifPresent(this::openIfNeeded);
    }

    void stop() {
        stopController();
        allowed = false;
        snapshot = Optional.empty();
        phase = AttendanceManagementState.Phase.IDLE;
        openedRevision = "";
        renderer.configure(false);
    }

    void onConversation(AttendanceConversation value) {
        conversation = Optional.of(value);
        if (snapshot.filter(current ->
                        current.conversationId().equals(value.id())
                                && current.revision() > value.revision())
                .isPresent()) {
            return;
        }
        if (allowed) {
            openIfNeeded(value);
        }
    }

    void saveState(Bundle state) {
        state.putString(STATE_KEY, attempt.key());
        state.putString(STATE_FINGERPRINT, attempt.fingerprint());
    }

    private void openIfNeeded(AttendanceConversation value) {
        String revisionKey = revisionKey(value.id(), value.revision());
        if (revisionKey.equals(openedRevision)
                || controller.isEmpty()) {
            return;
        }
        openedRevision = revisionKey;
        controller.orElseThrow().open(value);
    }

    private void retry() {
        openedRevision = "";
        conversation.ifPresent(this::openIfNeeded);
    }

    private void render(AttendanceManagementState state) {
        phase = state.phase();
        snapshot = state.snapshot();
        renderer.render(state);
        if (state.phase() == AttendanceManagementState.Phase.SUCCESS) {
            AttendanceManagementState.Snapshot current =
                    state.snapshot().orElseThrow();
            openedRevision = revisionKey(
                    current.conversationId(),
                    current.revision());
            attempt.reset();
            refreshConversation.run();
        } else if (state.phase()
                        == AttendanceManagementState.Phase.ERROR
                && state.failure().orElse(null)
                        == AttendanceFailureKind.CONFLICT) {
            attempt.reset();
            openedRevision = "";
            refreshConversation.run();
        } else if (state.phase()
                        == AttendanceManagementState.Phase.ERROR
                && state.failure().orElse(null)
                        == AttendanceFailureKind.IDEMPOTENCY_KEY_REUSED) {
            attempt.reset();
        }
    }

    private void openDialog() {
        if (!allowed
                || snapshot.isEmpty()
                || phase == AttendanceManagementState.Phase.LOADING
                || phase == AttendanceManagementState.Phase.SAVING) {
            return;
        }
        AttendanceManagementState.Snapshot current =
                snapshot.orElseThrow();
        dialog.show(current, selection -> save(current, selection));
    }

    private void save(
            AttendanceManagementState.Snapshot current,
            AttendanceManagementSelection selection) {
        if (selection.equals(current.selection())) {
            renderer.showNoChanges();
            return;
        }
        controller.ifPresent(value -> value.save(
                selection,
                attempt.keyFor(
                        current.conversationId(),
                        current.revision(),
                        selection)));
    }

    private void stopController() {
        controller.ifPresent(value -> {
            value.unsubscribe(listener);
            value.close();
        });
        controller = Optional.empty();
    }

    private static String revisionKey(String id, int revision) {
        return id + "#" + revision;
    }
}
