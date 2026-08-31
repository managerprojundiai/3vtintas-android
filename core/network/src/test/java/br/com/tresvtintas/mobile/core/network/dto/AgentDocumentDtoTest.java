package br.com.tresvtintas.mobile.core.network.dto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.util.List;
import org.junit.Test;

public final class AgentDocumentDtoTest {
    @Test
    public void acceptsTheProtectedWireContract() {
        AgentDocumentDto document = new AgentDocumentDto(
                "labor_quote_pdf",
                92,
                "orcamento-000092-labor.pdf");
        AgentMessageDto message = new AgentMessageDto(
                1,
                "assistant",
                "PDF autorizado.",
                List.of(document),
                List.of(),
                "00000000-0000-4000-8000-000000000102",
                "2026-07-27T12:00:00Z");

        assertEquals(
                "One protected document must be retained.",
                List.of(document),
                message.documents());
    }

    @Test
    public void rejectsUrlsPathsAndDocumentsOnUserMessages() {
        assertThrows(
                "A forged path must fail at the DTO boundary.",
                IllegalArgumentException.class,
                () -> new AgentDocumentDto(
                        "material_quote_pdf",
                        91,
                        "https://forbidden.example/file.pdf"));
        AgentDocumentDto valid = new AgentDocumentDto(
                "material_quote_pdf",
                91,
                "orcamento-000091-material.pdf");
        assertThrows(
                "A user message cannot inject a document.",
                IllegalArgumentException.class,
                () -> new AgentMessageDto(
                        2,
                        "user",
                        "Mensagem",
                        List.of(valid),
                        List.of(),
                        null,
                        "2026-07-27T12:00:00Z"));
    }
}
