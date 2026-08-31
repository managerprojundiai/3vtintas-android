package br.com.tresvtintas.mobile.core.network.dto;

import java.util.List;

public record OrganizationAccess(
        String mode,
        List<OrganizationScopeDto> organizations,
        boolean hasMore,
        Long defaultOrganizationId) {
    private static final int MAXIMUM_ORGANIZATIONS = 200;
    private static final long MINIMUM_IDENTIFIER = 1L;

    public OrganizationAccess {
        mode = DtoValidation.requireText(mode, "Organization access mode", 20);
        if (organizations == null) {
            throw new IllegalArgumentException("Organizations are required.");
        }
        organizations = List.copyOf(organizations);
        if (organizations.size() > MAXIMUM_ORGANIZATIONS) {
            throw new IllegalArgumentException("Organization list is too large.");
        }
        if (defaultOrganizationId != null && defaultOrganizationId < MINIMUM_IDENTIFIER) {
            throw new IllegalArgumentException("Default organization ID must be positive.");
        }
    }
}
