package br.com.tresvtintas.mobile.core.agent;

public enum AgentEventKind {
    ACCEPTED("accepted"),
    STARTED("started"),
    TOOL("tool"),
    TEXT_DELTA("text_delta"),
    COMPLETED("completed"),
    FAILED("failed"),
    CANCELLED("cancelled");

    private final String wireValue;

    AgentEventKind(String wireValue) {
        this.wireValue = wireValue;
    }

    public String wireValue() {
        return wireValue;
    }

    public boolean terminal() {
        return this == COMPLETED || this == FAILED || this == CANCELLED;
    }

    public static AgentEventKind fromWireValue(String value) {
        for (AgentEventKind kind : values()) {
            if (kind.wireValue.equals(value)) {
                return kind;
            }
        }
        throw new IllegalArgumentException("Agent event kind is invalid.");
    }
}
