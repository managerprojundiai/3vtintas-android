package br.com.tresvtintas.mobile.core.agent;

public enum AgentFinanceScope {
    PERSONAL("personal"),
    CORPORATE("corporate");

    private final String wireValue;

    AgentFinanceScope(String wireValue) {
        this.wireValue = wireValue;
    }

    public String wireValue() {
        return wireValue;
    }

    public static AgentFinanceScope fromWireValue(String value) {
        for (AgentFinanceScope scope : values()) {
            if (scope.wireValue.equals(value)) {
                return scope;
            }
        }
        throw new IllegalArgumentException(
                "Agent finance scope is invalid.");
    }
}
