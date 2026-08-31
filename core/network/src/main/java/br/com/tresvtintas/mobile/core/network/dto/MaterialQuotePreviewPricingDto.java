package br.com.tresvtintas.mobile.core.network.dto;

public record MaterialQuotePreviewPricingDto(
        String state,
        String priceListPublicId,
        String priceListCode,
        String priceListName,
        String priceListVersionPublicId,
        Integer priceListVersionNumber,
        Integer policyRevision,
        String selectionMode) {
    public MaterialQuotePreviewPricingDto {
        state = DtoValidation.requireText(state, "Preview pricing state", 16);
        priceListPublicId = DtoValidation.optionalText(
                priceListPublicId, "Preview price list ID", 36);
        priceListCode = DtoValidation.optionalText(
                priceListCode, "Preview price list code", 80);
        priceListName = DtoValidation.optionalText(
                priceListName, "Preview price list name", 160);
        priceListVersionPublicId = DtoValidation.optionalText(
                priceListVersionPublicId, "Preview price list version ID", 36);
        selectionMode = DtoValidation.optionalText(
                selectionMode, "Preview price selection mode", 32);
        boolean complete = priceListPublicId != null
                && priceListCode != null
                && priceListName != null
                && priceListVersionPublicId != null
                && priceListVersionNumber != null
                && priceListVersionNumber > 0
                && policyRevision != null
                && policyRevision > 0
                && selectionMode != null;
        if ("RESOLVED".equals(state) && !complete) {
            throw new IllegalArgumentException("Resolved preview pricing is incomplete.");
        }
        if ("LEGACY".equals(state) && (priceListPublicId != null
                || priceListCode != null
                || priceListName != null
                || priceListVersionPublicId != null
                || priceListVersionNumber != null
                || policyRevision != null
                || selectionMode != null)) {
            throw new IllegalArgumentException("Legacy preview pricing must be empty.");
        }
        if (!"LEGACY".equals(state) && !"RESOLVED".equals(state)) {
            throw new IllegalArgumentException("Preview pricing state is invalid.");
        }
    }
}
