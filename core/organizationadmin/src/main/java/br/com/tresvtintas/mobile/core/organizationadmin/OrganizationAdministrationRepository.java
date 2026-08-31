package br.com.tresvtintas.mobile.core.organizationadmin;

import br.com.tresvtintas.mobile.core.organizationadmin.OrganizationAdministrationModels.Mutation;
import br.com.tresvtintas.mobile.core.organizationadmin.OrganizationAdministrationModels.Organization;
import br.com.tresvtintas.mobile.core.organizationadmin.OrganizationAdministrationModels.Page;
import java.util.Optional;

public interface OrganizationAdministrationRepository {
    Page organizations(
            OrganizationAdministrationQuery query,
            Optional<String> cursor) throws OrganizationAdministrationException;

    Organization organization(long organizationId)
            throws OrganizationAdministrationException;

    Mutation create(String name, String slug, String idempotencyKey)
            throws OrganizationAdministrationException;

    Mutation rename(
            long organizationId,
            int expectedRevision,
            String name,
            String idempotencyKey) throws OrganizationAdministrationException;

    Mutation setStatus(
            long organizationId,
            int expectedRevision,
            OrganizationAdministrationStatus status,
            String idempotencyKey) throws OrganizationAdministrationException;
}
