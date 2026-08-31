package br.com.tresvtintas.mobile.core.agent;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Session-scoped coordinator for one visible personal-agent conversation. Sensitive text exists
 * only in memory, durable turn state stays server-side, and streams run only while the screen is
 * observed.
 */
public final class AgentTurnCoordinator implements AutoCloseable {
    public interface Listener {
        void onAgentTurnStateChanged(AgentTurnState state);

        void onAgentHistoryInvalidated(String conversationId);
    }

    private static final int MAXIMUM_PARTIAL_CHARS = 8_000;
    private final Object lock = new Object();
    private final AgentRepository repository;
    private final AgentEventSource eventSource;
    private final Executor worker;
    private final ScheduledExecutorService scheduler;
    private final Executor main;
    private final Supplier<String> keyFactory;
    private final AgentStreamRetryPolicy retryPolicy;
    private final Set<Listener> listeners = new LinkedHashSet<>();
    private AgentTurnState state = AgentTurnState.closed();
    private Optional<AgentEventSubscription> stream = Optional.empty();
    private Optional<ScheduledFuture<?>> reconnect = Optional.empty();
    private Optional<PendingSend> pendingSend = Optional.empty();
    private Optional<String> cancelKey = Optional.empty();
    private Optional<String> recoveryTurnId = Optional.empty();
    private long operationGeneration;
    private long streamGeneration;
    private boolean closed;

    public AgentTurnCoordinator(
            AgentRepository repository,
            AgentEventSource eventSource,
            Executor worker,
            ScheduledExecutorService scheduler,
            Executor main) {
        this(
                repository,
                eventSource,
                worker,
                scheduler,
                main,
                () -> UUID.randomUUID().toString(),
                Math::random);
    }

    AgentTurnCoordinator(
            AgentRepository repository,
            AgentEventSource eventSource,
            Executor worker,
            ScheduledExecutorService scheduler,
            Executor main,
            Supplier<String> keyFactory,
            java.util.function.DoubleSupplier jitter) {
        this.repository = Objects.requireNonNull(
                repository,
                "Agent repository is required.");
        this.eventSource = Objects.requireNonNull(
                eventSource,
                "Agent event source is required.");
        this.worker = Objects.requireNonNull(
                worker,
                "Agent worker is required.");
        this.scheduler = Objects.requireNonNull(
                scheduler,
                "Agent scheduler is required.");
        this.main = Objects.requireNonNull(
                main,
                "Agent main executor is required.");
        this.keyFactory = Objects.requireNonNull(
                keyFactory,
                "Agent idempotency key factory is required.");
        retryPolicy = new AgentStreamRetryPolicy(jitter);
    }

    public AgentEventSubscription observe(
            String conversationId,
            Listener listener) {
        String requiredConversation = requireConversation(conversationId);
        Listener requiredListener = Objects.requireNonNull(
                listener,
                "Agent turn listener is required.");
        AgentTurnState snapshot;
        synchronized (lock) {
            requireOpen();
            if (state.conversationId()
                    .filter(requiredConversation::equals)
                    .isEmpty()) {
                switchConversationLocked(requiredConversation);
            }
            listeners.add(requiredListener);
            resumeStreamLocked();
            snapshot = state;
        }
        main.execute(() ->
                requiredListener.onAgentTurnStateChanged(snapshot));
        return () -> stopObserving(requiredListener);
    }

    public AgentTurnState currentState() {
        synchronized (lock) {
            return state;
        }
    }

    public void send(String conversationId, String message) {
        String requiredConversation = requireConversation(conversationId);
        String normalized = AgentText.message(message);
        PendingSend pending;
        long operation;
        AgentTurnState next;
        synchronized (lock) {
            requireOpen();
            requireCurrentConversation(requiredConversation);
            if (!state.canSend()) {
                return;
            }
            pending = new PendingSend(
                    requiredConversation,
                    normalized,
                    requireKey(keyFactory.get()));
            pendingSend = Optional.of(pending);
            cancelKey = Optional.empty();
            operationGeneration++;
            operation = operationGeneration;
            next = AgentTurnState.submitting(requiredConversation);
            state = next;
        }
        publish(next);
        worker.execute(() -> enqueue(operation, pending));
    }

    public void retrySend() {
        PendingSend pending;
        long operation;
        AgentTurnState next;
        synchronized (lock) {
            requireOpen();
            if (pendingSend.isEmpty()
                    || state.phase() != AgentTurnState.Phase.ERROR) {
                return;
            }
            pending = pendingSend.orElseThrow();
            operationGeneration++;
            operation = operationGeneration;
            next = AgentTurnState.submitting(
                    pending.conversationId());
            state = next;
        }
        publish(next);
        worker.execute(() -> enqueue(operation, pending));
    }

    public void recover(
            String conversationId,
            Optional<String> pendingTurnId) {
        String requiredConversation = requireConversation(conversationId);
        Optional<String> candidate = Objects.requireNonNull(
                pendingTurnId,
                "Pending agent turn is required.")
                .map(AgentTurnCoordinator::requireTurn);
        if (candidate.isEmpty()) {
            return;
        }
        long operation;
        String turnId = candidate.orElseThrow();
        synchronized (lock) {
            requireOpen();
            requireCurrentConversation(requiredConversation);
            if (state.active()
                    || state.turn()
                            .map(AgentTurn::id)
                            .filter(turnId::equals)
                            .isPresent()
                    || recoveryTurnId
                            .filter(turnId::equals)
                            .isPresent()) {
                return;
            }
            recoveryTurnId = Optional.of(turnId);
            operationGeneration++;
            operation = operationGeneration;
        }
        worker.execute(() -> recover(
                operation,
                requiredConversation,
                turnId));
    }

    public void cancel() {
        AgentTurn turn;
        String key;
        long operation;
        AgentTurnState next;
        synchronized (lock) {
            requireOpen();
            if (!state.canCancel() || state.turn().isEmpty()) {
                return;
            }
            turn = state.turn().orElseThrow();
            key = cancelKey.orElseGet(() -> requireKey(
                    keyFactory.get()));
            cancelKey = Optional.of(key);
            operationGeneration++;
            operation = operationGeneration;
            next = AgentTurnState.cancelling(state);
            state = next;
        }
        publish(next);
        worker.execute(() -> cancel(operation, turn, key));
    }

    public void acknowledgeHistory(String conversationId) {
        String requiredConversation = requireConversation(conversationId);
        AgentTurnState next;
        synchronized (lock) {
            requireOpen();
            if (!state.terminal()
                    || state.turn().isEmpty()
                    || state.conversationId()
                            .filter(requiredConversation::equals)
                            .isEmpty()
                    || state.partialText().isEmpty()) {
                return;
            }
            next = terminal(
                    state.turn().orElseThrow(),
                    "",
                    state.lastEventId(),
                    state.runtimeFailureCode(),
                    state.requiresHuman(),
                    state.blocked());
            state = next;
        }
        publish(next);
    }

    @Override
    public void close() {
        Set<Listener> snapshot;
        synchronized (lock) {
            if (closed) {
                return;
            }
            closed = true;
            operationGeneration++;
            streamGeneration++;
            stopStreamLocked();
            pendingSend = Optional.empty();
            cancelKey = Optional.empty();
            recoveryTurnId = Optional.empty();
            state = AgentTurnState.closed();
            snapshot = new LinkedHashSet<>(listeners);
            listeners.clear();
        }
        publish(snapshot, AgentTurnState.closed());
    }

    private void enqueue(long operation, PendingSend pending) {
        try {
            AgentTurn turn = repository.enqueue(
                    pending.conversationId(),
                    pending.message(),
                    pending.key());
            AgentTurnState next;
            synchronized (lock) {
                if (!currentOperation(operation)
                        || !pending.conversationId().equals(
                                turn.conversationId())) {
                    return;
                }
                pendingSend = Optional.empty();
                cancelKey = Optional.empty();
                next = AgentTurnState.queued(turn);
                state = next;
                resumeStreamLocked();
            }
            publish(next);
            publishHistory(pending.conversationId());
        } catch (AgentException failure) {
            operationFailed(
                    operation,
                    pending.conversationId(),
                    Optional.empty(),
                    "",
                    Optional.empty(),
                    failure,
                    retainPending(failure.kind()));
        }
    }

    private void recover(
            long operation,
            String conversationId,
            String turnId) {
        try {
            AgentTurn turn = repository.turn(turnId);
            AgentTurnState next;
            boolean invalidate;
            synchronized (lock) {
                if (!currentOperation(operation)
                        || !conversationId.equals(
                                turn.conversationId())) {
                    return;
                }
                recoveryTurnId = Optional.empty();
                next = stateForRecovered(turn);
                state = next;
                invalidate = turn.status().terminal();
                resumeStreamLocked();
            }
            publish(next);
            if (invalidate) {
                publishHistory(conversationId);
            }
        } catch (AgentException failure) {
            synchronized (lock) {
                recoveryTurnId = Optional.empty();
            }
            operationFailed(
                    operation,
                    conversationId,
                    Optional.empty(),
                    "",
                    Optional.empty(),
                    failure,
                    false);
        }
    }

    private void cancel(
            long operation,
            AgentTurn previous,
            String key) {
        try {
            AgentTurn cancelled = repository.cancel(
                    previous.id(),
                    key);
            AgentTurnState next;
            synchronized (lock) {
                if (!currentOperation(operation)) {
                    return;
                }
                cancelKey = Optional.empty();
                stopStreamLocked();
                next = terminal(
                        cancelled,
                        state.partialText(),
                        state.lastEventId(),
                        Optional.empty(),
                        false,
                        false);
                state = next;
            }
            publish(next);
            publishHistory(cancelled.conversationId());
        } catch (AgentException failure) {
            operationFailed(
                    operation,
                    previous.conversationId(),
                    Optional.of(previous),
                    state.partialText(),
                    state.lastEventId(),
                    failure,
                    true);
        }
    }

    private void connect(long expectedStream) {
        AgentTurn turn;
        Optional<String> cursor;
        synchronized (lock) {
            if (!streamCurrent(expectedStream)
                    || state.turn().isEmpty()
                    || !streamable(state)) {
                return;
            }
            turn = state.turn().orElseThrow();
            cursor = state.lastEventId();
            reconnect = Optional.empty();
        }
        try {
            installStream(
                    expectedStream,
                    eventSource.subscribe(
                            turn.id(),
                            cursor,
                            new StreamListener(
                                    expectedStream,
                                    turn.id())));
        } catch (AgentException failure) {
            streamFailed(expectedStream, turn.id(), failure);
        }
    }

    private void installStream(
            long expectedStream,
            AgentEventSubscription subscription) {
        AgentEventSubscription required = Objects.requireNonNull(
                subscription,
                "Agent event subscription is required.");
        synchronized (lock) {
            if (!streamCurrent(expectedStream)
                    || !streamable(state)) {
                required.close();
                return;
            }
            stream = Optional.of(required);
        }
    }

    private void event(
            long expectedStream,
            String expectedTurn,
            AgentEvent event) {
        AgentTurnState next;
        boolean invalidate = false;
        synchronized (lock) {
            if (!streamCurrent(expectedStream)
                    || state.turn().isEmpty()
                    || !expectedTurn.equals(event.turnId())
                    || !expectedTurn.equals(
                            state.turn().orElseThrow().id())) {
                return;
            }
            int previous = state.lastEventId()
                    .map(Integer::parseInt)
                    .orElse(0);
            if (event.sequence() <= previous) {
                protocolFailureLocked(expectedStream, expectedTurn);
                return;
            }
            AgentTurn turn = state.turn().orElseThrow();
            Optional<String> cursor = Optional.of(event.cursor());
            if (event.kind() == AgentEventKind.TEXT_DELTA
                    && state.partialText().length()
                            + event.textDelta()
                                    .orElseThrow()
                                    .length()
                            > MAXIMUM_PARTIAL_CHARS) {
                protocolFailureLocked(expectedStream, expectedTurn);
                return;
            }
            next = switch (event.kind()) {
                case ACCEPTED -> AgentTurnState.queued(
                        turn.status() == AgentTurnStatus.QUEUED
                                ? turn
                                : new AgentTurn(
                                        turn.id(),
                                        turn.conversationId(),
                                        AgentTurnStatus.QUEUED,
                                        turn.createdAt(),
                                        Optional.empty(),
                                        Optional.empty()),
                        cursor);
                case STARTED -> AgentTurnState.running(
                        turn.withStatus(
                                AgentTurnStatus.RUNNING,
                                event.createdAt()),
                        event.resetPartialResponse()
                                ? ""
                                : state.partialText(),
                        cursor,
                        false);
                case TOOL -> AgentTurnState.running(
                        runningTurn(turn, event.createdAt()),
                        state.partialText(),
                        cursor,
                        true);
                case TEXT_DELTA -> AgentTurnState.running(
                        runningTurn(turn, event.createdAt()),
                        append(state.partialText(), event),
                        cursor,
                        false);
                case COMPLETED -> terminal(
                        turn.withStatus(
                                AgentTurnStatus.COMPLETED,
                                event.createdAt()),
                        state.partialText(),
                        cursor,
                        Optional.empty(),
                        event.requiresHuman(),
                        event.blocked());
                case FAILED -> terminal(
                        turn.withStatus(
                                AgentTurnStatus.FAILED,
                                event.createdAt()),
                        state.partialText(),
                        cursor,
                        event.failureCode(),
                        false,
                        false);
                case CANCELLED -> terminal(
                        turn.withStatus(
                                AgentTurnStatus.CANCELLED,
                                event.createdAt()),
                        state.partialText(),
                        cursor,
                        Optional.empty(),
                        false,
                        false);
            };
            state = next;
            if (event.kind().terminal()) {
                invalidate = true;
                stopStreamLocked();
            }
        }
        publish(next);
        if (invalidate) {
            publishHistory(
                    next.conversationId().orElseThrow());
        }
    }

    private void protocolFailureLocked(
            long expectedStream,
            String turnId) {
        AgentException failure = new AgentException(
                AgentFailureKind.PROTOCOL,
                "Agent stream cursor order is invalid.");
        scheduler.execute(() ->
                streamFailed(expectedStream, turnId, failure));
    }

    private void streamFailed(
            long expectedStream,
            String expectedTurn,
            AgentException failure) {
        AgentTurnState next;
        synchronized (lock) {
            if (!streamCurrent(expectedStream)
                    || state.turn().isEmpty()
                    || !expectedTurn.equals(
                            state.turn().orElseThrow().id())
                    || !streamable(state)) {
                return;
            }
            stream.ifPresent(AgentEventSubscription::close);
            stream = Optional.empty();
            if (terminalFailure(failure.kind())) {
                next = AgentTurnState.error(
                        state.conversationId().orElseThrow(),
                        state.turn(),
                        state.partialText(),
                        state.lastEventId(),
                        failure);
                state = next;
                cancelReconnectLocked();
            } else {
                int attempt = Math.min(
                        state.reconnectAttempt() + 1,
                        30);
                next = AgentTurnState.reconnecting(
                        state.turn().orElseThrow(),
                        state.partialText(),
                        state.lastEventId(),
                        failure.kind(),
                        attempt);
                state = next;
                scheduleConnectLocked(retryPolicy.delay(attempt));
            }
        }
        publish(next);
    }

    private void streamClosed(
            long expectedStream,
            String expectedTurn) {
        long operation;
        synchronized (lock) {
            if (!streamCurrent(expectedStream)
                    || state.turn().isEmpty()
                    || !expectedTurn.equals(
                            state.turn().orElseThrow().id())
                    || !streamable(state)) {
                return;
            }
            stream = Optional.empty();
            operationGeneration++;
            operation = operationGeneration;
        }
        worker.execute(() -> reconcileClosedStream(
                operation,
                expectedTurn));
    }

    private void reconcileClosedStream(
            long operation,
            String turnId) {
        try {
            AgentTurn turn = repository.turn(turnId);
            AgentTurnState next;
            boolean invalidate;
            synchronized (lock) {
                if (!currentOperation(operation)
                        || state.turn().isEmpty()
                        || !turnId.equals(
                                state.turn().orElseThrow().id())) {
                    return;
                }
                if (turn.status().terminal()) {
                    next = terminal(
                            turn,
                            state.partialText(),
                            state.lastEventId(),
                            Optional.empty(),
                            false,
                            false);
                    stopStreamLocked();
                    invalidate = true;
                } else {
                    int attempt = Math.min(
                            state.reconnectAttempt() + 1,
                            30);
                    next = AgentTurnState.reconnecting(
                            turn,
                            state.partialText(),
                            state.lastEventId(),
                            AgentFailureKind.NETWORK,
                            attempt);
                    state = next;
                    scheduleConnectLocked(
                            retryPolicy.delay(attempt));
                    invalidate = false;
                }
                state = next;
            }
            publish(next);
            if (invalidate) {
                publishHistory(turn.conversationId());
            }
        } catch (AgentException failure) {
            streamFailed(
                    currentStreamGeneration(),
                    turnId,
                    failure);
        }
    }

    private void operationFailed(
            long operation,
            String conversationId,
            Optional<AgentTurn> turn,
            String partial,
            Optional<String> cursor,
            AgentException failure,
            boolean retainKey) {
        AgentTurnState next;
        synchronized (lock) {
            if (!currentOperation(operation)) {
                return;
            }
            if (!retainKey) {
                pendingSend = Optional.empty();
                cancelKey = Optional.empty();
            }
            next = AgentTurnState.error(
                    conversationId,
                    turn,
                    partial,
                    cursor,
                    failure);
            state = next;
        }
        publish(next);
    }

    private void switchConversationLocked(String conversationId) {
        operationGeneration++;
        streamGeneration++;
        stopStreamLocked();
        pendingSend = Optional.empty();
        cancelKey = Optional.empty();
        recoveryTurnId = Optional.empty();
        state = AgentTurnState.idle(conversationId);
    }

    private void stopObserving(Listener listener) {
        synchronized (lock) {
            listeners.remove(listener);
            if (!listeners.isEmpty() || closed) {
                return;
            }
            streamGeneration++;
            stopStreamLocked();
        }
    }

    private void resumeStreamLocked() {
        if (listeners.isEmpty()
                || !streamable(state)
                || stream.isPresent()
                || reconnect.filter(value -> !value.isDone()).isPresent()) {
            return;
        }
        scheduleConnectLocked(0);
    }

    private void scheduleConnectLocked(long delayMillis) {
        cancelReconnectLocked();
        streamGeneration++;
        long expected = streamGeneration;
        reconnect = Optional.of(scheduler.schedule(
                () -> connect(expected),
                delayMillis,
                TimeUnit.MILLISECONDS));
    }

    private void stopStreamLocked() {
        stream.ifPresent(AgentEventSubscription::close);
        stream = Optional.empty();
        cancelReconnectLocked();
    }

    private void cancelReconnectLocked() {
        reconnect.ifPresent(value -> value.cancel(false));
        reconnect = Optional.empty();
    }

    private boolean streamCurrent(long expected) {
        return !closed
                && !listeners.isEmpty()
                && streamGeneration == expected;
    }

    private boolean currentOperation(long expected) {
        return !closed && operationGeneration == expected;
    }

    private long currentStreamGeneration() {
        synchronized (lock) {
            return streamGeneration;
        }
    }

    private void publish(AgentTurnState next) {
        Set<Listener> snapshot;
        synchronized (lock) {
            snapshot = new LinkedHashSet<>(listeners);
        }
        publish(snapshot, next);
    }

    private void publish(
            Set<Listener> snapshot,
            AgentTurnState next) {
        main.execute(() -> snapshot.forEach(listener ->
                listener.onAgentTurnStateChanged(next)));
    }

    private void publishHistory(String conversationId) {
        Set<Listener> snapshot;
        synchronized (lock) {
            snapshot = new LinkedHashSet<>(listeners);
        }
        main.execute(() -> snapshot.forEach(listener ->
                listener.onAgentHistoryInvalidated(conversationId)));
    }

    private void requireOpen() {
        if (closed) {
            throw new IllegalStateException(
                    "Agent turn coordinator is closed.");
        }
    }

    private void requireCurrentConversation(String conversationId) {
        if (state.conversationId()
                .filter(conversationId::equals)
                .isEmpty()) {
            throw new IllegalStateException(
                    "Agent conversation is not being observed.");
        }
    }

    private static boolean streamable(AgentTurnState value) {
        return value.turn().isPresent()
                && switch (value.phase()) {
                    case QUEUED, RUNNING, RECONNECTING -> true;
                    default -> false;
                };
    }

    private static AgentTurnState stateForRecovered(AgentTurn turn) {
        return switch (turn.status()) {
            case QUEUED -> AgentTurnState.queued(turn);
            case RUNNING -> AgentTurnState.running(
                    turn,
                    "",
                    Optional.empty(),
                    false);
            case COMPLETED, FAILED, CANCELLED -> terminal(
                    turn,
                    "",
                    Optional.empty(),
                    Optional.empty(),
                    false,
                    false);
        };
    }

    private static AgentTurnState terminal(
            AgentTurn turn,
            String partial,
            Optional<String> cursor,
            Optional<String> runtimeFailureCode,
            boolean requiresHuman,
            boolean blocked) {
        return AgentTurnState.terminal(
                turn,
                partial,
                cursor,
                runtimeFailureCode,
                requiresHuman,
                blocked);
    }

    private static AgentTurn runningTurn(
            AgentTurn turn,
            Instant occurredAt) {
        return turn.status() == AgentTurnStatus.RUNNING
                ? turn
                : turn.withStatus(
                        AgentTurnStatus.RUNNING,
                        occurredAt);
    }

    private static String append(
            String current,
            AgentEvent event) {
        String next = current + event.textDelta().orElseThrow();
        if (next.length() > MAXIMUM_PARTIAL_CHARS) {
            throw new IllegalArgumentException(
                    "Agent partial response is too large.");
        }
        return next;
    }

    private static boolean retainPending(AgentFailureKind kind) {
        return kind == AgentFailureKind.NETWORK
                || kind == AgentFailureKind.RATE_LIMITED
                || kind == AgentFailureKind.SERVICE_UNAVAILABLE
                || kind == AgentFailureKind.IDEMPOTENCY_IN_PROGRESS;
    }

    private static boolean terminalFailure(AgentFailureKind kind) {
        return kind == AgentFailureKind.ACCESS_REVOKED
                || kind == AgentFailureKind.AUTH_REJECTED
                || kind == AgentFailureKind.FORBIDDEN
                || kind == AgentFailureKind.NOT_FOUND
                || kind == AgentFailureKind.INVALID_REQUEST
                || kind == AgentFailureKind.PROTOCOL
                || kind == AgentFailureKind.UPDATE_REQUIRED;
    }

    private static String requireConversation(String value) {
        return AgentIdentifiers.requireUuid(
                value,
                "Agent conversation ID is invalid.");
    }

    private static String requireTurn(String value) {
        return AgentIdentifiers.requireUuid(
                value,
                "Agent turn ID is invalid.");
    }

    private static String requireKey(String value) {
        return AgentIdentifiers.requireUuid(
                value,
                "Agent idempotency key is invalid.");
    }

    private record PendingSend(
            String conversationId,
            String message,
            String key) {
        private PendingSend {
            conversationId = requireConversation(conversationId);
            message = AgentText.message(message);
            key = requireKey(key);
        }
    }

    private final class StreamListener
            implements AgentEventSource.Listener {
        private final long expectedStream;
        private final String expectedTurn;

        private StreamListener(
                long expectedStream,
                String expectedTurn) {
            this.expectedStream = expectedStream;
            this.expectedTurn = expectedTurn;
        }

        @Override
        public void onEvent(AgentEvent event) {
            AgentTurnCoordinator.this.event(
                    expectedStream,
                    expectedTurn,
                    Objects.requireNonNull(
                            event,
                            "Agent event is required."));
        }

        @Override
        public void onFailure(AgentException failure) {
            streamFailed(
                    expectedStream,
                    expectedTurn,
                    Objects.requireNonNull(
                            failure,
                            "Agent stream failure is required."));
        }

        @Override
        public void onClosed() {
            streamClosed(expectedStream, expectedTurn);
        }
    }
}
