package br.com.tresvtintas.mobile.core.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.OptionalLong;
import java.util.Set;
import org.junit.Test;

public final class AuthorizationSnapshotTest {
    private static final String REVISION = "a".repeat(64);

    @Test
    public void wireCatalogMatchesServerContractWithoutDerivingRoleGrants() {
        assertEquals(
                "Capability catalog must match the server.",
                66,
                Capability.values().length);
        assertEquals(
                "Dashboard capability must map exactly.",
                Capability.DASHBOARD_READ,
                Capability.fromWireValue("dashboard.read").orElseThrow());
        assertEquals(
                "Team capability must map exactly.",
                Capability.TEAM_READ,
                Capability.fromWireValue("team.read").orElseThrow());
        assertEquals(
                "Painter management capability must map exactly.",
                Capability.PAINTER_MANAGE,
                Capability.fromWireValue("painter.manage").orElseThrow());
        assertEquals(
                "Access request review capability must map exactly.",
                Capability.ACCESS_REQUEST_REVIEW,
                Capability.fromWireValue("access_request.review").orElseThrow());
        assertEquals(
                "Pricing read capability must map exactly.",
                Capability.PRICING_READ,
                Capability.fromWireValue("pricing.read").orElseThrow());
        assertEquals(
                "Pricing table management capability must map exactly.",
                Capability.PRICING_TABLE_MANAGE,
                Capability.fromWireValue("pricing.table.manage").orElseThrow());
        assertEquals(
                "Global pricing policy capability must map exactly.",
                Capability.PRICING_POLICY_MANAGE_GLOBAL,
                Capability.fromWireValue("pricing.policy.manage_global").orElseThrow());
        assertEquals(
                "Store pricing policy capability must map exactly.",
                Capability.PRICING_POLICY_MANAGE_STORE,
                Capability.fromWireValue("pricing.policy.manage_store").orElseThrow());
        assertEquals(
                "Pricing selection capability must map exactly.",
                Capability.PRICING_SALE_SELECT,
                Capability.fromWireValue("pricing.sale.select").orElseThrow());
        assertEquals(
                "Pricing override capability must map exactly.",
                Capability.PRICING_SALE_OVERRIDE,
                Capability.fromWireValue("pricing.sale.override").orElseThrow());
        assertEquals(
                "Pricing audit capability must map exactly.",
                Capability.PRICING_AUDIT_READ,
                Capability.fromWireValue("pricing.audit.read").orElseThrow());
        assertEquals(
                "Commission approval capability must map exactly.",
                Capability.COMMISSION_APPROVE_TEAM,
                Capability.fromWireValue("commission.approve_team").orElseThrow());
        assertEquals(
                "Commission cancellation capability must map exactly.",
                Capability.COMMISSION_CANCEL_TEAM,
                Capability.fromWireValue("commission.cancel_team").orElseThrow());
        assertEquals(
                "Commission payment capability must map exactly.",
                Capability.COMMISSION_PAY,
                Capability.fromWireValue("commission.pay").orElseThrow());
        assertEquals(
                "Organization management capability must map exactly.",
                Capability.ORGANIZATION_MANAGE,
                Capability.fromWireValue("organization.manage").orElseThrow());
        assertEquals(
                "Audit read capability must map exactly.",
                Capability.AUDIT_READ,
                Capability.fromWireValue("audit.read").orElseThrow());
        assertEquals(
                "Agent replay capability must map exactly.",
                Capability.AUDIT_REPLAY_READ,
                Capability.fromWireValue("audit.replay.read").orElseThrow());
        assertEquals(
                "Third-party security capability must map exactly.",
                Capability.SECURITY_MANAGE_USERS,
                Capability.fromWireValue("security.manage_users").orElseThrow());
        assertEquals(
                "Personal agent capability must map exactly.",
                Capability.AGENT_USE,
                Capability.fromWireValue("agent.use").orElseThrow());
        assertEquals(
                "Self notification capability must map exactly.",
                Capability.NOTIFICATION_MANAGE_SELF,
                Capability.fromWireValue("notification.manage_self").orElseThrow());
        assertEquals(
                "Global attendance read capability must map exactly.",
                Capability.ATTENDANCE_READ_ALL,
                Capability.fromWireValue("attendance.read_all").orElseThrow());
        assertEquals(
                "Team attendance read capability must map exactly.",
                Capability.ATTENDANCE_READ_TEAM,
                Capability.fromWireValue("attendance.read_team").orElseThrow());
        assertEquals(
                "Assigned attendance read capability must map exactly.",
                Capability.ATTENDANCE_READ_ASSIGNED,
                Capability.fromWireValue("attendance.read_assigned").orElseThrow());
        assertEquals(
                "Attendance reply capability must map exactly.",
                Capability.ATTENDANCE_REPLY,
                Capability.fromWireValue("attendance.reply").orElseThrow());
        assertEquals(
                "Attendance management capability must map exactly.",
                Capability.ATTENDANCE_MANAGE,
                Capability.fromWireValue("attendance.manage").orElseThrow());
        assertEquals(
                "Delivery scheduling capability must map exactly.",
                Capability.DELIVERY_SCHEDULE,
                Capability.fromWireValue("delivery.schedule").orElseThrow());
        assertEquals(
                "Delivery assignment capability must map exactly.",
                Capability.DELIVERY_ASSIGN,
                Capability.fromWireValue("delivery.assign").orElseThrow());
        assertEquals(
                "Managed delivery completion capability must map exactly.",
                Capability.DELIVERY_COMPLETE_MANAGEMENT,
                Capability.fromWireValue("delivery.complete_management").orElseThrow());
        assertEquals(
                "Order payment capability must map exactly.",
                Capability.ORDER_PAYMENT_RECEIVE,
                Capability.fromWireValue(
                                "order.payment.receive")
                        .orElseThrow());
        assertEquals(
                "Appointment team write capability must map exactly.",
                Capability.APPOINTMENT_WRITE_TEAM,
                Capability.fromWireValue(
                                "appointment.write_team")
                        .orElseThrow());
        assertEquals(
                "Quote status capability must map exactly.",
                Capability.QUOTE_STATUS_WRITE,
                Capability.fromWireValue("quote.status.write").orElseThrow());
        assertEquals(
                "Labor quote capability must map exactly.",
                Capability.LABOR_QUOTE_CREATE,
                Capability.fromWireValue("quote.labor.create").orElseThrow());
        assertEquals(
                "Wire capability must map exactly.",
                Capability.LOCATION_TRACK_SELF,
                Capability.fromWireValue("location.track_self").orElseThrow());
        assertTrue(
                "Unknown capability must fail closed.",
                Capability.fromWireValue("future.capability").isEmpty());
        assertEquals(
                "Wire role must map exactly.",
                AppRole.DELIVERY_DRIVER,
                AppRole.fromWireValue("delivery_driver").orElseThrow());
    }

    @Test
    public void assignedSnapshotRequiresValidDefaultAndUniqueStores() {
        OrganizationScope store = store(9);
        AuthorizationSnapshot snapshot = new AuthorizationSnapshot(
                Set.of(Capability.CATALOG_READ),
                OrganizationAccessMode.ASSIGNED,
                List.of(store),
                false,
                OptionalLong.of(9),
                REVISION,
                1);

        assertTrue("Granted capability must be present.", snapshot.has(Capability.CATALOG_READ));
        assertFalse(
                "Role must not invent a system grant.",
                snapshot.has(Capability.SYSTEM_MANAGE));
        assertEquals(
                "Unknown capability count must remain observable.",
                1,
                snapshot.ignoredCapabilityCount());

        assertThrows(
                "Duplicate organizations must be rejected.",
                IllegalArgumentException.class,
                () -> new AuthorizationSnapshot(
                        Set.of(),
                        OrganizationAccessMode.ASSIGNED,
                        List.of(store, store),
                        false,
                        OptionalLong.empty(),
                        REVISION,
                        0));
    }

    @Test
    public void globalAndNoneModesCannotSmuggleEnumeratedStores() {
        assertThrows(
                "Global mode must not carry enumerated stores.",
                IllegalArgumentException.class,
                () -> new AuthorizationSnapshot(
                        Set.of(),
                        OrganizationAccessMode.ALL,
                        List.of(store(4)),
                        false,
                        OptionalLong.empty(),
                        REVISION,
                        0));
    }

    private static OrganizationScope store(long id) {
        return new OrganizationScope(
                id,
                "3V Centro",
                "3v-centro",
                OrganizationMembershipRole.SALESPERSON);
    }
}
