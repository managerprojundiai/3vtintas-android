package br.com.tresvtintas.mobile.feature.finance;

import android.view.View;
import br.com.tresvtintas.mobile.core.finance.FinanceAction;
import br.com.tresvtintas.mobile.core.finance.FinanceDetail;
import br.com.tresvtintas.mobile.core.finance.FinanceDetailState;
import br.com.tresvtintas.mobile.core.finance.FinanceMutationState;
import br.com.tresvtintas.mobile.core.finance.FinanceSummary;
import br.com.tresvtintas.mobile.feature.finance.databinding.FinanceActivityDetailBinding;

final class FinanceDetailRenderer {
    private final FinanceActivityDetailBinding binding;

    FinanceDetailRenderer(FinanceActivityDetailBinding binding) {
        this.binding = binding;
    }

    void render(FinanceDetailState state) {
        boolean loading = state.phase() == FinanceDetailState.Phase.LOADING;
        binding.financeDetailProgress.setVisibility(
                loading ? View.VISIBLE : View.INVISIBLE);
        binding.financeDetailError.setVisibility(View.GONE);
        if (state.phase() == FinanceDetailState.Phase.ERROR) {
            binding.financeDetailContent.setVisibility(View.GONE);
            binding.financeDetailErrorMessage.setText(FinanceText.failure(
                    binding.getRoot().getContext(),
                    state.failure().orElseThrow()));
            state.requestId().ifPresent(requestId ->
                    binding.financeDetailErrorMessage.append(
                            "\n\n"
                                    + binding.getRoot().getContext().getString(
                                            R.string.finance_support_code,
                                            requestId)));
            binding.financeDetailError.setVisibility(View.VISIBLE);
            return;
        }
        state.detail().ifPresent(this::detail);
    }

    void renderMutation(FinanceMutationState state) {
        boolean running = state.phase() == FinanceMutationState.Phase.RUNNING;
        binding.financeDetailProgress.setVisibility(
                running ? View.VISIBLE : View.INVISIBLE);
        binding.financeDetailSettle.setEnabled(!running);
        binding.financeDetailCancel.setEnabled(!running);
        if (state.phase() == FinanceMutationState.Phase.ERROR) {
            binding.financeDetailNotice.setText(FinanceText.failure(
                    binding.getRoot().getContext(),
                    state.failure().orElseThrow()));
            state.requestId().ifPresent(requestId ->
                    binding.financeDetailNotice.append(
                            "\n"
                                    + binding.getRoot().getContext().getString(
                                            R.string.finance_support_code,
                                            requestId)));
            binding.financeDetailNoticeCard.setVisibility(View.VISIBLE);
        }
    }

    void success(boolean replayed) {
        binding.financeDetailNotice.setText(replayed
                ? R.string.finance_mutation_replayed
                : R.string.finance_mutation_success);
        binding.financeDetailNoticeCard.setVisibility(View.VISIBLE);
    }

    private void detail(FinanceDetail value) {
        FinanceSummary summary = value.summary();
        var context = binding.getRoot().getContext();
        String notInformed = context.getString(R.string.finance_not_informed);
        binding.financeDetailReference.setText(context.getString(
                R.string.finance_reference,
                summary.id()));
        binding.financeDetailStatus.setText(FinanceText.status(summary.status()));
        binding.financeDetailAmount.setText(FinanceText.money(summary.amount()));
        binding.financeDetailIdentity.setText(context.getString(
                R.string.finance_detail_identity,
                summary.title(),
                context.getString(FinanceText.type(summary.type())),
                context.getString(FinanceText.source(summary.source()))));
        summary.organization().ifPresent(organization ->
                binding.financeDetailIdentity.append(
                        "\n"
                                + context.getString(
                                        R.string.finance_detail_organization,
                                        organization.name(),
                                        organization.id())));
        binding.financeDetailDates.setText(context.getString(
                R.string.finance_detail_dates,
                FinanceText.date(summary.createdAt()),
                FinanceText.date(summary.updatedAt()),
                summary.dueAt().map(FinanceText::date).orElse(notInformed),
                summary.settledAt().map(FinanceText::date).orElse(notInformed)));
        binding.financeDetailCustomer.setText(summary.customer()
                .map(item -> item.name() + " (#" + item.id() + ")")
                .orElse(notInformed));
        binding.financeDetailNotes.setText(value.notes().orElse(notInformed));
        binding.financeDetailPayment.setText(value.payment()
                .map(payment -> context.getString(
                        R.string.finance_detail_payment,
                        payment.method()
                                .map(item -> context.getString(
                                        FinanceText.payment(item)))
                                .orElse(notInformed),
                        payment.reference().orElse(notInformed)))
                .orElse(notInformed));
        binding.financeDetailSettle.setVisibility(
                summary.allowedActions().contains(FinanceAction.SETTLE)
                        ? View.VISIBLE
                        : View.GONE);
        binding.financeDetailCancel.setVisibility(
                summary.allowedActions().contains(FinanceAction.CANCEL)
                        ? View.VISIBLE
                        : View.GONE);
        binding.financeDetailContent.setVisibility(View.VISIBLE);
    }
}
