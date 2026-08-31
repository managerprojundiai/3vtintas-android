package br.com.tresvtintas.mobile.core.painteradmin;

import br.com.tresvtintas.mobile.core.painteradmin.PainterAdministrationListState.Snapshot;
import br.com.tresvtintas.mobile.core.painteradmin.PainterAdministrationModels.AccessRequest;
import br.com.tresvtintas.mobile.core.painteradmin.PainterAdministrationModels.Page;
import br.com.tresvtintas.mobile.core.painteradmin.PainterAdministrationModels.Painter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public final class PainterAdministrationListController {
    @FunctionalInterface
    public interface Listener {
        void onPainterAdministrationStateChanged(
                PainterAdministrationListState state);
    }

    private final PainterAdministrationRepository repository;
    private final Executor worker;
    private final Executor main;
    private final Set<Listener> listeners = new CopyOnWriteArraySet<>();
    private final AtomicBoolean busy = new AtomicBoolean();
    private final AtomicLong generation = new AtomicLong();
    private volatile PainterAdministrationListState current =
            PainterAdministrationListState.empty();

    public PainterAdministrationListController(
            PainterAdministrationRepository repository,
            Executor worker,
            Executor main) {
        this.repository = Objects.requireNonNull(repository, "Repository is required.");
        this.worker = Objects.requireNonNull(worker, "Worker is required.");
        this.main = Objects.requireNonNull(main, "Main executor is required.");
    }

    public void subscribe(Listener listener) {
        Listener required = Objects.requireNonNull(listener, "Listener is required.");
        listeners.add(required);
        PainterAdministrationListState snapshot = current;
        main.execute(() -> required
                .onPainterAdministrationStateChanged(snapshot));
    }

    public void unsubscribe(Listener listener) {
        listeners.remove(listener);
    }

    public void open() {
        load(PainterAdministrationQuery.initial(), false);
    }

    public void refresh() {
        PainterAdministrationQuery query = current.snapshot()
                .map(Snapshot::query)
                .orElseGet(PainterAdministrationQuery::initial);
        load(query, false);
    }

    public void apply(PainterAdministrationQuery query) {
        load(Objects.requireNonNull(query, "Query is required."), false);
    }

    public void loadMorePainters() {
        if (current.snapshot()
                .flatMap(Snapshot::painterCursor)
                .isPresent()) {
            load(current.snapshot().orElseThrow().query(), true);
        }
    }

    public void loadMoreRequests() {
        Snapshot snapshot = current.snapshot().orElse(null);
        if (snapshot == null
                || snapshot.requestCursor().isEmpty()
                || !busy.compareAndSet(false, true)) {
            return;
        }
        long operation = generation.incrementAndGet();
        publish(PainterAdministrationListState.busy(snapshot, true));
        worker.execute(() -> {
            try {
                Page<AccessRequest> page = repository.accessRequests(
                        snapshot.requestCursor(),
                        snapshot.query().pageSize());
                List<AccessRequest> combined =
                        new ArrayList<>(snapshot.requests());
                combined.addAll(page.items());
                complete(operation, PainterAdministrationListState.ready(
                        new Snapshot(
                                snapshot.options(),
                                snapshot.query(),
                                snapshot.painters(),
                                snapshot.painterCursor(),
                                combined,
                                page.nextCursor())));
            } catch (PainterAdministrationException failure) {
                complete(operation, PainterAdministrationListState.stale(
                        snapshot,
                        failure));
            }
        });
    }

    public void close() {
        generation.incrementAndGet();
        busy.set(false);
        publish(PainterAdministrationListState.closed());
        listeners.clear();
    }

    private void load(
            PainterAdministrationQuery query,
            boolean morePainters) {
        if (!busy.compareAndSet(false, true)) {
            return;
        }
        Optional<Snapshot> previous = current.snapshot();
        long operation = generation.incrementAndGet();
        publish(previous
                .map(value -> PainterAdministrationListState.busy(
                        value,
                        morePainters))
                .orElseGet(PainterAdministrationListState::loading));
        worker.execute(() -> execute(
                operation,
                query,
                previous,
                morePainters));
    }

    private void execute(
            long operation,
            PainterAdministrationQuery query,
            Optional<Snapshot> previous,
            boolean morePainters) {
        try {
            if (morePainters) {
                Snapshot old = previous.orElseThrow();
                Page<Painter> page = repository.painters(
                        query,
                        old.painterCursor());
                List<Painter> combined =
                        new ArrayList<>(old.painters());
                combined.addAll(page.items());
                complete(operation, PainterAdministrationListState.ready(
                        new Snapshot(
                                old.options(),
                                query,
                                combined,
                                page.nextCursor(),
                                old.requests(),
                                old.requestCursor())));
                return;
            }
            Page<Painter> painters =
                    repository.painters(query, Optional.empty());
            Page<AccessRequest> requests = repository.accessRequests(
                    Optional.empty(),
                    query.pageSize());
            complete(operation, PainterAdministrationListState.ready(
                    new Snapshot(
                            repository.options(),
                            query,
                            painters.items(),
                            painters.nextCursor(),
                            requests.items(),
                            requests.nextCursor())));
        } catch (PainterAdministrationException failure) {
            PainterAdministrationListState next = previous.isPresent()
                    && transientFailure(failure.kind())
                    ? PainterAdministrationListState.stale(
                            previous.orElseThrow(),
                            failure)
                    : PainterAdministrationListState.error(failure);
            complete(operation, next);
        }
    }

    private void complete(
            long operation,
            PainterAdministrationListState state) {
        if (generation.get() != operation
                || current.phase()
                        == PainterAdministrationListState.Phase.CLOSED) {
            return;
        }
        busy.set(false);
        publish(state);
    }

    private void publish(PainterAdministrationListState state) {
        current = state;
        main.execute(() -> listeners.forEach(listener ->
                listener.onPainterAdministrationStateChanged(state)));
    }

    private static boolean transientFailure(
            PainterAdministrationFailureKind kind) {
        return kind == PainterAdministrationFailureKind.NETWORK
                || kind == PainterAdministrationFailureKind.RATE_LIMITED
                || kind == PainterAdministrationFailureKind.SERVICE_UNAVAILABLE;
    }
}
