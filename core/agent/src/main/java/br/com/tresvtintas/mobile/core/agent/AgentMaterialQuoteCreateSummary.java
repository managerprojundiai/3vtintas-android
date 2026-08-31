package br.com.tresvtintas.mobile.core.agent;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record AgentMaterialQuoteCreateSummary(
        String quoteTitle,
        String customerName,
        Optional<String> notes,
        Optional<LocalDate> validUntil,
        List<AgentMaterialQuoteCreateItem> items,
        BigDecimal subtotal,
        BigDecimal total,
        AgentActionTargetStatus targetStatus,
        boolean pricingWillBeRevalidated)
        implements AgentActionSummary {
    private static final int MAXIMUM_ITEMS = 100;
    private static final int MAXIMUM_NOTES_LENGTH = 4_000;

    public AgentMaterialQuoteCreateSummary {
        quoteTitle = requireText(quoteTitle);
        customerName = requireText(customerName);
        notes = Objects.requireNonNull(
                notes,
                "Agent action notes are required.");
        notes.ifPresent(value -> {
            if (value.length() > MAXIMUM_NOTES_LENGTH) {
                throw new IllegalArgumentException(
                        "Agent action notes are invalid.");
            }
        });
        validUntil = Objects.requireNonNull(
                validUntil,
                "Agent action validity is required.");
        items = List.copyOf(Objects.requireNonNull(
                items,
                "Agent action items are required."));
        if (items.isEmpty() || items.size() > MAXIMUM_ITEMS) {
            throw new IllegalArgumentException(
                    "Agent action items are invalid.");
        }
        subtotal = requireMoney(subtotal);
        total = requireMoney(total);
        BigDecimal itemTotal = items.stream()
                .map(AgentMaterialQuoteCreateItem::total)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (subtotal.compareTo(total) != 0
                || itemTotal.compareTo(subtotal) != 0) {
            throw new IllegalArgumentException(
                    "Agent action totals are inconsistent.");
        }
        targetStatus = Objects.requireNonNull(
                targetStatus,
                "Agent action target status is required.");
        if (targetStatus != AgentActionTargetStatus.DRAFT
                || !pricingWillBeRevalidated) {
            throw new IllegalArgumentException(
                    "Agent action create summary is invalid.");
        }
    }

    public int itemCount() {
        return items.size();
    }

    private static String requireText(String value) {
        if (value == null
                || value.isBlank()
                || value.length() > 200) {
            throw new IllegalArgumentException(
                    "Agent action text is invalid.");
        }
        return value;
    }

    private static BigDecimal requireMoney(BigDecimal value) {
        BigDecimal required = Objects.requireNonNull(
                value,
                "Agent action money is required.");
        if (required.scale() != 2
                || required.signum() < 0
                || required.compareTo(new BigDecimal("99999999.99")) > 0) {
            throw new IllegalArgumentException(
                    "Agent action money is invalid.");
        }
        return required;
    }
}
