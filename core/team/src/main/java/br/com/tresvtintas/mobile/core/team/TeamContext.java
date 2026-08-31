package br.com.tresvtintas.mobile.core.team;

import java.util.Objects;
import java.util.Optional;

public record TeamContext(
        Visibility visibility,
        Optional<Organization> organization) {
    public TeamContext {
        Objects.requireNonNull(visibility, "Team visibility is required.");
        organization = organization == null ? Optional.empty() : organization;
        if (visibility == Visibility.TEAM && organization.isEmpty()) {
            throw new IllegalArgumentException(
                    "Team visibility requires an organization.");
        }
    }

    public enum Visibility {
        TEAM,
        ALL
    }

    public record Organization(long id, String name) {
        public Organization {
            if (id < 1 || name == null || name.isBlank()) {
                throw new IllegalArgumentException(
                        "Team organization is invalid.");
            }
        }
    }
}
