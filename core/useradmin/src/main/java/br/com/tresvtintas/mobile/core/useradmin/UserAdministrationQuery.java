package br.com.tresvtintas.mobile.core.useradmin;

import br.com.tresvtintas.mobile.core.model.AppRole;
import java.util.Optional;
import java.util.OptionalLong;

public record UserAdministrationQuery(
        OptionalLong organizationId,
        Optional<AppRole> role,
        Optional<UserAdministrationStatus> status,
        Optional<String> search,
        int pageSize) {
    public UserAdministrationQuery {
        organizationId = organizationId == null ? OptionalLong.empty() : organizationId;
        role = role == null ? Optional.empty() : role;
        status = status == null ? Optional.empty() : status;
        search = search == null
                ? Optional.empty()
                : search.map(String::strip).filter(value -> !value.isEmpty());
        if ((organizationId.isPresent() && organizationId.orElseThrow() < 1)
                || search.map(String::length).orElse(0) > 80
                || pageSize < 1
                || pageSize > 100) {
            throw new IllegalArgumentException("User administration query is invalid.");
        }
    }

    public static UserAdministrationQuery initial() {
        return new UserAdministrationQuery(
                OptionalLong.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                30);
    }

    public UserAdministrationQuery withOrganization(OptionalLong value) {
        return new UserAdministrationQuery(value, role, status, search, pageSize);
    }

    public UserAdministrationQuery withRole(Optional<AppRole> value) {
        return new UserAdministrationQuery(organizationId, value, status, search, pageSize);
    }

    public UserAdministrationQuery withStatus(Optional<UserAdministrationStatus> value) {
        return new UserAdministrationQuery(organizationId, role, value, search, pageSize);
    }

    public UserAdministrationQuery withSearch(String value) {
        return new UserAdministrationQuery(
                organizationId,
                role,
                status,
                Optional.ofNullable(value),
                pageSize);
    }
}
