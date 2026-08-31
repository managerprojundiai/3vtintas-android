package br.com.tresvtintas.mobile.core.audit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import br.com.tresvtintas.mobile.core.audit.AuditModels.Event;
import br.com.tresvtintas.mobile.core.audit.AuditModels.Page;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.Executor;
import org.junit.Test;

public final class AuditControllerTest {
    @Test
    public void filtersAndPaginatesWithoutLosingPreviousEvents() {
        Queue<Page> pages = new ArrayDeque<>();
        pages.add(new Page(List.of(event("first")), Optional.of("next")));
        pages.add(new Page(List.of(event("second")), Optional.empty()));
        AuditController controller = new AuditController(
                (query, cursor) -> pages.remove(), Runnable::run, Runnable::run);
        List<AuditState> states = new ArrayList<>();
        controller.subscribe(states::add);

        controller.apply(AuditQuery.initial().withEntity("order"));
        controller.loadMore();

        AuditState.Snapshot snapshot = states.get(states.size() - 1)
                .snapshot().orElseThrow();
        assertEquals("Both pages must be retained.", 2, snapshot.events().size());
        assertEquals("The exact entity filter must remain active.",
                Optional.of("order"), snapshot.query().entity());
        assertTrue("The terminal cursor must be empty.", snapshot.nextCursor().isEmpty());
    }

    @Test
    public void transientRefreshFailureKeepsTheLastSafeSnapshot() {
        Queue<Object> results = new ArrayDeque<>();
        results.add(new Page(List.of(event("first")), Optional.empty()));
        results.add(new AuditException(AuditFailureKind.NETWORK, "offline"));
        AuditRepository repository = (query, cursor) -> {
            Object result = results.remove();
            if (result instanceof AuditException failure) {
                throw failure;
            }
            return (Page) result;
        };
        Executor direct = Runnable::run;
        AuditController controller = new AuditController(repository, direct, direct);
        List<AuditState> states = new ArrayList<>();
        controller.subscribe(states::add);

        controller.open();
        controller.refresh();

        AuditState state = states.get(states.size() - 1);
        assertEquals("Transient failures must surface as stale.",
                AuditState.Phase.STALE, state.phase());
        assertEquals("The previously validated event must remain visible.",
                1, state.snapshot().orElseThrow().events().size());
    }

    private static Event event(String action) {
        return new Event(
                action,
                Optional.empty(),
                Optional.empty(),
                Instant.parse("2026-07-31T12:00:00Z"));
    }
}
