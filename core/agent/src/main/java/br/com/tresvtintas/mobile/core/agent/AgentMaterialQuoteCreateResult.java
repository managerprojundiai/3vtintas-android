package br.com.tresvtintas.mobile.core.agent;

import java.math.BigDecimal;
import java.util.Objects;

public record AgentMaterialQuoteCreateResult(
        long quoteId,
        int revision,
        BigDecimal total)
        implements AgentActionResult {
    public AgentMaterialQuoteCreateResult {
        if (quoteId < 1 || revision < 1) {
            throw new IllegalArgumentException(
                    "Agent create result identity is invalid.");
        }
        total = Objects.requireNonNull(
                total,
                "Agent create result total is required.");
        if (total.scale() != 2 || total.signum() < 0) {
            throw new IllegalArgumentException(
                    "Agent create result total is invalid.");
        }
    }
}
