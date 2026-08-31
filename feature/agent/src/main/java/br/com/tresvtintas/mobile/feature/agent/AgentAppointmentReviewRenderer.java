package br.com.tresvtintas.mobile.feature.agent;

import android.view.View;
import br.com.tresvtintas.mobile.core.agent.AgentAppointmentOperation;
import br.com.tresvtintas.mobile.core.agent.AgentAppointmentSummary;
import br.com.tresvtintas.mobile.feature.agent.databinding.AgentDialogActionReviewBinding;
import java.util.Objects;

final class AgentAppointmentReviewRenderer {
    private final AgentDialogActionReviewBinding binding;

    AgentAppointmentReviewRenderer(
            AgentDialogActionReviewBinding binding) {
        this.binding = Objects.requireNonNull(
                binding,
                "Agent appointment review binding is required.");
    }

    void render(AgentAppointmentSummary summary) {
        binding.agentActionReviewQuote.setVisibility(View.VISIBLE);
        binding.agentActionReviewQuote.setText(summary.title());
        binding.agentActionReviewCustomer.setVisibility(View.GONE);
        binding.agentActionReviewTotal.setVisibility(View.GONE);
        binding.agentActionReviewAppointment.setVisibility(View.VISIBLE);
        binding.agentActionReviewAppointmentOperation.setText(
                AgentAppointmentText.operation(
                        binding.getRoot().getContext(),
                        summary.operation()));
        binding.agentActionReviewAppointmentKind.setText(
                binding.getRoot().getContext().getString(
                        R.string.agent_action_appointment_kind,
                        AgentAppointmentText.kind(
                                binding.getRoot().getContext(),
                                summary.kind())));
        binding.agentActionReviewAppointmentResponsible.setText(
                binding.getRoot().getContext().getString(
                        R.string.agent_action_appointment_responsible,
                        summary.responsibleName()));
        renderOptional(
                binding.agentActionReviewAppointmentOrganization,
                summary.organizationName(),
                R.string.agent_action_appointment_organization);
        renderOptional(
                binding.agentActionReviewAppointmentCustomer,
                summary.customerName(),
                R.string.agent_action_appointment_customer);
        summary.before().ifPresentOrElse(
                before -> {
                    binding.agentActionReviewAppointmentBeforeLabel
                            .setVisibility(View.VISIBLE);
                    binding.agentActionReviewAppointmentBefore
                            .setVisibility(View.VISIBLE);
                    binding.agentActionReviewAppointmentBefore.setText(
                            AgentAppointmentText.snapshot(
                                    binding.getRoot().getContext(),
                                    before));
                },
                () -> {
                    binding.agentActionReviewAppointmentBeforeLabel
                            .setVisibility(View.GONE);
                    binding.agentActionReviewAppointmentBefore
                            .setVisibility(View.GONE);
                });
        binding.agentActionReviewAppointmentAfter.setText(
                AgentAppointmentText.snapshot(
                        binding.getRoot().getContext(),
                        summary.after()));
        binding.agentActionReviewNotice.setText(notice(summary.operation()));
    }

    void hide() {
        binding.agentActionReviewAppointment.setVisibility(View.GONE);
    }

    private void renderOptional(
            android.widget.TextView view,
            java.util.Optional<String> value,
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

    private static int notice(AgentAppointmentOperation operation) {
        return switch (operation) {
            case CREATE ->
                    R.string
                            .agent_action_appointment_create_revalidation_notice;
            case RESCHEDULE ->
                    R.string
                            .agent_action_appointment_reschedule_revalidation_notice;
            case CANCEL ->
                    R.string
                            .agent_action_appointment_cancel_revalidation_notice;
        };
    }
}
