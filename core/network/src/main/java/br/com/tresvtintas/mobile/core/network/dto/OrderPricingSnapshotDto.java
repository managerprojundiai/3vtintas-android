package br.com.tresvtintas.mobile.core.network.dto;

public record OrderPricingSnapshotDto(
        String state,
        String priceListCode,
        String priceListName,
        String priceListVersionPublicId,
        Integer priceListVersionNumber,
        Integer policyRevision,
        String selectionMode,
        String resolvedAt) {
    public OrderPricingSnapshotDto {
        state = DtoValidation.requireText(state, "Order pricing state", 16);
        priceListCode = DtoValidation.optionalText(priceListCode, "Order price list code", 80);
        priceListName = DtoValidation.optionalText(priceListName, "Order price list name", 160);
        priceListVersionPublicId = DtoValidation.optionalText(
                priceListVersionPublicId, "Order price list version ID", 36);
        selectionMode = DtoValidation.optionalText(
                selectionMode, "Order price selection mode", 32);
        resolvedAt = DtoValidation.optionalText(
                resolvedAt, "Order price resolution date", 64);
        boolean complete = priceListCode != null
                && priceListName != null
                && priceListVersionPublicId != null
                && priceListVersionNumber != null
                && priceListVersionNumber > 0
                && policyRevision != null
                && policyRevision > 0
                && selectionMode != null
                && resolvedAt != null;
        if ("RESOLVED".equals(state) && !complete) {
            throw new IllegalArgumentException("Resolved order pricing snapshot is incomplete.");
        }
        if ("LEGACY".equals(state) && complete) {
            throw new IllegalArgumentException("Legacy order pricing snapshot must be empty.");
        }
        if (!"LEGACY".equals(state) && !"RESOLVED".equals(state)) {
            throw new IllegalArgumentException("Order pricing snapshot state is invalid.");
        }
        if ("LEGACY".equals(state)
                && (priceListCode != null
                || priceListName != null
                || priceListVersionPublicId != null
                || priceListVersionNumber != null
                || policyRevision != null
                || selectionMode != null
                || resolvedAt != null)) {
            throw new IllegalArgumentException("Legacy order pricing snapshot must be empty.");
        }
    }
}
