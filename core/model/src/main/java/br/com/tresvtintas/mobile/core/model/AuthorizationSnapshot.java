package br.com.tresvtintas.mobile.core.model;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.OptionalLong;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Immutable authorization projection returned by the API. It is suitable for presentation gating
 * only; every backend request remains independently authorized.
 */
public record AuthorizationSnapshot(
        Set<Capability> capabilities,
        OrganizationAccessMode organizationAccessMode,
        List<OrganizationScope> organizations,
        boolean organizationPageHasMore,
        OptionalLong defaultOrganizationId,
        String revision,
        int ignoredCapabilityCount) {
    private static final Pattern REVISION = Pattern.compile("^[0-9a-f]{64}$");

    public AuthorizationSnapshot {
        capabilities = Set.copyOf(Objects.requireNonNull(
                capabilities, "Capabilities are required."));
        Objects.requireNonNull(organizationAccessMode, "Organization access mode is required.");
        organizations = List.copyOf(Objects.requireNonNull(
                organizations, "Organizations are required."));
        defaultOrganizationId = defaultOrganizationId == null
                ? OptionalLong.empty()
                : defaultOrganizationId;
        if (ignoredCapabilityCount < 0) {
            throw new IllegalArgumentException("Ignored capability count cannot be negative.");
        }
        if (revision == null || !REVISION.matcher(revision).matches()) {
            throw new IllegalArgumentException("Authorization revision is invalid.");
        }
        validateOrganizationScope(
                organizationAccessMode,
                organizations,
                organizationPageHasMore,
                defaultOrganizationId);
    }

    public boolean has(Capability capability) {
        return capabilities.contains(Objects.requireNonNull(
                capability, "Capability is required."));
    }

    private static void validateOrganizationScope(
            OrganizationAccessMode mode,
            List<OrganizationScope> organizations,
            boolean hasMore,
            OptionalLong defaultOrganizationId) {
        if (mode != OrganizationAccessMode.ASSIGNED
                && (!organizations.isEmpty() || hasMore || defaultOrganizationId.isPresent())) {
            throw new IllegalArgumentException(
                    "Only assigned organization access may enumerate organizations.");
        }
        Set<Long> identifiers = new HashSet<>();
        for (OrganizationScope organization : organizations) {
            if (!identifiers.add(organization.id())) {
                throw new IllegalArgumentException("Organization IDs must be unique.");
            }
        }
        if (defaultOrganizationId.isPresent()
                && (hasMore || !identifiers.contains(defaultOrganizationId.getAsLong()))) {
            throw new IllegalArgumentException(
                    "Default organization must be present in the complete organization list.");
        }
    }
}
