package br.com.tresvtintas.mobile.core.attendance;

public enum AttendanceChannel {
    WHATSAPP("whatsapp"),
    SITE_CHAT("site_chat");

    private final String wireValue;

    AttendanceChannel(String wireValue) {
        this.wireValue = wireValue;
    }

    public String wireValue() {
        return wireValue;
    }
}
