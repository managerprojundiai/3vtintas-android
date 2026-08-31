package br.com.tresvtintas.mobile.core.appointment;

public enum AppointmentScope {
    SELF("self"),
    TEAM("team"),
    ALL("all");

    private final String queryValue;

    AppointmentScope(String queryValue) {
        this.queryValue = queryValue;
    }

    public String queryValue() {
        return queryValue;
    }
}
