package br.com.tresvtintas.mobile.data.finance;

import br.com.tresvtintas.mobile.core.finance.FinanceAction;
import br.com.tresvtintas.mobile.core.finance.FinanceDetail;
import br.com.tresvtintas.mobile.core.finance.FinanceEntrySource;
import br.com.tresvtintas.mobile.core.finance.FinanceEntryStatus;
import br.com.tresvtintas.mobile.core.finance.FinanceEntryType;
import br.com.tresvtintas.mobile.core.finance.FinanceOverview;
import br.com.tresvtintas.mobile.core.finance.FinancePage;
import br.com.tresvtintas.mobile.core.finance.FinancePaymentMethod;
import br.com.tresvtintas.mobile.core.finance.FinanceSummary;
import br.com.tresvtintas.mobile.core.network.dto.PersonalFinanceDetailDto;
import br.com.tresvtintas.mobile.core.network.dto.PersonalFinanceMoneyTotalDto;
import br.com.tresvtintas.mobile.core.network.dto.PersonalFinanceOverviewDto;
import br.com.tresvtintas.mobile.core.network.dto.PersonalFinancePageDto;
import br.com.tresvtintas.mobile.core.network.dto.PersonalFinanceSummaryDto;
import br.com.tresvtintas.mobile.core.network.dto.PersonalFinanceTypeTotalsDto;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

final class FinanceDtoMapper {
    private FinanceDtoMapper() {
    }

    static FinancePage page(PersonalFinancePageDto value) {
        return new FinancePage(
                value.items().stream()
                        .map(FinanceDtoMapper::summary)
                        .toList(),
                overview(value.overview()),
                Optional.ofNullable(value.nextCursor()));
    }

    static FinanceDetail detail(PersonalFinanceDetailDto value) {
        PersonalFinanceSummaryDto summaryDto = new PersonalFinanceSummaryDto(
                value.id(),
                value.type(),
                value.status(),
                value.source(),
                value.title(),
                value.amount(),
                value.currency(),
                value.dueAt(),
                value.settledAt(),
                value.customer(),
                value.allowedActions(),
                value.createdAt(),
                value.updatedAt());
        return new FinanceDetail(
                summary(summaryDto),
                Optional.ofNullable(value.notes()),
                Optional.ofNullable(value.payment())
                        .map(FinanceDtoMapper::payment));
    }

    private static FinanceSummary summary(PersonalFinanceSummaryDto value) {
        return new FinanceSummary(
                value.id(),
                enumValue(FinanceEntryType.class, value.type()),
                enumValue(FinanceEntryStatus.class, value.status()),
                enumValue(FinanceEntrySource.class, value.source()),
                value.title(),
                new BigDecimal(value.amount()),
                instant(value.dueAt()),
                instant(value.settledAt()),
                Optional.empty(),
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

    private static FinanceOverview overview(PersonalFinanceOverviewDto value) {
        return new FinanceOverview(
                totals(value.pending()),
                totals(value.settled()),
                totals(value.cancelled()),
                totals(value.overdue()));
    }

    private static FinanceOverview.TypeTotals totals(
            PersonalFinanceTypeTotalsDto value) {
        return new FinanceOverview.TypeTotals(
                money(value.expense()),
                money(value.payable()),
                money(value.receivable()));
    }

    private static FinanceOverview.MoneyTotal money(
            PersonalFinanceMoneyTotalDto value) {
        return new FinanceOverview.MoneyTotal(
                value.count(),
                new BigDecimal(value.amount()));
    }

    private static FinanceDetail.Payment payment(
            PersonalFinanceDetailDto.Payment value) {
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
