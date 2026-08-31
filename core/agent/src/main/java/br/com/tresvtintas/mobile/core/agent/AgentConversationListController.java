package br.com.tresvtintas.mobile.core.agent;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

public final class AgentConversationListController {
    private static final int PAGE_SIZE = 30;

    @FunctionalInterface
    public interface Listener {
        void onAgentConversationListStateChanged(
                AgentConversationListState state);
    }

    private final AgentRepository repository;
    private final Executor worker;
    private final Executor main;
    private final Supplier<String> keyFactory;
    private final Set<Listener> listeners = new CopyOnWriteArraySet<>();
    private final AtomicBoolean busy = new AtomicBoolean();
    private final AtomicLong generation = new AtomicLong();
    private volatile AgentConversationListState current =
            AgentConversationListState.empty();
    private volatile Optional<PendingCreate> pendingCreate =
            Optional.empty();

    public AgentConversationListController(
            AgentRepository repository,
            Executor worker,
            Executor main) {
        this(repository, worker, main, () -> UUID.randomUUID().toString());
    }

    AgentConversationListController(
            AgentRepository repository,
            Executor worker,
            Executor main,
            Supplier<String> keyFactory) {
        this.repository = Objects.requireNonNull(
                repository,
                "Agent repository is required.");
        this.worker = Objects.requireNonNull(
                worker,
                "Agent worker is required.");
        this.main = Objects.requireNonNull(
                main,
                "Agent main executor is required.");
        this.keyFactory = Objects.requireNonNull(
                keyFactory,
                "Agent idempotency key factory is required.");
    }

    public void subscribe(Listener listener) {
        Listener required = Objects.requireNonNull(
                listener,
                "Agent conversation listener is required.");
        listeners.add(required);
        AgentConversationListState snapshot = current;
        main.execute(() ->
                required.onAgentConversationListStateChanged(snapshot));
    }

    public void unsubscribe(Listener listener) {
        listeners.remove(listener);
    }

    public void open() {
        if (!begin()) {
            return;
        }
        long operation = generation.incrementAndGet();
        publish(AgentConversationListState.loading());
        worker.execute(() -> first(operation, Optional.empty()));
    }

    public void refresh() {
        Optional<AgentConversationListState.Snapshot> snapshot =
                current.snapshot();
        if (snapshot.isEmpty() || !begin()) {
            return;
        }
        long operation = generation.incrementAndGet();
        publish(AgentConversationListState.refreshing(
                snapshot.orElseThrow()));
        worker.execute(() -> first(operation, snapshot));
    }

    public void loadMore() {
        Optional<AgentConversationListState.Snapshot> snapshot =
                current.snapshot();
        if (snapshot.isEmpty()
                || !snapshot.orElseThrow().hasMore()
                || !begin()) {
            return;
        }
        AgentConversationListState.Snapshot value =
                snapshot.orElseThrow();
        long operation = generation.incrementAndGet();
        publish(AgentConversationListState.loadingMore(value));
        worker.execute(() -> next(operation, value));
    }

    public void create(String title) {
        if (!begin()) {
            return;
        }
        final String normalized;
        try {
            normalized = AgentText.title(title);
        } catch (IllegalArgumentException failure) {
            busy.set(false);
            publish(AgentConversationListState.error(
                    new AgentException(
                            AgentFailureKind.INVALID_REQUEST,
                            "Agent conversation title is invalid.",
                            failure)));
            return;
        }
        PendingCreate pending = new PendingCreate(
                normalized,
                keyFactory.get());
        pendingCreate = Optional.of(pending);
        create(pending);
    }

    public void retryCreate() {
        if (pendingCreate.isEmpty() || !begin()) {
            return;
        }
        create(pendingCreate.orElseThrow());
    }

    public void close() {
        generation.incrementAndGet();
        busy.set(false);
        pendingCreate = Optional.empty();
        publish(AgentConversationListState.closed());
        listeners.clear();
    }

    private void create(PendingCreate pending) {
        long operation = generation.incrementAndGet();
        AgentConversationListState.Snapshot fallback =
                current.snapshot().orElseGet(() ->
                        new AgentConversationListState.Snapshot(
                                java.util.List.of(),
                                Optional.empty()));
        publish(AgentConversationListState.creating(fallback));
        worker.execute(() -> {
            try {
                AgentConversation created =
                        repository.createConversation(
                                pending.title(),
                                pending.key());
                if (!current(operation)) {
                    return;
                }
                busy.set(false);
                pendingCreate = Optional.empty();
                publish(AgentConversationListState.created(
                        fallback.prepend(created),
                        created));
            } catch (AgentException failure) {
                fail(operation, Optional.of(fallback), failure);
            }
        });
    }

    private void first(
            long operation,
            Optional<AgentConversationListState.Snapshot> fallback) {
        try {
            AgentConversationPage page = repository.conversations(
                    Optional.empty(),
                    PAGE_SIZE);
            ready(operation, new AgentConversationListState.Snapshot(
                    page.items(),
                    page.nextCursor()));
        } catch (AgentException failure) {
            fail(operation, fallback, failure);
        }
    }

    private void next(
            long operation,
            AgentConversationListState.Snapshot snapshot) {
        try {
            ready(operation, snapshot.append(repository.conversations(
                    snapshot.nextCursor(),
                    PAGE_SIZE)));
        } catch (AgentException failure) {
            fail(operation, Optional.of(snapshot), failure);
        }
    }

    private void ready(
            long operation,
            AgentConversationListState.Snapshot snapshot) {
        if (!current(operation)) {
            return;
        }
        busy.set(false);
        publish(AgentConversationListState.ready(snapshot));
    }

    private void fail(
            long operation,
            Optional<AgentConversationListState.Snapshot> fallback,
            AgentException failure) {
        if (!current(operation)) {
            return;
        }
        busy.set(false);
        if (fallback.isPresent() && transientFailure(failure.kind())) {
            publish(AgentConversationListState.warning(
                    fallback.orElseThrow(),
                    failure));
        } else {
            publish(AgentConversationListState.error(failure));
        }
    }

    private boolean begin() {
        return busy.compareAndSet(false, true);
    }

    private boolean current(long operation) {
        return generation.get() == operation
                && current.phase()
                        != AgentConversationListState.Phase.CLOSED;
    }

    private void publish(AgentConversationListState state) {
        current = state;
        main.execute(() -> listeners.forEach(listener ->
                listener.onAgentConversationListStateChanged(state)));
    }

    private static boolean transientFailure(AgentFailureKind kind) {
        return kind == AgentFailureKind.NETWORK
                || kind == AgentFailureKind.RATE_LIMITED
                || kind == AgentFailureKind.SERVICE_UNAVAILABLE;
    }

    private record PendingCreate(String title, String key) {
        private PendingCreate {
            Objects.requireNonNull(title, "Pending agent title is required.");
            AgentIdentifiers.requireUuid(
                    key,
                    "Pending agent key is invalid.");
        }
    }
}
