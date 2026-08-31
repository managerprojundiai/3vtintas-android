package br.com.tresvtintas.mobile.feature.order;

import android.content.Context;
import android.view.View;
import br.com.tresvtintas.mobile.core.order.OrderDetail;
import br.com.tresvtintas.mobile.core.order.OrderAction;
import br.com.tresvtintas.mobile.core.order.OrderDetailState;
import br.com.tresvtintas.mobile.core.order.OrderItem;
import br.com.tresvtintas.mobile.core.order.OrderPaymentStatus;
import br.com.tresvtintas.mobile.core.order.OrderPricingSnapshot;
import br.com.tresvtintas.mobile.core.order.OrderSummary;
import br.com.tresvtintas.mobile.feature.order.databinding.OrderActivityDetailBinding;
import com.google.android.material.textview.MaterialTextView;

final class OrderDetailRenderer {
    private final OrderActivityDetailBinding binding;

    OrderDetailRenderer(OrderActivityDetailBinding binding) {
        this.binding = binding;
    }

    void render(OrderDetailState state) {
        binding.orderDetailProgress.setVisibility(state.phase() == OrderDetailState.Phase.LOADING
                ? View.VISIBLE : View.INVISIBLE);
        binding.orderDetailContent.setVisibility(View.GONE);
        binding.orderDetailError.setVisibility(View.GONE);
        if (state.phase() == OrderDetailState.Phase.ERROR) {
            String message = binding.getRoot().getContext()
                    .getString(OrderText.failure(state.failure().orElseThrow()));
            if (state.requestId().isPresent()) {
                message += "\n" + binding.getRoot().getContext()
                        .getString(R.string.order_support_code, state.requestId().orElseThrow());
            }
            binding.orderDetailErrorMessage.setText(message);
            binding.orderDetailError.setVisibility(View.VISIBLE);
        } else if (state.phase() == OrderDetailState.Phase.READY) {
            show(state.order().orElseThrow());
            binding.orderDetailContent.setVisibility(View.VISIBLE);
        }
    }

    private void show(OrderDetail detail) {
        Context context = binding.getRoot().getContext();
        OrderSummary order = detail.summary();
        binding.orderDetailReference.setText(OrderText.reference(context, order.id()));
        binding.orderDetailStatus.setText(OrderText.status(order.status()));
        binding.orderDetailTotal.setText(order.total().map(total ->
                context.getResources().getQuantityString(
                        R.plurals.order_total_items,
                        order.itemCount(),
                        OrderText.money(total),
                        order.itemCount())).orElseGet(() ->
                context.getResources().getQuantityString(
                        R.plurals.order_restricted_total_items,
                        order.itemCount(),
                        order.itemCount())));
        String store = order.organization()
                .map(value -> value.name())
                .orElse(context.getString(R.string.order_not_informed));
        binding.orderDetailMeta.setText(context.getString(R.string.order_meta_value,
                context.getString(OrderText.type(order.type())), store,
                OrderText.date(order.createdAt())));
        binding.orderDetailPayment.setText(
                order.paymentStatus() == OrderPaymentStatus.RECEIVED
                        ? R.string.order_payment_received
                        : R.string.order_payment_pending);
        showPricing(context, detail.pricing());
        showActions(order);
        binding.orderDetailCustomer.setText(detail.customerContact().map(value -> context.getString(
                R.string.order_contact_value,
                value.name(),
                value.phone().orElse(context.getString(R.string.order_not_informed)),
                value.address().orElse(context.getString(R.string.order_not_informed))))
                .orElseGet(() -> order.customer()
                        .map(value -> value.name())
                        .orElse(context.getString(R.string.order_not_informed))));
        binding.orderDetailDelivery.setText(detail.deliveryDetails().map(value -> context.getString(
                R.string.order_delivery_value,
                value.address().orElse(context.getString(R.string.order_not_informed)),
                value.estimatedAt().map(OrderText::date).orElse(context.getString(R.string.order_not_informed)),
                value.trackingCode().orElse(context.getString(R.string.order_not_informed))))
                .orElseGet(() -> context.getString(R.string.order_not_informed)));
        binding.orderDetailNotes.setText(detail.notes().orElse(context.getString(R.string.order_not_informed)));
        binding.orderDetailItems.removeAllViews();
        int spacing = context.getResources().getDimensionPixelSize(R.dimen.order_space_small);
        for (OrderItem item : detail.items()) {
            binding.orderDetailItems.addView(itemView(context, item, spacing));
        }
    }

    void setActionsBusy(boolean busy) {
        binding.orderActionConfirm.setEnabled(!busy);
        binding.orderActionStart.setEnabled(!busy);
        binding.orderActionComplete.setEnabled(!busy);
        binding.orderActionPayment.setEnabled(!busy);
        binding.orderActionCancel.setEnabled(!busy);
        binding.orderDetailProgress.setVisibility(
                busy ? View.VISIBLE : View.INVISIBLE);
    }

    void showActionNotice(int message, String requestId) {
        Context context = binding.getRoot().getContext();
        String text = context.getString(message);
        if (requestId != null && !requestId.isBlank()) {
            text += "\n" + context.getString(
                    R.string.order_support_code,
                    requestId);
        }
        binding.orderActionNotice.setText(text);
        binding.orderActionNotice.setVisibility(View.VISIBLE);
    }

    private void showActions(OrderSummary order) {
        binding.orderActionConfirm.setVisibility(visibility(
                order,
                OrderAction.CONFIRM));
        binding.orderActionStart.setVisibility(visibility(
                order,
                OrderAction.START_FULFILLMENT));
        binding.orderActionComplete.setVisibility(visibility(
                order,
                OrderAction.COMPLETE));
        binding.orderActionPayment.setVisibility(visibility(
                order,
                OrderAction.RECORD_PAYMENT));
        binding.orderActionCancel.setVisibility(visibility(
                order,
                OrderAction.CANCEL));
        binding.orderActions.setVisibility(
                order.allowedActions().isEmpty() ? View.GONE : View.VISIBLE);
        setActionsBusy(false);
    }

    private static int visibility(
            OrderSummary order,
            OrderAction action) {
        return order.allowedActions().contains(action)
                ? View.VISIBLE
                : View.GONE;
    }

    private void showPricing(
            Context context,
            java.util.Optional<OrderPricingSnapshot> pricing) {
        if (pricing.isEmpty()) {
            binding.orderDetailPricing.setText(null);
            binding.orderDetailPricing.setVisibility(View.GONE);
            return;
        }
        OrderPricingSnapshot value = pricing.orElseThrow();
        binding.orderDetailPricing.setText(value.resolved()
                ? context.getString(
                        R.string.order_pricing_resolved,
                        value.priceListName().orElseThrow(),
                        value.priceListCode().orElseThrow(),
                        value.priceListVersionNumber().orElseThrow(),
                        value.policyRevision().orElseThrow())
                : context.getString(R.string.order_pricing_legacy));
        binding.orderDetailPricing.setVisibility(View.VISIBLE);
    }

    private static MaterialTextView itemView(Context context, OrderItem item, int spacing) {
        MaterialTextView view = new MaterialTextView(context);
        String quantity = item.quantity().stripTrailingZeros().toPlainString();
        view.setText(item.unitPrice().isPresent()
                ? context.getString(
                        R.string.order_item_value,
                        item.description(),
                        quantity,
                        OrderText.money(item.unitPrice().orElseThrow()),
                        OrderText.money(item.total().orElseThrow()))
                : context.getString(
                        R.string.order_item_information_value,
                        item.description(),
                        quantity,
                        item.unit().orElse(context.getString(R.string.order_not_informed))));
        view.setPadding(0, spacing, 0, spacing);
        view.setTextIsSelectable(true);
        return view;
    }
}
