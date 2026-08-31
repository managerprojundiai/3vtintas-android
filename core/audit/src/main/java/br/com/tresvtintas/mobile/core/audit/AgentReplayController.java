package br.com.tresvtintas.mobile.core.audit;

import br.com.tresvtintas.mobile.core.audit.AgentReplayModels.Page;
import br.com.tresvtintas.mobile.core.audit.AgentReplayModels.Turn;
import br.com.tresvtintas.mobile.core.audit.AgentReplayState.Snapshot;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public final class AgentReplayController {
    @FunctionalInterface
    public interface Listener {
        void onAgentReplayStateChanged(AgentReplayState state);
    }

    private final AgentReplayRepository repository;
    private final Executor worker;
    private final Executor main;
    private final Set<Listener> listeners = new CopyOnWriteArraySet<>();
    private final AtomicBoolean busy = new AtomicBoolean();
    private final AtomicLong generation = new AtomicLong();
    private volatile AgentReplayState current = AgentReplayState.empty();

    public AgentReplayController(
            AgentReplayRepository repository,
            Executor worker,
            Executor main) {
        this.repository = Objects.requireNonNull(
                repository,
                "Agent replay repository is required.");
        this.worker = Objects.requireNonNull(worker, "Agent replay worker is required.");
        this.main = Objects.requireNonNull(main, "Main executor is required.");
    }

    public void subscribe(Listener listener) {
        Listener required = Objects.requireNonNull(
                listener,
                "Agent replay listener is required.");
        listeners.add(required);
        AgentReplayState snapshot = current;
        main.execute(() -> required.onAgentReplayStateChanged(snapshot));
    }

    public void unsubscribe(Listener listener) {
        listeners.remove(listener);
    }

    public void open() {
        load(AgentReplayQuery.initial(), false);
    }

    public void refresh() {
        load(current.snapshot().map(Snapshot::query)
                .orElseGet(AgentReplayQuery::initial), false);
    }

    public void apply(AgentReplayQuery query) {
        load(Objects.requireNonNull(query, "Agent replay query is required."), false);
    }

    public void loadMore() {
        Snapshot snapshot = current.snapshot().orElse(null);
        if (snapshot != null && snapshot.nextCursor().isPresent()) {
            load(snapshot.query(), true);
        }
    }

    public void close() {
        generation.incrementAndGet();
        busy.set(false);
        publish(AgentReplayState.closed());
        listeners.clear();
    }

    private void load(AgentReplayQuery query, boolean more) {
        if (!busy.compareAndSet(false, true)) {
            return;
        }
        Optional<Snapshot> previous = current.snapshot();
        long operation = generation.incrementAndGet();
        publish(previous.map(value -> AgentReplayState.busy(value, more))
                .orElseGet(AgentReplayState::loading));
        worker.execute(() -> execute(operation, query, previous, more));
    }

    private void execute(
            long operation,
            AgentReplayQuery query,
            Optional<Snapshot> previous,
            boolean more) {
        try {
            Optional<String> cursor = more
                    ? previous.orElseThrow().nextCursor()
                    : Optional.empty();
            Page page = repository.turns(query, cursor);
            List<Turn> turns = new ArrayList<>();
            if (more) {
                turns.addAll(previous.orElseThrow().turns());
            }
            turns.addAll(page.items());
            complete(operation, AgentReplayState.ready(
                    new Snapshot(query, turns, page.nextCursor())));
        } catch (AuditException failure) {
            AgentReplayState next = previous.isPresent() && transientFailure(failure.kind())
                    ? AgentReplayState.stale(previous.orElseThrow(), failure)
                    : AgentReplayState.error(failure);
            complete(operation, next);
        }
    }

    private void complete(long operation, AgentReplayState state) {
        if (generation.get() != operation
                || current.phase() == AgentReplayState.Phase.CLOSED) {
            return;
        }
        busy.set(false);
        publish(state);
    }

    private void publish(AgentReplayState state) {
        current = state;
        main.execute(() -> listeners.forEach(
                listener -> listener.onAgentReplayStateChanged(state)));
    }

    private static boolean transientFailure(AuditFailureKind kind) {
        return kind == AuditFailureKind.NETWORK
                || kind == AuditFailureKind.RATE_LIMITED
                || kind == AuditFailureKind.SERVICE_UNAVAILABLE;
    }
}
