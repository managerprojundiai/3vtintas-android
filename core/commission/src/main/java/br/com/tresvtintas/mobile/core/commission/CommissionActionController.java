package br.com.tresvtintas.mobile.core.commission;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public final class CommissionActionController {
    @FunctionalInterface
    public interface Listener {
        void onCommissionActionStateChanged(CommissionActionState state);
    }

    private final CommissionRepository repository;
    private final Executor worker;
    private final Executor main;
    private final Set<Listener> listeners = new CopyOnWriteArraySet<>();
    private final AtomicBoolean busy = new AtomicBoolean();
    private final AtomicLong generation = new AtomicLong();
    private volatile CommissionActionState current = CommissionActionState.idle();

    public CommissionActionController(
            CommissionRepository repository,
            Executor worker,
            Executor main) {
        this.repository = Objects.requireNonNull(
                repository,
                "Commission repository is required.");
        this.worker = Objects.requireNonNull(
                worker,
                "Commission worker is required.");
        this.main = Objects.requireNonNull(
                main,
                "Main executor is required.");
    }

    public void subscribe(Listener listener) {
        Listener required = Objects.requireNonNull(
                listener,
                "Commission action listener is required.");
        listeners.add(required);
        CommissionActionState snapshot = current;
        main.execute(() -> required.onCommissionActionStateChanged(snapshot));
    }

    public void unsubscribe(Listener listener) {
        listeners.remove(listener);
    }

    public void execute(
            CommissionMutationCommand command,
            String idempotencyKey) {
        if (command == null || !validKey(idempotencyKey)) {
            CommissionAction safeAction = command == null
                    ? CommissionAction.APPROVE
                    : command.action();
            publish(CommissionActionState.error(
                    safeAction,
                    new CommissionException(
                            CommissionFailureKind.INVALID_REQUEST,
                            "Commission action request is invalid.")));
            return;
        }
        if (!busy.compareAndSet(false, true)) {
            return;
        }
        long operation = generation.incrementAndGet();
        publish(CommissionActionState.running(command.action()));
        worker.execute(() -> run(operation, command, idempotencyKey));
    }

    public void close() {
        generation.incrementAndGet();
        busy.set(false);
        publish(CommissionActionState.closed());
        listeners.clear();
    }

    private void run(
            long operation,
            CommissionMutationCommand command,
            String idempotencyKey) {
        CommissionActionState state;
        try {
            state = CommissionActionState.success(
                    repository.transition(command, idempotencyKey));
        } catch (CommissionException failure) {
            state = CommissionActionState.error(command.action(), failure);
        }
        if (generation.get() == operation
                && current.phase() != CommissionActionState.Phase.CLOSED) {
            busy.set(false);
            publish(state);
        }
    }

    private void publish(CommissionActionState state) {
        current = state;
        main.execute(() -> listeners.forEach(
                listener -> listener.onCommissionActionStateChanged(state)));
    }

    private static boolean validKey(String value) {
        return value != null
                && value.length() >= 16
                && value.length() <= 255
                && value.matches("^[\\x21-\\x7e]+$");
    }
}
