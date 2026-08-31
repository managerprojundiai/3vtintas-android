package br.com.tresvtintas.mobile.feature.shell;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import br.com.tresvtintas.mobile.core.model.AuthorizationSnapshot;
import br.com.tresvtintas.mobile.core.model.Capability;
import br.com.tresvtintas.mobile.core.model.OrganizationAccessMode;
import java.util.List;
import java.util.OptionalLong;
import java.util.Set;
import org.junit.Test;

public final class ShellMenuPolicyTest {
    @Test
    public void areaNeedsBothServerCapabilityAndRealImplementation() {
        AuthorizationSnapshot authorization = new AuthorizationSnapshot(
                Set.of(Capability.CATALOG_READ, Capability.ORDER_READ),
                OrganizationAccessMode.NONE,
                List.of(),
                false,
                OptionalLong.empty(),
                "c".repeat(64),
                0);

        Set<MobileArea> enabled = ShellMenuPolicy.enabledAreas(
                authorization,
                Set.of(MobileArea.CATALOG, MobileArea.CUSTOMERS));

        assertEquals(
                "Only implemented and authorized areas must be enabled.",
                Set.of(MobileArea.CATALOG),
                enabled);
        assertFalse(
                "Authorized but unimplemented order area must remain disabled.",
                enabled.contains(MobileArea.ORDERS));
    }

    @Test
    public void plannedAreaWithoutDedicatedServerCapabilityCannotBecomeClickable() {
        AuthorizationSnapshot authorization = new AuthorizationSnapshot(
                Set.of(Capability.SYSTEM_MANAGE),
                OrganizationAccessMode.ALL,
                List.of(),
                false,
                OptionalLong.empty(),
                "d".repeat(64),
                0);

        Set<MobileArea> enabled = ShellMenuPolicy.enabledAreas(
                authorization,
                Set.of(MobileArea.PERSONAL_AI_AGENT));

        assertTrue(
                "Area without a dedicated capability must fail closed.",
                enabled.isEmpty());
    }

    @Test
    public void ownAccountSecurityRequiresAuthenticationButNoBusinessCapability() {
        AuthorizationSnapshot authorization = new AuthorizationSnapshot(
                Set.of(),
                OrganizationAccessMode.NONE,
                List.of(),
                false,
                OptionalLong.empty(),
                "7".repeat(64),
                0);

        assertEquals(
                "Every authenticated user may manage only their own accesses.",
                Set.of(MobileArea.ACCOUNT_SECURITY),
                ShellMenuPolicy.enabledAreas(
                        authorization,
                        Set.of(MobileArea.ACCOUNT_SECURITY)));
        assertTrue(
                "Planned capability-less areas must remain fail-closed.",
                ShellMenuPolicy.enabledAreas(
                                authorization,
                                Set.of(MobileArea.DASHBOARD))
                        .isEmpty());
    }

    @Test
    public void laborQuotesNeedDedicatedReadCapabilityAndImplementation() {
        AuthorizationSnapshot authorization = new AuthorizationSnapshot(
                Set.of(Capability.LABOR_QUOTE_READ),
                OrganizationAccessMode.NONE,
                List.of(),
                false,
                OptionalLong.empty(),
                "e".repeat(64),
                0);

        Set<MobileArea> enabled = ShellMenuPolicy.enabledAreas(
                authorization,
                Set.of(MobileArea.LABOR_QUOTES, MobileArea.MATERIAL_QUOTES));

        assertEquals(
                "Labor quotes require their own server capability.",
                Set.of(MobileArea.LABOR_QUOTES),
                enabled);
    }

    @Test
    public void teamCommissionReadDoesNotImplyApprovalAuthority() {
        AuthorizationSnapshot authorization = new AuthorizationSnapshot(
                Set.of(Capability.COMMISSION_READ_TEAM),
                OrganizationAccessMode.ASSIGNED,
                List.of(),
                false,
                OptionalLong.empty(),
                "f".repeat(64),
                0);

        Set<MobileArea> enabled = ShellMenuPolicy.enabledAreas(
                authorization,
                Set.of(
                        MobileArea.COMMISSION_TEAM,
                        MobileArea.COMMISSION_APPROVALS));

        assertEquals(
                "Team consultation may be exposed without implying a financial write.",
                Set.of(MobileArea.COMMISSION_TEAM),
                enabled);
        assertFalse(
                "Approval remains fail-closed until it has a dedicated capability.",
                enabled.contains(MobileArea.COMMISSION_APPROVALS));
    }

    @Test
    public void personalFinanceNeedsItsDedicatedReadCapability() {
        AuthorizationSnapshot readable = new AuthorizationSnapshot(
                Set.of(Capability.FINANCIAL_PERSONAL_READ_SELF),
                OrganizationAccessMode.NONE,
                List.of(),
                false,
                OptionalLong.empty(),
                "a".repeat(64),
                0);
        AuthorizationSnapshot writeOnly = new AuthorizationSnapshot(
                Set.of(Capability.FINANCIAL_PERSONAL_WRITE_SELF),
                OrganizationAccessMode.NONE,
                List.of(),
                false,
                OptionalLong.empty(),
                "b".repeat(64),
                0);

        assertEquals(
                "Authorized personal finance must become available when implemented.",
                Set.of(MobileArea.PERSONAL_FINANCE),
                ShellMenuPolicy.enabledAreas(
                        readable,
                        Set.of(MobileArea.PERSONAL_FINANCE)));
        assertTrue(
                "Write permission alone must not expose the personal finance surface.",
                ShellMenuPolicy.enabledAreas(
                                writeOnly,
                                Set.of(MobileArea.PERSONAL_FINANCE))
                        .isEmpty());
    }

    @Test
    public void attendanceAcceptsOnlyGlobalOrTeamReadScope() {
        assertAttendanceAreaEnabled(
                Capability.ATTENDANCE_READ_ALL);
        assertAttendanceAreaEnabled(
                Capability.ATTENDANCE_READ_TEAM);
    }

    @Test
    public void assignedAttendanceCapabilityCannotExposeTheSurface() {
        AuthorizationSnapshot authorization = new AuthorizationSnapshot(
                Set.of(Capability.ATTENDANCE_READ_ASSIGNED),
                OrganizationAccessMode.ASSIGNED,
                List.of(),
                false,
                OptionalLong.empty(),
                "6".repeat(64),
                0);

        assertTrue(
                "Operational roles must not recover Attendance through a legacy capability.",
                ShellMenuPolicy.enabledAreas(
                                authorization,
                                Set.of(MobileArea.CUSTOMER_SERVICE))
                        .isEmpty());
    }

    @Test
    public void attendanceWriteAloneCannotExposeReadSurface() {
        AuthorizationSnapshot authorization = new AuthorizationSnapshot(
                Set.of(Capability.ATTENDANCE_REPLY),
                OrganizationAccessMode.ASSIGNED,
                List.of(),
                false,
                OptionalLong.empty(),
                "8".repeat(64),
                0);

        assertTrue(
                "Reply authority must not imply list authority.",
                ShellMenuPolicy.enabledAreas(
                                authorization,
                                Set.of(MobileArea.CUSTOMER_SERVICE))
                        .isEmpty());
    }

    @Test
    public void registryCoversEveryExplicitPanelArea() {
        assertEquals(
                "Registry must cover the explicit panel scope.",
                Set.of(
                        MobileArea.ACCOUNT_SECURITY,
                        MobileArea.DASHBOARD,
                        MobileArea.TEAM,
                        MobileArea.PAINTERS_AND_APPROVALS,
                        MobileArea.CATALOG,
                        MobileArea.CUSTOMERS,
                        MobileArea.MATERIAL_QUOTES,
                        MobileArea.LABOR_QUOTES,
                        MobileArea.ORDERS,
                        MobileArea.DELIVERIES,
                        MobileArea.COMMISSIONS,
                        MobileArea.COMMISSION_TEAM,
                        MobileArea.COMMISSION_APPROVALS,
                        MobileArea.PERSONAL_FINANCE,
                        MobileArea.CORPORATE_FINANCE,
                        MobileArea.AGENDA,
                        MobileArea.TEAM_AGENDA,
                        MobileArea.GLOBAL_AGENDA,
                        MobileArea.PERSONAL_AI_AGENT,
                        MobileArea.NOTIFICATIONS,
                        MobileArea.WORKFORCE_LOCATION,
                        MobileArea.CUSTOMER_SERVICE,
                        MobileArea.WHATSAPP_INTEGRATIONS,
                        MobileArea.TEAM_AND_USERS,
                        MobileArea.ORGANIZATION_ADMINISTRATION,
                        MobileArea.AUDIT,
                        MobileArea.LIVE_LOCATION_AND_HISTORY,
                        MobileArea.AGENT_OPERATIONS,
                        MobileArea.SETTINGS_AND_PROFILE),
                Set.of(MobileArea.values()));
        assertTrue(
                "Account security must be explicitly authentication-scoped.",
                MobileArea.ACCOUNT_SECURITY.authenticatedOnly());
        assertEquals(
                "The personal agenda must use the self scope.",
                Capability.APPOINTMENT_READ_SELF,
                MobileArea.AGENDA.requiredCapability().orElseThrow());
        assertEquals(
                "The team agenda must use the team scope.",
                Capability.APPOINTMENT_READ_TEAM,
                MobileArea.TEAM_AGENDA.requiredCapability().orElseThrow());
        assertEquals(
                "The global agenda must use the global scope.",
                Capability.APPOINTMENT_READ_ALL,
                MobileArea.GLOBAL_AGENDA.requiredCapability().orElseThrow());
        assertEquals(
                "Notification settings must use the self-management scope.",
                Capability.NOTIFICATION_MANAGE_SELF,
                MobileArea.NOTIFICATIONS.requiredCapability().orElseThrow());
        assertEquals(
                "The managerial team area must use the canonical team scope.",
                Capability.TEAM_READ,
                MobileArea.TEAM.requiredCapability().orElseThrow());
        assertEquals(
                "Organization administration must require the master-only capability.",
                Capability.ORGANIZATION_MANAGE,
                MobileArea.ORGANIZATION_ADMINISTRATION.requiredCapability().orElseThrow());
        assertEquals(
                "Audit must require its dedicated read capability.",
                Capability.AUDIT_READ,
                MobileArea.AUDIT.requiredCapability().orElseThrow());
        assertEquals(
                "Live location and history must require the team history capability.",
                Capability.LOCATION_HISTORY_READ_TEAM,
                MobileArea.LIVE_LOCATION_AND_HISTORY.requiredCapability().orElseThrow());
    }

    @Test
    public void auditCannotBeRecoveredFromGenericSystemAuthority() {
        AuthorizationSnapshot generic = new AuthorizationSnapshot(
                Set.of(Capability.SYSTEM_MANAGE),
                OrganizationAccessMode.ALL,
                List.of(),
                false,
                OptionalLong.empty(),
                "4".repeat(64),
                0);
        AuthorizationSnapshot dedicated = new AuthorizationSnapshot(
                Set.of(Capability.AUDIT_READ),
                OrganizationAccessMode.ALL,
                List.of(),
                false,
                OptionalLong.empty(),
                "5".repeat(64),
                0);

        assertTrue(
                "Generic system management must not expose audit events.",
                ShellMenuPolicy.enabledAreas(generic, Set.of(MobileArea.AUDIT)).isEmpty());
        assertEquals(
                "Only the server-issued audit capability may expose the area.",
                Set.of(MobileArea.AUDIT),
                ShellMenuPolicy.enabledAreas(dedicated, Set.of(MobileArea.AUDIT)));
    }

    @Test
    public void whatsappAdministrationRequiresExplicitSystemAuthority() {
        AuthorizationSnapshot administrator = new AuthorizationSnapshot(
                Set.of(Capability.SYSTEM_MANAGE),
                OrganizationAccessMode.ALL,
                List.of(),
                false,
                OptionalLong.empty(),
                "1".repeat(64),
                0);
        AuthorizationSnapshot operationalUser = new AuthorizationSnapshot(
                Set.of(
                        Capability.CATALOG_READ,
                        Capability.ORDER_READ,
                        Capability.DELIVERY_READ),
                OrganizationAccessMode.ASSIGNED,
                List.of(),
                false,
                OptionalLong.empty(),
                "2".repeat(64),
                0);

        assertEquals(
                "System administrators may open per-store WhatsApp management.",
                Set.of(MobileArea.WHATSAPP_INTEGRATIONS),
                ShellMenuPolicy.enabledAreas(
                        administrator,
                        Set.of(MobileArea.WHATSAPP_INTEGRATIONS)));
        assertTrue(
                "Operational roles must not see or open WhatsApp administration.",
                ShellMenuPolicy.enabledAreas(
                                operationalUser,
                                Set.of(MobileArea.WHATSAPP_INTEGRATIONS))
                        .isEmpty());
    }

    private static void assertAttendanceAreaEnabled(
            Capability capability) {
        AuthorizationSnapshot authorization = new AuthorizationSnapshot(
                Set.of(capability),
                OrganizationAccessMode.ASSIGNED,
                List.of(),
                false,
                OptionalLong.empty(),
                "9".repeat(64),
                0);

        assertEquals(
                "Each canonical attendance read scope must expose the same journey.",
                Set.of(MobileArea.CUSTOMER_SERVICE),
                ShellMenuPolicy.enabledAreas(
                        authorization,
                        Set.of(MobileArea.CUSTOMER_SERVICE)));
    }
}
