package br.com.tresvtintas.mobile.data.dashboard;

import br.com.tresvtintas.mobile.core.dashboard.DashboardContext;
import br.com.tresvtintas.mobile.core.dashboard.DashboardCountAmount;
import br.com.tresvtintas.mobile.core.dashboard.DashboardFinanceTotals;
import br.com.tresvtintas.mobile.core.dashboard.DashboardSnapshot;
import br.com.tresvtintas.mobile.core.dashboard.DashboardVisibility;
import br.com.tresvtintas.mobile.core.dashboard.DashboardWorkload;
import br.com.tresvtintas.mobile.core.network.dto.DashboardResponseDto;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;

final class DashboardDtoMapper {
    private DashboardDtoMapper() {
    }

    static DashboardSnapshot snapshot(DashboardResponseDto value) {
        return new DashboardSnapshot(
                Instant.parse(value.generatedAt()),
                context(value.context()),
                workload(value.workload()),
                Optional.ofNullable(value.commissions())
                        .map(DashboardDtoMapper::commissions),
                Optional.ofNullable(value.appointments())
                        .map(DashboardDtoMapper::appointments),
                Optional.ofNullable(value.finance())
                        .map(DashboardDtoMapper::finance));
    }

    private static DashboardContext context(
            DashboardResponseDto.Context value) {
        return new DashboardContext(
                DashboardVisibility.fromWireValue(value.visibility())
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Dashboard visibility is unknown.")),
                Optional.ofNullable(value.organization())
                        .map(organization -> new DashboardContext.Organization(
                                organization.id(),
                                organization.name())));
    }

    private static DashboardWorkload workload(
            DashboardResponseDto.Workload value) {
        return new DashboardWorkload(
                Optional.ofNullable(value.orders())
                        .map(orders -> new DashboardWorkload.Orders(
                                orders.total(),
                                orders.active(),
                                orders.awaitingPayment())),
                Optional.ofNullable(value.quotes())
                        .map(quotes -> new DashboardWorkload.Quotes(
                                quotes.activeMaterial(),
                                quotes.activeLabor(),
                                quotes.expiringSoon())),
                Optional.ofNullable(value.deliveries())
                        .map(deliveries -> new DashboardWorkload.Deliveries(
                                deliveries.active(),
                                deliveries.today(),
                                deliveries.inTransit(),
                                Optional.ofNullable(
                                        deliveries.awaitingSchedule()))));
    }

    private static DashboardSnapshot.Commissions commissions(
            DashboardResponseDto.Commissions value) {
        return new DashboardSnapshot.Commissions(
                enumValue(
                        DashboardSnapshot.CommissionScope.class,
                        value.scope()),
                amount(value.pending()),
                amount(value.approved()));
    }

    private static DashboardSnapshot.Appointments appointments(
            DashboardResponseDto.Appointments value) {
        return new DashboardSnapshot.Appointments(
                enumValue(
                        DashboardSnapshot.AppointmentScope.class,
                        value.scope()),
                value.today(),
                value.upcoming());
    }

    private static DashboardSnapshot.Finance finance(
            DashboardResponseDto.Finance value) {
        return new DashboardSnapshot.Finance(
                enumValue(
                        DashboardSnapshot.FinanceScope.class,
                        value.scope()),
                financeTotals(value.pending()),
                financeTotals(value.overdue()));
    }

    private static DashboardFinanceTotals financeTotals(
            DashboardResponseDto.FinanceTotals value) {
        return new DashboardFinanceTotals(
                amount(value.expense()),
                amount(value.payable()),
                amount(value.receivable()));
    }

    private static DashboardCountAmount amount(
            DashboardResponseDto.CountAmount value) {
        return new DashboardCountAmount(
                value.count(),
                new BigDecimal(value.amount()));
    }

    private static <T extends Enum<T>> T enumValue(
            Class<T> type,
            String value) {
        return Enum.valueOf(type, value.toUpperCase(Locale.ROOT));
    }
}
