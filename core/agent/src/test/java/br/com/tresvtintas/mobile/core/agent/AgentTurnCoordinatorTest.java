package br.com.tresvtintas.mobile.core.agent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public final class AgentTurnCoordinatorTest {
    private static final String CONVERSATION_ID =
            "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa";
    private static final String TURN_ID =
            "bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb";
    private static final String SEND_KEY =
            "cccccccc-cccc-4ccc-8ccc-cccccccccccc";
    private static final String CANCEL_KEY =
            "dddddddd-dddd-4ddd-8ddd-dddddddddddd";
    private static final Instant NOW =
            Instant.parse("2026-07-27T12:00:00Z");
    private ScheduledExecutorService scheduler;
    private FakeRepository repository;
    private FakeEventSource source;
    private AgentTurnCoordinator coordinator;

    @Before
    public void setUp() {
        scheduler = Executors.newSingleThreadScheduledExecutor();
        repository = new FakeRepository();
        source = new FakeEventSource();
        AtomicInteger keys = new AtomicInteger();
        coordinator = new AgentTurnCoordinator(
                repository,
                source,
                Runnable::run,
                scheduler,
                Runnable::run,
                () -> keys.getAndIncrement() == 0
                        ? SEND_KEY
                        : CANCEL_KEY,
                () -> 0.5);
    }

    @After
    public void tearDown() {
        coordinator.close();
        scheduler.shutdownNow();
    }

    @Test
    public void streamsPartialTextAndClearsItOnWorkerRetry()
            throws Exception {
        Probe probe = new Probe();
        try (AgentEventSubscription ignored =
                     coordinator.observe(CONVERSATION_ID, probe)) {
            coordinator.send(CONVERSATION_ID, "Consulte o preço");
            assertTrue(
                    "Agent stream must be opened.",
                    source.awaitSubscription());

            source.event(event(
                    1,
                    AgentEventKind.ACCEPTED,
                    Optional.empty(),
                    false));
            source.event(event(
                    2,
                    AgentEventKind.STARTED,
                    Optional.empty(),
                    false));
            source.event(event(
                    3,
                    AgentEventKind.TEXT_DELTA,
                    Optional.of("Resposta antiga"),
                    false));
            source.event(event(
                    4,
                    AgentEventKind.STARTED,
                    Optional.empty(),
                    true));
            source.event(event(
                    5,
                    AgentEventKind.TEXT_DELTA,
                    Optional.of("Resposta nova"),
                    false));
            source.event(event(
                    6,
                    AgentEventKind.COMPLETED,
                    Optional.empty(),
                    false));

            AgentTurnState state = coordinator.currentState();
            assertEquals(
                    "Turn must end as completed.",
                    AgentTurnState.Phase.COMPLETED,
                    state.phase());
            assertEquals(
                    "Retry marker must discard stale partial text.",
                    "Resposta nova",
                    state.partialText());
            assertEquals(
                    "Latest durable cursor must be retained.",
                    Optional.of("6"),
                    state.lastEventId());
            assertTrue(
                    "History must refresh after enqueue and completion.",
                    probe.invalidations.get() >= 2);

            coordinator.acknowledgeHistory(CONVERSATION_ID);

            AgentTurnState acknowledged =
                    coordinator.currentState();
            assertEquals(
                    "Acknowledged turn must remain completed.",
                    AgentTurnState.Phase.COMPLETED,
                    acknowledged.phase());
            assertEquals(
                    "Confirmed history must release the streamed copy.",
                    "",
                    acknowledged.partialText());
            assertEquals(
                    "Acknowledgement must retain the durable cursor.",
                    Optional.of("6"),
                    acknowledged.lastEventId());
        }
    }

    @Test
    public void closesStreamWhenScreenStopsWithoutCancellingTurn()
            throws Exception {
        try (AgentEventSubscription observation =
                     coordinator.observe(CONVERSATION_ID, new Probe())) {
            coordinator.send(CONVERSATION_ID, "Acompanhe");
            assertTrue(
                    "Agent stream must be opened.",
                    source.awaitSubscription());

            observation.close();

            assertTrue(
                    "Foreground stream must close with the screen.",
                    source.awaitClosed());
            assertEquals(
                    "Server-side turn must remain queued.",
                    AgentTurnState.Phase.QUEUED,
                    coordinator.currentState().phase());
        }
    }

    private static AgentEvent event(
            int sequence,
            AgentEventKind kind,
            Optional<String> delta,
            boolean reset) {
        return new AgentEvent(
                TURN_ID,
                sequence,
                kind,
                NOW.plusSeconds(sequence),
                delta,
                reset,
                kind == AgentEventKind.FAILED
                        ? Optional.of("runtime_failed")
                        : Optional.empty(),
                false,
                false);
    }

    private static AgentTurn queuedTurn() {
        return new AgentTurn(
                TURN_ID,
                CONVERSATION_ID,
                AgentTurnStatus.QUEUED,
                NOW,
                Optional.empty(),
                Optional.empty());
    }

    private static final class Probe
            implements AgentTurnCoordinator.Listener {
        private final AtomicInteger invalidations =
                new AtomicInteger();

        @Override
        public void onAgentTurnStateChanged(AgentTurnState state) {
            // State is asserted from the coordinator's synchronized snapshot.
        }

        @Override
        public void onAgentHistoryInvalidated(String conversationId) {
            if (CONVERSATION_ID.equals(conversationId)) {
                invalidations.incrementAndGet();
            }
        }
    }

    private static final class FakeEventSource
            implements AgentEventSource {
        private final CountDownLatch subscribed = new CountDownLatch(1);
        private final CountDownLatch closedSignal =
                new CountDownLatch(1);
        private final AtomicReference<Listener> listener =
                new AtomicReference<>();
        private final java.util.concurrent.atomic.AtomicBoolean closed =
                new java.util.concurrent.atomic.AtomicBoolean();

        @Override
        public AgentEventSubscription subscribe(
                String turnId,
                Optional<String> lastEventId,
                Listener value) {
            if (!TURN_ID.equals(turnId)) {
                throw new IllegalArgumentException(
                        "Unexpected test turn.");
            }
            listener.set(value);
            subscribed.countDown();
            return () -> {
                closed.set(true);
                closedSignal.countDown();
            };
        }

        private boolean awaitSubscription()
                throws InterruptedException {
            return subscribed.await(5, TimeUnit.SECONDS);
        }

        private boolean awaitClosed() throws InterruptedException {
            return closedSignal.await(5, TimeUnit.SECONDS)
                    && closed.get();
        }

        private void event(AgentEvent value) {
            listener.get().onEvent(value);
        }
    }

    private static final class FakeRepository
            implements AgentRepository {
        @Override
        public AgentConversationPage conversations(
                Optional<String> cursor,
                int limit) {
            return new AgentConversationPage(
                    List.of(),
                    Optional.empty());
        }

        @Override
        public AgentConversation createConversation(
                String title,
                String idempotencyKey) {
            return conversation();
        }

        @Override
        public AgentConversation conversation(String conversationId) {
            return conversation();
        }

        @Override
        public AgentMessagePage messages(
                String conversationId,
                Optional<String> cursor,
                int limit) {
            return new AgentMessagePage(
                    CONVERSATION_ID,
                    List.of(),
                    Optional.empty());
        }

        @Override
        public AgentTurn enqueue(
                String conversationId,
                String message,
                String idempotencyKey) {
            assertEquals(
                    "Send key must be stable.",
                    SEND_KEY,
                    idempotencyKey);
            return queuedTurn();
        }

        @Override
        public AgentTurn turn(String turnId) {
            return queuedTurn();
        }

        @Override
        public AgentTurn cancel(
                String turnId,
                String idempotencyKey) {
            return queuedTurn().withStatus(
                    AgentTurnStatus.CANCELLED,
                    NOW.plusSeconds(2));
        }

        @Override
        public AgentActionDecisionResult decideAction(
                String actionId,
                AgentActionDecision decision,
                String idempotencyKey) {
            throw new UnsupportedOperationException(
                    "Not used by this test.");
        }

        private static AgentConversation conversation() {
            return new AgentConversation(
                    CONVERSATION_ID,
                    "Nova conversa",
                    AgentConversationStatus.ACTIVE,
                    true,
                    NOW,
                    NOW,
                    NOW);
        }
    }
}
