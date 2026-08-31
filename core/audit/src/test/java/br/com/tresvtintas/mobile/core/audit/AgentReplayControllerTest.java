package br.com.tresvtintas.mobile.core.audit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import br.com.tresvtintas.mobile.core.audit.AgentReplayModels.Channel;
import br.com.tresvtintas.mobile.core.audit.AgentReplayModels.ChannelFilter;
import br.com.tresvtintas.mobile.core.audit.AgentReplayModels.Outcome;
import br.com.tresvtintas.mobile.core.audit.AgentReplayModels.Page;
import br.com.tresvtintas.mobile.core.audit.AgentReplayModels.Turn;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import org.junit.Test;

public final class AgentReplayControllerTest {
    @Test
    public void filtersAndPaginatesWithoutLosingValidatedTurns() {
        Queue<Page> pages = new ArrayDeque<>();
        pages.add(AgentReplayModels.page(
                List.of(turn("2026-07-31T12:00:00Z")),
                Optional.of("next")));
        pages.add(AgentReplayModels.page(
                List.of(turn("2026-07-31T11:00:00Z")),
                Optional.empty()));
        AgentReplayController controller = new AgentReplayController(
                (query, cursor) -> pages.remove(),
                Runnable::run,
                Runnable::run);
        List<AgentReplayState> states = new ArrayList<>();
        controller.subscribe(states::add);

        controller.apply(AgentReplayQuery.initial().withChannel(ChannelFilter.WHATSAPP));
        controller.loadMore();

        AgentReplayState.Snapshot snapshot = states.get(states.size() - 1)
                .snapshot().orElseThrow();
        assertEquals("Both sanitized pages must remain visible.",
                2, snapshot.turns().size());
        assertEquals("The exact channel filter must remain active.",
                ChannelFilter.WHATSAPP, snapshot.query().channel());
        assertTrue("The terminal cursor must be empty.",
                snapshot.nextCursor().isEmpty());
    }

    @Test
    public void transientRefreshFailureKeepsTheLastSafeSnapshot() {
        Queue<Object> results = new ArrayDeque<>();
        results.add(AgentReplayModels.page(
                List.of(turn("2026-07-31T12:00:00Z")),
                Optional.empty()));
        results.add(new AuditException(AuditFailureKind.NETWORK, "offline"));
        AgentReplayRepository repository = (query, cursor) -> {
            Object result = results.remove();
            if (result instanceof AuditException failure) {
                throw failure;
            }
            return (Page) result;
        };
        AgentReplayController controller = new AgentReplayController(
                repository,
                Runnable::run,
                Runnable::run);
        List<AgentReplayState> states = new ArrayList<>();
        controller.subscribe(states::add);

        controller.open();
        controller.refresh();

        AgentReplayState state = states.get(states.size() - 1);
        assertEquals("Transient failures must surface as stale.",
                AgentReplayState.Phase.STALE, state.phase());
        assertEquals("The previously validated turn must remain visible.",
                1, state.snapshot().orElseThrow().turns().size());
    }

    private static Turn turn(String startedAt) {
        Instant start = Instant.parse(startedAt);
        return new Turn(
                Channel.WHATSAPP,
                start,
                start.plusSeconds(2),
                Outcome.COMPLETED,
                List.of(),
                false);
    }
}
