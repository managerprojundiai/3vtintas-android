package br.com.tresvtintas.mobile.core.agent;

public enum AgentFinanceOperation {
    CREATE("create"),
    SETTLE("settle"),
    CANCEL("cancel");

    private final String wireValue;

    AgentFinanceOperation(String wireValue) {
        this.wireValue = wireValue;
    }

    public String wireValue() {
        return wireValue;
    }

    public static AgentFinanceOperation fromWireValue(String value) {
        for (AgentFinanceOperation operation : values()) {
            if (operation.wireValue.equals(value)) {
                return operation;
            }
        }
        throw new IllegalArgumentException(
                "Agent finance operation is invalid.");
    }
}
