package br.com.tresvtintas.mobile.feature.attendance;

import android.view.View;
import br.com.tresvtintas.mobile.core.attendance.AttendanceConversation;
import br.com.tresvtintas.mobile.core.attendance.AttendanceDetailState;
import br.com.tresvtintas.mobile.core.attendance.AttendanceFailureKind;
import br.com.tresvtintas.mobile.feature.attendance.databinding.AttendanceActivityDetailBinding;

final class AttendanceDetailRenderer {
    private final AttendanceActivityDetailBinding binding;
    private final AttendanceMessageAdapter adapter;

    AttendanceDetailRenderer(
            AttendanceActivityDetailBinding binding,
            AttendanceMessageAdapter adapter) {
        this.binding = binding;
        this.adapter = adapter;
    }

    void render(AttendanceDetailState state) {
        boolean busy =
                state.phase() == AttendanceDetailState.Phase.LOADING
                        || state.phase()
                                == AttendanceDetailState.Phase.REFRESHING
                        || state.phase()
                                == AttendanceDetailState.Phase.LOADING_MORE;
        binding.attendanceDetailProgress.setVisibility(
                busy ? View.VISIBLE : View.INVISIBLE);
        binding.attendanceDetailRefresh.setEnabled(
                !busy && state.snapshot().isPresent());
        binding.attendanceDetailNotice.setVisibility(View.GONE);
        binding.attendanceDetailRetry.setVisibility(View.GONE);

        if (state.snapshot().isEmpty()) {
            adapter.submitList(java.util.List.of());
            binding.attendanceDetailContent.setVisibility(View.GONE);
            binding.attendanceDetailMessages.setVisibility(View.GONE);
            binding.attendanceDetailLoadMore.setVisibility(View.GONE);
            binding.attendanceDetailEmptyGroup.setVisibility(View.VISIBLE);
            binding.attendanceDetailEmptyTitle.setText(
                    state.phase() == AttendanceDetailState.Phase.LOADING
                            ? R.string.attendance_detail_loading
                            : R.string.attendance_detail_error_title);
            binding.attendanceDetailEmptyMessage.setText(
                    state.failure()
                            .map(AttendanceText::failure)
                            .orElse(R.string.attendance_detail_loading_message));
            state.failure().ifPresent(failure ->
                    binding.attendanceDetailRetry.setVisibility(
                            AttendanceText.retryable(failure)
                                    ? View.VISIBLE
                                    : View.GONE));
            showRequestId(state);
            return;
        }

        AttendanceDetailState.Snapshot snapshot =
                state.snapshot().orElseThrow();
        renderHeader(snapshot.conversation());
        adapter.submitList(snapshot.messages());
        binding.attendanceDetailContent.setVisibility(View.VISIBLE);
        boolean empty = snapshot.messages().isEmpty();
        binding.attendanceDetailMessages.setVisibility(
                empty ? View.GONE : View.VISIBLE);
        binding.attendanceDetailEmptyGroup.setVisibility(
                empty ? View.VISIBLE : View.GONE);
        if (empty) {
            binding.attendanceDetailEmptyTitle.setText(
                    R.string.attendance_detail_empty_title);
            binding.attendanceDetailEmptyMessage.setText(
                    R.string.attendance_detail_empty_message);
        }
        binding.attendanceDetailLoadMore.setVisibility(
                snapshot.hasMore() ? View.VISIBLE : View.GONE);
        binding.attendanceDetailLoadMore.setEnabled(!busy);
        state.failure().ifPresent(failure -> {
            binding.attendanceDetailNotice.setText(
                    failure == AttendanceFailureKind.NETWORK
                            ? R.string.attendance_warning_stale
                            : AttendanceText.failure(failure));
            binding.attendanceDetailNotice.setVisibility(View.VISIBLE);
        });
        showRequestId(state);
    }

    private void renderHeader(AttendanceConversation value) {
        var context = binding.getRoot().getContext();
        binding.attendanceDetailCustomer.setText(
                value.customer()
                        .displayName()
                        .filter(name -> !name.isBlank())
                        .orElseGet(() -> context.getString(
                                R.string.attendance_customer_unknown)));
        String organization = value.organization()
                .map(AttendanceConversation.Organization::name)
                .orElseGet(() -> context.getString(
                        R.string.attendance_organization_global));
        binding.attendanceDetailChannel.setText(context.getString(
                R.string.attendance_item_channel,
                AttendanceText.channel(context, value.channel()),
                organization));
        String owner = value.assignedUser()
                .flatMap(AttendanceConversation.AssignedUser::name)
                .filter(name -> !name.isBlank())
                .orElseGet(() -> context.getString(
                        R.string.attendance_unassigned));
        binding.attendanceDetailOwner.setText(context.getString(
                R.string.attendance_item_owner,
                owner,
                AttendanceText.mode(context, value.handlingMode())));
        String received = context.getResources().getQuantityString(
                R.plurals.attendance_detail_received,
                value.stats().inboundCount(),
                value.stats().inboundCount());
        String sent = context.getResources().getQuantityString(
                R.plurals.attendance_detail_sent,
                value.stats().outboundCount(),
                value.stats().outboundCount());
        binding.attendanceDetailMeta.setText(context.getString(
                R.string.attendance_detail_meta,
                AttendanceText.folder(context, value.folder()),
                AttendanceText.priority(context, value.priority()),
                received,
                sent));
    }

    private void showRequestId(AttendanceDetailState state) {
        state.requestId().ifPresent(requestId -> {
            binding.attendanceDetailNotice.setText(
                    binding.getRoot().getContext().getString(
                            R.string.attendance_request_id,
                            requestId));
            binding.attendanceDetailNotice.setVisibility(View.VISIBLE);
        });
    }
}
