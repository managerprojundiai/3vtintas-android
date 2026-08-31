package br.com.tresvtintas.mobile.feature.dashboard;

import android.content.Context;
import android.view.View;
import br.com.tresvtintas.mobile.core.dashboard.DashboardContext;
import br.com.tresvtintas.mobile.core.dashboard.DashboardCountAmount;
import br.com.tresvtintas.mobile.core.dashboard.DashboardFinanceTotals;
import br.com.tresvtintas.mobile.core.dashboard.DashboardSnapshot;
import br.com.tresvtintas.mobile.core.dashboard.DashboardState;
import br.com.tresvtintas.mobile.core.dashboard.DashboardWorkload;
import br.com.tresvtintas.mobile.feature.dashboard.databinding.DashboardActivityBinding;
import java.util.Optional;

final class DashboardRenderer {
    private final DashboardActivityBinding binding;

    DashboardRenderer(DashboardActivityBinding binding) {
        if (binding == null) {
            throw new IllegalArgumentException(
                    "Dashboard binding is required.");
        }
        this.binding = binding;
    }

    void render(DashboardState state) {
        boolean loading = state.phase() == DashboardState.Phase.LOADING
                || state.phase() == DashboardState.Phase.REFRESHING;
        binding.dashboardProgress.setVisibility(
                loading ? View.VISIBLE : View.INVISIBLE);
        binding.dashboardRefresh.setEnabled(!loading);
        if (state.snapshot().isPresent()) {
            showSnapshot(state.snapshot().orElseThrow());
            binding.dashboardErrorGroup.setVisibility(View.GONE);
            showNotice(state);
            return;
        }
        binding.dashboardContent.setVisibility(View.GONE);
        binding.dashboardNoticeCard.setVisibility(View.GONE);
        boolean error = state.phase() == DashboardState.Phase.ERROR;
        binding.dashboardErrorGroup.setVisibility(
                error ? View.VISIBLE : View.GONE);
        if (error) {
            showError(state);
        }
    }

    private void showSnapshot(DashboardSnapshot value) {
        Context context = context();
        binding.dashboardContent.setVisibility(View.VISIBLE);
        binding.dashboardContext.setText(context.getString(
                R.string.dashboard_context_value,
                context.getString(DashboardText.visibility(
                        value.context().visibility())),
                organization(value.context())));
        binding.dashboardGeneratedAt.setText(context.getString(
                R.string.dashboard_generated_value,
                DashboardText.date(value.generatedAt())));
        renderOrders(value.workload().orders());
        renderQuotes(value.workload().quotes());
        renderDeliveries(value.workload().deliveries());
        renderCommissions(value.commissions());
        renderAppointments(value.appointments());
        renderFinance(value.finance());
        binding.dashboardNoMetrics.setVisibility(
                hasMetrics(value) ? View.GONE : View.VISIBLE);
    }

    private void renderOrders(Optional<DashboardWorkload.Orders> value) {
        binding.dashboardOrdersCard.setVisibility(
                value.isPresent() ? View.VISIBLE : View.GONE);
        value.ifPresent(item -> binding.dashboardOrders.setText(
                context().getString(
                        R.string.dashboard_orders_value,
                        item.total(),
                        item.active(),
                        item.awaitingPayment())));
    }

    private void renderQuotes(Optional<DashboardWorkload.Quotes> value) {
        binding.dashboardQuotesCard.setVisibility(
                value.isPresent() ? View.VISIBLE : View.GONE);
        value.ifPresent(item -> binding.dashboardQuotes.setText(
                context().getString(
                        R.string.dashboard_quotes_value,
                        item.activeMaterial(),
                        item.activeLabor(),
                        item.expiringSoon())));
    }

    private void renderDeliveries(
            Optional<DashboardWorkload.Deliveries> value) {
        binding.dashboardDeliveriesCard.setVisibility(
                value.isPresent() ? View.VISIBLE : View.GONE);
        value.ifPresent(item -> binding.dashboardDeliveries.setText(
                context().getString(
                        R.string.dashboard_deliveries_value,
                        item.active(),
                        item.today(),
                        item.inTransit(),
                        item.awaitingSchedule()
                                .map(String::valueOf)
                                .orElse(context().getString(
                                        R.string.dashboard_not_available)))));
    }

    private void renderCommissions(
            Optional<DashboardSnapshot.Commissions> value) {
        binding.dashboardCommissionsCard.setVisibility(
                value.isPresent() ? View.VISIBLE : View.GONE);
        value.ifPresent(item -> binding.dashboardCommissions.setText(
                context().getString(
                        R.string.dashboard_commissions_value,
                        context().getString(DashboardText.commissionScope(
                                item.scope())),
                        aggregate(item.pending()),
                        aggregate(item.approved()))));
    }

    private void renderAppointments(
            Optional<DashboardSnapshot.Appointments> value) {
        binding.dashboardAppointmentsCard.setVisibility(
                value.isPresent() ? View.VISIBLE : View.GONE);
        value.ifPresent(item -> binding.dashboardAppointments.setText(
                context().getString(
                        R.string.dashboard_appointments_value,
                        context().getString(DashboardText.appointmentScope(
                                item.scope())),
                        item.today(),
                        item.upcoming())));
    }

    private void renderFinance(Optional<DashboardSnapshot.Finance> value) {
        binding.dashboardFinanceCard.setVisibility(
                value.isPresent() ? View.VISIBLE : View.GONE);
        value.ifPresent(item -> binding.dashboardFinance.setText(
                context().getString(
                        R.string.dashboard_finance_value,
                        context().getString(DashboardText.financeScope(
                                item.scope())),
                        finance(item.pending()),
                        finance(item.overdue()))));
    }

    private void showNotice(DashboardState state) {
        boolean stale = state.failure().isPresent();
        binding.dashboardNoticeCard.setVisibility(
                stale ? View.VISIBLE : View.GONE);
        if (stale) {
            binding.dashboardNotice.setText(
                    R.string.dashboard_warning_stale);
        }
    }

    private void showError(DashboardState state) {
        int message = state.failure()
                .map(DashboardText::failure)
                .orElse(R.string.dashboard_error_generic);
        binding.dashboardErrorMessage.setText(message);
        binding.dashboardSupportCode.setVisibility(
                state.requestId().isPresent() ? View.VISIBLE : View.GONE);
        state.requestId().ifPresent(value ->
                binding.dashboardSupportCode.setText(context().getString(
                        R.string.dashboard_support_code,
                        value)));
    }

    private String aggregate(DashboardCountAmount value) {
        return context().getString(
                R.string.dashboard_aggregate_value,
                value.count(),
                DashboardText.money(value.amount()));
    }

    private String finance(DashboardFinanceTotals value) {
        return context().getString(
                R.string.dashboard_finance_totals,
                aggregate(value.expense()),
                aggregate(value.payable()),
                aggregate(value.receivable()));
    }

    private String organization(DashboardContext value) {
        return value.organization()
                .map(DashboardContext.Organization::name)
                .orElse(context().getString(
                        R.string.dashboard_all_organizations));
    }

    private static boolean hasMetrics(DashboardSnapshot value) {
        return value.workload().orders().isPresent()
                || value.workload().quotes().isPresent()
                || value.workload().deliveries().isPresent()
                || value.commissions().isPresent()
                || value.appointments().isPresent()
                || value.finance().isPresent();
    }

    private Context context() {
        return binding.getRoot().getContext();
    }
}
