package br.com.tresvtintas.mobile.feature.team;

import android.content.Context;
import android.view.View;
import br.com.tresvtintas.mobile.core.team.TeamSnapshot;
import br.com.tresvtintas.mobile.core.team.TeamState;
import br.com.tresvtintas.mobile.feature.team.databinding.TeamActivityBinding;

final class TeamRenderer {
    private final TeamActivityBinding binding;
    private final TeamMemberAdapter adapter;

    TeamRenderer(
            TeamActivityBinding binding,
            TeamMemberAdapter adapter) {
        if (binding == null || adapter == null) {
            throw new IllegalArgumentException(
                    "Team presentation is required.");
        }
        this.binding = binding;
        this.adapter = adapter;
    }

    void render(TeamState state) {
        boolean loading = state.phase() == TeamState.Phase.LOADING
                || state.phase() == TeamState.Phase.REFRESHING;
        binding.teamProgress.setVisibility(
                loading ? View.VISIBLE : View.INVISIBLE);
        binding.teamRefresh.setEnabled(!loading);
        if (state.snapshot().isPresent()) {
            showSnapshot(state.snapshot().orElseThrow());
            binding.teamErrorGroup.setVisibility(View.GONE);
            showNotice(state);
            return;
        }
        binding.teamContent.setVisibility(View.GONE);
        binding.teamNoticeCard.setVisibility(View.GONE);
        boolean error = state.phase() == TeamState.Phase.ERROR;
        binding.teamErrorGroup.setVisibility(
                error ? View.VISIBLE : View.GONE);
        if (error) {
            showError(state);
        }
    }

    private void showSnapshot(TeamSnapshot value) {
        Context context = context();
        binding.teamContent.setVisibility(View.VISIBLE);
        binding.teamContext.setText(value.context().organization()
                .map(organization -> context.getString(
                        R.string.team_context_organization,
                        organization.name()))
                .orElseGet(() -> context.getString(
                        R.string.team_context_all)));
        binding.teamGeneratedAt.setText(context.getString(
                R.string.team_generated_at,
                TeamText.date(value.generatedAt())));
        binding.teamPainters.setText(String.valueOf(
                value.summary().teamPainters()));
        binding.teamOrders.setText(String.valueOf(
                value.summary().totalOrders()));
        binding.teamQuotes.setText(String.valueOf(
                value.summary().totalQuotes()));
        binding.teamSales.setText(TeamText.money(
                value.summary().totalSales()));
        binding.teamTopRegion.setText(value.summary().topRegion()
                .map(region -> context.getString(
                        R.string.team_top_region_value,
                        region.label(),
                        context.getResources().getQuantityString(
                                R.plurals.team_top_region_orders,
                                region.count(),
                                region.count())))
                .orElseGet(() -> context.getString(
                        R.string.team_not_informed)));
        adapter.submitList(value.members());
        binding.teamEmpty.setVisibility(value.members().isEmpty()
                ? View.VISIBLE
                : View.GONE);
        binding.teamMembers.setVisibility(value.members().isEmpty()
                ? View.GONE
                : View.VISIBLE);
    }

    private void showNotice(TeamState state) {
        if (state.failure().isEmpty()) {
            binding.teamNoticeCard.setVisibility(View.GONE);
            return;
        }
        binding.teamNotice.setText(TeamText.failure(
                state.failure().orElseThrow()));
        binding.teamNoticeCard.setVisibility(View.VISIBLE);
    }

    private void showError(TeamState state) {
        binding.teamErrorMessage.setText(TeamText.failure(
                state.failure().orElseThrow()));
        if (state.requestId().isPresent()) {
            binding.teamSupportCode.setText(context().getString(
                    R.string.team_support_code,
                    state.requestId().orElseThrow()));
            binding.teamSupportCode.setVisibility(View.VISIBLE);
        } else {
            binding.teamSupportCode.setVisibility(View.GONE);
        }
    }

    private Context context() {
        return binding.getRoot().getContext();
    }
}
