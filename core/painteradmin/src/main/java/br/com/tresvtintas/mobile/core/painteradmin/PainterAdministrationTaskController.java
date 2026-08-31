package br.com.tresvtintas.mobile.core.painteradmin;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public final class PainterAdministrationTaskController<T> {
    @FunctionalInterface
    public interface Task<T> {
        T execute() throws PainterAdministrationException;
    }

    @FunctionalInterface
    public interface Listener<T> {
        void onPainterAdministrationTaskChanged(
                PainterAdministrationTaskState<T> state);
    }

    private final Executor worker;
    private final Executor main;
    private final Set<Listener<T>> listeners = new CopyOnWriteArraySet<>();
    private final AtomicBoolean busy = new AtomicBoolean();
    private final AtomicLong generation = new AtomicLong();
    private volatile PainterAdministrationTaskState<T> current =
            PainterAdministrationTaskState.idle();

    public PainterAdministrationTaskController(
            Executor worker,
            Executor main) {
        this.worker = Objects.requireNonNull(worker, "Worker is required.");
        this.main = Objects.requireNonNull(main, "Main executor is required.");
    }

    public void subscribe(Listener<T> listener) {
        Listener<T> required = Objects.requireNonNull(
                listener,
                "Task listener is required.");
        listeners.add(required);
        PainterAdministrationTaskState<T> snapshot = current;
        main.execute(() -> required
                .onPainterAdministrationTaskChanged(snapshot));
    }

    public void unsubscribe(Listener<T> listener) {
        listeners.remove(listener);
    }

    public void submit(Task<T> task) {
        Objects.requireNonNull(task, "Task is required.");
        if (!busy.compareAndSet(false, true)) {
            return;
        }
        long operation = generation.incrementAndGet();
        publish(PainterAdministrationTaskState.running());
        worker.execute(() -> {
            try {
                complete(
                        operation,
                        PainterAdministrationTaskState.success(
                                task.execute()));
            } catch (PainterAdministrationException failure) {
                complete(
                        operation,
                        PainterAdministrationTaskState.error(failure));
            }
        });
    }

    public void close() {
        generation.incrementAndGet();
        busy.set(false);
        publish(PainterAdministrationTaskState.closed());
        listeners.clear();
    }

    private void complete(
            long operation,
            PainterAdministrationTaskState<T> state) {
        if (generation.get() != operation
                || current.phase()
                        == PainterAdministrationTaskState.Phase.CLOSED) {
            return;
        }
        busy.set(false);
        publish(state);
    }

    private void publish(PainterAdministrationTaskState<T> state) {
        current = state;
        main.execute(() -> listeners.forEach(listener ->
                listener.onPainterAdministrationTaskChanged(state)));
    }
}
