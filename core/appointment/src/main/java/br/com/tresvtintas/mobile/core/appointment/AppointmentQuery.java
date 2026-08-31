package br.com.tresvtintas.mobile.core.appointment;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalLong;

public record AppointmentQuery(
        AppointmentScope scope,
        OptionalLong organizationId,
        Optional<String> search,
        Optional<AppointmentStatus> status,
        Optional<AppointmentKind> kind,
        Instant from,
        Instant toExclusive,
        int pageSize) {
    public AppointmentQuery {
        organizationId = organizationId == null
                ? OptionalLong.empty()
                : organizationId;
        search = search == null ? Optional.empty() : search;
        search = search.map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(value -> value.toLowerCase(Locale.ROOT));
        status = status == null ? Optional.empty() : status;
        kind = kind == null ? Optional.empty() : kind;
        if (scope == null
                || (organizationId.isPresent()
                        && organizationId.orElseThrow() < 1)
                || search.map(String::length).orElse(0) > 100
                || from == null
                || toExclusive == null
                || pageSize < 1
                || pageSize > 100) {
            throw new IllegalArgumentException(
                    "Appointment query is invalid.");
        }
        Duration period = Duration.between(from, toExclusive);
        if (period.isZero()
                || period.isNegative()
                || period.compareTo(Duration.ofDays(366)) > 0) {
            throw new IllegalArgumentException(
                    "Appointment period is invalid.");
        }
    }

    public static AppointmentQuery around(
            AppointmentScope scope,
            Instant now) {
        Instant reference = java.util.Objects.requireNonNull(
                now,
                "Appointment reference instant is required.");
        return new AppointmentQuery(
                scope,
                OptionalLong.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                reference.minus(30, ChronoUnit.DAYS),
                reference.plus(180, ChronoUnit.DAYS),
                30);
    }

    public AppointmentQuery withSearch(String value) {
        return copy(
                organizationId,
                Optional.ofNullable(value),
                status,
                kind);
    }

    public AppointmentQuery withStatus(AppointmentStatus value) {
        return copy(
                organizationId,
                search,
                Optional.ofNullable(value),
                kind);
    }

    public AppointmentQuery withKind(AppointmentKind value) {
        return copy(
                organizationId,
                search,
                status,
                Optional.ofNullable(value));
    }

    public AppointmentQuery withOrganization(OptionalLong value) {
        return copy(value, search, status, kind);
    }

    private AppointmentQuery copy(
            OptionalLong nextOrganization,
            Optional<String> nextSearch,
            Optional<AppointmentStatus> nextStatus,
            Optional<AppointmentKind> nextKind) {
        return new AppointmentQuery(
                scope,
                nextOrganization,
                nextSearch,
                nextStatus,
                nextKind,
                from,
                toExclusive,
                pageSize);
    }
}
