package br.com.tresvtintas.mobile.core.agent;

import br.com.tresvtintas.mobile.core.commission.CommissionStatus;
import java.util.Objects;

public record AgentCommissionResult(
        long commissionId,
        CommissionStatus status,
        int revision,
        boolean changed) implements AgentActionResult {
    public AgentCommissionResult {
        status = Objects.requireNonNull(
                status,
                "Agent commission result status is required.");
        if (commissionId <= 0 || revision <= 0) {
            throw new IllegalArgumentException(
                    "Agent commission result is invalid.");
        }
    }
}
