package br.com.tresvtintas.mobile.core.agent;

public enum AgentActionDecision {
    CONFIRM("confirm", "CONFIRM_AGENT_ACTION"),
    REJECT("reject", "REJECT_AGENT_ACTION");

    private final String wireValue;
    private final String confirmation;

    AgentActionDecision(
            String wireValue,
            String confirmation) {
        this.wireValue = wireValue;
        this.confirmation = confirmation;
    }

    public String wireValue() {
        return wireValue;
    }

    public String confirmation() {
        return confirmation;
    }
}
