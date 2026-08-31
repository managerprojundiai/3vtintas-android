package br.com.tresvtintas.mobile.core.finance;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;

public record FinanceQuery(
        Optional<String> search,
        Optional<FinanceEntryStatus> status,
        Optional<FinanceEntryType> type,
        Optional<FinanceEntrySource> source,
        FinanceDueFilter due,
        FinanceDateBasis dateBasis,
        Optional<Instant> dueFrom,
        Optional<Instant> dueToExclusive,
        int pageSize) {
    public FinanceQuery {
        search = search == null ? Optional.empty() : search;
        search = search.map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(value -> value.toLowerCase(Locale.ROOT));
        status = status == null ? Optional.empty() : status;
        type = type == null ? Optional.empty() : type;
        source = source == null ? Optional.empty() : source;
        dueFrom = dueFrom == null ? Optional.empty() : dueFrom;
        dueToExclusive = dueToExclusive == null
                ? Optional.empty()
                : dueToExclusive;
        if (due == null
                || dateBasis == null
                || search.map(String::length).orElse(0) > 100
                || pageSize < 1
                || pageSize > 100
                || (due == FinanceDueFilter.WITHOUT_DUE_DATE
                        && (dueFrom.isPresent() || dueToExclusive.isPresent()))
                || (dateBasis == FinanceDateBasis.AGENDA
                        && due != FinanceDueFilter.ALL)) {
            throw new IllegalArgumentException("Finance query is invalid.");
        }
        if (dueFrom.isPresent() && dueToExclusive.isPresent()) {
            Duration period = Duration.between(
                    dueFrom.orElseThrow(),
                    dueToExclusive.orElseThrow());
            if (period.isNegative() || period.compareTo(Duration.ofDays(366)) > 0) {
                throw new IllegalArgumentException("Finance period is invalid.");
            }
        }
    }

    public static FinanceQuery initial() {
        return new FinanceQuery(
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                FinanceDueFilter.ALL,
                FinanceDateBasis.DUE,
                Optional.empty(),
                Optional.empty(),
                30);
    }

    public FinanceQuery withSearch(String value) {
        return new FinanceQuery(
                Optional.ofNullable(value),
                status,
                type,
                source,
                due,
                dateBasis,
                dueFrom,
                dueToExclusive,
                pageSize);
    }

    public FinanceQuery withStatus(FinanceEntryStatus value) {
        return new FinanceQuery(
                search,
                Optional.ofNullable(value),
                type,
                source,
                due,
                dateBasis,
                dueFrom,
                dueToExclusive,
                pageSize);
    }

    public FinanceQuery withType(FinanceEntryType value) {
        return new FinanceQuery(
                search,
                status,
                Optional.ofNullable(value),
                source,
                due,
                dateBasis,
                dueFrom,
                dueToExclusive,
                pageSize);
    }

    public FinanceQuery withSource(FinanceEntrySource value) {
        return new FinanceQuery(
                search,
                status,
                type,
                Optional.ofNullable(value),
                due,
                dateBasis,
                dueFrom,
                dueToExclusive,
                pageSize);
    }

    public FinanceQuery withDue(FinanceDueFilter value) {
        return new FinanceQuery(
                search,
                status,
                type,
                source,
                value,
                FinanceDateBasis.DUE,
                Optional.empty(),
                Optional.empty(),
                pageSize);
    }

    public FinanceQuery forAgendaWindow(
            Instant from,
            Instant toExclusive) {
        return new FinanceQuery(
                search,
                status,
                type,
                source,
                FinanceDueFilter.ALL,
                FinanceDateBasis.AGENDA,
                Optional.ofNullable(from),
                Optional.ofNullable(toExclusive),
                pageSize);
    }
}
