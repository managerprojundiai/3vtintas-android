package br.com.tresvtintas.mobile.core.network.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record MaterialQuotePricingSelectionRequest(
        int expectedPolicyRevision,
        String selectedPriceListVersionPublicId) {
    private static final int MINIMUM_POLICY_REVISION = 1;

    public MaterialQuotePricingSelectionRequest {
        if (expectedPolicyRevision < MINIMUM_POLICY_REVISION) {
            throw new IllegalArgumentException("Pricing policy revision is invalid.");
        }
        if (selectedPriceListVersionPublicId != null) {
            selectedPriceListVersionPublicId = UUID.fromString(
                    selectedPriceListVersionPublicId).toString();
        }
    }
}
