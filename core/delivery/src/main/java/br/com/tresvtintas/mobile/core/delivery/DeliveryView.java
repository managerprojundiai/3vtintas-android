package br.com.tresvtintas.mobile.core.delivery;

public enum DeliveryView {
    ACTIVE("active"),
    HISTORY("history"),
    ALL("all"),
    CALENDAR("calendar");

    private final String wireValue;

    DeliveryView(String wireValue) {
        this.wireValue = wireValue;
    }

    public String wireValue() {
        return wireValue;
    }
}
