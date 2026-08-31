package br.com.tresvtintas.mobile.core.agent;

public enum AgentCommissionOperation {
    APPROVE("approve"),
    CANCEL("cancel"),
    PAY("pay");

    private final String wireValue;

    AgentCommissionOperation(String wireValue) {
        this.wireValue = wireValue;
    }

    public String wireValue() {
        return wireValue;
    }

    public static AgentCommissionOperation fromWireValue(String value) {
        for (AgentCommissionOperation operation : values()) {
            if (operation.wireValue.equals(value)) {
                return operation;
            }
        }
        throw new IllegalArgumentException(
                "Agent commission operation is invalid.");
    }
}
