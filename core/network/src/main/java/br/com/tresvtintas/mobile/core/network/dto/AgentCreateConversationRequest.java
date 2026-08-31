package br.com.tresvtintas.mobile.core.network.dto;

public record AgentCreateConversationRequest(String title) {
    private static final int MAXIMUM_TITLE_CHARS = 120;

    public AgentCreateConversationRequest {
        title = DtoValidation.requireText(
                title,
                "Agent conversation title",
                MAXIMUM_TITLE_CHARS);
    }
}
