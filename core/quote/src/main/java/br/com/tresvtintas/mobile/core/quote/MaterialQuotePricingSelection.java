package br.com.tresvtintas.mobile.core.quote;

import java.util.Optional;
import java.util.UUID;

public record MaterialQuotePricingSelection(
        int expectedPolicyRevision,
        Optional<String> selectedPriceListVersionPublicId) {
    private static final int MINIMUM_POLICY_REVISION = 1;

    public MaterialQuotePricingSelection {
        if (expectedPolicyRevision < MINIMUM_POLICY_REVISION) {
            throw new IllegalArgumentException("Pricing policy revision is invalid.");
        }
        selectedPriceListVersionPublicId = normalize(
                selectedPriceListVersionPublicId);
    }

    private static Optional<String> normalize(Optional<String> value) {
        if (value == null || value.isEmpty()) {
            return Optional.empty();
        }
        String publicId = value.orElseThrow().trim();
        try {
            return Optional.of(UUID.fromString(publicId).toString());
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "Price-list version public ID is invalid.",
                    exception);
        }
    }
}
