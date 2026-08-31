package br.com.tresvtintas.mobile.core.network.dto;

import java.util.List;

public record LaborQuoteUpdateRequest(
        int expectedRevision,
        long customerId,
        String title,
        String notes,
        String validUntil,
        String discount,
        List<LaborQuoteLineRequest> items) {
    private static final int MINIMUM_REVISION = 1;

    public LaborQuoteUpdateRequest {
        if (expectedRevision < MINIMUM_REVISION) {
            throw new IllegalArgumentException("Labor quote revision is invalid.");
        }
        new LaborQuoteCreateRequest(
                customerId,
                title,
                notes,
                validUntil,
                discount,
                items);
        items = List.copyOf(items);
    }
}
