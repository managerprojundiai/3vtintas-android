package br.com.tresvtintas.mobile.core.delivery;

public enum DeliveryRouteStopStatus {
    PLANNED,
    EN_ROUTE,
    ARRIVED,
    COMPLETED,
    SKIPPED;

    public boolean isTerminal() {
        return this == COMPLETED || this == SKIPPED;
    }
}
