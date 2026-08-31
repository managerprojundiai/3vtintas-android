package br.com.tresvtintas.mobile.core.painteradmin;

import java.util.Optional;
import java.util.OptionalLong;

public record PainterAdministrationQuery(
        OptionalLong organizationId,
        Optional<PainterAdministrationStatus> status,
        Optional<String> search,
        int pageSize) {
    public PainterAdministrationQuery {
        organizationId = organizationId == null
                ? OptionalLong.empty()
                : organizationId;
        status = status == null ? Optional.empty() : status;
        search = search == null ? Optional.empty() : search
                .map(String::strip)
                .filter(value -> !value.isEmpty());
        if (organizationId.isPresent()
                && organizationId.orElseThrow() < 1
                || search.map(String::length).orElse(0) > 80
                || pageSize < 1
                || pageSize > 100) {
            throw new IllegalArgumentException(
                    "Painter administration query is invalid.");
        }
    }

    public static PainterAdministrationQuery initial() {
        return new PainterAdministrationQuery(
                OptionalLong.empty(),
                Optional.empty(),
                Optional.empty(),
                30);
    }

    public PainterAdministrationQuery withSearch(String value) {
        return new PainterAdministrationQuery(
                organizationId,
                status,
                Optional.ofNullable(value),
                pageSize);
    }

    public PainterAdministrationQuery withOrganization(
            OptionalLong value) {
        return new PainterAdministrationQuery(value, status, search, pageSize);
    }

    public PainterAdministrationQuery withStatus(
            Optional<PainterAdministrationStatus> value) {
        return new PainterAdministrationQuery(
                organizationId,
                value,
                search,
                pageSize);
    }
}
