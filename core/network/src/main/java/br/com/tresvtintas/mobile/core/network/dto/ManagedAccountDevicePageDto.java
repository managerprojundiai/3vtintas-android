package br.com.tresvtintas.mobile.core.network.dto;

import java.util.List;

public record ManagedAccountDevicePageDto(
        ManagedSecurityTargetDto target,
        List<AccountDeviceDto> items,
        String nextCursor) {
    private static final int MAXIMUM_PAGE_SIZE = 100;

    public ManagedAccountDevicePageDto {
        if (target == null
                || items == null
                || items.size() > MAXIMUM_PAGE_SIZE) {
            throw new IllegalArgumentException(
                    "Managed account device page is invalid.");
        }
        items = List.copyOf(items);
        nextCursor = DtoValidation.optionalText(
                nextCursor,
                "Managed account device cursor",
                256);
    }
}
