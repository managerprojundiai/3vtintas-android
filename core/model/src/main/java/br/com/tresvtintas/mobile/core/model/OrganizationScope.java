package br.com.tresvtintas.mobile.core.model;

import java.util.Objects;

public record OrganizationScope(
        long id,
        String name,
        String slug,
        OrganizationMembershipRole membershipRole) {
    private static final long MINIMUM_IDENTIFIER = 1L;

    public OrganizationScope {
        if (id < MINIMUM_IDENTIFIER) {
            throw new IllegalArgumentException("Organization ID must be positive.");
        }
        name = requireText(name, "Organization name", 200);
        slug = requireText(slug, "Organization slug", 120);
        Objects.requireNonNull(membershipRole, "Organization membership role is required.");
    }

    private static String requireText(String value, String label, int maximumLength) {
        if (value == null || value.isBlank() || value.length() > maximumLength) {
            throw new IllegalArgumentException(label + " is invalid.");
        }
        return value;
    }
}
