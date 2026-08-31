package br.com.tresvtintas.mobile.core.organizationadmin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import br.com.tresvtintas.mobile.core.organizationadmin.OrganizationAdministrationModels.Organization;
import java.time.Instant;
import java.util.Optional;
import org.junit.Test;

public final class OrganizationAdministrationModelsTest {
    private static final Instant NOW = Instant.parse("2026-07-30T12:00:00Z");

    @Test
    public void organizationPreservesAuditableSummary() {
        Organization organization = organization();

        assertEquals(
                "The auditable summary must preserve the display name",
                "3V Jundiaí",
                organization.name());
        assertEquals(
                "The auditable summary must preserve active members",
                4,
                organization.activeMemberCount());
        assertEquals(
                "The auditable summary must preserve manager count",
                1,
                organization.managerCount());
        assertTrue(
                "An active organization must accept a status transition",
                OrganizationAdministrationModels.isMutableStatus(
                        organization.status()));
    }

    @Test
    public void organizationRejectsInconsistentCounts() {
        assertThrows(
                "Role counts cannot exceed the active member total",
                IllegalArgumentException.class,
                () -> new Organization(
                        1,
                        "3v-jundiai",
                        "3V Jundiaí",
                        OrganizationAdministrationStatus.ACTIVE,
                        1,
                        2,
                        1,
                        1,
                        1,
                        NOW,
                        NOW));
    }

    @Test
    public void queryNormalizesSearchAndBoundsPage() {
        OrganizationAdministrationQuery query =
                OrganizationAdministrationQuery.initial()
                        .withStatus(Optional.of(
                                OrganizationAdministrationStatus.ACTIVE))
                        .withSearch("  Jundiaí  ");

        assertEquals(
                "Search text must be normalized before transport",
                "Jundiaí",
                query.search().orElseThrow());
        assertEquals(
                "The selected status must survive query normalization",
                OrganizationAdministrationStatus.ACTIVE,
                query.status().orElseThrow());
        assertThrows(
                "Page sizes above the contract limit must fail closed",
                IllegalArgumentException.class,
                () -> new OrganizationAdministrationQuery(
                        Optional.empty(),
                        Optional.empty(),
                        101));
    }

    private static Organization organization() {
        return new Organization(
                1,
                "3v-jundiai",
                "3V Jundiaí",
                OrganizationAdministrationStatus.ACTIVE,
                1,
                4,
                1,
                2,
                1,
                NOW,
                NOW);
    }
}
