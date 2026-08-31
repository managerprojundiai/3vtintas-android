package br.com.tresvtintas.mobile.feature.commission;

import android.content.Context;
import android.view.View;
import br.com.tresvtintas.mobile.core.commission.CommissionAction;
import br.com.tresvtintas.mobile.core.commission.CommissionDetail;
import br.com.tresvtintas.mobile.core.commission.CommissionDetailState;
import br.com.tresvtintas.mobile.core.commission.CommissionSummary;
import br.com.tresvtintas.mobile.feature.commission.databinding.CommissionActivityDetailBinding;

final class CommissionDetailRenderer {
    private final CommissionActivityDetailBinding binding;

    CommissionDetailRenderer(CommissionActivityDetailBinding binding) {
        this.binding = binding;
    }

    void render(CommissionDetailState state) {
        boolean busy = state.phase() == CommissionDetailState.Phase.LOADING;
        binding.commissionDetailProgress.setVisibility(
                busy ? View.VISIBLE : View.INVISIBLE);
        binding.commissionDetailContent.setVisibility(View.GONE);
        binding.commissionDetailError.setVisibility(View.GONE);
        if (state.phase() == CommissionDetailState.Phase.ERROR) {
            binding.commissionDetailErrorMessage.setText(
                    CommissionText.failure(state.failure().orElseThrow()));
            state.requestId().ifPresent(requestId ->
                    binding.commissionDetailErrorMessage.append(
                            "\n\n"
                                    + binding.getRoot().getContext().getString(
                                            R.string.commission_support_code,
                                            requestId)));
            binding.commissionDetailError.setVisibility(View.VISIBLE);
            return;
        }
        state.detail().ifPresent(this::detail);
    }

    void setActionsBusy(boolean busy) {
        binding.commissionActionApprove.setEnabled(!busy);
        binding.commissionActionCancel.setEnabled(!busy);
        binding.commissionActionPay.setEnabled(!busy);
        binding.commissionDetailProgress.setVisibility(
                busy ? View.VISIBLE : View.INVISIBLE);
    }

    void showActionNotice(int message, String requestId) {
        Context context = binding.getRoot().getContext();
        String text = context.getString(message);
        if (requestId != null && !requestId.isBlank()) {
            text += "\n" + context.getString(
                    R.string.commission_support_code,
                    requestId);
        }
        binding.commissionActionNotice.setText(text);
        binding.commissionActionNotice.setVisibility(View.VISIBLE);
    }

    private void detail(CommissionDetail value) {
        CommissionSummary summary = value.summary();
        var context = binding.getRoot().getContext();
        String notInformed = context.getString(
                R.string.commission_not_informed);
        binding.commissionDetailReference.setText(
                CommissionText.reference(context, summary.id()));
        binding.commissionDetailStatus.setText(
                CommissionText.status(summary.status()));
        binding.commissionDetailAmount.setText(
                CommissionText.money(summary.calculation().amount()));
        String recipient = summary.recipient().name().orElse(notInformed);
        binding.commissionDetailRecipient.setText(context.getString(
                R.string.commission_recipient_value,
                recipient,
                context.getString(
                        CommissionText.role(summary.recipient().role()))));

        String origin = summary.order()
                .map(order -> context.getString(
                        R.string.commission_order_value,
                        order.id(),
                        context.getString("material".equals(order.type())
                                ? R.string.commission_order_material
                                : R.string.commission_order_labor),
                        context.getString("received".equals(order.paymentStatus())
                                ? R.string.commission_payment_received
                                : R.string.commission_payment_pending)))
                .orElseGet(() -> context.getString(
                        R.string.commission_no_order));
        if (summary.organization().isPresent()) {
            origin = origin
                    + "\n"
                    + context.getString(
                            R.string.commission_item_store,
                            summary.organization().orElseThrow().name());
        }
        binding.commissionDetailOrigin.setText(origin);

        CommissionSummary.Calculation calculation = summary.calculation();
        binding.commissionDetailCalculation.setText(context.getString(
                R.string.commission_calculation_value,
                CommissionText.money(calculation.baseAmount()),
                CommissionText.percentage(calculation.ratePercent()),
                CommissionText.money(calculation.amount()),
                calculation.ruleVersion()));
        showActions(summary);
        binding.commissionDetailTimeline.setText(context.getString(
                R.string.commission_timeline_value,
                context.getString(CommissionText.status(summary.status())),
                CommissionText.date(summary.createdAt()),
                summary.approvedAt().map(CommissionText::date).orElse(notInformed),
                summary.paidAt().map(CommissionText::date).orElse(notInformed),
                summary.cancelledAt().map(CommissionText::date)
                        .orElse(notInformed)));

        String approvedBy = value.approvedBy()
                .flatMap(CommissionDetail.Actor::name)
                .orElse(notInformed);
        StringBuilder trace = new StringBuilder(context.getString(
                R.string.commission_trace_value,
                value.workflowId().orElse(notInformed),
                value.batchId().isPresent()
                        ? Long.toString(value.batchId().orElseThrow())
                        : notInformed,
                approvedBy));
        if (value.cancellation().isPresent()) {
            CommissionDetail.Cancellation cancellation =
                    value.cancellation().orElseThrow();
            trace.append(context.getString(
                    R.string.commission_cancellation_trace,
                    cancellation.reason().orElse(notInformed),
                    cancellation.cancelledBy()
                            .flatMap(CommissionDetail.Actor::name)
                            .orElse(notInformed)));
        }
        if (value.payment().isPresent()) {
            CommissionDetail.Payment payment = value.payment().orElseThrow();
            trace.append(context.getString(
                    R.string.commission_payment_trace,
                    payment.method()
                            .map(CommissionText::paymentMethod)
                            .map(context::getString)
                            .orElse(notInformed),
                    payment.reference().orElse(notInformed),
                    payment.paidBy()
                            .flatMap(CommissionDetail.Actor::name)
                            .orElse(notInformed)));
        }
        binding.commissionDetailTrace.setText(trace.toString());
        binding.commissionDetailContent.setVisibility(View.VISIBLE);
    }

    private void showActions(CommissionSummary summary) {
        binding.commissionActionApprove.setVisibility(
                visibility(summary, CommissionAction.APPROVE));
        binding.commissionActionCancel.setVisibility(
                visibility(summary, CommissionAction.CANCEL));
        binding.commissionActionPay.setVisibility(
                visibility(summary, CommissionAction.PAY));
        setActionsBusy(false);
    }

    private static int visibility(
            CommissionSummary summary,
            CommissionAction action) {
        return summary.allowedActions().contains(action)
                ? View.VISIBLE
                : View.GONE;
    }
}
