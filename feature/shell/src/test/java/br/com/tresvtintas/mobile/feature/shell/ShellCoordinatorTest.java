package br.com.tresvtintas.mobile.feature.shell;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

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

public final class ShellCoordinatorTest {
    private static final String REVISION = "b".repeat(64);

    @Test
    public void serverDefaultSelectsSingleAssignedStore() {
        ShellAccessState state = new ShellCoordinator().apply(snapshot(
                OrganizationAccessMode.ASSIGNED,
                List.of(store(9)),
                false,
                OptionalLong.of(9)));

        assertTrue("Default store must make the shell operational.", state.isOperational());
        assertEquals(
                "Default store must select organization scope.",
                ShellScopeKind.SELECTED_ORGANIZATION,
                state.scopeKind());
        assertEquals(
                "Default store ID must be preserved.",
                9,
                state.selectedOrganization().orElseThrow().id());
    }

    @Test
    public void multipleStoresRequireExplicitAuthorizedSelection() {
        ShellCoordinator coordinator = new ShellCoordinator();
        ShellAccessState initial = coordinator.apply(snapshot(
                OrganizationAccessMode.ASSIGNED,
                List.of(store(9), store(10)),
                false,
                OptionalLong.empty()));

        assertFalse(
                "Multiple stores without a default require a choice.",
                initial.isOperational());
        assertEquals(
                "Shell must expose the selection requirement.",
                ShellScopeKind.REQUIRES_ORGANIZATION_SELECTION,
                initial.scopeKind());
        assertEquals(
                "Authorized store must be selectable.",
                10,
                coordinator.selectOrganization(10)
                        .selectedOrganization()
                        .orElseThrow()
                        .id());
        assertThrows(
                "Unauthorized store must not be selectable.",
                IllegalArgumentException.class,
                () -> coordinator.selectOrganization(99));
    }

    @Test
    public void authorizationRefreshRevokesRemovedStoreSelection() {
        ShellCoordinator coordinator = new ShellCoordinator();
        coordinator.apply(snapshot(
                OrganizationAccessMode.ASSIGNED,
                List.of(store(9), store(10)),
                false,
                OptionalLong.empty()));
        coordinator.selectOrganization(10);

        ShellAccessState refreshed = coordinator.apply(snapshot(
                OrganizationAccessMode.ASSIGNED,
                List.of(store(9)),
                false,
                OptionalLong.of(9)));

        assertEquals(
                "Revoked store selection must reset to the authorized default.",
                9,
                refreshed.selectedOrganization().orElseThrow().id());
    }

    @Test
    public void incompleteStorePageFailsClosedWhileGlobalAndPersonalRemainUsable() {
        ShellCoordinator coordinator = new ShellCoordinator();
        assertEquals(
                "Incomplete store page must fail closed.",
                ShellScopeKind.ORGANIZATION_LIST_INCOMPLETE,
                coordinator.apply(snapshot(
                        OrganizationAccessMode.ASSIGNED,
                        List.of(store(9)),
                        true,
                        OptionalLong.empty())).scopeKind());
        assertEquals(
                "Global scope must remain usable.",
                ShellScopeKind.GLOBAL,
                coordinator.apply(snapshot(
                        OrganizationAccessMode.ALL,
                        List.of(),
                        false,
                        OptionalLong.empty())).scopeKind());
        assertEquals(
                "Personal scope must remain usable.",
                ShellScopeKind.PERSONAL,
                coordinator.apply(snapshot(
                        OrganizationAccessMode.NONE,
                        List.of(),
                        false,
                        OptionalLong.empty())).scopeKind());
    }

    private static BootstrapSnapshot snapshot(
            OrganizationAccessMode mode,
            List<OrganizationScope> organizations,
            boolean hasMore,
            OptionalLong defaultId) {
        AuthenticatedUser user = new AuthenticatedUser(
                41, "Pessoa", "pessoa@example.test", "salesperson");
        return new BootstrapSnapshot(
                user,
                new SessionIdentity(
                        "00000000-0000-4000-8000-000000000041",
                        "00000000-0000-4000-8000-000000000042"),
                AppRole.SALESPERSON,
                new AuthorizationSnapshot(
                        Set.of(Capability.CATALOG_READ),
                        mode,
                        organizations,
                        hasMore,
                        defaultId,
                        REVISION,
                        0),
                "v1",
                "0.5.0",
                Instant.parse("2026-07-25T13:00:00Z"));
    }

    private static OrganizationScope store(long id) {
        return new OrganizationScope(
                id,
                "3V Loja " + id,
                "3v-loja-" + id,
                OrganizationMembershipRole.SALESPERSON);
    }
}
