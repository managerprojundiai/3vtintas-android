package br.com.tresvtintas.mobile.feature.organizationadmin;

import br.com.tresvtintas.mobile.core.organizationadmin.OrganizationAdministrationModels.Organization;
import br.com.tresvtintas.mobile.core.organizationadmin.OrganizationAdministrationStatus;
import java.util.Objects;

record OrganizationAdministrationStatusConfirmation(
        OrganizationAdministrationStatus target,
        int titleResource,
        int messageResource,
        int actionResource) {

    OrganizationAdministrationStatusConfirmation {
        Objects.requireNonNull(target, "Target status is required.");
    }

    static OrganizationAdministrationStatusConfirmation forOrganization(
            Organization organization) {
        Organization required = Objects.requireNonNull(
                organization,
                "Organization is required.");
        if (required.status() == OrganizationAdministrationStatus.ACTIVE) {
            return new OrganizationAdministrationStatusConfirmation(
                    OrganizationAdministrationStatus.BLOCKED,
                    R.string.organization_admin_block_confirmation_title,
                    R.string.organization_admin_block_confirmation_message,
                    R.string.organization_admin_block);
        }
        if (required.status() == OrganizationAdministrationStatus.BLOCKED) {
            return new OrganizationAdministrationStatusConfirmation(
                    OrganizationAdministrationStatus.ACTIVE,
                    R.string.organization_admin_activate_confirmation_title,
                    R.string.organization_admin_activate_confirmation_message,
                    R.string.organization_admin_activate);
        }
        throw new IllegalArgumentException(
                "Only active or blocked organizations can change status.");
    }
}
