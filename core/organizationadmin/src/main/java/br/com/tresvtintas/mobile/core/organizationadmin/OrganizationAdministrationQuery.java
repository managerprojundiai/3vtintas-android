package br.com.tresvtintas.mobile.core.organizationadmin;

import java.util.Optional;

public record OrganizationAdministrationQuery(
        Optional<OrganizationAdministrationStatus> status,
        Optional<String> search,
        int pageSize) {
    public OrganizationAdministrationQuery {
        status = status == null ? Optional.empty() : status;
        search = search == null
                ? Optional.empty()
                : search.map(String::strip).filter(value -> !value.isEmpty());
        if (search.map(String::length).orElse(0) > 80
                || pageSize < 1
                || pageSize > 100) {
            throw new IllegalArgumentException(
                    "Organization administration query is invalid.");
        }
    }

    public static OrganizationAdministrationQuery initial() {
        return new OrganizationAdministrationQuery(
                Optional.empty(),
                Optional.empty(),
                30);
    }

    public OrganizationAdministrationQuery withStatus(
            Optional<OrganizationAdministrationStatus> value) {
        return new OrganizationAdministrationQuery(value, search, pageSize);
    }

    public OrganizationAdministrationQuery withSearch(String value) {
        return new OrganizationAdministrationQuery(
                status,
                Optional.ofNullable(value),
                pageSize);
    }
}
