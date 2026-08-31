package br.com.tresvtintas.mobile.core.laborquote;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;

public record LaborQuoteSummary(
        long id,
        LaborQuotePerson customer,
        LaborQuotePerson painter,
        OptionalLong organizationId,
        String title,
        LaborQuoteStatus status,
        BigDecimal subtotal,
        BigDecimal discount,
        BigDecimal total,
        int revision,
        int itemCount,
        Optional<Instant> validUntil,
        Instant createdAt,
        Instant updatedAt) {
    public LaborQuoteSummary {
        if (id < 1 || revision < 1 || itemCount < 1 || itemCount > 100) {
            throw new IllegalArgumentException("Labor quote identity is invalid.");
        }
        Objects.requireNonNull(customer, "Labor quote customer is required.");
        Objects.requireNonNull(painter, "Labor quote painter is required.");
        organizationId = organizationId == null ? OptionalLong.empty() : organizationId;
        if (organizationId.isPresent() && organizationId.getAsLong() < 1) {
            throw new IllegalArgumentException("Labor quote organization is invalid.");
        }
        if (title == null || title.isBlank() || title.length() > 200) {
            throw new IllegalArgumentException("Labor quote title is invalid.");
        }
        Objects.requireNonNull(status, "Labor quote status is required.");
        validateMoney(subtotal);
        validateMoney(discount);
        validateMoney(total);
        validUntil = validUntil == null ? Optional.empty() : validUntil;
        Objects.requireNonNull(createdAt, "Labor quote creation time is required.");
        Objects.requireNonNull(updatedAt, "Labor quote update time is required.");
    }

    private static void validateMoney(BigDecimal value) {
        if (value == null || value.scale() != 2 || value.signum() < 0) {
            throw new IllegalArgumentException("Labor quote money is invalid.");
        }
    }
}
