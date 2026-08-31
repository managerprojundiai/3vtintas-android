package br.com.tresvtintas.mobile.core.bootstrap;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public final class BootstrapController {
    private final BootstrapRepository repository;
    private final Executor workerExecutor;
    private final Executor mainExecutor;
    private final Set<BootstrapStateListener> listeners = new CopyOnWriteArraySet<>();
    private final AtomicBoolean operationInProgress = new AtomicBoolean();
    private final AtomicLong generation = new AtomicLong();
    private volatile BootstrapState current = BootstrapState.empty();
    private volatile Optional<String> loadedSessionKey = Optional.empty();

    BootstrapController(
            BootstrapRepository repository,
            Executor workerExecutor,
            Executor mainExecutor) {
        this.repository = Objects.requireNonNull(repository, "Bootstrap repository is required.");
        this.workerExecutor = Objects.requireNonNull(workerExecutor, "Worker executor is required.");
        this.mainExecutor = Objects.requireNonNull(mainExecutor, "Main executor is required.");
    }

    public void subscribe(BootstrapStateListener listener) {
        BootstrapStateListener required = Objects.requireNonNull(
                listener, "Bootstrap listener is required.");
        listeners.add(required);
        BootstrapState snapshot = current;
        mainExecutor.execute(() -> required.onBootstrapStateChanged(snapshot));
    }

    public void unsubscribe(BootstrapStateListener listener) {
        listeners.remove(listener);
    }

    public BootstrapState currentState() {
        return current;
    }

    public void load(ExpectedBootstrapIdentity identity) {
        load(identity, false);
    }

    public void refresh(ExpectedBootstrapIdentity identity) {
        load(identity, true);
    }

    public void clear() {
        generation.incrementAndGet();
        loadedSessionKey = Optional.empty();
        operationInProgress.set(false);
        publish(BootstrapState.empty());
    }

    private void load(ExpectedBootstrapIdentity identity, boolean force) {
        Objects.requireNonNull(identity, "Expected bootstrap identity is required.");
        String key = identity.stableKey();
        if (!force
                && loadedSessionKey.filter(key::equals).isPresent()
                && current.phase() == BootstrapState.Phase.READY) {
            /*
             * A resumed screen may have rendered the authenticated state after receiving the
             * cached bootstrap snapshot. Replaying the authoritative READY state keeps the UI
             * deterministic without issuing another network request.
             */
            publish(current);
            return;
        }
        if (!operationInProgress.compareAndSet(false, true)) {
            return;
        }
        long operationGeneration = generation.incrementAndGet();
        publish(BootstrapState.loading());
        workerExecutor.execute(() -> executeLoad(identity, key, operationGeneration));
    }

    private void executeLoad(
            ExpectedBootstrapIdentity identity,
            String key,
            long operationGeneration) {
        BootstrapState next;
        try {
            next = BootstrapState.ready(repository.load(identity));
        } catch (BootstrapException exception) {
            next = BootstrapState.error(exception);
        }
        if (generation.get() != operationGeneration) {
            return;
        }
        if (next.phase() == BootstrapState.Phase.READY) {
            loadedSessionKey = Optional.of(key);
        }
        operationInProgress.set(false);
        publish(next);
    }

    private void publish(BootstrapState next) {
        current = next;
        mainExecutor.execute(() -> {
            for (BootstrapStateListener listener : listeners) {
                listener.onBootstrapStateChanged(next);
            }
        });
    }
}
