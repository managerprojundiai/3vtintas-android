package br.com.tresvtintas.mobile.core.audit;

import br.com.tresvtintas.mobile.core.audit.AuditModels.Event;
import br.com.tresvtintas.mobile.core.audit.AuditModels.Page;
import br.com.tresvtintas.mobile.core.audit.AuditState.Snapshot;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public final class AuditController {
    @FunctionalInterface
    public interface Listener {
        void onAuditStateChanged(AuditState state);
    }

    private final AuditRepository repository;
    private final Executor worker;
    private final Executor main;
    private final Set<Listener> listeners = new CopyOnWriteArraySet<>();
    private final AtomicBoolean busy = new AtomicBoolean();
    private final AtomicLong generation = new AtomicLong();
    private volatile AuditState current = AuditState.empty();

    public AuditController(AuditRepository repository, Executor worker, Executor main) {
        this.repository = Objects.requireNonNull(repository, "Audit repository is required.");
        this.worker = Objects.requireNonNull(worker, "Audit worker is required.");
        this.main = Objects.requireNonNull(main, "Main executor is required.");
    }

    public void subscribe(Listener listener) {
        Listener required = Objects.requireNonNull(listener, "Audit listener is required.");
        listeners.add(required);
        AuditState snapshot = current;
        main.execute(() -> required.onAuditStateChanged(snapshot));
    }

    public void unsubscribe(Listener listener) {
        listeners.remove(listener);
    }

    public void open() {
        load(AuditQuery.initial(), false);
    }

    public void refresh() {
        load(current.snapshot().map(Snapshot::query).orElseGet(AuditQuery::initial), false);
    }

    public void apply(AuditQuery query) {
        load(Objects.requireNonNull(query, "Audit query is required."), false);
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
        publish(AuditState.closed());
        listeners.clear();
    }

    private void load(AuditQuery query, boolean more) {
        if (!busy.compareAndSet(false, true)) {
            return;
        }
        Optional<Snapshot> previous = current.snapshot();
        long operation = generation.incrementAndGet();
        publish(previous.map(value -> AuditState.busy(value, more))
                .orElseGet(AuditState::loading));
        worker.execute(() -> execute(operation, query, previous, more));
    }

    private void execute(
            long operation,
            AuditQuery query,
            Optional<Snapshot> previous,
            boolean more) {
        try {
            Optional<String> cursor = more
                    ? previous.orElseThrow().nextCursor()
                    : Optional.empty();
            Page page = repository.events(query, cursor);
            List<Event> events = new ArrayList<>();
            if (more) {
                events.addAll(previous.orElseThrow().events());
            }
            events.addAll(page.items());
            complete(operation, AuditState.ready(
                    new Snapshot(query, events, page.nextCursor())));
        } catch (AuditException failure) {
            AuditState next = previous.isPresent() && transientFailure(failure.kind())
                    ? AuditState.stale(previous.orElseThrow(), failure)
                    : AuditState.error(failure);
            complete(operation, next);
        }
    }

    private void complete(long operation, AuditState state) {
        if (generation.get() != operation || current.phase() == AuditState.Phase.CLOSED) {
            return;
        }
        busy.set(false);
        publish(state);
    }

    private void publish(AuditState state) {
        current = state;
        main.execute(() -> listeners.forEach(listener -> listener.onAuditStateChanged(state)));
    }

    private static boolean transientFailure(AuditFailureKind kind) {
        return kind == AuditFailureKind.NETWORK
                || kind == AuditFailureKind.RATE_LIMITED
                || kind == AuditFailureKind.SERVICE_UNAVAILABLE;
    }
}
