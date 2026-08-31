package br.com.tresvtintas.mobile.feature.agent;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import br.com.tresvtintas.mobile.core.agent.AgentMaterialQuoteAmendChange;
import br.com.tresvtintas.mobile.core.agent.AgentMaterialQuoteAmendItem;
import br.com.tresvtintas.mobile.core.agent.AgentMaterialQuoteAmendSnapshot;
import br.com.tresvtintas.mobile.core.agent.AgentMaterialQuoteAmendSummary;
import br.com.tresvtintas.mobile.feature.agent.databinding.AgentDialogActionReviewBinding;
import br.com.tresvtintas.mobile.feature.agent.databinding.AgentItemActionChangeBinding;
import br.com.tresvtintas.mobile.feature.agent.databinding.AgentItemActionReviewBinding;
import com.google.android.material.textview.MaterialTextView;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Optional;

final class AgentQuoteAmendReviewRenderer {
    private final AgentDialogActionReviewBinding binding;
    private final NumberFormat currency =
            NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));

    AgentQuoteAmendReviewRenderer(
            AgentDialogActionReviewBinding binding) {
        this.binding = java.util.Objects.requireNonNull(
                binding,
                "Agent amendment review binding is required.");
    }

    void render(AgentMaterialQuoteAmendSummary summary) {
        visible(
                binding.agentActionReviewChangesLabel,
                binding.agentActionReviewChanges,
                binding.agentActionReviewBeforeLabel,
                binding.agentActionReviewBeforeItems,
                binding.agentActionReviewBeforeSubtotal,
                binding.agentActionReviewBeforeDiscount,
                binding.agentActionReviewBeforeTotal,
                binding.agentActionReviewAfterLabel,
                binding.agentActionReviewAfterItems,
                binding.agentActionReviewAfterSubtotal,
                binding.agentActionReviewAfterDiscount);
        binding.agentActionReviewChanges.removeAllViews();
        for (AgentMaterialQuoteAmendChange change : summary.changes()) {
            renderChange(change);
        }
        renderItems(
                binding.agentActionReviewBeforeItems,
                summary.before());
        renderItems(
                binding.agentActionReviewAfterItems,
                summary.after());
        bindSnapshotAmounts(
                summary.before(),
                binding.agentActionReviewBeforeSubtotal,
                binding.agentActionReviewBeforeDiscount);
        binding.agentActionReviewBeforeTotal.setText(
                binding.getRoot().getContext().getString(
                        R.string.agent_action_total,
                        currency.format(summary.before().total())));
        bindSnapshotAmounts(
                summary.after(),
                binding.agentActionReviewAfterSubtotal,
                binding.agentActionReviewAfterDiscount);
    }

    void hide() {
        gone(
                binding.agentActionReviewChangesLabel,
                binding.agentActionReviewChanges,
                binding.agentActionReviewBeforeLabel,
                binding.agentActionReviewBeforeItems,
                binding.agentActionReviewBeforeSubtotal,
                binding.agentActionReviewBeforeDiscount,
                binding.agentActionReviewBeforeTotal,
                binding.agentActionReviewAfterLabel,
                binding.agentActionReviewAfterItems,
                binding.agentActionReviewAfterSubtotal,
                binding.agentActionReviewAfterDiscount);
        binding.agentActionReviewChanges.removeAllViews();
        binding.agentActionReviewBeforeItems.removeAllViews();
        binding.agentActionReviewAfterItems.removeAllViews();
    }

    private void renderChange(AgentMaterialQuoteAmendChange change) {
        AgentItemActionChangeBinding line =
                AgentItemActionChangeBinding.inflate(
                        LayoutInflater.from(
                                binding.getRoot().getContext()),
                        binding.agentActionReviewChanges,
                        false);
        line.agentActionChangeOperation.setText(
                switch (change.operation()) {
                    case ADD -> R.string.agent_action_change_add;
                    case UPDATE_QUANTITY ->
                            R.string.agent_action_change_update;
                    case REMOVE ->
                            R.string.agent_action_change_remove;
                });
        line.agentActionChangeDescription.setText(
                change.description());
        line.agentActionChangeQuantities.setText(
                binding.getRoot().getContext().getString(
                        R.string.agent_action_change_quantities,
                        quantity(change.beforeQuantity()),
                        quantity(change.afterQuantity())));
        binding.agentActionReviewChanges.addView(line.getRoot());
    }

    private void renderItems(
            LinearLayout container,
            AgentMaterialQuoteAmendSnapshot snapshot) {
        container.removeAllViews();
        for (AgentMaterialQuoteAmendItem item : snapshot.items()) {
            AgentItemActionReviewBinding line =
                    AgentItemActionReviewBinding.inflate(
                            LayoutInflater.from(
                                    binding.getRoot().getContext()),
                            container,
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
            container.addView(line.getRoot());
        }
    }

    private void bindSnapshotAmounts(
            AgentMaterialQuoteAmendSnapshot snapshot,
            MaterialTextView subtotal,
            MaterialTextView discount) {
        subtotal.setText(binding.getRoot().getContext().getString(
                R.string.agent_action_subtotal,
                currency.format(snapshot.subtotal())));
        discount.setText(binding.getRoot().getContext().getString(
                R.string.agent_action_discount,
                currency.format(snapshot.discount())));
    }

    private String quantity(Optional<BigDecimal> value) {
        return value.map(AgentQuoteAmendReviewRenderer::quantity)
                .orElseGet(() -> binding.getRoot().getContext()
                        .getString(
                                R.string.agent_action_change_no_quantity));
    }

    private static String quantity(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }

    private static void visible(View... views) {
        for (View view : views) {
            view.setVisibility(View.VISIBLE);
        }
    }

    private static void gone(View... views) {
        for (View view : views) {
            view.setVisibility(View.GONE);
        }
    }
}
