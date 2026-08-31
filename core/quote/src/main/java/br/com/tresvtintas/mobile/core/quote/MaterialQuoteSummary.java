package br.com.tresvtintas.mobile.core.quote;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;

public record MaterialQuoteSummary(
        long id,
        MaterialQuoteCustomer customer,
        OptionalLong organizationId,
        String title,
        MaterialQuoteStatus status,
        BigDecimal subtotal,
        BigDecimal discount,
        BigDecimal total,
        int revision,
        int itemCount,
        Optional<Instant> validUntil,
        Instant createdAt,
        Instant updatedAt) {
    public MaterialQuoteSummary {
        if (id < 1 || revision < 1 || itemCount < 1 || itemCount > 100) {
            throw new IllegalArgumentException("Quote identity is invalid.");
        }
        Objects.requireNonNull(customer, "Quote customer is required.");
        organizationId = organizationId == null ? OptionalLong.empty() : organizationId;
        if (organizationId.isPresent() && organizationId.getAsLong() < 1) {
            throw new IllegalArgumentException("Quote organization is invalid.");
        }
        if (title == null || title.isBlank() || title.length() > 200) {
            throw new IllegalArgumentException("Quote title is invalid.");
        }
        Objects.requireNonNull(status, "Quote status is required.");
        validateMoney(subtotal);
        validateMoney(discount);
        validateMoney(total);
        validUntil = validUntil == null ? Optional.empty() : validUntil;
        Objects.requireNonNull(createdAt, "Quote creation time is required.");
        Objects.requireNonNull(updatedAt, "Quote update time is required.");
    }

    private static void validateMoney(BigDecimal value) {
        if (value == null || value.scale() != 2 || value.signum() < 0) {
            throw new IllegalArgumentException("Quote money is invalid.");
        }
    }
}
