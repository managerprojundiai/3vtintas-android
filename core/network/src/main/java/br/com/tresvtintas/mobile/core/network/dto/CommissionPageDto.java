package br.com.tresvtintas.mobile.core.network.dto;

import java.util.List;

public record CommissionPageDto(
        List<CommissionSummaryDto> items,
        CommissionOverviewDto overview,
        String nextCursor) {
    public CommissionPageDto {
        if (items == null
                || items.size() > 100
                || items.contains(null)
                || overview == null) {
            throw new IllegalArgumentException("Commission page is invalid.");
        }
        items = List.copyOf(items);
        nextCursor = DtoValidation.optionalText(
                nextCursor,
                "Commission cursor",
                96);
    }
}
