package br.com.tresvtintas.mobile.core.agent;

import br.com.tresvtintas.mobile.core.finance.FinanceEntryStatus;
import java.util.Objects;

public record AgentFinanceResult(
        AgentFinanceScope scope,
        long entryId,
        FinanceEntryStatus status,
        boolean changed) implements AgentActionResult {
    public AgentFinanceResult {
        scope = Objects.requireNonNull(
                scope,
                "Agent finance result scope is required.");
        status = Objects.requireNonNull(
                status,
                "Agent finance result status is required.");
        if (entryId <= 0) {
            throw new IllegalArgumentException(
                    "Agent finance result entry ID is invalid.");
        }
    }
}
