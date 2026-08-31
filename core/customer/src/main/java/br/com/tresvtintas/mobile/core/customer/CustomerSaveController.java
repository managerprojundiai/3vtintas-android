package br.com.tresvtintas.mobile.core.customer;

import java.util.Objects;
import java.util.OptionalLong;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public final class CustomerSaveController {
    private static final int MINIMUM_CUSTOMER_ID = 1;
    private final CustomerRepository repository;
    private final Executor workerExecutor;
    private final Executor mainExecutor;
    private final Set<CustomerSaveStateListener> listeners =
            new CopyOnWriteArraySet<>();
    private final AtomicBoolean operationInProgress = new AtomicBoolean();
    private final AtomicLong generation = new AtomicLong();
    private volatile CustomerSaveState current = CustomerSaveState.idle();

    public CustomerSaveController(
            CustomerRepository repository,
            Executor workerExecutor,
            Executor mainExecutor) {
        this.repository = Objects.requireNonNull(
                repository,
                "Customer repository is required.");
        this.workerExecutor = Objects.requireNonNull(
                workerExecutor,
                "Worker executor is required.");
        this.mainExecutor = Objects.requireNonNull(
                mainExecutor,
                "Main executor is required.");
    }

    public void subscribe(CustomerSaveStateListener listener) {
        CustomerSaveStateListener required = Objects.requireNonNull(
                listener,
                "Customer save listener is required.");
        listeners.add(required);
        CustomerSaveState snapshot = current;
        mainExecutor.execute(() -> required.onCustomerSaveStateChanged(snapshot));
    }

    public void unsubscribe(CustomerSaveStateListener listener) {
        listeners.remove(listener);
    }

    public CustomerSaveState currentState() {
        return current;
    }

    public void create(
            OptionalLong organizationId,
            CustomerDraft customer,
            String idempotencyKey) {
        CustomerDraft required = Objects.requireNonNull(
                customer,
                "Customer draft is required.");
        String key = validKey(idempotencyKey);
        execute(
                () -> repository.create(
                        organizationId == null
                                ? OptionalLong.empty()
                                : organizationId,
                        required,
                        key));
    }

    public void update(
            long customerId,
            CustomerDraft customer,
            String idempotencyKey) {
        if (customerId < MINIMUM_CUSTOMER_ID) {
            publish(CustomerSaveState.error(new CustomerException(
                    CustomerFailureKind.INVALID_REQUEST,
                    "Customer ID is invalid.")));
            return;
        }
        CustomerDraft required = Objects.requireNonNull(
                customer,
                "Customer draft is required.");
        String key = validKey(idempotencyKey);
        execute(() -> repository.update(
                customerId,
                required,
                key));
    }

    public void close() {
        generation.incrementAndGet();
        operationInProgress.set(false);
        publish(CustomerSaveState.closed());
        listeners.clear();
    }

    private void execute(CustomerSaveOperation operation) {
        Objects.requireNonNull(operation, "Customer save operation is required.");
        if (!operationInProgress.compareAndSet(false, true)) {
            return;
        }
        long operationGeneration = generation.incrementAndGet();
        publish(CustomerSaveState.saving());
        workerExecutor.execute(() -> {
            try {
                complete(
                        operationGeneration,
                        CustomerSaveState.success(operation.run()));
            } catch (CustomerException exception) {
                complete(
                        operationGeneration,
                        CustomerSaveState.error(exception));
            }
        });
    }

    private void complete(long operationGeneration, CustomerSaveState state) {
        if (generation.get() != operationGeneration
                || current.phase() == CustomerSaveState.Phase.CLOSED) {
            return;
        }
        operationInProgress.set(false);
        publish(state);
    }

    private void publish(CustomerSaveState next) {
        current = next;
        mainExecutor.execute(() -> {
            for (CustomerSaveStateListener listener : listeners) {
                listener.onCustomerSaveStateChanged(next);
            }
        });
    }

    private static String validKey(String value) {
        if (value == null
                || value.length() < 16
                || value.length() > 255
                || !value.matches("^[\\x21-\\x7e]+$")) {
            throw new IllegalArgumentException(
                    "Customer idempotency key is invalid.");
        }
        return value;
    }

    @FunctionalInterface
    private interface CustomerSaveOperation {
        CustomerMutationResult run() throws CustomerException;
    }
}
