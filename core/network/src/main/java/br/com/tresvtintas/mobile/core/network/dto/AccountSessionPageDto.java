package br.com.tresvtintas.mobile.core.network.dto;

import java.util.List;

public record AccountSessionPageDto(
        List<AccountSessionDto> items,
        String nextCursor) {
    private static final int MAXIMUM_PAGE_SIZE = 100;

    public AccountSessionPageDto {
        if (items == null || items.size() > MAXIMUM_PAGE_SIZE) {
            throw new IllegalArgumentException(
                    "Account session page items are invalid.");
        }
        items = List.copyOf(items);
        nextCursor = DtoValidation.optionalText(
                nextCursor,
                "Account session cursor",
                256);
    }
}
