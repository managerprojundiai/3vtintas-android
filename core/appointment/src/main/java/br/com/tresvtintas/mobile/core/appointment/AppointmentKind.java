package br.com.tresvtintas.mobile.core.appointment;

public enum AppointmentKind {
    GENERAL,
    DELIVERY,
    COLLECTION;

    public boolean isWritable() {
        return this != DELIVERY;
    }
}
