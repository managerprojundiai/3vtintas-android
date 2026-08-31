package br.com.tresvtintas.mobile.core.attendance;

public enum AttendanceAssignment {
    ALL("all"),
    MINE("mine"),
    UNASSIGNED("unassigned");

    private final String wireValue;

    AttendanceAssignment(String wireValue) {
        this.wireValue = wireValue;
    }

    public String wireValue() {
        return wireValue;
    }
}
