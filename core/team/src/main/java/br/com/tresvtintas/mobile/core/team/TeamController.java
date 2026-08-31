package br.com.tresvtintas.mobile.core.team;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public final class TeamController {
    private final TeamRepository repository;
    private final Executor worker;
    private final Executor main;
    private final Set<TeamStateListener> listeners =
            new CopyOnWriteArraySet<>();
    private final AtomicBoolean busy = new AtomicBoolean();
    private final AtomicLong generation = new AtomicLong();
    private volatile TeamState current = TeamState.empty();

    public TeamController(
            TeamRepository repository,
            Executor worker,
            Executor main) {
        this.repository = Objects.requireNonNull(
                repository,
                "Team repository is required.");
        this.worker = Objects.requireNonNull(worker, "Worker is required.");
        this.main = Objects.requireNonNull(main, "Main executor is required.");
    }

    public void subscribe(TeamStateListener listener) {
        TeamStateListener required = Objects.requireNonNull(
                listener,
                "Team listener is required.");
        listeners.add(required);
        TeamState snapshot = current;
        main.execute(() -> required.onTeamStateChanged(snapshot));
    }

    public void unsubscribe(TeamStateListener listener) {
        listeners.remove(listener);
    }

    public void load() {
        start(Optional.empty());
    }

    public void refresh() {
        start(current.snapshot());
    }

    public void close() {
        generation.incrementAndGet();
        busy.set(false);
        publish(TeamState.closed());
        listeners.clear();
    }

    private void start(Optional<TeamSnapshot> previous) {
        if (!busy.compareAndSet(false, true)) {
            return;
        }
        long operation = generation.incrementAndGet();
        publish(previous
                .map(TeamState::refreshing)
                .orElseGet(TeamState::loading));
        worker.execute(() -> execute(operation, previous));
    }

    private void execute(
            long operation,
            Optional<TeamSnapshot> previous) {
        try {
            complete(operation, TeamState.ready(repository.load()));
        } catch (TeamException failure) {
            TeamState next = previous.isPresent()
                    && transientFailure(failure.kind())
                    ? TeamState.stale(previous.orElseThrow(), failure)
                    : TeamState.error(failure);
            complete(operation, next);
        }
    }

    private void complete(long operation, TeamState state) {
        if (generation.get() != operation
                || current.phase() == TeamState.Phase.CLOSED) {
            return;
        }
        busy.set(false);
        publish(state);
    }

    private void publish(TeamState state) {
        current = state;
        main.execute(() -> listeners.forEach(
                listener -> listener.onTeamStateChanged(state)));
    }

    private static boolean transientFailure(TeamFailureKind kind) {
        return kind == TeamFailureKind.NETWORK
                || kind == TeamFailureKind.RATE_LIMITED
                || kind == TeamFailureKind.SERVICE_UNAVAILABLE;
    }
}
