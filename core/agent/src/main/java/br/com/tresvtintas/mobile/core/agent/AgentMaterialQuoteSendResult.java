package br.com.tresvtintas.mobile.core.agent;

public record AgentMaterialQuoteSendResult(
        long quoteId,
        int revision,
        boolean pricingChanged)
        implements AgentActionResult {
    public AgentMaterialQuoteSendResult {
        if (quoteId < 1 || revision < 1) {
            throw new IllegalArgumentException(
                    "Agent send result identity is invalid.");
        }
    }
}
