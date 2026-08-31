package br.com.tresvtintas.mobile.core.agent;

public enum AgentMessageRole {
    USER("user"),
    ASSISTANT("assistant");

    private final String wireValue;

    AgentMessageRole(String wireValue) {
        this.wireValue = wireValue;
    }

    public String wireValue() {
        return wireValue;
    }

    public static AgentMessageRole fromWireValue(String value) {
        for (AgentMessageRole role : values()) {
            if (role.wireValue.equals(value)) {
                return role;
            }
        }
        throw new IllegalArgumentException("Agent message role is invalid.");
    }
}
