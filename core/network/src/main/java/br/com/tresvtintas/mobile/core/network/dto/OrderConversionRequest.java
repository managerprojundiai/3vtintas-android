package br.com.tresvtintas.mobile.core.network.dto;

public record OrderConversionRequest(int expectedRevision, boolean confirmed) {
    public OrderConversionRequest {
        if (expectedRevision < 1 || !confirmed) {
            throw new IllegalArgumentException("Order conversion confirmation is invalid.");
        }
    }
}
