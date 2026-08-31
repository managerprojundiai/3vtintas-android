package br.com.tresvtintas.mobile.core.agent;

public enum AgentAttendanceDeliveryState {
    AVAILABLE("available"),
    QUEUED("queued");

    private final String wireValue;

    AgentAttendanceDeliveryState(String wireValue) {
        this.wireValue = wireValue;
    }

    public String wireValue() {
        return wireValue;
    }

    public static AgentAttendanceDeliveryState fromWireValue(
            String value) {
        for (AgentAttendanceDeliveryState state : values()) {
            if (state.wireValue.equals(value)) {
                return state;
            }
        }
        throw new IllegalArgumentException(
                "Agent attendance delivery state is invalid.");
    }
}
