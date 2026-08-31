package br.com.tresvtintas.mobile.feature.attendance;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import br.com.tresvtintas.mobile.core.attendance.AttendanceAssignment;
import br.com.tresvtintas.mobile.core.attendance.AttendanceChannel;
import br.com.tresvtintas.mobile.core.attendance.AttendanceException;
import br.com.tresvtintas.mobile.core.attendance.AttendanceFailureKind;
import br.com.tresvtintas.mobile.core.attendance.AttendanceFolder;
import br.com.tresvtintas.mobile.core.attendance.AttendanceListController;
import br.com.tresvtintas.mobile.core.attendance.AttendanceListState;
import br.com.tresvtintas.mobile.core.attendance.AttendancePriority;
import br.com.tresvtintas.mobile.core.attendance.AttendanceQuery;
import br.com.tresvtintas.mobile.feature.attendance.databinding.AttendanceActivityListBinding;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import java.util.List;
import java.util.Optional;

public final class AttendanceListActivity extends AppCompatActivity {
    private static final long SEARCH_DELAY_MILLIS = 350;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final AttendanceListController.Listener listener =
            this::render;
    private AttendanceActivityListBinding binding;
    private AttendanceListRenderer renderer;
    private AttendanceRealtimeUiCoordinator realtime;
    private Optional<AttendanceListController> controller =
            Optional.empty();
    private Optional<Runnable> pendingSearch = Optional.empty();
    private Optional<AttendanceFeatureRuntime> runtime = Optional.empty();

    public static Intent intent(Context context) {
        return new Intent(context, AttendanceListActivity.class);
    }

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        AttendancePrivacy.protect(this);
        EdgeToEdge.enable(this);
        binding = AttendanceActivityListBinding.inflate(
                getLayoutInflater());
        AttendanceConversationAdapter adapter =
                new AttendanceConversationAdapter(
                        conversation -> startActivity(
                                AttendanceDetailActivity.intent(
                                        this,
                                        conversation.id())));
        renderer = new AttendanceListRenderer(binding, adapter);
        realtime = new AttendanceRealtimeUiCoordinator(
                binding.attendanceRealtimeStatus);
        setContentView(binding.getRoot());
        AttendanceInsets.applySystemBars(binding.getRoot());
        binding.attendanceList.setLayoutManager(
                new LinearLayoutManager(this));
        binding.attendanceList.setAdapter(adapter);
        binding.attendanceToolbar.setNavigationOnClickListener(
                ignored -> finish());
        binding.attendanceRefresh.setOnClickListener(
                ignored -> controller.ifPresent(
                        AttendanceListController::refresh));
        binding.attendanceRetry.setOnClickListener(
                ignored -> openCurrent());
        binding.attendanceLoadMore.setOnClickListener(
                ignored -> controller.ifPresent(
                        AttendanceListController::loadMore));
        binding.attendanceSearch.addTextChangedListener(
                searchWatcher());
        configureChannel();
        configureFolder();
        configurePriority();
        binding.attendanceAssignments
                .setOnCheckedStateChangeListener(
                        (group, checked) -> {
                            if (!checked.isEmpty()) {
                                changeAssignment(checked.get(0));
                            }
                        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        runtime = runtime();
        if (runtime.isEmpty()) {
            render(AttendanceListState.error(
                    new AttendanceException(
                            AttendanceFailureKind.ACCESS_REVOKED,
                            "Attendance runtime is unavailable.")));
            return;
        }
        AttendanceFeatureRuntime value = runtime.orElseThrow();
        binding.attendanceAssignments.setVisibility(
                value.assignedOnly() ? View.GONE : View.VISIBLE);
        binding.attendanceScopeNotice.setText(
                value.assignedOnly()
                        ? R.string.attendance_scope_assigned
                        : value.organizationId().isPresent()
                                ? R.string.attendance_scope_store
                                : R.string.attendance_scope_global);
        AttendanceListController next = new AttendanceListController(
                value.repository(),
                value.workerExecutor(),
                ContextCompat.getMainExecutor(this));
        controller = Optional.of(next);
        next.subscribe(listener);
        next.open(AttendanceQuery.initial(
                value.organizationId(),
                value.assignedOnly()));
        realtime.start(
                value,
                () -> controller.ifPresent(
                        AttendanceListController::refresh));
    }

    @Override
    protected void onStop() {
        realtime.stop();
        controller.ifPresent(value -> {
            value.unsubscribe(listener);
            value.close();
        });
        controller = Optional.empty();
        runtime = Optional.empty();
        cancelSearch();
        super.onStop();
    }

    private void configureChannel() {
        List<Optional<AttendanceChannel>> values = List.of(
                Optional.empty(),
                Optional.of(AttendanceChannel.WHATSAPP),
                Optional.of(AttendanceChannel.SITE_CHAT));
        configure(
                binding.attendanceChannel,
                List.of(
                        getString(R.string.attendance_filter_all_channels),
                        getString(R.string.attendance_channel_whatsapp),
                        getString(R.string.attendance_channel_site)),
                position -> changeQuery(query ->
                        query.withChannel(values.get(position))));
    }

    private void configureFolder() {
        List<Optional<AttendanceFolder>> values = List.of(
                Optional.empty(),
                Optional.of(AttendanceFolder.INBOX),
                Optional.of(AttendanceFolder.MINE),
                Optional.of(AttendanceFolder.UNASSIGNED),
                Optional.of(AttendanceFolder.URGENT),
                Optional.of(AttendanceFolder.FOLLOW_UP),
                Optional.of(AttendanceFolder.SUPPLIERS),
                Optional.of(AttendanceFolder.VIP),
                Optional.of(AttendanceFolder.RESOLVED));
        configure(
                binding.attendanceFolder,
                List.of(
                        getString(R.string.attendance_filter_all_folders),
                        getString(R.string.attendance_folder_inbox),
                        getString(R.string.attendance_folder_mine),
                        getString(R.string.attendance_folder_unassigned),
                        getString(R.string.attendance_folder_urgent),
                        getString(R.string.attendance_folder_follow_up),
                        getString(R.string.attendance_folder_suppliers),
                        getString(R.string.attendance_folder_vip),
                        getString(R.string.attendance_folder_resolved)),
                position -> changeQuery(query ->
                        query.withFolder(values.get(position))));
    }

    private void configurePriority() {
        List<Optional<AttendancePriority>> values = List.of(
                Optional.empty(),
                Optional.of(AttendancePriority.LOW),
                Optional.of(AttendancePriority.NORMAL),
                Optional.of(AttendancePriority.HIGH),
                Optional.of(AttendancePriority.URGENT));
        configure(
                binding.attendancePriority,
                List.of(
                        getString(R.string.attendance_filter_all_priorities),
                        getString(R.string.attendance_priority_low),
                        getString(R.string.attendance_priority_normal),
                        getString(R.string.attendance_priority_high),
                        getString(R.string.attendance_priority_urgent)),
                position -> changeQuery(query ->
                        query.withPriority(values.get(position))));
    }

    private void configure(
            MaterialAutoCompleteTextView view,
            List<String> labels,
            PositionListener listener) {
        view.setAdapter(new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                labels));
        view.setText(labels.get(0), false);
        view.setOnItemClickListener(
                (parent, row, position, id) -> {
                    if (position >= 0 && position < labels.size()) {
                        listener.onSelected(position);
                    }
                });
    }

    private void changeAssignment(int id) {
        AttendanceAssignment assignment =
                id == R.id.attendance_assignment_mine
                        ? AttendanceAssignment.MINE
                        : id == R.id.attendance_assignment_unassigned
                                ? AttendanceAssignment.UNASSIGNED
                                : AttendanceAssignment.ALL;
        changeQuery(query -> query.withAssignment(assignment));
    }

    private TextWatcher searchWatcher() {
        return new TextWatcher() {
            @Override
            public void beforeTextChanged(
                    CharSequence value,
                    int start,
                    int count,
                    int after) {
            }

            @Override
            public void onTextChanged(
                    CharSequence value,
                    int start,
                    int before,
                    int count) {
                scheduleSearch(value == null ? "" : value.toString());
            }

            @Override
            public void afterTextChanged(Editable value) {
            }
        };
    }

    private void scheduleSearch(String value) {
        cancelSearch();
        Runnable action = () -> changeQuery(
                query -> query.withSearch(value));
        pendingSearch = Optional.of(action);
        handler.postDelayed(action, SEARCH_DELAY_MILLIS);
    }

    private void cancelSearch() {
        pendingSearch.ifPresent(handler::removeCallbacks);
        pendingSearch = Optional.empty();
    }

    private void changeQuery(QueryChange change) {
        controller.ifPresent(value ->
                value.currentQuery().ifPresent(query ->
                        value.open(change.apply(query))));
    }

    private void openCurrent() {
        if (controller.isEmpty() || runtime.isEmpty()) {
            return;
        }
        AttendanceListController controllerValue =
                controller.orElseThrow();
        if (controllerValue.currentQuery().isPresent()) {
            controllerValue.open(
                    controllerValue.currentQuery().orElseThrow());
            return;
        }
        AttendanceFeatureRuntime runtimeValue = runtime.orElseThrow();
        AttendanceQuery initial = AttendanceQuery.initial(
                runtimeValue.organizationId(),
                runtimeValue.assignedOnly());
        controllerValue.open(initial);
    }

    private void render(AttendanceListState state) {
        renderer.render(state);
    }

    private Optional<AttendanceFeatureRuntime> runtime() {
        return getApplication()
                        instanceof AttendanceRuntimeProvider provider
                ? provider.attendanceRuntime()
                : Optional.empty();
    }

    @FunctionalInterface
    private interface PositionListener {
        void onSelected(int position);
    }

    @FunctionalInterface
    private interface QueryChange {
        AttendanceQuery apply(AttendanceQuery query);
    }
}
