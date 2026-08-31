package br.com.tresvtintas.mobile.core.quote;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.HashSet;

public record MaterialQuoteDraft(
        long customerId,
        String customerName,
        Optional<String> title,
        Optional<String> notes,
        Optional<LocalDate> validUntil,
        Optional<MaterialQuotePricingSelection> pricing,
        List<MaterialQuoteDraftLine> items) {
    public MaterialQuoteDraft {
        if (customerId < 1 || customerName == null || customerName.isBlank()) {
            throw new IllegalArgumentException("Quote draft customer is invalid.");
        }
        title = normalize(title, 200);
        notes = normalize(notes, 4_000);
        validUntil = validUntil == null ? Optional.empty() : validUntil;
        pricing = pricing == null ? Optional.empty() : pricing;
        if (items == null || items.isEmpty() || items.size() > 100) {
            throw new IllegalArgumentException("Quote draft items are invalid.");
        }
        items = List.copyOf(items);
        Set<String> lineIdentities = new HashSet<>();
        if (items.stream().anyMatch(item ->
                !lineIdentities.add(item.identityKey()))) {
            throw new IllegalArgumentException("Quote line identity cannot repeat.");
        }
    }

    public MaterialQuoteDraft(
            long customerId,
            String customerName,
            Optional<String> title,
            Optional<String> notes,
            Optional<LocalDate> validUntil,
            List<MaterialQuoteDraftLine> items) {
        this(
                customerId,
                customerName,
                title,
                notes,
                validUntil,
                Optional.empty(),
                items);
    }

    private static Optional<String> normalize(
            Optional<String> value,
            int maximumLength) {
        if (value == null || value.isEmpty()) {
            return Optional.empty();
        }
        String normalized = value.orElseThrow().trim();
        if (normalized.isEmpty()) {
            return Optional.empty();
        }
        if (normalized.length() > maximumLength) {
            throw new IllegalArgumentException("Quote text is too long.");
        }
        return Optional.of(normalized);
    }
}
