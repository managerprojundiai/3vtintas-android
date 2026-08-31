package br.com.tresvtintas.mobile.core.dashboard;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public final class DashboardController {
    private final DashboardRepository repository;
    private final Executor worker;
    private final Executor main;
    private final Set<DashboardStateListener> listeners =
            new CopyOnWriteArraySet<>();
    private final AtomicBoolean busy = new AtomicBoolean();
    private final AtomicLong generation = new AtomicLong();
    private volatile DashboardState current = DashboardState.empty();

    public DashboardController(
            DashboardRepository repository,
            Executor worker,
            Executor main) {
        this.repository = Objects.requireNonNull(
                repository,
                "Dashboard repository is required.");
        this.worker = Objects.requireNonNull(worker, "Worker is required.");
        this.main = Objects.requireNonNull(main, "Main executor is required.");
    }

    public void subscribe(DashboardStateListener listener) {
        DashboardStateListener required = Objects.requireNonNull(
                listener,
                "Dashboard listener is required.");
        listeners.add(required);
        DashboardState snapshot = current;
        main.execute(() -> required.onDashboardStateChanged(snapshot));
    }

    public void unsubscribe(DashboardStateListener listener) {
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
        publish(DashboardState.closed());
        listeners.clear();
    }

    private void start(Optional<DashboardSnapshot> previous) {
        if (!busy.compareAndSet(false, true)) {
            return;
        }
        long operation = generation.incrementAndGet();
        publish(previous
                .map(DashboardState::refreshing)
                .orElseGet(DashboardState::loading));
        worker.execute(() -> execute(operation, previous));
    }

    private void execute(
            long operation,
            Optional<DashboardSnapshot> previous) {
        try {
            complete(operation, DashboardState.ready(repository.load()));
        } catch (DashboardException failure) {
            DashboardState next = previous.isPresent()
                            && transientFailure(failure.kind())
                    ? DashboardState.stale(previous.orElseThrow(), failure)
                    : DashboardState.error(failure);
            complete(operation, next);
        }
    }

    private void complete(long operation, DashboardState state) {
        if (generation.get() != operation
                || current.phase() == DashboardState.Phase.CLOSED) {
            return;
        }
        busy.set(false);
        publish(state);
    }

    private void publish(DashboardState state) {
        current = state;
        main.execute(() -> listeners.forEach(
                listener -> listener.onDashboardStateChanged(state)));
    }

    private static boolean transientFailure(DashboardFailureKind kind) {
        return kind == DashboardFailureKind.NETWORK
                || kind == DashboardFailureKind.RATE_LIMITED
                || kind == DashboardFailureKind.SERVICE_UNAVAILABLE;
    }
}
