package br.com.tresvtintas.mobile.core.attendance;

public enum AttendancePriority {
    LOW("low"),
    NORMAL("normal"),
    HIGH("high"),
    URGENT("urgent");

    private final String wireValue;

    AttendancePriority(String wireValue) {
        this.wireValue = wireValue;
    }

    public String wireValue() {
        return wireValue;
    }
}
