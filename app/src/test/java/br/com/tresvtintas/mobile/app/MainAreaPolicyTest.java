package br.com.tresvtintas.mobile.app;

import static org.junit.Assert.assertEquals;

import br.com.tresvtintas.mobile.core.bootstrap.BootstrapSnapshot;
import br.com.tresvtintas.mobile.core.model.AppRole;
import br.com.tresvtintas.mobile.core.model.AuthorizationSnapshot;
import br.com.tresvtintas.mobile.core.model.Capability;
import br.com.tresvtintas.mobile.core.model.OrganizationAccessMode;
import br.com.tresvtintas.mobile.core.model.OrganizationMembershipRole;
import br.com.tresvtintas.mobile.core.model.OrganizationScope;
import br.com.tresvtintas.mobile.core.network.dto.AuthenticatedUser;
import br.com.tresvtintas.mobile.core.network.dto.SessionIdentity;
import br.com.tresvtintas.mobile.feature.shell.MobileArea;
import br.com.tresvtintas.mobile.feature.shell.ShellAccessState;
import br.com.tresvtintas.mobile.feature.shell.ShellScopeKind;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import org.junit.Test;

public final class MainAreaPolicyTest {
    @Test
    public void storeSelectionCannotBlockOwnAccountSecurity() {
        ShellAccessState access = access(
                ShellScopeKind.REQUIRES_ORGANIZATION_SELECTION);

        assertEquals(
                "Only authentication-scoped journeys survive a missing store.",
                Set.of(MobileArea.ACCOUNT_SECURITY),
                MainAreaPolicy.enabledAreas(
                        access,
                        Set.of(
                                MobileArea.ACCOUNT_SECURITY,
                                MobileArea.CATALOG)));
    }

    @Test
    public void operationalScopeIntersectsAuthorizationAndImplementation() {
        ShellAccessState access = access(ShellScopeKind.PERSONAL);

        assertEquals(
                "Operational scope may expose both authorized journeys.",
                Set.of(
                        MobileArea.ACCOUNT_SECURITY,
                        MobileArea.CATALOG),
                MainAreaPolicy.enabledAreas(
                        access,
                        Set.of(
                                MobileArea.ACCOUNT_SECURITY,
                                MobileArea.CATALOG,
                                MobileArea.ORDERS)));
    }

    @Test
    public void dashboardRequiresServerCapabilityAndOperationalScope() {
        ShellAccessState authorized = access(
                ShellScopeKind.PERSONAL,
                Set.of(Capability.DASHBOARD_READ));
        ShellAccessState unauthorized = access(
                ShellScopeKind.PERSONAL,
                Set.of(Capability.CATALOG_READ));

        assertEquals(
                "The dashboard must appear only when the server grants it.",
                Set.of(MobileArea.DASHBOARD),
                MainAreaPolicy.enabledAreas(
                        authorized,
                        Set.of(MobileArea.DASHBOARD)));
        assertEquals(
                "A local implementation flag cannot grant dashboard access.",
                Set.of(),
                MainAreaPolicy.enabledAreas(
                        unauthorized,
                        Set.of(MobileArea.DASHBOARD)));
    }

    private static ShellAccessState access(ShellScopeKind kind) {
        return access(kind, Set.of(Capability.CATALOG_READ));
    }

    private static ShellAccessState access(
            ShellScopeKind kind,
            Set<Capability> capabilities) {
        AuthenticatedUser user = new AuthenticatedUser(
                21,
                "Pessoa",
                "pessoa@example.test",
                "user");
        boolean personal = kind == ShellScopeKind.PERSONAL;
        return new ShellAccessState(
                new BootstrapSnapshot(
                        user,
                        new SessionIdentity(
                                "20000000-0000-4000-8000-000000000001",
                                "10000000-0000-4000-8000-000000000001"),
                        AppRole.USER,
                        new AuthorizationSnapshot(
                                capabilities,
                                personal
                                        ? OrganizationAccessMode.NONE
                                        : OrganizationAccessMode.ASSIGNED,
                                personal
                                        ? List.of()
                                        : List.of(
                                                store(9),
                                                store(10)),
                                false,
                                OptionalLong.empty(),
                                "c".repeat(64),
                                0),
                        "v1",
                        "0.29.0",
                        Instant.parse("2026-07-27T09:00:00Z")),
                kind,
                Optional.empty());
    }

    private static OrganizationScope store(long id) {
        return new OrganizationScope(
                id,
                "Loja " + id,
                "loja-" + id,
                OrganizationMembershipRole.SALESPERSON);
    }
}
