package br.com.tresvtintas.mobile.core.agent;

public enum AgentConversationStatus {
    ACTIVE("active"),
    ARCHIVED("archived");

    private final String wireValue;

    AgentConversationStatus(String wireValue) {
        this.wireValue = wireValue;
    }

    public String wireValue() {
        return wireValue;
    }

    public static AgentConversationStatus fromWireValue(String value) {
        for (AgentConversationStatus status : values()) {
            if (status.wireValue.equals(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException(
                "Agent conversation status is invalid.");
    }
}
