package br.com.tresvtintas.mobile.core.notifications;

import java.util.Arrays;
import java.util.Optional;

public enum NotificationRoute {
    HOME("home"),
    ATTENDANCE("attendance"),
    ORDERS("orders"),
    DELIVERIES("deliveries"),
    MATERIAL_QUOTES("quotes"),
    COMMISSIONS("commissions"),
    APPOINTMENTS("appointments"),
    FINANCE("finance"),
    AGENT("agent"),
    ACCOUNT_SECURITY("account_security");

    public static final String INTENT_EXTRA =
            "br.com.tresvtintas.mobile.extra.NOTIFICATION_ROUTE";
    public static final String EVENT_ID_EXTRA =
            "br.com.tresvtintas.mobile.extra.NOTIFICATION_EVENT_ID";

    private final String wireValue;

    NotificationRoute(String wireValue) {
        this.wireValue = wireValue;
    }

    public String wireValue() {
        return wireValue;
    }

    public static Optional<NotificationRoute> fromWireValue(String value) {
        return Arrays.stream(values())
                .filter(candidate -> candidate.wireValue.equals(value))
                .findFirst();
    }
}
