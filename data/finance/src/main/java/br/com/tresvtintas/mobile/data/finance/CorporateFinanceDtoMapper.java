package br.com.tresvtintas.mobile.data.finance;

import br.com.tresvtintas.mobile.core.corporatefinance.CorporateFinanceOrganization;
import br.com.tresvtintas.mobile.core.corporatefinance.CorporateFinanceOrganizationPage;
import br.com.tresvtintas.mobile.core.finance.FinanceAction;
import br.com.tresvtintas.mobile.core.finance.FinanceDetail;
import br.com.tresvtintas.mobile.core.finance.FinanceEntrySource;
import br.com.tresvtintas.mobile.core.finance.FinanceEntryStatus;
import br.com.tresvtintas.mobile.core.finance.FinanceEntryType;
import br.com.tresvtintas.mobile.core.finance.FinanceOverview;
import br.com.tresvtintas.mobile.core.finance.FinancePage;
import br.com.tresvtintas.mobile.core.finance.FinancePaymentMethod;
import br.com.tresvtintas.mobile.core.finance.FinanceSummary;
import br.com.tresvtintas.mobile.core.network.dto.CorporateFinanceDetailDto;
import br.com.tresvtintas.mobile.core.network.dto.CorporateFinanceMoneyTotalDto;
import br.com.tresvtintas.mobile.core.network.dto.CorporateFinanceOrganizationPageDto;
import br.com.tresvtintas.mobile.core.network.dto.CorporateFinanceOverviewDto;
import br.com.tresvtintas.mobile.core.network.dto.CorporateFinancePageDto;
import br.com.tresvtintas.mobile.core.network.dto.CorporateFinanceSummaryDto;
import br.com.tresvtintas.mobile.core.network.dto.CorporateFinanceTypeTotalsDto;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

final class CorporateFinanceDtoMapper {
    private CorporateFinanceDtoMapper() {
    }

    static CorporateFinanceOrganizationPage organizations(
            CorporateFinanceOrganizationPageDto value) {
        return new CorporateFinanceOrganizationPage(
                value.items().stream()
                        .map(item -> new CorporateFinanceOrganization(
                                item.id(),
                                item.name()))
                        .toList(),
                Optional.ofNullable(value.nextCursor()));
    }

    static FinancePage page(CorporateFinancePageDto value) {
        return new FinancePage(
                value.items().stream()
                        .map(CorporateFinanceDtoMapper::summary)
                        .toList(),
                overview(value.overview()),
                Optional.ofNullable(value.nextCursor()));
    }

    static FinanceDetail detail(CorporateFinanceDetailDto value) {
        CorporateFinanceSummaryDto summaryDto =
                new CorporateFinanceSummaryDto(
                        value.id(),
                        value.type(),
                        value.status(),
                        value.source(),
                        value.title(),
                        value.amount(),
                        value.currency(),
                        value.dueAt(),
                        value.settledAt(),
                        value.organization(),
                        value.customer(),
                        value.allowedActions(),
                        value.createdAt(),
                        value.updatedAt());
        return new FinanceDetail(
                summary(summaryDto),
                Optional.ofNullable(value.notes()),
                Optional.ofNullable(value.payment())
                        .map(CorporateFinanceDtoMapper::payment));
    }

    private static FinanceSummary summary(CorporateFinanceSummaryDto value) {
        return new FinanceSummary(
                value.id(),
                enumValue(FinanceEntryType.class, value.type()),
                enumValue(FinanceEntryStatus.class, value.status()),
                enumValue(FinanceEntrySource.class, value.source()),
                value.title(),
                new BigDecimal(value.amount()),
                instant(value.dueAt()),
                instant(value.settledAt()),
                Optional.of(new FinanceSummary.Organization(
                        value.organization().id(),
                        value.organization().name())),
                Optional.ofNullable(value.customer())
                        .map(item -> new FinanceSummary.Customer(
                                item.id(),
                                item.name())),
                value.allowedActions().stream()
                        .map(item -> enumValue(FinanceAction.class, item))
                        .collect(Collectors.toUnmodifiableSet()),
                Instant.parse(value.createdAt()),
                Instant.parse(value.updatedAt()));
    }

    private static FinanceOverview overview(
            CorporateFinanceOverviewDto value) {
        return new FinanceOverview(
                totals(value.pending()),
                totals(value.settled()),
                totals(value.cancelled()),
                totals(value.overdue()));
    }

    private static FinanceOverview.TypeTotals totals(
            CorporateFinanceTypeTotalsDto value) {
        return new FinanceOverview.TypeTotals(
                money(value.expense()),
                money(value.payable()),
                money(value.receivable()));
    }

    private static FinanceOverview.MoneyTotal money(
            CorporateFinanceMoneyTotalDto value) {
        return new FinanceOverview.MoneyTotal(
                value.count(),
                new BigDecimal(value.amount()));
    }

    private static FinanceDetail.Payment payment(
            CorporateFinanceDetailDto.Payment value) {
        return new FinanceDetail.Payment(
                Optional.ofNullable(value.method())
                        .map(item -> enumValue(
                                FinancePaymentMethod.class,
                                item)),
                Optional.ofNullable(value.reference()));
    }

    private static Optional<Instant> instant(String value) {
        return value == null
                ? Optional.empty()
                : Optional.of(Instant.parse(value));
    }

    private static <T extends Enum<T>> T enumValue(
            Class<T> type,
            String value) {
        return Enum.valueOf(type, value.toUpperCase(Locale.ROOT));
    }
}
