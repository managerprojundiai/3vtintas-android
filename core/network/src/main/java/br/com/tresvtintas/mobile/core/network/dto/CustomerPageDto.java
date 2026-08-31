package br.com.tresvtintas.mobile.core.network.dto;

import java.util.List;

public record CustomerPageDto(
        List<CustomerSummaryDto> items,
        String nextCursor) {
    private static final int MAXIMUM_PAGE_SIZE = 100;

    public CustomerPageDto {
        if (items == null
                || items.size() > MAXIMUM_PAGE_SIZE
                || items.stream().anyMatch(
                item -> item == null)) {
            throw new IllegalArgumentException("Customer items are invalid.");
        }
        items = List.copyOf(items);
        nextCursor = DtoValidation.optionalText(
                nextCursor,
                "Customer cursor",
                256);
    }
}
