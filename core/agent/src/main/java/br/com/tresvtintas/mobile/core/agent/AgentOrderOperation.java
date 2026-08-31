package br.com.tresvtintas.mobile.core.agent;

public enum AgentOrderOperation {
    CONFIRM("confirm"),
    START_FULFILLMENT("start_fulfillment"),
    COMPLETE("complete");

    private final String wireValue;

    AgentOrderOperation(String wireValue) {
        this.wireValue = wireValue;
    }

    public String wireValue() {
        return wireValue;
    }

    public static AgentOrderOperation fromWireValue(String value) {
        for (AgentOrderOperation operation : values()) {
            if (operation.wireValue.equals(value)) {
                return operation;
            }
        }
        throw new IllegalArgumentException(
                "Agent order operation is invalid.");
    }
}
