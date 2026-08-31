package br.com.tresvtintas.mobile.core.commission;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicLong;

public final class CommissionDetailController {
    private final CommissionRepository repository;
    private final Executor worker;
    private final Executor main;
    private final Set<CommissionDetailStateListener> listeners =
            new CopyOnWriteArraySet<>();
    private final AtomicLong generation = new AtomicLong();
    private volatile CommissionDetailState current = CommissionDetailState.empty();

    public CommissionDetailController(
            CommissionRepository repository,
            Executor worker,
            Executor main) {
        this.repository = Objects.requireNonNull(
                repository,
                "Commission repository is required.");
        this.worker = Objects.requireNonNull(worker, "Commission worker is required.");
        this.main = Objects.requireNonNull(main, "Main executor is required.");
    }

    public void subscribe(CommissionDetailStateListener listener) {
        CommissionDetailStateListener required = Objects.requireNonNull(
                listener,
                "Commission detail listener is required.");
        listeners.add(required);
        CommissionDetailState snapshot = current;
        main.execute(() -> required.onCommissionDetailStateChanged(snapshot));
    }

    public void unsubscribe(CommissionDetailStateListener listener) {
        listeners.remove(listener);
    }

    public void load(long commissionId, CommissionScope scope) {
        if (commissionId < 1 || scope == null) {
            publish(CommissionDetailState.error(new CommissionException(
                    CommissionFailureKind.INVALID_REQUEST,
                    "Commission detail input is invalid.")));
            return;
        }
        long operation = generation.incrementAndGet();
        publish(CommissionDetailState.loading());
        worker.execute(() -> {
            try {
                CommissionDetail detail = repository.detail(commissionId, scope);
                if (current(operation)) {
                    publish(CommissionDetailState.ready(detail));
                }
            } catch (CommissionException failure) {
                if (current(operation)) {
                    publish(CommissionDetailState.error(failure));
                }
            }
        });
    }

    public void close() {
        generation.incrementAndGet();
        publish(CommissionDetailState.closed());
        listeners.clear();
    }

    private boolean current(long operation) {
        return generation.get() == operation
                && current.phase() != CommissionDetailState.Phase.CLOSED;
    }

    private void publish(CommissionDetailState state) {
        current = state;
        main.execute(() -> listeners.forEach(
                listener -> listener.onCommissionDetailStateChanged(state)));
    }
}
