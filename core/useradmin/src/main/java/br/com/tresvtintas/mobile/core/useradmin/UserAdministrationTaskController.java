package br.com.tresvtintas.mobile.core.useradmin;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public final class UserAdministrationTaskController<T> {
    @FunctionalInterface
    public interface Task<T> {
        T execute() throws UserAdministrationException;
    }

    @FunctionalInterface
    public interface Listener<T> {
        void onUserAdministrationTaskChanged(UserAdministrationTaskState<T> state);
    }

    private final Executor worker;
    private final Executor main;
    private final Set<Listener<T>> listeners = new CopyOnWriteArraySet<>();
    private final AtomicBoolean busy = new AtomicBoolean();
    private final AtomicLong generation = new AtomicLong();
    private volatile UserAdministrationTaskState<T> current =
            UserAdministrationTaskState.idle();

    public UserAdministrationTaskController(Executor worker, Executor main) {
        this.worker = Objects.requireNonNull(worker, "Worker is required.");
        this.main = Objects.requireNonNull(main, "Main executor is required.");
    }

    public void subscribe(Listener<T> listener) {
        Listener<T> required = Objects.requireNonNull(listener, "Listener is required.");
        listeners.add(required);
        UserAdministrationTaskState<T> snapshot = current;
        main.execute(() -> required.onUserAdministrationTaskChanged(snapshot));
    }

    public void submit(Task<T> task) {
        Objects.requireNonNull(task, "Task is required.");
        if (!busy.compareAndSet(false, true)) {
            return;
        }
        long operation = generation.incrementAndGet();
        publish(UserAdministrationTaskState.running());
        worker.execute(() -> {
            try {
                complete(operation, UserAdministrationTaskState.success(task.execute()));
            } catch (UserAdministrationException failure) {
                complete(operation, UserAdministrationTaskState.error(failure));
            }
        });
    }

    public void close() {
        generation.incrementAndGet();
        busy.set(false);
        publish(UserAdministrationTaskState.closed());
        listeners.clear();
    }

    private void complete(long operation, UserAdministrationTaskState<T> state) {
        if (generation.get() != operation
                || current.phase() == UserAdministrationTaskState.Phase.CLOSED) {
            return;
        }
        busy.set(false);
        publish(state);
    }

    private void publish(UserAdministrationTaskState<T> state) {
        current = state;
        main.execute(() -> listeners.forEach(
                listener -> listener.onUserAdministrationTaskChanged(state)));
    }
}
