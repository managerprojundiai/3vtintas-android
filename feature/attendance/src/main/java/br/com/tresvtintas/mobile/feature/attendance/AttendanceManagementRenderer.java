package br.com.tresvtintas.mobile.feature.attendance;

import android.view.View;
import br.com.tresvtintas.mobile.core.attendance.AttendanceFailureKind;
import br.com.tresvtintas.mobile.core.attendance.AttendanceManagementState;
import br.com.tresvtintas.mobile.feature.attendance.databinding.AttendanceActivityDetailBinding;

final class AttendanceManagementRenderer {
    private final AttendanceActivityDetailBinding binding;
    private boolean allowed;

    AttendanceManagementRenderer(
            AttendanceActivityDetailBinding binding) {
        this.binding = binding;
    }

    void configure(boolean canManage) {
        allowed = canManage;
        binding.attendanceManagementGroup.setVisibility(
                canManage ? View.VISIBLE : View.GONE);
        if (!canManage) {
            binding.attendanceManagementStatus.setText("");
            binding.attendanceManagementStatus.setVisibility(View.GONE);
        }
    }

    void render(AttendanceManagementState state) {
        if (!allowed) {
            return;
        }
        boolean busy =
                state.phase() == AttendanceManagementState.Phase.LOADING
                        || state.phase()
                                == AttendanceManagementState.Phase.SAVING;
        boolean directoryReady = state.snapshot().isPresent();
        binding.attendanceManagementProgress.setVisibility(
                busy ? View.VISIBLE : View.INVISIBLE);
        binding.attendanceManagementOpen.setEnabled(
                directoryReady && !busy);
        binding.attendanceManagementRetry.setVisibility(
                state.phase() == AttendanceManagementState.Phase.ERROR
                                && !directoryReady
                                && state.failure()
                                        .map(AttendanceText::retryable)
                                        .orElse(false)
                        ? View.VISIBLE
                        : View.GONE);
        binding.attendanceManagementStatus.setVisibility(
                state.phase() == AttendanceManagementState.Phase.IDLE
                                || state.phase()
                                        == AttendanceManagementState.Phase.READY
                        ? View.GONE
                        : View.VISIBLE);

        switch (state.phase()) {
            case IDLE, READY ->
                    binding.attendanceManagementStatus.setText("");
            case LOADING ->
                    binding.attendanceManagementStatus.setText(
                            R.string.attendance_management_loading);
            case SAVING ->
                    binding.attendanceManagementStatus.setText(
                            R.string.attendance_management_saving);
            case SUCCESS ->
                    binding.attendanceManagementStatus.setText(
                            state.snapshot().orElseThrow().replayed()
                                    ? R.string
                                            .attendance_management_replayed
                                    : R.string
                                            .attendance_management_saved);
            case ERROR -> renderFailure(state);
            case CLOSED -> configure(false);
            default -> throw new IllegalStateException(
                    "Attendance management phase is unsupported.");
        }
    }

    void showNoChanges() {
        if (!allowed) {
            return;
        }
        binding.attendanceManagementStatus.setText(
                R.string.attendance_management_no_changes);
        binding.attendanceManagementStatus.setVisibility(View.VISIBLE);
    }

    private void renderFailure(AttendanceManagementState state) {
        var context = binding.getRoot().getContext();
        AttendanceFailureKind kind = state.failure().orElseThrow();
        String failure = context.getString(
                kind == AttendanceFailureKind.CONFLICT
                        ? R.string.attendance_management_conflict
                        : AttendanceText.failure(kind));
        binding.attendanceManagementStatus.setText(
                state.requestId()
                        .map(requestId -> context.getString(
                                R.string.attendance_reply_failure_request,
                                failure,
                                requestId))
                        .orElse(failure));
    }
}
