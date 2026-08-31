package br.com.tresvtintas.mobile.core.agent;

public enum AgentTurnStatus {
    QUEUED("queued"),
    RUNNING("running"),
    COMPLETED("completed"),
    FAILED("failed"),
    CANCELLED("cancelled");

    private final String wireValue;

    AgentTurnStatus(String wireValue) {
        this.wireValue = wireValue;
    }

    public String wireValue() {
        return wireValue;
    }

    public boolean terminal() {
        return this == COMPLETED || this == FAILED || this == CANCELLED;
    }

    public static AgentTurnStatus fromWireValue(String value) {
        for (AgentTurnStatus status : values()) {
            if (status.wireValue.equals(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Agent turn status is invalid.");
    }
}
