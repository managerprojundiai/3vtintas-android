package br.com.tresvtintas.mobile.core.quote;

import java.time.Instant;
import java.util.Optional;
import java.util.OptionalInt;

public record MaterialQuotePricingSnapshot(
        boolean resolved,
        Optional<String> priceListCode,
        Optional<String> priceListName,
        Optional<String> priceListVersionPublicId,
        OptionalInt priceListVersionNumber,
        OptionalInt policyRevision,
        Optional<String> selectionMode,
        Optional<Instant> resolvedAt) {
    public MaterialQuotePricingSnapshot {
        priceListCode = optional(priceListCode);
        priceListName = optional(priceListName);
        priceListVersionPublicId = optional(priceListVersionPublicId);
        priceListVersionNumber = priceListVersionNumber == null
                ? OptionalInt.empty() : priceListVersionNumber;
        policyRevision = policyRevision == null ? OptionalInt.empty() : policyRevision;
        selectionMode = optional(selectionMode);
        resolvedAt = resolvedAt == null ? Optional.empty() : resolvedAt;
        boolean complete = priceListCode.isPresent() && priceListName.isPresent()
                && priceListVersionPublicId.isPresent()
                && priceListVersionNumber.isPresent() && policyRevision.isPresent()
                && selectionMode.isPresent() && resolvedAt.isPresent();
        if (resolved != complete) {
            throw new IllegalArgumentException("Quote pricing snapshot is inconsistent.");
        }
    }

    private static <T> Optional<T> optional(Optional<T> value) {
        return value == null ? Optional.empty() : value;
    }
}
