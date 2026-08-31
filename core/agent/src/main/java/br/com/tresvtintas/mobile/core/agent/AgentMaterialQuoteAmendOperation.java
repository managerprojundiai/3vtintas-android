package br.com.tresvtintas.mobile.core.agent;

public enum AgentMaterialQuoteAmendOperation {
    ADD("add"),
    UPDATE_QUANTITY("update_quantity"),
    REMOVE("remove");

    private final String wireValue;

    AgentMaterialQuoteAmendOperation(String wireValue) {
        this.wireValue = wireValue;
    }

    public String wireValue() {
        return wireValue;
    }

    public static AgentMaterialQuoteAmendOperation fromWireValue(
            String value) {
        for (AgentMaterialQuoteAmendOperation operation : values()) {
            if (operation.wireValue.equals(value)) {
                return operation;
            }
        }
        throw new IllegalArgumentException(
                "Agent quote amendment operation is invalid.");
    }
}
