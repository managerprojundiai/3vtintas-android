package br.com.tresvtintas.mobile.core.bootstrap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import br.com.tresvtintas.mobile.core.model.AppRole;
import br.com.tresvtintas.mobile.core.model.Capability;
import br.com.tresvtintas.mobile.core.network.dto.BootstrapApiStatus;
import br.com.tresvtintas.mobile.core.network.dto.BootstrapResponse;
import br.com.tresvtintas.mobile.core.network.dto.SessionIdentity;
import java.util.List;
import org.junit.Test;

public final class BootstrapMapperTest {
    private static final String CONTRACT_VERSION = "0.5.0";
    private static final String SERVER_TIME = "2026-07-25T13:00:00Z";
    private final BootstrapMapper mapper = new BootstrapMapper(
            BootstrapTestFixtures.compatibility());

    @Test
    public void mapsServerAuthorizationAndIgnoresUnknownCapabilitiesFailClosed() throws Exception {
        BootstrapSnapshot snapshot = mapper.map(
                BootstrapTestFixtures.response(),
                BootstrapTestFixtures.expected());

        assertEquals("Server role must be preserved.", AppRole.SALESPERSON, snapshot.role());
        assertTrue(
                "Catalog capability must be granted.",
                snapshot.authorization().has(Capability.CATALOG_READ));
        assertTrue(
                "Quote capability must be granted.",
                snapshot.authorization().has(Capability.QUOTE_CREATE));
        assertEquals(
                "Unknown capabilities must be counted.",
                1,
                snapshot.authorization().ignoredCapabilityCount());
        assertEquals(
                "Default organization must be mapped.",
                9,
                snapshot.authorization().defaultOrganizationId().orElseThrow());
    }

    @Test
    public void rejectsIdentityMismatchInsteadOfDisplayingAnotherSession() {
        BootstrapResponse source = BootstrapTestFixtures.response();
        BootstrapResponse mismatched = new BootstrapResponse(
                source.user(),
                new SessionIdentity(
                        "00000000-0000-4000-8000-000000000099",
                        BootstrapTestFixtures.DEVICE_ID),
                source.authorization(),
                source.api());

        BootstrapException failure = assertThrows(
                "Mismatched identity must be rejected.",
                BootstrapException.class,
                () -> mapper.map(mismatched, BootstrapTestFixtures.expected()));

        assertEquals(
                "Identity mismatch is a protocol failure.",
                BootstrapFailureKind.PROTOCOL,
                failure.kind());
    }

    @Test
    public void enforcesMaintenanceAndClientCompatibilityLocally() {
        assertFailure(
                new BootstrapApiStatus(
                        "v1", CONTRACT_VERSION, "0.1.0", 2, 26, true, SERVER_TIME),
                BootstrapFailureKind.MAINTENANCE);
        assertFailure(
                new BootstrapApiStatus(
                        "v1", CONTRACT_VERSION, "9.0.0", 6, 26, false, SERVER_TIME),
                BootstrapFailureKind.UPDATE_REQUIRED);
        assertFailure(
                new BootstrapApiStatus(
                        "v1", CONTRACT_VERSION, "0.1.0", 2, 36, false, SERVER_TIME),
                BootstrapFailureKind.DEVICE_UNSUPPORTED);
        assertFailure(
                new BootstrapApiStatus(
                        "v2", CONTRACT_VERSION, "0.1.0", 2, 26, false, SERVER_TIME),
                BootstrapFailureKind.PROTOCOL);
    }

    @Test
    public void rejectsDuplicateCapabilitiesAsContractViolation() {
        BootstrapException failure = assertThrows(
                "Duplicate capabilities must be rejected.",
                BootstrapException.class,
                () -> mapper.map(
                        BootstrapTestFixtures.response(
                                BootstrapTestFixtures.response().api(),
                                List.of("catalog.read", "catalog.read")),
                        BootstrapTestFixtures.expected()));

        assertEquals(
                "Duplicate capability is a protocol failure.",
                BootstrapFailureKind.PROTOCOL,
                failure.kind());
    }

    private void assertFailure(BootstrapApiStatus api, BootstrapFailureKind expectedKind) {
        BootstrapException failure = assertThrows(
                "Compatibility failure must be surfaced.",
                BootstrapException.class,
                () -> mapper.map(
                        BootstrapTestFixtures.response(api, List.of("catalog.read")),
                        BootstrapTestFixtures.expected()));
        assertEquals("Failure kind must match the gate.", expectedKind, failure.kind());
    }
}
