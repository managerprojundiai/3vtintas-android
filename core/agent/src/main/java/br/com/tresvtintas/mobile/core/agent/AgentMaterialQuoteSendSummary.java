package br.com.tresvtintas.mobile.core.agent;

import java.math.BigDecimal;
import java.util.Objects;

public record AgentMaterialQuoteSendSummary(
        long quoteId,
        String quoteTitle,
        String customerName,
        BigDecimal total,
        AgentActionTargetStatus targetStatus,
        boolean pricingWillBeRevalidated)
        implements AgentActionSummary {
    private static final int MAXIMUM_TEXT_CHARS = 200;
    private static final int MINIMUM_QUOTE_ID = 1;
    private static final BigDecimal MAXIMUM_TOTAL =
            new BigDecimal("99999999.99");

    public AgentMaterialQuoteSendSummary {
        if (quoteId < MINIMUM_QUOTE_ID) {
            throw new IllegalArgumentException(
                    "Agent action quote ID is invalid.");
        }
        quoteTitle = requireText(
                quoteTitle,
                "Agent action quote title is invalid.");
        customerName = requireText(
                customerName,
                "Agent action customer is invalid.");
        total = Objects.requireNonNull(
                total,
                "Agent action total is required.");
        if (total.scale() != 2
                || total.signum() < 0
                || total.compareTo(MAXIMUM_TOTAL) > 0) {
            throw new IllegalArgumentException(
                    "Agent action total is invalid.");
        }
        targetStatus = Objects.requireNonNull(
                targetStatus,
                "Agent action target status is required.");
        if (targetStatus != AgentActionTargetStatus.SENT
                || !pricingWillBeRevalidated) {
            throw new IllegalArgumentException(
                    "Agent action send summary is invalid.");
        }
    }

    private static String requireText(
            String value,
            String message) {
        if (value == null
                || value.isBlank()
                || value.length() > MAXIMUM_TEXT_CHARS) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }
}
