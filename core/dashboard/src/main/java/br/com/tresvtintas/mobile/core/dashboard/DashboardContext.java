package br.com.tresvtintas.mobile.core.dashboard;

import java.util.Objects;
import java.util.Optional;

public record DashboardContext(
        DashboardVisibility visibility,
        Optional<Organization> organization) {

    public DashboardContext {
        Objects.requireNonNull(visibility, "Dashboard visibility is required.");
        organization = organization == null
                ? Optional.empty()
                : organization;
    }

    public record Organization(long id, String name) {
        public Organization {
            if (id < 1 || name == null || name.isBlank()) {
                throw new IllegalArgumentException(
                        "Dashboard organization is invalid.");
            }
        }
    }
}
