package br.com.tresvtintas.mobile.feature.delivery;

import android.content.Context;
import android.view.View;
import br.com.tresvtintas.mobile.core.delivery.DeliveryAction;
import br.com.tresvtintas.mobile.core.delivery.DeliveryDetail;
import br.com.tresvtintas.mobile.core.delivery.DeliveryDetailState;
import br.com.tresvtintas.mobile.core.delivery.DeliverySummary;
import br.com.tresvtintas.mobile.feature.delivery.databinding.DeliveryActivityDetailBinding;
import com.google.android.material.textview.MaterialTextView;
import java.util.Optional;

final class DeliveryDetailRenderer {
    private final DeliveryActivityDetailBinding binding;

    DeliveryDetailRenderer(
            DeliveryActivityDetailBinding binding) {
        this.binding = binding;
    }

    void render(DeliveryDetailState state) {
        binding.deliveryDetailProgress.setVisibility(
                state.phase() == DeliveryDetailState.Phase.LOADING
                        ? View.VISIBLE
                        : View.INVISIBLE);
        binding.deliveryDetailContent.setVisibility(View.GONE);
        binding.deliveryDetailError.setVisibility(View.GONE);
        if (state.phase() == DeliveryDetailState.Phase.ERROR) {
            String message = binding.getRoot().getContext()
                    .getString(DeliveryText.failure(
                            state.failure().orElseThrow()));
            if (state.requestId().isPresent()) {
                message += "\n" + binding.getRoot().getContext()
                        .getString(
                                R.string.delivery_request_id,
                                state.requestId().orElseThrow());
            }
            binding.deliveryDetailErrorMessage.setText(message);
            binding.deliveryDetailError.setVisibility(View.VISIBLE);
        } else if (state.phase() == DeliveryDetailState.Phase.READY) {
            show(state.delivery().orElseThrow());
            binding.deliveryDetailContent.setVisibility(View.VISIBLE);
        }
    }

    void setActionsBusy(boolean busy) {
        binding.deliveryActionStart.setEnabled(!busy);
        binding.deliveryActionComplete.setEnabled(!busy);
        binding.deliveryDetailProgress.setVisibility(
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
        binding.deliveryActionNotice.setText(text);
        binding.deliveryActionNotice.setVisibility(View.VISIBLE);
    }

    private void show(DeliveryDetail delivery) {
        Context context = binding.getRoot().getContext();
        DeliverySummary summary = delivery.summary();
        binding.deliveryDetailReference.setText(context.getString(
                R.string.delivery_order_reference,
                summary.order().id(),
                summary.id()));
        binding.deliveryDetailStatus.setText(
                DeliveryText.status(context, summary.status()));
        binding.deliveryDetailSchedule.setText(context.getString(
                R.string.delivery_schedule_value,
                DeliveryText.schedule(
                        context,
                        summary.scheduledAt())));
        binding.deliveryDetailDriver.setText(context.getString(
                R.string.delivery_driver_value,
                summary.assignedDriver()
                        .flatMap(DeliverySummary.Driver::name)
                        .orElseGet(() -> context.getString(
                                R.string.delivery_unassigned))));
        binding.deliveryDetailTracking.setText(context.getString(
                R.string.delivery_tracking_value,
                delivery.trackingCode().orElseGet(() ->
                        context.getString(
                                R.string.delivery_not_informed))));
        binding.deliveryDetailRecipient.setText(recipient(
                context,
                delivery.recipient()));
        binding.deliveryDetailDestination.setText(destination(
                context,
                delivery.destination()));
        binding.deliveryDetailInstructions.setText(
                delivery.instructions().orElseGet(() ->
                        context.getString(
                                R.string.delivery_not_informed)));
        showActions(summary);
        showItems(context, delivery);
    }

    private void showActions(DeliverySummary summary) {
        binding.deliveryActionStart.setVisibility(visibility(
                summary,
                DeliveryAction.START));
        binding.deliveryActionComplete.setVisibility(visibility(
                summary,
                DeliveryAction.COMPLETE));
        setActionsBusy(false);
    }

    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
    private void showItems(
            Context context,
            DeliveryDetail delivery) {
        binding.deliveryDetailItems.removeAllViews();
        int spacing = context.getResources().getDimensionPixelSize(
                R.dimen.delivery_space_small);
        for (DeliveryDetail.Item item : delivery.items()) {
            MaterialTextView view = new MaterialTextView(context);
            view.setText(context.getString(
                    R.string.delivery_item_value,
                    item.description(),
                    item.quantity()
                            .stripTrailingZeros()
                            .toPlainString(),
                    item.unit().orElseGet(() -> context.getString(
                            R.string.delivery_unit_default))));
            view.setPadding(0, spacing, 0, spacing);
            view.setTextIsSelectable(true);
            binding.deliveryDetailItems.addView(view);
        }
    }

    private static String recipient(
            Context context,
            Optional<DeliveryDetail.Recipient> recipient) {
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
            DeliveryDetail.Destination destination) {
        return context.getString(
                R.string.delivery_destination_value,
                destination.address().orElseGet(() -> context.getString(
                        R.string.delivery_not_informed)),
                DeliveryText.place(
                        context,
                        destination.city(),
                        destination.state()));
    }

    private static int visibility(
            DeliverySummary summary,
            DeliveryAction action) {
        return summary.allowedActions().contains(action)
                ? View.VISIBLE
                : View.GONE;
    }
}
