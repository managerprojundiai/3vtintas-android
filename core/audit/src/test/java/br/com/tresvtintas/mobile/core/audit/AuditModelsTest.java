package br.com.tresvtintas.mobile.core.audit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import br.com.tresvtintas.mobile.core.audit.AuditModels.Actor;
import br.com.tresvtintas.mobile.core.audit.AuditModels.Event;
import br.com.tresvtintas.mobile.core.audit.AuditModels.Page;
import br.com.tresvtintas.mobile.core.model.AppRole;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.Test;

public final class AuditModelsTest {
    @Test
    public void preservesOnlyTheSanitizedAuditProjection() {
        Event event = new Event(
                "order.status_changed",
                Optional.of("order"),
                Optional.of(new Actor("Cesar", AppRole.MASTER_ADMIN)),
                Instant.parse("2026-07-31T12:00:00Z"));
        Page page = new Page(List.of(event), Optional.of("opaque_cursor"));

        assertEquals("The safe action must be preserved.",
                "order.status_changed", page.items().get(0).action());
        assertEquals("The safe actor role must be preserved.",
                AppRole.MASTER_ADMIN,
                page.items().get(0).actor().orElseThrow().role());
        assertEquals("The opaque cursor must remain available.",
                Optional.of("opaque_cursor"), page.nextCursor());
    }

    @Test
    public void rejectsOversizedAndInvalidValues() {
        assertThrows(
                "Actions must remain within the public contract.",
                IllegalArgumentException.class,
                () -> new Event(
                        "a".repeat(101),
                        Optional.empty(),
                        Optional.empty(),
                        Instant.EPOCH));
        assertThrows(
                "Page sizes beyond the server limit must fail closed.",
                IllegalArgumentException.class,
                () -> new AuditQuery(Optional.empty(), Optional.empty(), 101));
    }
}
