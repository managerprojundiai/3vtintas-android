package br.com.tresvtintas.mobile.data.systemconfiguration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import br.com.tresvtintas.mobile.core.network.dto.SystemConfigurationDtos;
import br.com.tresvtintas.mobile.core.systemconfiguration.SystemConfigurationModels.Snapshot;
import br.com.tresvtintas.mobile.core.systemconfiguration.SystemConfigurationModels.Values;
import org.junit.Test;

public final class SystemConfigurationDtoMapperTest {
    @Test
    public void mapsOnlySafeConfigurationFields() {
        SystemConfigurationDtos.Configuration dto =
                new SystemConfigurationDtos.Configuration(
                        "3V Tintas Jundiaí",
                        "11 4000-0000",
                        "Rua das Tintas, 3",
                        "Bellarte Pinturas",
                        "11 4999-0000",
                        "3.00",
                        false,
                        7,
                        "2026-07-31T12:00:00Z");

        Snapshot result = SystemConfigurationDtoMapper.snapshot(dto);

        assertEquals("Revision must be mapped.", 7, result.revision());
        assertEquals(
                "Commission must be mapped.",
                "3.00",
                result.values().defaultCommissionRate());
        assertFalse(
                "Approval policy must be mapped.",
                result.values().autoApprovePainters());
    }

    @Test
    public void updateRequestRequiresExplicitConfirmation() {
        Values values = new Values(
                "3V Tintas Jundiaí",
                "11 4000-0000",
                "Rua das Tintas, 3",
                "Bellarte Pinturas",
                "11 4999-0000",
                "4.00",
                true);

        SystemConfigurationDtos.UpdateRequest request =
                SystemConfigurationDtoMapper.request(values, 9);

        assertEquals(
                "Expected revision must be retained.",
                9,
                request.expectedRevision());
        assertEquals(
                "Commission must be retained.",
                "4.00",
                request.defaultCommissionRate());
        assertTrue("Update must be explicitly confirmed.", request.confirmed());
    }
}
