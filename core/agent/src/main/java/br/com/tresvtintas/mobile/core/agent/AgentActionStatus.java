package br.com.tresvtintas.mobile.core.agent;

public enum AgentActionStatus {
    PENDING("pending"),
    EXECUTING("executing"),
    EXECUTED("executed"),
    REJECTED("rejected"),
    EXPIRED("expired"),
    SUPERSEDED("superseded");

    private final String wireValue;

    AgentActionStatus(String wireValue) {
        this.wireValue = wireValue;
    }

    public String wireValue() {
        return wireValue;
    }

    public boolean terminal() {
        return this == EXECUTED
                || this == REJECTED
                || this == EXPIRED
                || this == SUPERSEDED;
    }

    public static AgentActionStatus fromWireValue(String value) {
        for (AgentActionStatus status : values()) {
            if (status.wireValue.equals(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException(
                "Agent action status is invalid.");
    }
}
