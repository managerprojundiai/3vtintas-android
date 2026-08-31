package br.com.tresvtintas.mobile.core.model;

import java.util.Optional;

/**
 * Known wire capabilities. Their presence is copied from the signed-in server bootstrap; this enum
 * never derives or grants a capability from a role.
 */
public enum Capability {
    DASHBOARD_READ("dashboard.read"),
    TEAM_READ("team.read"),
    PAINTER_MANAGE("painter.manage"),
    ACCESS_REQUEST_REVIEW("access_request.review"),
    CATALOG_READ("catalog.read"),
    CATALOG_MANAGE("catalog.manage"),
    PRICING_READ("pricing.read"),
    PRICING_TABLE_MANAGE("pricing.table.manage"),
    PRICING_POLICY_MANAGE_GLOBAL("pricing.policy.manage_global"),
    PRICING_POLICY_MANAGE_STORE("pricing.policy.manage_store"),
    PRICING_SALE_SELECT("pricing.sale.select"),
    PRICING_SALE_OVERRIDE("pricing.sale.override"),
    PRICING_AUDIT_READ("pricing.audit.read"),
    CUSTOMER_READ("customer.read"),
    CUSTOMER_WRITE("customer.write"),
    CUSTOMER_ASSIGN("customer.assign"),
    QUOTE_READ("quote.read"),
    QUOTE_CREATE("quote.create"),
    QUOTE_STATUS_WRITE("quote.status.write"),
    QUOTE_PDF_READ("quote.pdf.read"),
    LABOR_QUOTE_READ("quote.labor.read"),
    LABOR_QUOTE_CREATE("quote.labor.create"),
    LABOR_QUOTE_STATUS_WRITE("quote.labor.status.write"),
    LABOR_QUOTE_PDF_READ("quote.labor.pdf.read"),
    ORDER_READ("order.read"),
    ORDER_CREATE("order.create"),
    ORDER_STATUS_WRITE("order.status.write"),
    ORDER_PAYMENT_RECEIVE("order.payment.receive"),
    ORDER_CANCEL("order.cancel"),
    COMMISSION_READ_SELF("commission.read_self"),
    COMMISSION_READ_TEAM("commission.read_team"),
    COMMISSION_APPROVE_TEAM("commission.approve_team"),
    COMMISSION_CANCEL_TEAM("commission.cancel_team"),
    COMMISSION_PAY("commission.pay"),
    FINANCIAL_PERSONAL_READ_SELF("financial.personal.read_self"),
    FINANCIAL_PERSONAL_WRITE_SELF("financial.personal.write_self"),
    FINANCIAL_CORPORATE_READ("financial.corporate.read"),
    FINANCIAL_CORPORATE_WRITE("financial.corporate.write"),
    APPOINTMENT_READ_SELF("appointment.read_self"),
    APPOINTMENT_WRITE_SELF("appointment.write_self"),
    APPOINTMENT_READ_TEAM("appointment.read_team"),
    APPOINTMENT_WRITE_TEAM("appointment.write_team"),
    APPOINTMENT_READ_ALL("appointment.read_all"),
    APPOINTMENT_WRITE_ALL("appointment.write_all"),
    DELIVERY_READ("delivery.read"),
    DELIVERY_UPDATE_ASSIGNED("delivery.update_assigned"),
    DELIVERY_SCHEDULE("delivery.schedule"),
    DELIVERY_ASSIGN("delivery.assign"),
    DELIVERY_COMPLETE_MANAGEMENT("delivery.complete_management"),
    ATTENDANCE_READ_ALL("attendance.read_all"),
    ATTENDANCE_READ_TEAM("attendance.read_team"),
    ATTENDANCE_READ_ASSIGNED("attendance.read_assigned"),
    ATTENDANCE_REPLY("attendance.reply"),
    ATTENDANCE_MANAGE("attendance.manage"),
    AGENT_USE("agent.use"),
    NOTIFICATION_MANAGE_SELF("notification.manage_self"),
    LOCATION_TRACK_SELF("location.track_self"),
    LOCATION_HISTORY_READ_SELF("location.history.read_self"),
    LOCATION_HISTORY_READ_TEAM("location.history.read_team"),
    LOCATION_HISTORY_READ_ALL("location.history.read_all"),
    USER_MANAGE("user.manage"),
    SECURITY_MANAGE_USERS("security.manage_users"),
    ORGANIZATION_MANAGE("organization.manage"),
    AUDIT_READ("audit.read"),
    AUDIT_REPLAY_READ("audit.replay.read"),
    SYSTEM_MANAGE("system.manage");

    private final String wireValue;

    Capability(String wireValue) {
        this.wireValue = wireValue;
    }

    public String wireValue() {
        return wireValue;
    }

    public static Optional<Capability> fromWireValue(String value) {
        if (value == null) {
            return Optional.empty();
        }
        for (Capability capability : values()) {
            if (capability.wireValue.equals(value)) {
                return Optional.of(capability);
            }
        }
        return Optional.empty();
    }
}
