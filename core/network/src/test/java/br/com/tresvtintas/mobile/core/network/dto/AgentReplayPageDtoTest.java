package br.com.tresvtintas.mobile.core.network.dto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.util.List;
import org.junit.Test;

public final class AgentReplayPageDtoTest {
    @Test
    public void acceptsOnlyTheDocumentedSafeWireValues() {
        AgentReplayPageDto.Step step = new AgentReplayPageDto.Step(
                "policy_checked",
                "blocked",
                "2026-07-31T12:00:01Z");
        AgentReplayPageDto.Turn turn = new AgentReplayPageDto.Turn(
                "whatsapp",
                "2026-07-31T12:00:00Z",
                "2026-07-31T12:00:02Z",
                "blocked",
                List.of(step),
                false);
        AgentReplayPageDto page = new AgentReplayPageDto(
                List.of(turn),
                "opaque_cursor");

        assertEquals("The safe phase must remain exact.",
                "policy_checked", page.items().get(0).steps().get(0).phase());
        assertEquals("The opaque cursor must remain exact.",
                "opaque_cursor", page.nextCursor());
    }

    @Test
    public void rejectsUnknownInternalsAndUnsafeCursors() {
        assertThrows(
                "Internal phase names must fail closed.",
                IllegalArgumentException.class,
                () -> new AgentReplayPageDto.Step(
                        "prompt_saved",
                        "ok",
                        "2026-07-31T12:00:01Z"));
        assertThrows(
                "Cursors cannot become a secondary query channel.",
                IllegalArgumentException.class,
                () -> new AgentReplayPageDto(List.of(), "next&userId=31"));
    }
}
