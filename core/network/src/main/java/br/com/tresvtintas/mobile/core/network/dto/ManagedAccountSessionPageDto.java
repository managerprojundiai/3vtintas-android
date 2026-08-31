package br.com.tresvtintas.mobile.core.network.dto;

import java.util.List;

public record ManagedAccountSessionPageDto(
        ManagedSecurityTargetDto target,
        List<AccountSessionDto> items,
        String nextCursor) {
    private static final int MAXIMUM_PAGE_SIZE = 100;

    public ManagedAccountSessionPageDto {
        if (target == null
                || items == null
                || items.size() > MAXIMUM_PAGE_SIZE) {
            throw new IllegalArgumentException(
                    "Managed account session page is invalid.");
        }
        items = List.copyOf(items);
        nextCursor = DtoValidation.optionalText(
                nextCursor,
                "Managed account session cursor",
                256);
    }
}
