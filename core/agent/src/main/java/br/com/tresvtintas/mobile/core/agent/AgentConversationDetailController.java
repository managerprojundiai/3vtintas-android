package br.com.tresvtintas.mobile.core.agent;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public final class AgentConversationDetailController {
    private static final int PAGE_SIZE = 50;

    @FunctionalInterface
    public interface Listener {
        void onAgentConversationDetailStateChanged(
                AgentConversationDetailState state);
    }

    private final AgentRepository repository;
    private final Executor worker;
    private final Executor main;
    private final Set<Listener> listeners = new CopyOnWriteArraySet<>();
    private final AtomicBoolean busy = new AtomicBoolean();
    private final AtomicLong generation = new AtomicLong();
    private volatile AgentConversationDetailState current =
            AgentConversationDetailState.empty();

    public AgentConversationDetailController(
            AgentRepository repository,
            Executor worker,
            Executor main) {
        this.repository = Objects.requireNonNull(
                repository,
                "Agent repository is required.");
        this.worker = Objects.requireNonNull(
                worker,
                "Agent worker is required.");
        this.main = Objects.requireNonNull(
                main,
                "Agent main executor is required.");
    }

    public void subscribe(Listener listener) {
        Listener required = Objects.requireNonNull(
                listener,
                "Agent detail listener is required.");
        listeners.add(required);
        AgentConversationDetailState snapshot = current;
        main.execute(() ->
                required.onAgentConversationDetailStateChanged(snapshot));
    }

    public void unsubscribe(Listener listener) {
        listeners.remove(listener);
    }

    public void open(String conversationId) {
        String required = requireConversationId(conversationId);
        busy.set(true);
        long operation = generation.incrementAndGet();
        publish(AgentConversationDetailState.loading());
        worker.execute(() -> first(
                required,
                operation,
                Optional.empty()));
    }

    public void refresh() {
        Optional<AgentConversationDetailState.Snapshot> snapshot =
                current.snapshot();
        if (snapshot.isEmpty()
                || !busy.compareAndSet(false, true)) {
            return;
        }
        AgentConversationDetailState.Snapshot fallback =
                snapshot.orElseThrow();
        long operation = generation.incrementAndGet();
        publish(AgentConversationDetailState.refreshing(fallback));
        worker.execute(() -> first(
                fallback.conversation().id(),
                operation,
                Optional.of(fallback)));
    }

    public void loadMore() {
        Optional<AgentConversationDetailState.Snapshot> snapshot =
                current.snapshot();
        if (snapshot.isEmpty()
                || !snapshot.orElseThrow().hasMore()
                || !busy.compareAndSet(false, true)) {
            return;
        }
        AgentConversationDetailState.Snapshot value =
                snapshot.orElseThrow();
        long operation = generation.incrementAndGet();
        publish(AgentConversationDetailState.loadingMore(value));
        worker.execute(() -> next(operation, value));
    }

    public void close() {
        generation.incrementAndGet();
        busy.set(false);
        publish(AgentConversationDetailState.closed());
        listeners.clear();
    }

    private void first(
            String conversationId,
            long operation,
            Optional<AgentConversationDetailState.Snapshot> fallback) {
        try {
            AgentConversation conversation =
                    repository.conversation(conversationId);
            AgentMessagePage page = repository.messages(
                    conversationId,
                    Optional.empty(),
                    PAGE_SIZE);
            if (!conversation.id().equals(page.conversationId())) {
                throw new AgentException(
                        AgentFailureKind.PROTOCOL,
                        "Agent response identifiers do not match.");
            }
            ready(operation, new AgentConversationDetailState.Snapshot(
                    conversation,
                    page.items(),
                    page.nextCursor()));
        } catch (AgentException failure) {
            failed(operation, fallback, failure);
        }
    }

    private void next(
            long operation,
            AgentConversationDetailState.Snapshot snapshot) {
        try {
            AgentMessagePage page = repository.messages(
                    snapshot.conversation().id(),
                    snapshot.nextCursor(),
                    PAGE_SIZE);
            ready(operation, snapshot.prepend(page));
        } catch (IllegalArgumentException failure) {
            failed(
                    operation,
                    Optional.of(snapshot),
                    new AgentException(
                            AgentFailureKind.PROTOCOL,
                            "Agent message page is inconsistent.",
                            failure));
        } catch (AgentException failure) {
            failed(operation, Optional.of(snapshot), failure);
        }
    }

    private void ready(
            long operation,
            AgentConversationDetailState.Snapshot snapshot) {
        if (!current(operation)) {
            return;
        }
        busy.set(false);
        publish(AgentConversationDetailState.ready(snapshot));
    }

    private void failed(
            long operation,
            Optional<AgentConversationDetailState.Snapshot> fallback,
            AgentException failure) {
        if (!current(operation)) {
            return;
        }
        busy.set(false);
        if (fallback.isPresent() && transientFailure(failure.kind())) {
            publish(AgentConversationDetailState.warning(
                    fallback.orElseThrow(),
                    failure));
        } else {
            publish(AgentConversationDetailState.error(failure));
        }
    }

    private boolean current(long operation) {
        return generation.get() == operation
                && current.phase()
                        != AgentConversationDetailState.Phase.CLOSED;
    }

    private void publish(AgentConversationDetailState state) {
        current = state;
        main.execute(() -> listeners.forEach(listener ->
                listener.onAgentConversationDetailStateChanged(state)));
    }

    private static String requireConversationId(String value) {
        return AgentIdentifiers.requireUuid(
                value,
                "Agent conversation ID is invalid.");
    }

    private static boolean transientFailure(AgentFailureKind kind) {
        return kind == AgentFailureKind.NETWORK
                || kind == AgentFailureKind.RATE_LIMITED
                || kind == AgentFailureKind.SERVICE_UNAVAILABLE;
    }
}
