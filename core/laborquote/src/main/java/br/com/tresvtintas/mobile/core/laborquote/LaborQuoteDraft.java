package br.com.tresvtintas.mobile.core.laborquote;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public record LaborQuoteDraft(
        long customerId,
        String customerName,
        Optional<String> title,
        Optional<String> notes,
        Optional<LocalDate> validUntil,
        BigDecimal discount,
        List<LaborQuoteDraftLine> items) {
    public LaborQuoteDraft {
        if (customerId < 1 || customerName == null || customerName.isBlank()) {
            throw new IllegalArgumentException("Labor quote customer is invalid.");
        }
        title = normalize(title, 200);
        notes = normalize(notes, 4_000);
        validUntil = validUntil == null ? Optional.empty() : validUntil;
        if (discount == null || discount.signum() < 0 || discount.scale() > 2) {
            throw new IllegalArgumentException("Labor quote discount is invalid.");
        }
        discount = discount.setScale(2);
        if (items == null || items.isEmpty() || items.size() > 100) {
            throw new IllegalArgumentException("Labor quote items are invalid.");
        }
        items = List.copyOf(items);
        if (discount.compareTo(subtotal(items)) > 0) {
            throw new IllegalArgumentException("Labor quote discount exceeds subtotal.");
        }
    }

    public BigDecimal subtotal() {
        return subtotal(items);
    }

    public BigDecimal total() {
        return subtotal().subtract(discount).setScale(2);
    }

    private static BigDecimal subtotal(List<LaborQuoteDraftLine> lines) {
        return lines.stream()
                .map(LaborQuoteDraftLine::total)
                .reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add);
    }

    private static Optional<String> normalize(Optional<String> value, int maximum) {
        if (value == null || value.isEmpty()) {
            return Optional.empty();
        }
        String normalized = value.orElseThrow().trim();
        if (normalized.isEmpty()) {
            return Optional.empty();
        }
        if (normalized.length() > maximum) {
            throw new IllegalArgumentException("Labor quote text is too long.");
        }
        return Optional.of(normalized);
    }
}
