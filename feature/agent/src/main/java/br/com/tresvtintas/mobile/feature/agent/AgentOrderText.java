package br.com.tresvtintas.mobile.feature.agent;

import android.content.Context;
import br.com.tresvtintas.mobile.core.agent.AgentOrderOperation;
import br.com.tresvtintas.mobile.core.agent.AgentOrderSnapshot;
import br.com.tresvtintas.mobile.core.agent.AgentOrderSummary;
import br.com.tresvtintas.mobile.core.order.OrderPaymentStatus;
import br.com.tresvtintas.mobile.core.order.OrderStatus;
import br.com.tresvtintas.mobile.core.order.OrderType;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Objects;

final class AgentOrderText {
    private AgentOrderText() {
        throw new AssertionError("No instances.");
    }

    static String compact(
            Context context,
            AgentOrderSummary summary) {
        Objects.requireNonNull(context, "Context is required.");
        Objects.requireNonNull(summary, "Agent order summary is required.");
        NumberFormat currency =
                NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
        return context.getString(
                R.string.agent_action_order_summary,
                summary.orderId(),
                context.getString(operation(summary.operation())),
                context.getString(status(summary.before().status())),
                context.getString(status(summary.after().status())),
                currency.format(summary.total()));
    }

    static String snapshot(
            Context context,
            AgentOrderSnapshot snapshot) {
        Objects.requireNonNull(context, "Context is required.");
        Objects.requireNonNull(snapshot, "Agent order snapshot is required.");
        return context.getString(
                R.string.agent_action_order_snapshot,
                context.getString(status(snapshot.status())),
                context.getString(payment(snapshot.paymentStatus())));
    }

    static int operation(AgentOrderOperation operation) {
        return switch (operation) {
            case CONFIRM ->
                    R.string.agent_action_order_operation_confirm;
            case START_FULFILLMENT ->
                    R.string
                            .agent_action_order_operation_start_fulfillment;
            case COMPLETE ->
                    R.string.agent_action_order_operation_complete;
        };
    }

    static int type(OrderType type) {
        OrderType required = Objects.requireNonNull(
                type,
                "Agent order type is required.");
        return required == OrderType.MATERIAL
                ? R.string.agent_action_order_type_material
                : R.string.agent_action_order_type_labor;
    }

    private static int status(OrderStatus status) {
        return switch (status) {
            case PENDING -> R.string.agent_action_order_status_pending;
            case CONFIRMED ->
                    R.string.agent_action_order_status_confirmed;
            case IN_PROGRESS ->
                    R.string.agent_action_order_status_in_progress;
            case DELIVERED ->
                    R.string.agent_action_order_status_delivered;
            case CANCELLED ->
                    R.string.agent_action_order_status_cancelled;
        };
    }

    private static int payment(OrderPaymentStatus status) {
        OrderPaymentStatus required = Objects.requireNonNull(
                status,
                "Agent order payment status is required.");
        return required == OrderPaymentStatus.PENDING
                ? R.string.agent_action_order_payment_pending
                : R.string.agent_action_order_payment_received;
    }
}
