package br.com.tresvtintas.mobile.core.notifications;

import java.util.Arrays;
import java.util.Optional;

public enum NotificationCategory {
    ATTENDANCE("attendance", NotificationRoute.ATTENDANCE),
    ORDERS("orders", NotificationRoute.ORDERS),
    DELIVERIES("deliveries", NotificationRoute.DELIVERIES),
    QUOTES("quotes", NotificationRoute.MATERIAL_QUOTES),
    COMMISSIONS("commissions", NotificationRoute.COMMISSIONS),
    APPOINTMENTS("appointments", NotificationRoute.APPOINTMENTS),
    FINANCE("finance", NotificationRoute.FINANCE),
    AGENT("agent", NotificationRoute.AGENT),
    SECURITY("security", NotificationRoute.ACCOUNT_SECURITY);

    private final String wireValue;
    private final NotificationRoute route;

    NotificationCategory(String wireValue, NotificationRoute route) {
        this.wireValue = wireValue;
        this.route = route;
    }

    public String wireValue() {
        return wireValue;
    }

    public NotificationRoute route() {
        return route;
    }

    public boolean configurable() {
        return this != SECURITY;
    }

    public static Optional<NotificationCategory> fromWireValue(String value) {
        return Arrays.stream(values())
                .filter(candidate -> candidate.wireValue.equals(value))
                .findFirst();
    }
}
