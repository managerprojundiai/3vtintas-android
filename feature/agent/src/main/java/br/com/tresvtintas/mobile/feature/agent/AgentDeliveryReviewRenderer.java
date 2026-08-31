package br.com.tresvtintas.mobile.feature.agent;

import android.view.View;
import br.com.tresvtintas.mobile.core.agent.AgentDeliveryOperation;
import br.com.tresvtintas.mobile.core.agent.AgentDeliverySummary;
import br.com.tresvtintas.mobile.feature.agent.databinding.AgentDialogActionReviewBinding;
import java.util.Objects;
import java.util.Optional;

final class AgentDeliveryReviewRenderer {
    private final AgentDialogActionReviewBinding binding;

    AgentDeliveryReviewRenderer(
            AgentDialogActionReviewBinding binding) {
        this.binding = Objects.requireNonNull(
                binding,
                "Agent delivery review binding is required.");
    }

    void render(AgentDeliverySummary summary) {
        binding.agentActionReviewQuote.setVisibility(View.VISIBLE);
        binding.agentActionReviewQuote.setText(
                binding.getRoot().getContext().getString(
                        R.string.agent_action_delivery_order,
                        summary.orderId(),
                        summary.deliveryId()));
        binding.agentActionReviewCustomer.setVisibility(View.GONE);
        binding.agentActionReviewTotal.setVisibility(View.GONE);
        binding.agentActionReviewDelivery.setVisibility(View.VISIBLE);
        binding.agentActionReviewDeliveryOperation.setText(
                summary.operation() == AgentDeliveryOperation.START
                        ? R.string.agent_action_delivery_operation_start
                        : R.string
                                .agent_action_delivery_operation_complete);
        binding.agentActionReviewDeliveryOrder.setText(
                binding.getRoot().getContext().getString(
                        R.string.agent_action_delivery_order,
                        summary.orderId(),
                        summary.deliveryId()));
        renderOptional(
                binding.agentActionReviewDeliveryOrganization,
                summary.organizationName(),
                R.string.agent_action_delivery_organization);
        renderOptional(
                binding.agentActionReviewDeliveryCustomer,
                summary.customerName(),
                R.string.agent_action_delivery_customer);
        binding.agentActionReviewDeliveryDriver.setText(
                binding.getRoot().getContext().getString(
                        R.string.agent_action_delivery_driver,
                        summary.assignedDriverName()));
        binding.agentActionReviewDeliverySchedule.setText(
                summary.scheduledAt()
                        .map(value -> binding.getRoot()
                                .getContext()
                                .getString(
                                        R.string
                                                .agent_action_delivery_schedule,
                                        AgentText.activity(value)))
                        .orElseGet(() -> binding.getRoot()
                                .getContext()
                                .getString(
                                        R.string
                                                .agent_action_delivery_no_schedule)));
        binding.agentActionReviewDeliveryItems.setText(
                binding.getRoot()
                        .getResources()
                        .getQuantityString(
                                R.plurals.agent_action_delivery_item_count,
                                summary.itemCount(),
                                summary.itemCount()));
        binding.agentActionReviewDeliveryBefore.setText(
                AgentDeliveryText.snapshot(
                        binding.getRoot().getContext(),
                        summary.before()));
        binding.agentActionReviewDeliveryAfter.setText(
                AgentDeliveryText.snapshot(
                        binding.getRoot().getContext(),
                        summary.after()));
        binding.agentActionReviewNotice.setText(
                summary.operation() == AgentDeliveryOperation.START
                        ? R.string
                                .agent_action_delivery_start_revalidation_notice
                        : R.string
                                .agent_action_delivery_complete_revalidation_notice);
    }

    void hide() {
        binding.agentActionReviewDelivery.setVisibility(View.GONE);
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
}
