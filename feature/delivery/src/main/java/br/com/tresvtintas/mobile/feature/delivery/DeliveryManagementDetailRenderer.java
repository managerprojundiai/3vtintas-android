package br.com.tresvtintas.mobile.feature.delivery;

import android.content.Context;
import android.view.View;
import br.com.tresvtintas.mobile.core.delivery.DeliveryManagementAction;
import br.com.tresvtintas.mobile.core.delivery.DeliveryManagementDetail;
import br.com.tresvtintas.mobile.core.delivery.DeliveryManagementDetailState;
import br.com.tresvtintas.mobile.core.delivery.DeliveryManagementSummary;
import br.com.tresvtintas.mobile.feature.delivery.databinding.DeliveryManagementActivityDetailBinding;
import com.google.android.material.textview.MaterialTextView;
import java.util.Optional;

final class DeliveryManagementDetailRenderer {
    private final DeliveryManagementActivityDetailBinding binding;
    private final boolean canSchedule;
    private final boolean canAssign;
    private final boolean canComplete;

    DeliveryManagementDetailRenderer(
            DeliveryManagementActivityDetailBinding binding,
            boolean canSchedule,
            boolean canAssign,
            boolean canComplete) {
        this.binding = binding;
        this.canSchedule = canSchedule;
        this.canAssign = canAssign;
        this.canComplete = canComplete;
    }

    void render(DeliveryManagementDetailState state) {
        binding.deliveryManagementDetailProgress.setVisibility(
                state.phase() == DeliveryManagementDetailState.Phase.LOADING
                        ? View.VISIBLE
                        : View.INVISIBLE);
        binding.deliveryManagementDetailContent.setVisibility(View.GONE);
        binding.deliveryManagementDetailError.setVisibility(View.GONE);
        if (state.phase() == DeliveryManagementDetailState.Phase.ERROR) {
            String message = binding.getRoot().getContext().getString(
                    DeliveryText.failure(state.failure().orElseThrow()));
            if (state.requestId().isPresent()) {
                message += "\n" + binding.getRoot().getContext().getString(
                        R.string.delivery_request_id,
                        state.requestId().orElseThrow());
            }
            binding.deliveryManagementDetailErrorMessage.setText(message);
            binding.deliveryManagementDetailError.setVisibility(View.VISIBLE);
        } else if (state.phase() == DeliveryManagementDetailState.Phase.READY) {
            show(state.detail().orElseThrow());
            binding.deliveryManagementDetailContent.setVisibility(View.VISIBLE);
        }
    }

    void setActionsBusy(boolean busy) {
        binding.deliveryManagementActionSchedule.setEnabled(!busy);
        binding.deliveryManagementActionAssign.setEnabled(!busy);
        binding.deliveryManagementActionUnassign.setEnabled(!busy);
        binding.deliveryManagementActionComplete.setEnabled(!busy);
        binding.deliveryManagementDetailProgress.setVisibility(
                busy ? View.VISIBLE : View.INVISIBLE);
    }

    void showActionNotice(int message, String requestId) {
        Context context = binding.getRoot().getContext();
        String text = context.getString(message);
        if (requestId != null && !requestId.isBlank()) {
            text += "\n" + context.getString(
                    R.string.delivery_request_id,
                    requestId);
        }
        binding.deliveryManagementActionNotice.setText(text);
        binding.deliveryManagementActionNotice.setVisibility(View.VISIBLE);
    }

    private void show(DeliveryManagementDetail detail) {
        Context context = binding.getRoot().getContext();
        DeliveryManagementSummary summary = detail.summary();
        binding.deliveryManagementDetailReference.setText(
                context.getResources().getQuantityString(
                R.plurals.delivery_management_order,
                summary.order().itemCount(),
                summary.order().id(),
                summary.order().itemCount()));
        binding.deliveryManagementDetailStatus.setText(
                summary.delivery()
                        .map(value -> DeliveryText.status(
                                context,
                                value.status()))
                        .orElseGet(() -> context.getString(
                                R.string.delivery_management_view_awaiting)));
        binding.deliveryManagementDetailSchedule.setText(context.getString(
                R.string.delivery_schedule_value,
                DeliveryText.schedule(
                        context,
                        summary.delivery().flatMap(
                                DeliveryManagementSummary.ManagedDelivery
                                    ::scheduledAt))));
        binding.deliveryManagementDetailDriver.setText(context.getString(
                R.string.delivery_driver_value,
                summary.delivery()
                        .flatMap(
                                DeliveryManagementSummary.ManagedDelivery
                                    ::assignedDriver)
                        .map(driver -> driver.displayName())
                        .orElseGet(() -> context.getString(
                                R.string.delivery_unassigned))));
        binding.deliveryManagementDetailRecipient.setText(
                recipient(context, detail.recipient()));
        binding.deliveryManagementDetailDestination.setText(
                destination(context, detail.destination()));
        binding.deliveryManagementDetailInstructions.setText(
                detail.instructions().orElseGet(() -> context.getString(
                        R.string.delivery_not_informed)));
        showActions(summary);
        showItems(context, detail);
    }

    private void showActions(DeliveryManagementSummary summary) {
        binding.deliveryManagementActionSchedule.setVisibility(
                visible(canSchedule
                        && summary.allowedActions().contains(
                                DeliveryManagementAction.SCHEDULE)));
        binding.deliveryManagementActionAssign.setVisibility(
                visible(canAssign
                        && summary.allowedActions().contains(
                                DeliveryManagementAction.ASSIGN)));
        binding.deliveryManagementActionUnassign.setVisibility(
                visible(canAssign
                        && summary.allowedActions().contains(
                                DeliveryManagementAction.UNASSIGN)));
        binding.deliveryManagementActionComplete.setVisibility(
                visible(canComplete
                        && summary.allowedActions().contains(
                                DeliveryManagementAction.COMPLETE)));
        setActionsBusy(false);
    }

    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
    private void showItems(
            Context context,
            DeliveryManagementDetail detail) {
        binding.deliveryManagementDetailItems.removeAllViews();
        int spacing = context.getResources().getDimensionPixelSize(
                R.dimen.delivery_space_small);
        for (DeliveryManagementDetail.Item item : detail.items()) {
            MaterialTextView view = new MaterialTextView(context);
            view.setText(context.getString(
                    R.string.delivery_item_value,
                    item.description(),
                    item.quantity().stripTrailingZeros().toPlainString(),
                    item.unit().orElseGet(() -> context.getString(
                            R.string.delivery_unit_default))));
            view.setPadding(0, spacing, 0, spacing);
            view.setTextIsSelectable(true);
            binding.deliveryManagementDetailItems.addView(view);
        }
    }

    private static String recipient(
            Context context,
            Optional<DeliveryManagementDetail.Recipient> recipient) {
        return recipient.map(value -> context.getString(
                R.string.delivery_recipient_value,
                value.name(),
                value.phone().orElseGet(() -> context.getString(
                        R.string.delivery_not_informed))))
                .orElseGet(() -> context.getString(
                        R.string.delivery_not_informed));
    }

    private static String destination(
            Context context,
            DeliveryManagementDetail.Destination destination) {
        return context.getString(
                R.string.delivery_destination_value,
                destination.address().orElseGet(() -> context.getString(
                        R.string.delivery_not_informed)),
                DeliveryText.place(
                        context,
                        destination.city(),
                        destination.state()));
    }

    private static int visible(boolean value) {
        return value ? View.VISIBLE : View.GONE;
    }
}
