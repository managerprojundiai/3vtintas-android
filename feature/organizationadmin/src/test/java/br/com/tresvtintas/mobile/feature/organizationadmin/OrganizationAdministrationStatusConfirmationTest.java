package br.com.tresvtintas.mobile.feature.organizationadmin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import br.com.tresvtintas.mobile.core.organizationadmin.OrganizationAdministrationModels.Organization;
import br.com.tresvtintas.mobile.core.organizationadmin.OrganizationAdministrationStatus;
import java.time.Instant;
import org.junit.Test;

public final class OrganizationAdministrationStatusConfirmationTest {
    private static final Instant NOW = Instant.parse("2026-07-30T12:00:00Z");

    @Test
    public void activeOrganizationRequiresExplicitBlockConfirmation() {
        var confirmation = OrganizationAdministrationStatusConfirmation
                .forOrganization(organization(OrganizationAdministrationStatus.ACTIVE));

        assertEquals(
                "Target status must be blocked.",
                OrganizationAdministrationStatus.BLOCKED,
                confirmation.target());
        assertEquals(
                "Block title must identify the destructive action.",
                R.string.organization_admin_block_confirmation_title,
                confirmation.titleResource());
        assertEquals(
                "Block message must explain the consequence.",
                R.string.organization_admin_block_confirmation_message,
                confirmation.messageResource());
        assertEquals(
                "Block action must be explicit.",
                R.string.organization_admin_block,
                confirmation.actionResource());
    }

    @Test
    public void blockedOrganizationRequiresExplicitActivationConfirmation() {
        var confirmation = OrganizationAdministrationStatusConfirmation
                .forOrganization(organization(OrganizationAdministrationStatus.BLOCKED));

        assertEquals(
                "Target status must be active.",
                OrganizationAdministrationStatus.ACTIVE,
                confirmation.target());
        assertEquals(
                "Activation title must identify the action.",
                R.string.organization_admin_activate_confirmation_title,
                confirmation.titleResource());
        assertEquals(
                "Activation message must explain the consequence.",
                R.string.organization_admin_activate_confirmation_message,
                confirmation.messageResource());
        assertEquals(
                "Activation action must be explicit.",
                R.string.organization_admin_activate,
                confirmation.actionResource());
    }

    @Test
    public void pendingOrganizationCannotOfferAStatusMutation() {
        assertThrows(
                "Pending organizations must not expose an unsupported transition.",
                IllegalArgumentException.class,
                () -> OrganizationAdministrationStatusConfirmation.forOrganization(
                        organization(OrganizationAdministrationStatus.PENDING)));
    }

    private static Organization organization(
            OrganizationAdministrationStatus status) {
        return new Organization(
                1L,
                "bellarte-pinturas",
                "Bellarte Pinturas",
                status,
                2,
                3,
                1,
                2,
                0,
                NOW,
                NOW);
    }
}
