package br.com.tresvtintas.mobile.core.network.dto;

public record MaterialQuoteProductStateDto(
        boolean active,
        String currentPrice,
        Integer stock) {
    public MaterialQuoteProductStateDto {
        if (currentPrice != null) {
            currentPrice = MaterialQuoteSummaryDto.money(currentPrice);
        }
    }
}
