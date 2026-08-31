package br.com.tresvtintas.mobile.core.quote;

import java.util.Optional;
import java.util.OptionalInt;

public record MaterialQuotePreviewPricing(
        boolean resolved,
        Optional<String> priceListName,
        OptionalInt versionNumber) {
    public MaterialQuotePreviewPricing {
        priceListName = priceListName == null ? Optional.empty() : priceListName;
        versionNumber = versionNumber == null ? OptionalInt.empty() : versionNumber;
        if (resolved && (priceListName.isEmpty() || versionNumber.isEmpty()
                || priceListName.orElseThrow().isBlank()
                || versionNumber.orElseThrow() < 1)) {
            throw new IllegalArgumentException("Resolved preview pricing is incomplete.");
        }
        if (!resolved && (priceListName.isPresent() || versionNumber.isPresent())) {
            throw new IllegalArgumentException("Legacy preview pricing must be empty.");
        }
    }
}
