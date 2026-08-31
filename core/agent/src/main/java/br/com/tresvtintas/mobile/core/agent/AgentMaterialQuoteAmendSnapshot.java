package br.com.tresvtintas.mobile.core.agent;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

public record AgentMaterialQuoteAmendSnapshot(
        List<AgentMaterialQuoteAmendItem> items,
        BigDecimal subtotal,
        BigDecimal discount,
        BigDecimal total) {
    private static final int MAXIMUM_ITEMS = 100;

    public AgentMaterialQuoteAmendSnapshot {
        items = List.copyOf(Objects.requireNonNull(
                items,
                "Agent amendment snapshot items are required."));
        if (items.isEmpty()
                || items.size() > MAXIMUM_ITEMS
                || new HashSet<>(items.stream()
                        .map(AgentMaterialQuoteAmendItem::productId)
                        .toList()).size()
                        != items.size()) {
            throw new IllegalArgumentException(
                    "Agent amendment snapshot items are invalid.");
        }
        subtotal = requireMoney(subtotal);
        discount = requireMoney(discount);
        total = requireMoney(total);
        BigDecimal lineTotal = items.stream()
                .map(AgentMaterialQuoteAmendItem::total)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (lineTotal.compareTo(subtotal) != 0
                || discount.compareTo(subtotal) > 0
                || subtotal.subtract(discount).compareTo(total) != 0) {
            throw new IllegalArgumentException(
                    "Agent amendment snapshot totals are inconsistent.");
        }
    }

    public int itemCount() {
        return items.size();
    }

    @Override
    public List<AgentMaterialQuoteAmendItem> items() {
        return List.copyOf(items);
    }

    private static BigDecimal requireMoney(BigDecimal value) {
        BigDecimal required = Objects.requireNonNull(
                value,
                "Agent amendment money is required.");
        if (required.scale() != 2
                || required.signum() < 0
                || required.compareTo(new BigDecimal("99999999.99")) > 0) {
            throw new IllegalArgumentException(
                    "Agent amendment money is invalid.");
        }
        return required;
    }
}
