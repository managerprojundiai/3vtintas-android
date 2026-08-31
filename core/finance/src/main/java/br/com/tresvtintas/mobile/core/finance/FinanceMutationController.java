package br.com.tresvtintas.mobile.core.finance;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public final class FinanceMutationController {
    private static final int MAX_REFERENCE_LENGTH = 2_000;

    private final FinanceRepository repository;
    private final Executor worker;
    private final Executor main;
    private final Set<FinanceMutationStateListener> listeners =
            new CopyOnWriteArraySet<>();
    private final AtomicBoolean operationInProgress = new AtomicBoolean();
    private final AtomicLong generation = new AtomicLong();
    private volatile FinanceMutationState current = FinanceMutationState.idle();

    public FinanceMutationController(
            FinanceRepository repository,
            Executor worker,
            Executor main) {
        this.repository = Objects.requireNonNull(
                repository,
                "Finance repository is required.");
        this.worker = Objects.requireNonNull(worker, "Finance worker is required.");
        this.main = Objects.requireNonNull(main, "Main executor is required.");
    }

    public void subscribe(FinanceMutationStateListener listener) {
        FinanceMutationStateListener required = Objects.requireNonNull(
                listener,
                "Finance mutation listener is required.");
        listeners.add(required);
        FinanceMutationState snapshot = current;
        main.execute(() -> required.onFinanceMutationStateChanged(snapshot));
    }

    public void unsubscribe(FinanceMutationStateListener listener) {
        listeners.remove(listener);
    }

    public void create(FinanceDraft draft, String idempotencyKey) {
        if (draft == null || !validKey(idempotencyKey)) {
            invalid(FinanceAction.CREATE);
            return;
        }
        execute(
                FinanceAction.CREATE,
                () -> repository.create(draft, idempotencyKey));
    }

    public void settle(
            long entryId,
            FinancePaymentMethod paymentMethod,
            Optional<String> paymentReference,
            String idempotencyKey) {
        Optional<String> normalized = normalize(paymentReference);
        if (entryId < 1
                || paymentMethod == null
                || !validKey(idempotencyKey)
                || exceeds(paymentReference, MAX_REFERENCE_LENGTH)) {
            invalid(FinanceAction.SETTLE);
            return;
        }
        execute(
                FinanceAction.SETTLE,
                () -> repository.settle(
                        entryId,
                        paymentMethod,
                        normalized,
                        idempotencyKey));
    }

    public void cancel(long entryId, String idempotencyKey) {
        if (entryId < 1 || !validKey(idempotencyKey)) {
            invalid(FinanceAction.CANCEL);
            return;
        }
        execute(
                FinanceAction.CANCEL,
                () -> repository.cancel(entryId, idempotencyKey));
    }

    public void close() {
        generation.incrementAndGet();
        operationInProgress.set(false);
        publish(FinanceMutationState.closed());
        listeners.clear();
    }

    private void execute(
            FinanceAction action,
            FinanceMutationOperation operation) {
        if (!operationInProgress.compareAndSet(false, true)) {
            return;
        }
        long operationGeneration = generation.incrementAndGet();
        publish(FinanceMutationState.running(action));
        worker.execute(() -> {
            try {
                complete(
                        operationGeneration,
                        FinanceMutationState.success(operation.run()));
            } catch (FinanceException failure) {
                complete(
                        operationGeneration,
                        FinanceMutationState.error(action, failure));
            }
        });
    }

    private void complete(
            long operationGeneration,
            FinanceMutationState state) {
        if (generation.get() != operationGeneration
                || current.phase() == FinanceMutationState.Phase.CLOSED) {
            return;
        }
        operationInProgress.set(false);
        publish(state);
    }

    private void invalid(FinanceAction action) {
        publish(FinanceMutationState.error(
                action,
                new FinanceException(
                        FinanceFailureKind.INVALID_REQUEST,
                        "Finance mutation request is invalid.")));
    }

    private void publish(FinanceMutationState state) {
        current = state;
        main.execute(() -> listeners.forEach(
                listener -> listener.onFinanceMutationStateChanged(state)));
    }

    private static boolean validKey(String value) {
        return value != null
                && value.length() >= 16
                && value.length() <= 255
                && value.matches("^[\\x21-\\x7e]+$");
    }

    private static Optional<String> normalize(Optional<String> value) {
        if (value == null || value.isEmpty()) {
            return Optional.empty();
        }
        String text = value.orElseThrow().trim();
        return text.isEmpty() ? Optional.empty() : Optional.of(text);
    }

    private static boolean exceeds(
            Optional<String> value,
            int maxLength) {
        return value != null
                && value.isPresent()
                && value.orElseThrow().trim().length() > maxLength;
    }

    @FunctionalInterface
    private interface FinanceMutationOperation {
        FinanceMutationResult run() throws FinanceException;
    }
}
