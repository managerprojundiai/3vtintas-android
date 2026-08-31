package br.com.tresvtintas.mobile.core.network.dto;

public record OrganizationScopeDto(
        long id,
        String name,
        String slug,
        String membershipRole) {
    private static final long MINIMUM_IDENTIFIER = 1L;

    public OrganizationScopeDto {
        if (id < MINIMUM_IDENTIFIER) {
            throw new IllegalArgumentException("Organization ID must be positive.");
        }
        name = DtoValidation.requireText(name, "Organization name", 200);
        slug = DtoValidation.requireText(slug, "Organization slug", 120);
        membershipRole = DtoValidation.requireText(
                membershipRole, "Organization membership role", 40);
    }
}
