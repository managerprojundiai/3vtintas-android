package br.com.tresvtintas.mobile.core.agent;

public enum AgentDeliveryOperation {
    START("start"),
    COMPLETE("complete");

    private final String wireValue;

    AgentDeliveryOperation(String wireValue) {
        this.wireValue = wireValue;
    }

    public String wireValue() {
        return wireValue;
    }

    public static AgentDeliveryOperation fromWireValue(String value) {
        for (AgentDeliveryOperation operation : values()) {
            if (operation.wireValue.equals(value)) {
                return operation;
            }
        }
        throw new IllegalArgumentException(
                "Agent delivery operation is invalid.");
    }
}
