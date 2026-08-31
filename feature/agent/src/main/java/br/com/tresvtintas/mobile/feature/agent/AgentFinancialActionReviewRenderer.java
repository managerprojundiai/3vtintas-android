package br.com.tresvtintas.mobile.feature.agent;

import android.view.View;
import br.com.tresvtintas.mobile.core.agent.AgentCommissionOperation;
import br.com.tresvtintas.mobile.core.agent.AgentCommissionSummary;
import br.com.tresvtintas.mobile.core.agent.AgentFinanceOperation;
import br.com.tresvtintas.mobile.core.agent.AgentFinanceScope;
import br.com.tresvtintas.mobile.core.agent.AgentFinanceSummary;
import br.com.tresvtintas.mobile.core.commission.CommissionPaymentMethod;
import br.com.tresvtintas.mobile.core.commission.CommissionStatus;
import br.com.tresvtintas.mobile.core.finance.FinanceEntryStatus;
import br.com.tresvtintas.mobile.core.finance.FinanceEntryType;
import br.com.tresvtintas.mobile.core.finance.FinancePaymentMethod;
import br.com.tresvtintas.mobile.feature.agent.databinding.AgentDialogActionReviewBinding;
import br.com.tresvtintas.mobile.feature.agent.databinding.AgentViewFinancialActionReviewBinding;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

final class AgentFinancialActionReviewRenderer {
    private final AgentDialogActionReviewBinding binding;
    private final AgentViewFinancialActionReviewBinding financial;
    private final NumberFormat currency =
            NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));

    AgentFinancialActionReviewRenderer(
            AgentDialogActionReviewBinding binding) {
        this.binding = java.util.Objects.requireNonNull(
                binding,
                "Agent financial review binding is required.");
        financial = AgentViewFinancialActionReviewBinding.bind(
                binding.getRoot().findViewById(
                        R.id.agent_action_review_financial));
    }

    void render(AgentFinanceSummary summary) {
        show();
        financial.agentActionReviewFinancialOperation.setText(
                financeOperation(summary.operation()));
        financial.agentActionReviewFinancialSubject.setText(summary.title());
        financial.agentActionReviewFinancialScope.setText(
                summary.scope() == AgentFinanceScope.PERSONAL
                        ? R.string.agent_action_finance_scope_personal
                        : R.string.agent_action_finance_scope_corporate);
        optional(
                financial.agentActionReviewFinancialOrganization,
                summary.organizationName().map(value ->
                        text(
                                R.string.agent_action_finance_organization,
                                value)).orElse(null));
        optional(
                financial.agentActionReviewFinancialRelated,
                summary.customerName().map(value ->
                        text(
                                R.string.agent_action_finance_customer,
                                value)).orElse(null));
        financial.agentActionReviewFinancialAmount.setText(
                money(summary.amount()));
        financial.agentActionReviewFinancialDetails.setText(
                financeDetails(summary));
        optional(
                financial.agentActionReviewFinancialContext,
                summary.notes().orElse(null));
        before(summary.beforeStatus()
                .map(this::financeStatus)
                .orElse(null));
        financial.agentActionReviewFinancialAfter.setText(
                financeStatus(summary.afterStatus()));
        binding.agentActionReviewNotice.setText(
                R.string.agent_action_finance_revalidation_notice);
    }

    void render(AgentCommissionSummary summary) {
        show();
        financial.agentActionReviewFinancialOperation.setText(
                commissionOperation(summary.operation()));
        financial.agentActionReviewFinancialSubject.setText(
                summary.recipientName());
        financial.agentActionReviewFinancialScope.setText(
                text(
                        R.string.agent_action_commission_recipient,
                        commissionRecipient(summary)));
        optional(
                financial.agentActionReviewFinancialOrganization,
                summary.organizationName().map(value ->
                        text(
                                R.string.agent_action_finance_organization,
                                value)).orElse(null));
        optional(
                financial.agentActionReviewFinancialRelated,
                summary.orderId().map(value ->
                        text(
                                R.string.agent_action_commission_order,
                                value)).orElse(null));
        financial.agentActionReviewFinancialAmount.setText(
                money(summary.amount()));
        financial.agentActionReviewFinancialDetails.setText(
                commissionDetails(summary));
        optional(
                financial.agentActionReviewFinancialContext,
                summary.cancellationReason()
                        .or(() -> summary.paymentReference())
                        .orElse(null));
        before(commissionStatus(summary.beforeStatus()));
        financial.agentActionReviewFinancialAfter.setText(
                commissionStatus(summary.afterStatus()));
        binding.agentActionReviewNotice.setText(
                R.string.agent_action_commission_revalidation_notice);
    }

    void hide() {
        financial.agentActionReviewFinancial.setVisibility(View.GONE);
    }

    private void show() {
        financial.agentActionReviewFinancial.setVisibility(View.VISIBLE);
        binding.agentActionReviewQuote.setVisibility(View.GONE);
        binding.agentActionReviewCustomer.setVisibility(View.GONE);
        binding.agentActionReviewTotal.setVisibility(View.GONE);
    }

    private void before(String value) {
        int visibility = value == null ? View.GONE : View.VISIBLE;
        financial.agentActionReviewFinancialBeforeLabel.setVisibility(
                visibility);
        financial.agentActionReviewFinancialBefore.setVisibility(visibility);
        if (value != null) {
            financial.agentActionReviewFinancialBefore.setText(value);
        }
    }

    private String financeDetails(AgentFinanceSummary summary) {
        StringBuilder details = new StringBuilder(
                text(
                        R.string.agent_action_finance_type,
                        financeType(summary.type())));
        summary.dueAt().ifPresent(value -> details
                .append('\n')
                .append(text(
                        R.string.agent_action_finance_due,
                        AgentText.activity(value))));
        summary.paymentMethod().ifPresent(value -> details
                .append('\n')
                .append(text(
                        R.string.agent_action_finance_payment,
                        financePayment(value))));
        return details.toString();
    }

    private String commissionDetails(AgentCommissionSummary summary) {
        StringBuilder details = new StringBuilder(
                text(
                        R.string.agent_action_commission_revision,
                        summary.expectedRevision()));
        summary.orderPaymentStatus().ifPresent(value -> details
                .append('\n')
                .append(text(
                        R.string.agent_action_commission_order_payment,
                        value.name().toLowerCase(Locale.ROOT))));
        summary.paymentMethod().ifPresent(value -> details
                .append('\n')
                .append(text(
                        R.string.agent_action_finance_payment,
                        commissionPayment(value))));
        return details.toString();
    }

    private String commissionRecipient(AgentCommissionSummary summary) {
        return switch (summary.recipientRole()) {
            case PAINTER -> text(
                    R.string.agent_action_commission_role_painter);
            case SALESPERSON -> text(
                    R.string.agent_action_commission_role_salesperson);
            case MASTER_ADMIN -> text(
                    R.string.agent_action_commission_role_master);
        };
    }

    private int financeOperation(AgentFinanceOperation value) {
        return switch (value) {
            case CREATE -> R.string.agent_action_finance_operation_create;
            case SETTLE -> R.string.agent_action_finance_operation_settle;
            case CANCEL -> R.string.agent_action_finance_operation_cancel;
        };
    }

    private int commissionOperation(AgentCommissionOperation value) {
        return switch (value) {
            case APPROVE ->
                    R.string.agent_action_commission_operation_approve;
            case CANCEL ->
                    R.string.agent_action_commission_operation_cancel;
            case PAY -> R.string.agent_action_commission_operation_pay;
        };
    }

    private String financeType(FinanceEntryType value) {
        return text(switch (value) {
            case EXPENSE -> R.string.agent_action_finance_type_expense;
            case PAYABLE -> R.string.agent_action_finance_type_payable;
            case RECEIVABLE ->
                    R.string.agent_action_finance_type_receivable;
        });
    }

    private String financeStatus(FinanceEntryStatus value) {
        return text(switch (value) {
            case PENDING -> R.string.agent_action_finance_status_pending;
            case SETTLED -> R.string.agent_action_finance_status_settled;
            case CANCELLED ->
                    R.string.agent_action_finance_status_cancelled;
        });
    }

    private String commissionStatus(CommissionStatus value) {
        return text(switch (value) {
            case PENDING ->
                    R.string.agent_action_commission_status_pending;
            case APPROVED ->
                    R.string.agent_action_commission_status_approved;
            case PAID -> R.string.agent_action_commission_status_paid;
            case CANCELLED ->
                    R.string.agent_action_commission_status_cancelled;
        });
    }

    private String financePayment(FinancePaymentMethod value) {
        return payment(value.name());
    }

    private String commissionPayment(CommissionPaymentMethod value) {
        return payment(value.name());
    }

    private String payment(String value) {
        return text(switch (value) {
            case "PIX" -> R.string.agent_action_payment_pix;
            case "TRANSFER" -> R.string.agent_action_payment_transfer;
            case "CASH" -> R.string.agent_action_payment_cash;
            case "BANK_SLIP" ->
                    R.string.agent_action_payment_bank_slip;
            default -> R.string.agent_action_payment_other;
        });
    }

    private String money(BigDecimal value) {
        return currency.format(value);
    }

    private String text(int resource, Object... arguments) {
        return binding.getRoot().getContext().getString(
                resource,
                arguments);
    }

    private static void optional(
            android.widget.TextView view,
            String value) {
        view.setVisibility(value == null ? View.GONE : View.VISIBLE);
        if (value != null) {
            view.setText(value);
        }
    }
}
