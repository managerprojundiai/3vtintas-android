package br.com.tresvtintas.mobile.feature.attendance;

import android.view.View;
import br.com.tresvtintas.mobile.core.attendance.AttendanceReplyDeliveryState;
import br.com.tresvtintas.mobile.core.attendance.AttendanceReplyState;
import br.com.tresvtintas.mobile.feature.attendance.databinding.AttendanceActivityDetailBinding;

final class AttendanceReplyRenderer {
    private final AttendanceActivityDetailBinding binding;
    private boolean allowed;

    AttendanceReplyRenderer(AttendanceActivityDetailBinding binding) {
        this.binding = binding;
    }

    void configure(boolean canReply) {
        allowed = canReply;
        binding.attendanceReplyGroup.setVisibility(
                canReply ? View.VISIBLE : View.GONE);
        if (!canReply) {
            binding.attendanceReplyInput.setText("");
            binding.attendanceReplyInputLayout.setError(null);
        }
    }

    void render(AttendanceReplyState state) {
        if (!allowed) {
            return;
        }
        boolean sending =
                state.phase() == AttendanceReplyState.Phase.SENDING;
        binding.attendanceReplyInput.setEnabled(!sending);
        binding.attendanceReplySend.setEnabled(!sending);
        binding.attendanceReplyProgress.setVisibility(
                sending ? View.VISIBLE : View.INVISIBLE);
        binding.attendanceReplyStatus.setVisibility(
                state.phase() == AttendanceReplyState.Phase.IDLE
                        ? View.GONE
                        : View.VISIBLE);

        switch (state.phase()) {
            case IDLE -> binding.attendanceReplyStatus.setText("");
            case SENDING -> binding.attendanceReplyStatus.setText(
                    R.string.attendance_reply_sending);
            case SUCCESS -> binding.attendanceReplyStatus.setText(
                    state.result().orElseThrow().deliveryState()
                                    == AttendanceReplyDeliveryState.QUEUED
                            ? R.string.attendance_reply_queued
                            : R.string.attendance_reply_available);
            case ERROR -> renderFailure(state);
            case CLOSED -> {
                binding.attendanceReplyInput.setEnabled(false);
                binding.attendanceReplySend.setEnabled(false);
                binding.attendanceReplyStatus.setVisibility(View.GONE);
            }
            default -> throw new IllegalStateException(
                    "Attendance reply phase is unsupported.");
        }
    }

    private void renderFailure(AttendanceReplyState state) {
        var context = binding.getRoot().getContext();
        String failure = context.getString(
                AttendanceText.failure(
                        state.failure().orElseThrow()));
        binding.attendanceReplyStatus.setText(
                state.requestId()
                        .map(requestId -> context.getString(
                                R.string.attendance_reply_failure_request,
                                failure,
                                requestId))
                        .orElse(failure));
    }
}
