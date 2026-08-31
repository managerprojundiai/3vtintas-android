package br.com.tresvtintas.mobile.feature.agent;

import android.view.LayoutInflater;
import android.view.View;
import br.com.tresvtintas.mobile.core.agent.AgentAction;
import br.com.tresvtintas.mobile.core.agent.AgentAppointmentSummary;
import br.com.tresvtintas.mobile.core.agent.AgentAttendanceChannel;
import br.com.tresvtintas.mobile.core.agent.AgentAttendanceReplySummary;
import br.com.tresvtintas.mobile.core.agent.AgentDeliverySummary;
import br.com.tresvtintas.mobile.core.agent.AgentCommissionSummary;
import br.com.tresvtintas.mobile.core.agent.AgentFinanceSummary;
import br.com.tresvtintas.mobile.core.agent.AgentMaterialQuoteAmendSummary;
import br.com.tresvtintas.mobile.core.agent.AgentMaterialQuoteCreateItem;
import br.com.tresvtintas.mobile.core.agent.AgentMaterialQuoteCreateSummary;
import br.com.tresvtintas.mobile.core.agent.AgentMaterialQuoteSendSummary;
import br.com.tresvtintas.mobile.core.agent.AgentOrderSummary;
import br.com.tresvtintas.mobile.feature.agent.databinding.AgentDialogActionReviewBinding;
import br.com.tresvtintas.mobile.feature.agent.databinding.AgentItemActionReviewBinding;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

final class AgentActionReviewRenderer {
    private static final DateTimeFormatter DATE =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final AgentDialogActionReviewBinding binding;
    private final AgentQuoteAmendReviewRenderer amendment;
    private final AgentAppointmentReviewRenderer appointment;
    private final AgentDeliveryReviewRenderer delivery;
    private final AgentOrderReviewRenderer order;
    private final AgentFinancialActionReviewRenderer financial;
    private final NumberFormat currency =
            NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));

    AgentActionReviewRenderer(
            AgentDialogActionReviewBinding binding) {
        this.binding = java.util.Objects.requireNonNull(
                binding,
                "Agent action review binding is required.");
        amendment = new AgentQuoteAmendReviewRenderer(binding);
        appointment = new AgentAppointmentReviewRenderer(binding);
        delivery = new AgentDeliveryReviewRenderer(binding);
        order = new AgentOrderReviewRenderer(binding);
        financial = new AgentFinancialActionReviewRenderer(binding);
    }

    void render(AgentAction action) {
        if (action.summary() instanceof AgentFinanceSummary finance) {
            hideNonFinancial();
            financial.render(finance);
            return;
        }
        if (action.summary()
                instanceof AgentCommissionSummary commission) {
            hideNonFinancial();
            financial.render(commission);
            return;
        }
        financial.hide();
        if (action.summary()
                instanceof AgentAppointmentSummary appointmentSummary) {
            delivery.hide();
            order.hide();
            hideAttendance();
            hideCreation();
            amendment.hide();
            appointment.render(appointmentSummary);
            return;
        }
        appointment.hide();
        if (action.summary()
                instanceof AgentDeliverySummary deliverySummary) {
            order.hide();
            hideAttendance();
            hideCreation();
            amendment.hide();
            delivery.render(deliverySummary);
            return;
        }
        delivery.hide();
        if (action.summary()
                instanceof AgentOrderSummary orderSummary) {
            hideAttendance();
            hideCreation();
            amendment.hide();
            order.render(orderSummary);
            return;
        }
        order.hide();
        if (action.summary()
                instanceof AgentAttendanceReplySummary attendance) {
            hideCreation();
            amendment.hide();
            renderAttendance(attendance);
            return;
        }
        hideAttendance();
        renderQuoteHeader(action);
        if (action.summary()
                instanceof AgentMaterialQuoteCreateSummary create) {
            amendment.hide();
            renderCreation(create);
            binding.agentActionReviewNotice.setText(
                    R.string.agent_action_create_revalidation_notice);
            return;
        }
        if (action.summary()
                instanceof AgentMaterialQuoteAmendSummary amend) {
            hideCreation();
            amendment.render(amend);
            binding.agentActionReviewNotice.setText(
                    R.string.agent_action_amend_revalidation_notice);
            return;
        }
        hideCreation();
        amendment.hide();
        binding.agentActionReviewNotice.setText(
                R.string.agent_action_send_revalidation_notice);
    }

    private void hideNonFinancial() {
        appointment.hide();
        delivery.hide();
        order.hide();
        hideAttendance();
        hideCreation();
        amendment.hide();
    }

    private void renderQuoteHeader(AgentAction action) {
        String quoteTitle;
        String customerName;
        BigDecimal total;
        if (action.summary()
                instanceof AgentMaterialQuoteCreateSummary create) {
            quoteTitle = create.quoteTitle();
            customerName = create.customerName();
            total = create.total();
        } else if (action.summary()
                instanceof AgentMaterialQuoteAmendSummary amend) {
            quoteTitle = amend.quoteTitle();
            customerName = amend.customerName();
            total = amend.total();
        } else {
            AgentMaterialQuoteSendSummary send =
                    (AgentMaterialQuoteSendSummary) action.summary();
            quoteTitle = send.quoteTitle();
            customerName = send.customerName();
            total = send.total();
        }
        binding.agentActionReviewQuote.setVisibility(View.VISIBLE);
        binding.agentActionReviewCustomer.setVisibility(View.VISIBLE);
        binding.agentActionReviewTotal.setVisibility(View.VISIBLE);
        binding.agentActionReviewQuote.setText(
                quoteTitle);
        binding.agentActionReviewCustomer.setText(
                binding.getRoot().getContext().getString(
                        R.string.agent_action_customer,
                        customerName));
        binding.agentActionReviewTotal.setText(
                binding.getRoot().getContext().getString(
                        R.string.agent_action_total,
                        currency.format(total)));
    }

    private void renderAttendance(
            AgentAttendanceReplySummary summary) {
        binding.agentActionReviewQuote.setVisibility(View.VISIBLE);
        binding.agentActionReviewQuote.setText(summary.customerName());
        binding.agentActionReviewCustomer.setVisibility(View.GONE);
        binding.agentActionReviewTotal.setVisibility(View.GONE);
        binding.agentActionReviewAttendanceChannel.setVisibility(
                View.VISIBLE);
        binding.agentActionReviewAttendanceChannel.setText(
                binding.getRoot().getContext().getString(
                        R.string.agent_action_attendance_channel,
                        binding.getRoot().getContext().getString(
                                summary.channel()
                                                == AgentAttendanceChannel
                                                        .WHATSAPP
                                        ? R.string
                                                .agent_action_attendance_channel_whatsapp
                                        : R.string
                                                .agent_action_attendance_channel_site_chat)));
        summary.organizationName().ifPresentOrElse(
                organization -> {
                    binding.agentActionReviewAttendanceOrganization
                            .setVisibility(View.VISIBLE);
                    binding.agentActionReviewAttendanceOrganization
                            .setText(binding.getRoot()
                                    .getContext()
                                    .getString(
                                            R.string
                                                    .agent_action_attendance_organization,
                                            organization));
                },
                () -> binding.agentActionReviewAttendanceOrganization
                        .setVisibility(View.GONE));
        summary.latestMessage().ifPresentOrElse(
                message -> {
                    binding.agentActionReviewAttendanceLatestLabel
                            .setVisibility(View.VISIBLE);
                    binding.agentActionReviewAttendanceLatest
                            .setVisibility(View.VISIBLE);
                    binding.agentActionReviewAttendanceLatest.setText(
                            binding.getRoot().getContext().getString(
                                    R.string.agent_action_attendance_latest,
                                    message.preview(),
                                    AgentText.activity(
                                            message.createdAt())));
                },
                () -> {
                    binding.agentActionReviewAttendanceLatestLabel
                            .setVisibility(View.GONE);
                    binding.agentActionReviewAttendanceLatest
                            .setVisibility(View.GONE);
                });
        binding.agentActionReviewAttendanceReplyLabel.setVisibility(
                View.VISIBLE);
        binding.agentActionReviewAttendanceReply.setVisibility(
                View.VISIBLE);
        binding.agentActionReviewAttendanceReply.setText(
                summary.content());
        binding.agentActionReviewNotice.setText(
                R.string.agent_action_attendance_revalidation_notice);
    }

    private void hideAttendance() {
        binding.agentActionReviewAttendanceChannel.setVisibility(View.GONE);
        binding.agentActionReviewAttendanceOrganization.setVisibility(
                View.GONE);
        binding.agentActionReviewAttendanceLatestLabel.setVisibility(
                View.GONE);
        binding.agentActionReviewAttendanceLatest.setVisibility(View.GONE);
        binding.agentActionReviewAttendanceReplyLabel.setVisibility(
                View.GONE);
        binding.agentActionReviewAttendanceReply.setVisibility(View.GONE);
    }

    private void renderCreation(
            AgentMaterialQuoteCreateSummary summary) {
        binding.agentActionReviewValidity.setVisibility(View.VISIBLE);
        binding.agentActionReviewValidity.setText(
                summary.validUntil()
                        .map(value -> binding.getRoot()
                                .getContext()
                                .getString(
                                        R.string.agent_action_validity,
                                        DATE.format(value)))
                        .orElseGet(() -> binding.getRoot()
                                .getContext()
                                .getString(
                                        R.string.agent_action_no_validity)));
        binding.agentActionReviewItemsLabel.setVisibility(View.VISIBLE);
        binding.agentActionReviewItems.setVisibility(View.VISIBLE);
        binding.agentActionReviewItems.removeAllViews();
        for (AgentMaterialQuoteCreateItem item : summary.items()) {
            AgentItemActionReviewBinding line =
                    AgentItemActionReviewBinding.inflate(
                            LayoutInflater.from(
                                    binding.getRoot().getContext()),
                            binding.agentActionReviewItems,
                            false);
            line.agentActionItemDescription.setText(
                    item.description());
            line.agentActionItemCalculation.setText(
                    binding.getRoot().getContext().getString(
                            R.string.agent_action_item_calculation,
                            quantity(item.quantity()),
                            item.unit(),
                            currency.format(item.unitPrice())));
            line.agentActionItemTotal.setText(
                    currency.format(item.total()));
            binding.agentActionReviewItems.addView(line.getRoot());
        }
        summary.notes().ifPresentOrElse(
                notes -> {
                    binding.agentActionReviewNotesLabel.setVisibility(
                            View.VISIBLE);
                    binding.agentActionReviewNotes.setVisibility(
                            View.VISIBLE);
                    binding.agentActionReviewNotes.setText(notes);
                },
                () -> {
                    binding.agentActionReviewNotesLabel.setVisibility(
                            View.GONE);
                    binding.agentActionReviewNotes.setVisibility(
                            View.GONE);
                });
        binding.agentActionReviewSubtotal.setVisibility(View.VISIBLE);
        binding.agentActionReviewSubtotal.setText(
                binding.getRoot().getContext().getString(
                        R.string.agent_action_subtotal,
                        currency.format(summary.subtotal())));
    }

    private void hideCreation() {
        binding.agentActionReviewValidity.setVisibility(View.GONE);
        binding.agentActionReviewItemsLabel.setVisibility(View.GONE);
        binding.agentActionReviewItems.setVisibility(View.GONE);
        binding.agentActionReviewItems.removeAllViews();
        binding.agentActionReviewNotesLabel.setVisibility(View.GONE);
        binding.agentActionReviewNotes.setVisibility(View.GONE);
        binding.agentActionReviewSubtotal.setVisibility(View.GONE);
    }

    private static String quantity(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }
}
