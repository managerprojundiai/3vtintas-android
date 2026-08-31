package br.com.tresvtintas.mobile.feature.attendance;

import android.view.View;
import br.com.tresvtintas.mobile.core.attendance.AttendanceFailureKind;
import br.com.tresvtintas.mobile.core.attendance.AttendanceListState;
import br.com.tresvtintas.mobile.feature.attendance.databinding.AttendanceActivityListBinding;
import java.util.List;

final class AttendanceListRenderer {
    private final AttendanceActivityListBinding binding;
    private final AttendanceConversationAdapter adapter;

    AttendanceListRenderer(
            AttendanceActivityListBinding binding,
            AttendanceConversationAdapter adapter) {
        this.binding = binding;
        this.adapter = adapter;
    }

    void render(AttendanceListState state) {
        boolean busy =
                state.phase() == AttendanceListState.Phase.LOADING
                        || state.phase()
                                == AttendanceListState.Phase.REFRESHING
                        || state.phase()
                                == AttendanceListState.Phase.LOADING_MORE;
        binding.attendanceProgress.setVisibility(
                busy ? View.VISIBLE : View.INVISIBLE);
        binding.attendanceRefresh.setEnabled(!busy);
        binding.attendanceNotice.setVisibility(View.GONE);
        binding.attendanceRetry.setVisibility(View.GONE);
        if (state.phase() == AttendanceListState.Phase.ERROR) {
            AttendanceFailureKind failure =
                    state.failure().orElseThrow();
            adapter.submitList(List.of());
            binding.attendanceList.setVisibility(View.GONE);
            binding.attendanceLoadMore.setVisibility(View.GONE);
            binding.attendanceEmptyGroup.setVisibility(View.VISIBLE);
            binding.attendanceEmptyTitle.setText(
                    R.string.attendance_title);
            binding.attendanceEmptyMessage.setText(
                    AttendanceText.failure(failure));
            binding.attendanceRetry.setVisibility(
                    AttendanceText.retryable(failure)
                            ? View.VISIBLE
                            : View.GONE);
            support(state);
            return;
        }
        state.snapshot().ifPresentOrElse(
                value -> snapshot(state, value),
                () -> {
                    binding.attendanceList.setVisibility(View.GONE);
                    binding.attendanceLoadMore.setVisibility(View.GONE);
                    binding.attendanceEmptyGroup.setVisibility(View.GONE);
                });
    }

    private void snapshot(
            AttendanceListState state,
            AttendanceListState.Snapshot snapshot) {
        adapter.submitList(snapshot.items());
        boolean empty = snapshot.items().isEmpty();
        binding.attendanceList.setVisibility(
                empty ? View.GONE : View.VISIBLE);
        binding.attendanceEmptyGroup.setVisibility(
                empty ? View.VISIBLE : View.GONE);
        binding.attendanceLoadMore.setVisibility(
                !empty && snapshot.hasMore()
                        ? View.VISIBLE
                        : View.GONE);
        binding.attendanceLoadMore.setEnabled(
                state.phase()
                        != AttendanceListState.Phase.LOADING_MORE);
        if (state.failure().isPresent()) {
            binding.attendanceNotice.setText(
                    R.string.attendance_warning_stale);
            binding.attendanceNotice.setVisibility(View.VISIBLE);
            support(state);
        }
    }

    private void support(AttendanceListState state) {
        state.requestId().ifPresent(value -> {
            binding.attendanceNotice.setText(
                    binding.getRoot().getContext().getString(
                            R.string.attendance_request_id,
                            value));
            binding.attendanceNotice.setVisibility(View.VISIBLE);
        });
    }
}
