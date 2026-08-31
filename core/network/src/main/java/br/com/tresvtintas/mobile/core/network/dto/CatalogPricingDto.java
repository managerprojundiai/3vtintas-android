package br.com.tresvtintas.mobile.core.network.dto;

import java.util.UUID;

public record CatalogPricingDto(
        long organizationId,
        String currency,
        String priceListPublicId,
        String priceListCode,
        String priceListName,
        String priceListVersionPublicId,
        int priceListVersionNumber,
        int policyRevision,
        String selectionMode) {
    public CatalogPricingDto {
        if (organizationId < 1
                || !"BRL".equals(currency)
                || priceListCode == null || priceListCode.isBlank()
                || priceListName == null || priceListName.isBlank()
                || priceListVersionNumber < 1
                || policyRevision < 1
                || selectionMode == null || selectionMode.isBlank()) {
            throw new IllegalArgumentException("Catalog pricing response is invalid.");
        }
        priceListPublicId = UUID.fromString(priceListPublicId).toString();
        priceListVersionPublicId = UUID.fromString(priceListVersionPublicId).toString();
    }
}
