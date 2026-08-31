package br.com.tresvtintas.mobile.core.agent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.Test;

public final class AgentDocumentTest {
    @Test
    public void acceptsOnlyDeterministicQuoteDocumentNames() {
        AgentDocument material = new AgentDocument(
                AgentDocumentType.MATERIAL_QUOTE_PDF,
                91,
                "orcamento-000091-material.pdf");
        AgentDocument labor = new AgentDocument(
                AgentDocumentType.LABOR_QUOTE_PDF,
                1_234_567,
                "orcamento-1234567-labor.pdf");

        assertEquals(
                "Material filename must be deterministic.",
                "orcamento-000091-material.pdf",
                material.filename());
        assertEquals(
                "Labor filename must be deterministic.",
                "orcamento-1234567-labor.pdf",
                labor.filename());
    }

    @Test
    public void rejectsForgedFilenameAndUserAttachment() {
        assertThrows(
                "Paths from the network must never be accepted.",
                IllegalArgumentException.class,
                () -> new AgentDocument(
                        AgentDocumentType.MATERIAL_QUOTE_PDF,
                        91,
                        "../forged.pdf"));
        AgentDocument valid = new AgentDocument(
                AgentDocumentType.MATERIAL_QUOTE_PDF,
                91,
                "orcamento-000091-material.pdf");
        assertThrows(
                "User messages cannot carry server documents.",
                IllegalArgumentException.class,
                () -> new AgentMessage(
                        1,
                        AgentMessageRole.USER,
                        "Mensagem",
                        List.of(valid),
                        List.of(),
                        Optional.empty(),
                        Instant.EPOCH));
    }
}
