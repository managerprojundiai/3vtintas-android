package br.com.tresvtintas.mobile.core.organizationadmin;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public final class OrganizationAdministrationTaskController<T> {
    @FunctionalInterface
    public interface Task<T> {
        T execute() throws OrganizationAdministrationException;
    }

    @FunctionalInterface
    public interface Listener<T> {
        void onOrganizationAdministrationTaskChanged(
                OrganizationAdministrationTaskState<T> state);
    }

    private final Executor worker;
    private final Executor main;
    private final Set<Listener<T>> listeners = new CopyOnWriteArraySet<>();
    private final AtomicBoolean busy = new AtomicBoolean();
    private final AtomicLong generation = new AtomicLong();
    private volatile OrganizationAdministrationTaskState<T> current =
            OrganizationAdministrationTaskState.idle();

    public OrganizationAdministrationTaskController(Executor worker, Executor main) {
        this.worker = Objects.requireNonNull(worker, "Worker is required.");
        this.main = Objects.requireNonNull(main, "Main executor is required.");
    }

    public void subscribe(Listener<T> listener) {
        Listener<T> required = Objects.requireNonNull(listener, "Listener is required.");
        listeners.add(required);
        OrganizationAdministrationTaskState<T> snapshot = current;
        main.execute(() ->
                required.onOrganizationAdministrationTaskChanged(snapshot));
    }

    public void submit(Task<T> task) {
        Objects.requireNonNull(task, "Task is required.");
        if (!busy.compareAndSet(false, true)) {
            return;
        }
        long operation = generation.incrementAndGet();
        publish(OrganizationAdministrationTaskState.running());
        worker.execute(() -> {
            try {
                complete(operation, OrganizationAdministrationTaskState.success(
                        task.execute()));
            } catch (OrganizationAdministrationException failure) {
                complete(operation, OrganizationAdministrationTaskState.error(failure));
            }
        });
    }

    public void close() {
        generation.incrementAndGet();
        busy.set(false);
        publish(OrganizationAdministrationTaskState.closed());
        listeners.clear();
    }

    private void complete(
            long operation,
            OrganizationAdministrationTaskState<T> state) {
        if (generation.get() != operation
                || current.phase()
                        == OrganizationAdministrationTaskState.Phase.CLOSED) {
            return;
        }
        busy.set(false);
        publish(state);
    }

    private void publish(OrganizationAdministrationTaskState<T> state) {
        current = state;
        main.execute(() -> listeners.forEach(listener ->
                listener.onOrganizationAdministrationTaskChanged(state)));
    }
}
