package br.com.tresvtintas.mobile.core.network.dto;

public record AgentCreateTurnRequest(String message) {
    private static final int MAXIMUM_MESSAGE_CHARS = 4_000;

    public AgentCreateTurnRequest {
        message = DtoValidation.requireText(
                message,
                "Agent message",
                MAXIMUM_MESSAGE_CHARS);
    }
}
