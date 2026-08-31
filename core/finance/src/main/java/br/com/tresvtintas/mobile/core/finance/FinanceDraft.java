package br.com.tresvtintas.mobile.core.finance;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.OptionalLong;

public record FinanceDraft(
        FinanceEntryType type,
        String title,
        BigDecimal amount,
        Optional<Instant> dueAt,
        OptionalLong customerId,
        Optional<String> notes) {
    public FinanceDraft {
        title = title == null ? "" : title.trim();
        dueAt = dueAt == null ? Optional.empty() : dueAt;
        customerId = customerId == null ? OptionalLong.empty() : customerId;
        notes = normalize(notes);
        if (type == null
                || title == null
                || title.isEmpty()
                || title.length() > 200
                || amount == null
                || amount.signum() <= 0
                || amount.scale() > 2
                || amount.precision() - amount.scale() > 8
                || (customerId.isPresent() && customerId.orElseThrow() < 1)
                || notes.map(String::length).orElse(0) > 20_000) {
            throw new IllegalArgumentException("Finance draft is invalid.");
        }
        amount = amount.setScale(2);
    }

    private static Optional<String> normalize(Optional<String> value) {
        if (value == null || value.isEmpty()) {
            return Optional.empty();
        }
        String normalized = value.orElseThrow().trim();
        return normalized.isEmpty() ? Optional.empty() : Optional.of(normalized);
    }
}
