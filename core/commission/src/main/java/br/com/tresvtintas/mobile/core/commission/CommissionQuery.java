package br.com.tresvtintas.mobile.core.commission;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalLong;

public record CommissionQuery(
        CommissionScope scope,
        Optional<String> search,
        Optional<CommissionStatus> status,
        Optional<CommissionKind> kind,
        OptionalLong organizationId,
        Optional<LocalDate> from,
        Optional<LocalDate> to,
        int pageSize) {
    public CommissionQuery {
        if (scope == null) {
            throw new IllegalArgumentException("Commission scope is required.");
        }
        search = search == null ? Optional.empty() : search;
        search = search.map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(value -> value.toLowerCase(Locale.ROOT));
        status = status == null ? Optional.empty() : status;
        kind = kind == null ? Optional.empty() : kind;
        organizationId = organizationId == null ? OptionalLong.empty() : organizationId;
        from = from == null ? Optional.empty() : from;
        to = to == null ? Optional.empty() : to;
        if (search.map(String::length).orElse(0) > 80
                || (organizationId.isPresent() && organizationId.orElseThrow() < 1)
                || from.isPresent() != to.isPresent()
                || pageSize < 1
                || pageSize > 100) {
            throw new IllegalArgumentException("Commission query is invalid.");
        }
        if (from.isPresent()) {
            long days = ChronoUnit.DAYS.between(from.orElseThrow(), to.orElseThrow());
            if (days < 0 || days > 366) {
                throw new IllegalArgumentException("Commission period is invalid.");
            }
        }
    }

    public static CommissionQuery initial(CommissionScope scope) {
        return new CommissionQuery(
                scope,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                OptionalLong.empty(),
                Optional.empty(),
                Optional.empty(),
                30);
    }

    public CommissionQuery withSearch(String value) {
        return new CommissionQuery(
                scope,
                Optional.ofNullable(value),
                status,
                kind,
                organizationId,
                from,
                to,
                pageSize);
    }

    public CommissionQuery withStatus(CommissionStatus value) {
        return new CommissionQuery(
                scope,
                search,
                Optional.ofNullable(value),
                kind,
                organizationId,
                from,
                to,
                pageSize);
    }

    public CommissionQuery withOrganization(OptionalLong value) {
        return new CommissionQuery(
                scope,
                search,
                status,
                kind,
                value,
                from,
                to,
                pageSize);
    }
}
