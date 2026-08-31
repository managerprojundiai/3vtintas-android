package br.com.tresvtintas.mobile.data.organizationadmin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import br.com.tresvtintas.mobile.core.network.dto.OrganizationAdministrationDtos;
import br.com.tresvtintas.mobile.core.organizationadmin.OrganizationAdministrationStatus;
import java.util.List;
import org.junit.Test;

public final class OrganizationAdministrationDtoMapperTest {
    @Test
    public void mapperPreservesSafeOrganizationFields() {
        var page = OrganizationAdministrationDtoMapper.page(
                new OrganizationAdministrationDtos.Page(
                        List.of(organization("active")),
                        null));

        assertEquals(
                "The mapper must preserve the page item count",
                1,
                page.items().size());
        assertEquals(
                "The mapper must preserve the validated status",
                OrganizationAdministrationStatus.ACTIVE,
                page.items().get(0).status());
        assertEquals(
                "The mapper must preserve the salesperson summary",
                2,
                page.items().get(0).salespersonCount());
    }

    @Test
    public void mapperFailsClosedForUnknownStatus() {
        assertThrows(
                "Unknown backend status values must fail closed",
                IllegalArgumentException.class,
                () -> OrganizationAdministrationDtoMapper.organization(
                        organization("unknown")));
    }

    private static OrganizationAdministrationDtos.Organization organization(
            String status) {
        return new OrganizationAdministrationDtos.Organization(
                1,
                "3v-jundiai",
                "3V Jundiaí",
                status,
                1,
                4,
                1,
                2,
                1,
                "2026-07-30T12:00:00Z",
                "2026-07-30T12:00:00Z");
    }
}
