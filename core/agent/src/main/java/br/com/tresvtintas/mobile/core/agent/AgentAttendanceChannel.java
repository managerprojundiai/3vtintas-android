package br.com.tresvtintas.mobile.core.agent;

public enum AgentAttendanceChannel {
    WHATSAPP("whatsapp"),
    SITE_CHAT("site_chat");

    private final String wireValue;

    AgentAttendanceChannel(String wireValue) {
        this.wireValue = wireValue;
    }

    public String wireValue() {
        return wireValue;
    }

    public static AgentAttendanceChannel fromWireValue(String value) {
        for (AgentAttendanceChannel channel : values()) {
            if (channel.wireValue.equals(value)) {
                return channel;
            }
        }
        throw new IllegalArgumentException(
                "Agent attendance channel is invalid.");
    }
}
