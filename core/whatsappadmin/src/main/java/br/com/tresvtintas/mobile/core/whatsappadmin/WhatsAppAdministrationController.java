package br.com.tresvtintas.mobile.core.whatsappadmin;

import br.com.tresvtintas.mobile.core.whatsappadmin.WhatsAppAdministrationModels.ActionResult;
import br.com.tresvtintas.mobile.core.whatsappadmin.WhatsAppAdministrationModels.Snapshot;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public final class WhatsAppAdministrationController {
    @FunctionalInterface
    private interface ActionCommand {
        ActionResult execute(String idempotencyKey)
                throws WhatsAppAdministrationException;
    }

    private final WhatsAppAdministrationRepository repository;
    private final Executor worker;
    private final Executor main;
    private final Set<WhatsAppAdministrationStateListener> listeners =
            new CopyOnWriteArraySet<>();
    private final AtomicBoolean busy = new AtomicBoolean();
    private final AtomicBoolean closed = new AtomicBoolean();
    private final AtomicLong generation = new AtomicLong();
    private volatile WhatsAppAdministrationState current =
            WhatsAppAdministrationState.empty();

    public WhatsAppAdministrationController(
            WhatsAppAdministrationRepository repository,
            Executor worker,
            Executor main) {
        this.repository = Objects.requireNonNull(repository, "Repository is required.");
        this.worker = Objects.requireNonNull(worker, "Worker is required.");
        this.main = Objects.requireNonNull(main, "Main executor is required.");
    }

    public void subscribe(WhatsAppAdministrationStateListener listener) {
        WhatsAppAdministrationStateListener required = Objects.requireNonNull(
                listener,
                "Listener is required.");
        listeners.add(required);
        WhatsAppAdministrationState snapshot = current;
        main.execute(() -> required.onWhatsAppAdministrationStateChanged(snapshot));
    }

    public void unsubscribe(WhatsAppAdministrationStateListener listener) {
        listeners.remove(listener);
    }

    public WhatsAppAdministrationState currentState() {
        return current;
    }

    public void load() {
        if (!start()) {
            return;
        }
        long operation = generation.incrementAndGet();
        publish(WhatsAppAdministrationState.loading(current.snapshot()));
        worker.execute(() -> {
            try {
                complete(operation, WhatsAppAdministrationState.ready(repository.load()));
            } catch (WhatsAppAdministrationException failure) {
                fail(operation, failure);
            }
        });
    }

    public void configureMeta(
            long organizationId,
            String phoneNumberId,
            Optional<String> phoneNumber) {
        String normalizedId = WhatsAppAdministrationModels.phoneNumberId(
                phoneNumberId);
        Optional<String> normalizedPhone =
                WhatsAppAdministrationModels.phoneNumber(phoneNumber);
        mutate(key -> repository.configureMeta(
                organizationId,
                normalizedId,
                normalizedPhone,
                key));
    }

    public void provisionEvolution(long organizationId) {
        mutate(key -> repository.provisionEvolution(organizationId, key));
    }

    public void setMode(long organizationId, WhatsAppChannelMode mode) {
        WhatsAppChannelMode required = Objects.requireNonNull(mode, "Mode is required.");
        mutate(key -> repository.setMode(organizationId, required, key));
    }

    public void refreshEvolution(long connectionId) {
        mutate(key -> repository.refreshEvolution(connectionId, key));
    }

    public void requestQr(long connectionId) {
        Optional<Snapshot> previous = current.snapshot();
        if (previous.isEmpty() || !start()) {
            return;
        }
        long operation = generation.incrementAndGet();
        publish(WhatsAppAdministrationState.qrLoading(previous.orElseThrow()));
        worker.execute(() -> {
            try {
                complete(operation, current.withQr(repository.requestQr(connectionId)));
            } catch (WhatsAppAdministrationException failure) {
                fail(operation, failure);
            }
        });
    }

    public void clearEphemeralQr() {
        if (closed.get()) {
            return;
        }
        WhatsAppAdministrationState state = current;
        if (state.ephemeralQr().isPresent()) {
            publish(state.withoutQr());
        }
    }

    public void acknowledgeTransient() {
        if (closed.get()) {
            return;
        }
        WhatsAppAdministrationState state = current;
        if (state.failure().isPresent()
                || state.lastAction().isPresent()
                || state.ephemeralQr().isPresent()) {
            publish(state.withoutTransient());
        }
    }

    /**
     * Invalidates work that may finish after the protected screen leaves the foreground. A fresh
     * load on resume reconciles mutations that the server may already have accepted.
     */
    public void pause() {
        if (closed.get()) {
            return;
        }
        generation.incrementAndGet();
        busy.set(false);
        Optional<Snapshot> snapshot = current.snapshot();
        publish(snapshot.isPresent()
                ? WhatsAppAdministrationState.ready(snapshot.orElseThrow())
                : WhatsAppAdministrationState.empty());
    }

    public void close() {
        closed.set(true);
        generation.incrementAndGet();
        busy.set(false);
        publish(WhatsAppAdministrationState.closed());
        listeners.clear();
    }

    private void mutate(ActionCommand command) {
        Optional<Snapshot> previous = current.snapshot();
        if (previous.isEmpty() || !start()) {
            return;
        }
        long operation = generation.incrementAndGet();
        publish(WhatsAppAdministrationState.mutating(previous.orElseThrow()));
        worker.execute(() -> {
            try {
                ActionResult result = command.execute(UUID.randomUUID().toString());
                Snapshot fresh = repository.load();
                complete(operation, current.withAction(fresh, result));
            } catch (WhatsAppAdministrationException failure) {
                fail(operation, failure);
            }
        });
    }

    private boolean start() {
        return !closed.get() && busy.compareAndSet(false, true);
    }

    private void fail(long operation, WhatsAppAdministrationException failure) {
        WhatsAppAdministrationState state = current;
        if (state.snapshot().isPresent()) {
            complete(operation, state.withFailure(failure));
            return;
        }
        complete(operation, new WhatsAppAdministrationState(
                WhatsAppAdministrationState.Phase.READY,
                Optional.empty(),
                Optional.of(failure),
                Optional.empty(),
                Optional.empty()));
    }

    private void complete(long operation, WhatsAppAdministrationState state) {
        if (operation != generation.get() || closed.get()) {
            return;
        }
        busy.set(false);
        publish(state);
    }

    private void publish(WhatsAppAdministrationState state) {
        current = Objects.requireNonNull(state, "State is required.");
        main.execute(() -> listeners.forEach(listener ->
                listener.onWhatsAppAdministrationStateChanged(state)));
    }
}
