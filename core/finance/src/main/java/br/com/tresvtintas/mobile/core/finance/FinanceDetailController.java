package br.com.tresvtintas.mobile.core.finance;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicLong;

public final class FinanceDetailController {
    private static final long MINIMUM_ENTRY_ID = 1L;
    private final FinanceRepository repository;
    private final Executor worker;
    private final Executor main;
    private final Set<FinanceDetailStateListener> listeners =
            new CopyOnWriteArraySet<>();
    private final AtomicLong generation = new AtomicLong();
    private volatile FinanceDetailState current = FinanceDetailState.empty();

    public FinanceDetailController(
            FinanceRepository repository,
            Executor worker,
            Executor main) {
        this.repository = Objects.requireNonNull(
                repository,
                "Finance repository is required.");
        this.worker = Objects.requireNonNull(worker, "Finance worker is required.");
        this.main = Objects.requireNonNull(main, "Main executor is required.");
    }

    public void subscribe(FinanceDetailStateListener listener) {
        FinanceDetailStateListener required = Objects.requireNonNull(
                listener,
                "Finance detail listener is required.");
        listeners.add(required);
        FinanceDetailState snapshot = current;
        main.execute(() -> required.onFinanceDetailStateChanged(snapshot));
    }

    public void unsubscribe(FinanceDetailStateListener listener) {
        listeners.remove(listener);
    }

    public void load(long entryId) {
        if (entryId < MINIMUM_ENTRY_ID) {
            publish(FinanceDetailState.error(new FinanceException(
                    FinanceFailureKind.INVALID_REQUEST,
                    "Finance detail input is invalid.")));
            return;
        }
        long operation = generation.incrementAndGet();
        publish(FinanceDetailState.loading());
        worker.execute(() -> {
            try {
                FinanceDetail detail = repository.detail(entryId);
                if (current(operation)) {
                    publish(FinanceDetailState.ready(detail));
                }
            } catch (FinanceException failure) {
                if (current(operation)) {
                    publish(FinanceDetailState.error(failure));
                }
            }
        });
    }

    public void close() {
        generation.incrementAndGet();
        publish(FinanceDetailState.closed());
        listeners.clear();
    }

    private boolean current(long operation) {
        return generation.get() == operation
                && current.phase() != FinanceDetailState.Phase.CLOSED;
    }

    private void publish(FinanceDetailState state) {
        current = state;
        main.execute(() -> listeners.forEach(
                listener -> listener.onFinanceDetailStateChanged(state)));
    }
}
