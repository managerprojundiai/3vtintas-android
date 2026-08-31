package br.com.tresvtintas.mobile.core.network.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record OrderCancellationRequest(
        int expectedRevision,
        String reason,
        boolean confirmed) {
    public OrderCancellationRequest {
        if (expectedRevision < 1 || !confirmed) {
            throw new IllegalArgumentException(
                    "Order cancellation request is invalid.");
        }
        reason = DtoValidation.optionalText(reason, "Order cancellation reason", 1_000);
    }
}
