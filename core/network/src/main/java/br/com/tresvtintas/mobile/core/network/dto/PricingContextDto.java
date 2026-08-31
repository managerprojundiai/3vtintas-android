package br.com.tresvtintas.mobile.core.network.dto;

import java.util.List;

public record PricingContextDto(
        long organizationId,
        boolean enabled,
        String engineMode,
        int policyRevision,
        boolean selectionRequired,
        List<PricingSelectionDto> selections) {
    public PricingContextDto {
        if (organizationId < 1 || policyRevision < 1 || selections == null) {
            throw new IllegalArgumentException("Pricing context response is invalid.");
        }
        selections = List.copyOf(selections);
    }

    public record PricingSelectionDto(
            String priceListPublicId,
            String priceListCode,
            String priceListName,
            String priceListVersionPublicId,
            int priceListVersionNumber,
            boolean isPrimary) {
    }
}
