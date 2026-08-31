package br.com.tresvtintas.mobile.core.agent;

public enum AgentActionTargetStatus {
    SENT("sent"),
    DRAFT("draft");

    private final String wireValue;

    AgentActionTargetStatus(String wireValue) {
        this.wireValue = wireValue;
    }

    public String wireValue() {
        return wireValue;
    }

    public static AgentActionTargetStatus fromWireValue(String value) {
        for (AgentActionTargetStatus status : values()) {
            if (status.wireValue.equals(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException(
                "Agent action target status is invalid.");
    }
}
