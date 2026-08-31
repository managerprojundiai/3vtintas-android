package br.com.tresvtintas.mobile.feature.agent;

import android.view.View;
import br.com.tresvtintas.mobile.core.agent.AgentOrderOperation;
import br.com.tresvtintas.mobile.core.agent.AgentOrderSummary;
import br.com.tresvtintas.mobile.feature.agent.databinding.AgentDialogActionReviewBinding;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

final class AgentOrderReviewRenderer {
    private final AgentDialogActionReviewBinding binding;
    private final NumberFormat currency =
            NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));

    AgentOrderReviewRenderer(
            AgentDialogActionReviewBinding binding) {
        this.binding = Objects.requireNonNull(
                binding,
                "Agent order review binding is required.");
    }

    void render(AgentOrderSummary summary) {
        binding.agentActionReviewQuote.setVisibility(View.VISIBLE);
        binding.agentActionReviewQuote.setText(
                binding.getRoot().getContext().getString(
                        R.string.agent_action_order_identifier,
                        summary.orderId()));
        binding.agentActionReviewCustomer.setVisibility(View.GONE);
        binding.agentActionReviewTotal.setVisibility(View.GONE);
        binding.agentActionReviewOrder.setVisibility(View.VISIBLE);
        binding.agentActionReviewOrderOperation.setText(
                AgentOrderText.operation(summary.operation()));
        binding.agentActionReviewOrderType.setText(
                binding.getRoot().getContext().getString(
                        R.string.agent_action_order_type,
                        binding.getRoot().getContext().getString(
                                AgentOrderText.type(summary.orderType()))));
        renderOptional(
                binding.agentActionReviewOrderOrganization,
                summary.organizationName(),
                R.string.agent_action_order_organization);
        renderOptional(
                binding.agentActionReviewOrderCustomer,
                summary.customerName(),
                R.string.agent_action_order_customer);
        binding.agentActionReviewOrderTotal.setText(
                binding.getRoot().getContext().getString(
                        R.string.agent_action_order_total,
                        currency.format(summary.total())));
        binding.agentActionReviewOrderItems.setText(
                binding.getRoot()
                        .getResources()
                        .getQuantityString(
                                R.plurals.agent_action_order_item_count,
                                summary.itemCount(),
                                summary.itemCount()));
        binding.agentActionReviewOrderBefore.setText(
                AgentOrderText.snapshot(
                        binding.getRoot().getContext(),
                        summary.before()));
        binding.agentActionReviewOrderAfter.setText(
                AgentOrderText.snapshot(
                        binding.getRoot().getContext(),
                        summary.after()));
        binding.agentActionReviewNotice.setText(
                revalidationNotice(summary.operation()));
    }

    void hide() {
        binding.agentActionReviewOrder.setVisibility(View.GONE);
    }

    private void renderOptional(
            android.widget.TextView view,
            Optional<String> value,
            int resource) {
        value.ifPresentOrElse(
                text -> {
                    view.setVisibility(View.VISIBLE);
                    view.setText(binding.getRoot().getContext().getString(
                            resource,
                            text));
                },
                () -> view.setVisibility(View.GONE));
    }

    private static int revalidationNotice(
            AgentOrderOperation operation) {
        return switch (operation) {
            case CONFIRM ->
                    R.string.agent_action_order_confirm_revalidation_notice;
            case START_FULFILLMENT ->
                    R.string
                            .agent_action_order_start_fulfillment_revalidation_notice;
            case COMPLETE ->
                    R.string.agent_action_order_complete_revalidation_notice;
        };
    }
}
