package br.com.tresvtintas.mobile.core.systemconfiguration;

import br.com.tresvtintas.mobile.core.systemconfiguration.SystemConfigurationModels.Mutation;
import br.com.tresvtintas.mobile.core.systemconfiguration.SystemConfigurationModels.Snapshot;
import br.com.tresvtintas.mobile.core.systemconfiguration.SystemConfigurationModels.Values;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public final class SystemConfigurationController {
    private final SystemConfigurationRepository repository;
    private final Executor worker;
    private final Executor main;
    private final Set<SystemConfigurationStateListener> listeners =
            new CopyOnWriteArraySet<>();
    private final AtomicBoolean busy = new AtomicBoolean();
    private final AtomicBoolean closed = new AtomicBoolean();
    private final AtomicLong generation = new AtomicLong();
    private volatile SystemConfigurationState current =
            SystemConfigurationState.empty();

    public SystemConfigurationController(
            SystemConfigurationRepository repository,
            Executor worker,
            Executor main) {
        this.repository = Objects.requireNonNull(
                repository,
                "Configuration repository is required.");
        this.worker = Objects.requireNonNull(worker, "Worker is required.");
        this.main = Objects.requireNonNull(main, "Main executor is required.");
    }

    public void subscribe(SystemConfigurationStateListener listener) {
        SystemConfigurationStateListener required = Objects.requireNonNull(
                listener,
                "Configuration listener is required.");
        listeners.add(required);
        SystemConfigurationState snapshot = current;
        main.execute(() -> required.onSystemConfigurationStateChanged(snapshot));
    }

    public void unsubscribe(SystemConfigurationStateListener listener) {
        listeners.remove(listener);
    }

    public SystemConfigurationState currentState() {
        return current;
    }

    public void load() {
        if (closed.get() || !busy.compareAndSet(false, true)) {
            return;
        }
        long operation = generation.incrementAndGet();
        publish(SystemConfigurationState.loading());
        worker.execute(() -> {
            try {
                complete(operation, SystemConfigurationState.ready(repository.load()));
            } catch (SystemConfigurationException failure) {
                completeFailure(operation, Optional.empty(), failure);
            }
        });
    }

    public void update(Values values) {
        Optional<Snapshot> previous = current.configuration();
        if (closed.get()
                || previous.isEmpty()
                || !busy.compareAndSet(false, true)) {
            return;
        }
        long operation = generation.incrementAndGet();
        Snapshot original = previous.orElseThrow();
        publish(SystemConfigurationState.saving(original));
        worker.execute(() -> save(operation, original, values));
    }

    public void close() {
        closed.set(true);
        generation.incrementAndGet();
        busy.set(false);
        publish(SystemConfigurationState.closed());
        listeners.clear();
    }

    private void save(long operation, Snapshot previous, Values values) {
        try {
            Mutation result = repository.update(
                    values,
                    previous.revision(),
                    UUID.randomUUID().toString());
            complete(operation, SystemConfigurationState.ready(
                    result.configuration(),
                    Optional.empty(),
                    result.changed(),
                    result.replayed()));
        } catch (SystemConfigurationException failure) {
            if (failure.kind() == SystemConfigurationFailureKind.CONFLICT) {
                try {
                    Snapshot fresh = repository.load();
                    complete(operation, SystemConfigurationState.ready(
                            fresh,
                            Optional.of(failure),
                            false,
                            false));
                    return;
                } catch (SystemConfigurationException reloadFailure) {
                    completeFailure(operation, Optional.empty(), reloadFailure);
                    return;
                }
            }
            completeFailure(operation, Optional.of(previous), failure);
        }
    }

    private void completeFailure(
            long operation,
            Optional<Snapshot> previous,
            SystemConfigurationException failure) {
        if (previous.isPresent()) {
            complete(operation, SystemConfigurationState.ready(
                    previous.orElseThrow(),
                    Optional.of(failure),
                    false,
                    false));
        } else {
            complete(operation, new SystemConfigurationState(
                    SystemConfigurationState.Phase.READY,
                    Optional.empty(),
                    Optional.of(failure),
                    false,
                    false));
        }
    }

    private void complete(long operation, SystemConfigurationState state) {
        if (generation.get() != operation || closed.get()) {
            return;
        }
        busy.set(false);
        publish(state);
    }

    private void publish(SystemConfigurationState state) {
        current = state;
        main.execute(() -> listeners.forEach(
                listener -> listener.onSystemConfigurationStateChanged(state)));
    }
}
