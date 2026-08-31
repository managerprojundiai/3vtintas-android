package br.com.tresvtintas.mobile.core.agent;

public enum AgentAttendanceMessageDirection {
    INBOUND("inbound"),
    OUTBOUND("outbound");

    private final String wireValue;

    AgentAttendanceMessageDirection(String wireValue) {
        this.wireValue = wireValue;
    }

    public String wireValue() {
        return wireValue;
    }

    public static AgentAttendanceMessageDirection fromWireValue(
            String value) {
        for (AgentAttendanceMessageDirection direction : values()) {
            if (direction.wireValue.equals(value)) {
                return direction;
            }
        }
        throw new IllegalArgumentException(
                "Agent attendance message direction is invalid.");
    }
}
