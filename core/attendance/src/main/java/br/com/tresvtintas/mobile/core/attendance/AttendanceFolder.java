package br.com.tresvtintas.mobile.core.attendance;

public enum AttendanceFolder {
    INBOX("inbox"),
    MINE("mine"),
    UNASSIGNED("unassigned"),
    URGENT("urgent"),
    FOLLOW_UP("follow_up"),
    SUPPLIERS("fornecedores"),
    VIP("vip"),
    RESOLVED("resolved");

    private final String wireValue;

    AttendanceFolder(String wireValue) {
        this.wireValue = wireValue;
    }

    public String wireValue() {
        return wireValue;
    }
}
