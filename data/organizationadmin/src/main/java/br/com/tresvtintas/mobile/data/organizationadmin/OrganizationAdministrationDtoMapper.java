package br.com.tresvtintas.mobile.data.organizationadmin;

import br.com.tresvtintas.mobile.core.network.dto.OrganizationAdministrationDtos;
import br.com.tresvtintas.mobile.core.organizationadmin.OrganizationAdministrationModels.Mutation;
import br.com.tresvtintas.mobile.core.organizationadmin.OrganizationAdministrationModels.Organization;
import br.com.tresvtintas.mobile.core.organizationadmin.OrganizationAdministrationModels.Page;
import br.com.tresvtintas.mobile.core.organizationadmin.OrganizationAdministrationStatus;
import java.time.Instant;
import java.util.Optional;

final class OrganizationAdministrationDtoMapper {
    private OrganizationAdministrationDtoMapper() {
        throw new AssertionError("No instances.");
    }

    static Page page(OrganizationAdministrationDtos.Page dto) {
        return new Page(
                dto.items().stream()
                        .map(OrganizationAdministrationDtoMapper::organization)
                        .toList(),
                Optional.ofNullable(dto.nextCursor()));
    }

    static Organization organization(
            OrganizationAdministrationDtos.Organization dto) {
        return new Organization(
                dto.id(),
                dto.slug(),
                dto.name(),
                OrganizationAdministrationStatus.fromWireValue(dto.status())
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Organization status is unsupported.")),
                dto.revision(),
                dto.activeMemberCount(),
                dto.managerCount(),
                dto.salespersonCount(),
                dto.deliveryDriverCount(),
                Instant.parse(dto.createdAt()),
                Instant.parse(dto.updatedAt()));
    }

    static Mutation mutation(
            OrganizationAdministrationDtos.Mutation dto,
            boolean replayed) {
        return new Mutation(
                dto.resourceId(),
                dto.revision(),
                dto.changed(),
                replayed);
    }
}
