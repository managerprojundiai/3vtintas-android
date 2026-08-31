package br.com.tresvtintas.mobile.feature.shell;

import br.com.tresvtintas.mobile.core.model.Capability;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;

/**
 * Canonical mobile coverage registry. Presence here is a scope commitment, not proof that a journey
 * is implemented. {@link ShellMenuPolicy} also requires explicit implementation readiness.
 */
public enum MobileArea {
    ACCOUNT_SECURITY(true),
    DASHBOARD(Capability.DASHBOARD_READ),
    TEAM(Capability.TEAM_READ),
    PAINTERS_AND_APPROVALS(
            Capability.PAINTER_MANAGE,
            Capability.ACCESS_REQUEST_REVIEW),
    CATALOG(Capability.CATALOG_READ),
    CUSTOMERS(Capability.CUSTOMER_READ),
    MATERIAL_QUOTES(Capability.QUOTE_READ),
    LABOR_QUOTES(Capability.LABOR_QUOTE_READ),
    ORDERS(Capability.ORDER_READ),
    DELIVERIES(Capability.DELIVERY_READ),
    COMMISSIONS(Capability.COMMISSION_READ_SELF),
    COMMISSION_TEAM(Capability.COMMISSION_READ_TEAM),
    COMMISSION_APPROVALS(),
    PERSONAL_FINANCE(Capability.FINANCIAL_PERSONAL_READ_SELF),
    CORPORATE_FINANCE(Capability.FINANCIAL_CORPORATE_READ),
    AGENDA(Capability.APPOINTMENT_READ_SELF),
    TEAM_AGENDA(Capability.APPOINTMENT_READ_TEAM),
    GLOBAL_AGENDA(Capability.APPOINTMENT_READ_ALL),
    PERSONAL_AI_AGENT(Capability.AGENT_USE),
    NOTIFICATIONS(Capability.NOTIFICATION_MANAGE_SELF),
    WORKFORCE_LOCATION(Capability.LOCATION_TRACK_SELF),
    CUSTOMER_SERVICE(
            Capability.ATTENDANCE_READ_ALL,
            Capability.ATTENDANCE_READ_TEAM),
    WHATSAPP_INTEGRATIONS(Capability.SYSTEM_MANAGE),
    TEAM_AND_USERS(Capability.USER_MANAGE),
    ORGANIZATION_ADMINISTRATION(Capability.ORGANIZATION_MANAGE),
    AUDIT(Capability.AUDIT_READ),
    LIVE_LOCATION_AND_HISTORY(Capability.LOCATION_HISTORY_READ_TEAM),
    AGENT_OPERATIONS(Capability.SYSTEM_MANAGE),
    SETTINGS_AND_PROFILE(Capability.SYSTEM_MANAGE);

    private final boolean authenticatedOnly;
    private final Set<Capability> requiredCapabilities;

    MobileArea(Capability... requiredCapabilities) {
        this(false, requiredCapabilities);
    }

    MobileArea(
            boolean authenticatedOnly,
            Capability... requiredCapabilities) {
        this.authenticatedOnly = authenticatedOnly;
        this.requiredCapabilities = Set.copyOf(Arrays.asList(
                requiredCapabilities));
    }

    public boolean authenticatedOnly() {
        return authenticatedOnly;
    }

    public Optional<Capability> requiredCapability() {
        return requiredCapabilities.size() == 1
                ? requiredCapabilities.stream().findFirst()
                : Optional.empty();
    }

    public Set<Capability> requiredCapabilities() {
        return requiredCapabilities;
    }
}
