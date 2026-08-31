package br.com.tresvtintas.mobile.core.delivery;

public enum DeliveryManagementView {
    AWAITING_SCHEDULE("awaiting_schedule"),
    TODAY("today"),
    SCHEDULED("scheduled"),
    ALL("all");

    private final String wireValue;

    DeliveryManagementView(String wireValue) {
        this.wireValue = wireValue;
    }

    public String wireValue() {
        return wireValue;
    }
}
