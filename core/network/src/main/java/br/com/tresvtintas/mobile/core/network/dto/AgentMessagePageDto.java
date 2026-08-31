package br.com.tresvtintas.mobile.core.network.dto;

import java.util.List;
import java.util.Objects;

public record AgentMessagePageDto(
        String conversationId,
        List<AgentMessageDto> items,
        String nextCursor) {
    private static final int MAXIMUM_PAGE_SIZE = 100;

    public AgentMessagePageDto {
        conversationId = DtoValidation.requireUuid(
                conversationId,
                "Agent message conversation ID");
        if (items == null
                || items.size() > MAXIMUM_PAGE_SIZE
                || items.stream().anyMatch(Objects::isNull)
                || !validCursor(nextCursor)) {
            throw new IllegalArgumentException(
                    "Agent message page is invalid.");
        }
        items = List.copyOf(items);
    }

    private static boolean validCursor(String value) {
        return value == null
                || value.matches("^[A-Za-z0-9_-]{1,256}$");
    }
}
