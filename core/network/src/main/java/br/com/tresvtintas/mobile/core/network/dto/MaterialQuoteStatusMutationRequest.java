package br.com.tresvtintas.mobile.core.network.dto;

import java.util.Set;

public record MaterialQuoteStatusMutationRequest(
        int expectedRevision,
        String status) {
    private static final Set<String> MANUAL_STATUSES = Set.of(
            "draft", "sent", "accepted", "rejected", "expired");

    public MaterialQuoteStatusMutationRequest {
        if (expectedRevision < 1 || !MANUAL_STATUSES.contains(status)) {
            throw new IllegalArgumentException(
                    "Quote status mutation request is invalid.");
        }
    }
}
