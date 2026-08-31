package br.com.tresvtintas.mobile.core.agent;

public enum AgentAppointmentOperation {
    CREATE("create"),
    RESCHEDULE("reschedule"),
    CANCEL("cancel");

    private final String wireValue;

    AgentAppointmentOperation(String wireValue) {
        this.wireValue = wireValue;
    }

    public String wireValue() {
        return wireValue;
    }

    public static AgentAppointmentOperation fromWireValue(String value) {
        for (AgentAppointmentOperation operation : values()) {
            if (operation.wireValue.equals(value)) {
                return operation;
            }
        }
        throw new IllegalArgumentException(
                "Agent appointment operation is invalid.");
    }
}
