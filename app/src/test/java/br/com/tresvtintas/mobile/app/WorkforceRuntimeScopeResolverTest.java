package br.com.tresvtintas.mobile.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import br.com.tresvtintas.mobile.core.bootstrap.BootstrapSnapshot;
import br.com.tresvtintas.mobile.core.model.AppRole;
import br.com.tresvtintas.mobile.core.model.AuthorizationSnapshot;
import br.com.tresvtintas.mobile.core.model.Capability;
import br.com.tresvtintas.mobile.core.model.OrganizationAccessMode;
import br.com.tresvtintas.mobile.core.model.OrganizationMembershipRole;
import br.com.tresvtintas.mobile.core.model.OrganizationScope;
import br.com.tresvtintas.mobile.core.network.dto.AuthenticatedUser;
import br.com.tresvtintas.mobile.core.network.dto.SessionIdentity;
import java.time.Instant;
import java.util.List;
import java.util.OptionalLong;
import java.util.Set;
import org.junit.Test;

public final class WorkforceRuntimeScopeResolverTest {
    @Test
    public void defaultStoreIsRecoveredFromCurrentAuthorization() {
        OptionalLong result = WorkforceRuntimeScopeResolver.resolve(bootstrap(
                Set.of(Capability.LOCATION_TRACK_SELF),
                List.of(store(9), store(10)),
                OptionalLong.of(10),
                false));

        assertEquals("The authorized default store must resume.", 10L, result.orElseThrow());
    }

    @Test
    public void soleAssignedStoreResumesWithoutDailySelection() {
        OptionalLong result = WorkforceRuntimeScopeResolver.resolve(bootstrap(
                Set.of(Capability.LOCATION_TRACK_SELF),
                List.of(store(9)),
                OptionalLong.empty(),
                false));

        assertEquals("A sole authorized store is unambiguous.", 9L, result.orElseThrow());
    }

    @Test
    public void missingCapabilityFailsClosed() {
        OptionalLong result = WorkforceRuntimeScopeResolver.resolve(bootstrap(
                Set.of(Capability.CATALOG_READ),
                List.of(store(9)),
                OptionalLong.of(9),
                false));

        assertFalse("A store cannot imply tracking permission.", result.isPresent());
    }

    @Test
    public void ambiguousOrIncompleteScopeFailsClosed() {
        OptionalLong ambiguous = WorkforceRuntimeScopeResolver.resolve(bootstrap(
                Set.of(Capability.LOCATION_TRACK_SELF),
                List.of(store(9), store(10)),
                OptionalLong.empty(),
                false));
        OptionalLong incomplete = WorkforceRuntimeScopeResolver.resolve(bootstrap(
                Set.of(Capability.LOCATION_TRACK_SELF),
                List.of(store(9)),
                OptionalLong.empty(),
                true));

        assertFalse("Multiple stores require an explicit default.", ambiguous.isPresent());
        assertFalse("An incomplete scope cannot authorize tracking.", incomplete.isPresent());
    }

    private static BootstrapSnapshot bootstrap(
            Set<Capability> capabilities,
            List<OrganizationScope> organizations,
            OptionalLong defaultOrganizationId,
            boolean hasMore) {
        return new BootstrapSnapshot(
                new AuthenticatedUser(21, "Pessoa", "pessoa@example.test", "user"),
                new SessionIdentity(
                        "20000000-0000-4000-8000-000000000001",
                        "10000000-0000-4000-8000-000000000001"),
                AppRole.USER,
                new AuthorizationSnapshot(
                        capabilities,
                        OrganizationAccessMode.ASSIGNED,
                        organizations,
                        hasMore,
                        defaultOrganizationId,
                        "c".repeat(64),
                        0),
                "v1",
                "0.52.0",
                Instant.parse("2026-08-13T12:00:00Z"));
    }

    private static OrganizationScope store(long id) {
        return new OrganizationScope(
                id,
                "Loja " + id,
                "loja-" + id,
                OrganizationMembershipRole.SALESPERSON);
    }
}
