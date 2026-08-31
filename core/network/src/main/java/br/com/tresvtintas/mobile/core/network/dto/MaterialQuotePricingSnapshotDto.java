package br.com.tresvtintas.mobile.core.network.dto;

public record MaterialQuotePricingSnapshotDto(
        String state,
        String priceListCode,
        String priceListName,
        String priceListVersionPublicId,
        Integer priceListVersionNumber,
        Integer policyRevision,
        String selectionMode,
        String resolvedAt) {
    public MaterialQuotePricingSnapshotDto {
        state = DtoValidation.requireText(state, "Pricing state", 16);
        priceListCode = DtoValidation.optionalText(priceListCode, "Price list code", 80);
        priceListName = DtoValidation.optionalText(priceListName, "Price list name", 160);
        priceListVersionPublicId = DtoValidation.optionalText(
                priceListVersionPublicId, "Price list version ID", 36);
        selectionMode = DtoValidation.optionalText(selectionMode, "Price selection mode", 32);
        resolvedAt = DtoValidation.optionalText(resolvedAt, "Price resolution date", 64);
        if ("RESOLVED".equals(state)
                && (priceListCode == null || priceListName == null
                || priceListVersionPublicId == null
                || priceListVersionNumber == null || priceListVersionNumber < 1
                || policyRevision == null || policyRevision < 1
                || selectionMode == null || resolvedAt == null)) {
            throw new IllegalArgumentException("Resolved pricing snapshot is incomplete.");
        }
        if ("LEGACY".equals(state)
                && (priceListCode != null || priceListName != null
                || priceListVersionPublicId != null
                || priceListVersionNumber != null || policyRevision != null
                || selectionMode != null || resolvedAt != null)) {
            throw new IllegalArgumentException("Legacy pricing snapshot must be empty.");
        }
        if (!"LEGACY".equals(state) && !"RESOLVED".equals(state)) {
            throw new IllegalArgumentException("Pricing snapshot state is invalid.");
        }
    }
}
