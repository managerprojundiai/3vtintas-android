package br.com.tresvtintas.mobile.feature.agent;

import android.content.Context;
import br.com.tresvtintas.mobile.core.agent.AgentDeliverySnapshot;
import br.com.tresvtintas.mobile.core.delivery.DeliveryStatus;
import br.com.tresvtintas.mobile.core.order.OrderStatus;
import java.util.Objects;

final class AgentDeliveryText {
    private AgentDeliveryText() {
        throw new AssertionError("No instances.");
    }

    static String snapshot(
            Context context,
            AgentDeliverySnapshot snapshot) {
        Objects.requireNonNull(context, "Context is required.");
        Objects.requireNonNull(snapshot, "Delivery snapshot is required.");
        return context.getString(
                R.string.agent_action_delivery_snapshot,
                context.getString(deliveryStatus(snapshot.deliveryStatus())),
                context.getString(orderStatus(snapshot.orderStatus())));
    }

    static String compact(
            Context context,
            br.com.tresvtintas.mobile.core.agent.AgentDeliverySummary
                    summary) {
        Objects.requireNonNull(summary, "Delivery summary is required.");
        return context.getString(
                R.string.agent_action_delivery_summary,
                summary.deliveryId(),
                summary.orderId(),
                context.getString(
                        deliveryStatus(
                                summary.before().deliveryStatus())),
                context.getString(
                        deliveryStatus(
                                summary.after().deliveryStatus())));
    }

    private static int deliveryStatus(DeliveryStatus status) {
        return switch (status) {
            case PENDING ->
                    R.string.agent_action_delivery_status_pending;
            case SHIPPED ->
                    R.string.agent_action_delivery_status_shipped;
            case IN_TRANSIT ->
                    R.string.agent_action_delivery_status_in_transit;
            case DELIVERED ->
                    R.string.agent_action_delivery_status_delivered;
            case FAILED ->
                    R.string.agent_action_delivery_status_failed;
        };
    }

    private static int orderStatus(OrderStatus status) {
        return switch (status) {
            case PENDING ->
                    R.string.agent_action_delivery_order_status_pending;
            case CONFIRMED ->
                    R.string.agent_action_delivery_order_status_confirmed;
            case IN_PROGRESS ->
                    R.string
                            .agent_action_delivery_order_status_in_progress;
            case DELIVERED ->
                    R.string.agent_action_delivery_order_status_delivered;
            case CANCELLED ->
                    R.string.agent_action_delivery_order_status_cancelled;
        };
    }
}
