package br.com.tresvtintas.mobile.core.network.dto;

import java.util.List;

public record AccountDevicePageDto(
        List<AccountDeviceDto> items,
        String nextCursor) {
    private static final int MAXIMUM_PAGE_SIZE = 100;

    public AccountDevicePageDto {
        if (items == null || items.size() > MAXIMUM_PAGE_SIZE) {
            throw new IllegalArgumentException(
                    "Account device page items are invalid.");
        }
        items = List.copyOf(items);
        nextCursor = DtoValidation.optionalText(
                nextCursor,
                "Account device cursor",
                256);
    }
}
