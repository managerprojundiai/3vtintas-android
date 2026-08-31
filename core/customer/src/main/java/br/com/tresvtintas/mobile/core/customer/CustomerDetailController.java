package br.com.tresvtintas.mobile.core.customer;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicLong;

public final class CustomerDetailController {
    private static final int MINIMUM_CUSTOMER_ID = 1;
    private final CustomerRepository repository;
    private final Executor workerExecutor;
    private final Executor mainExecutor;
    private final Set<CustomerDetailStateListener> listeners =
            new CopyOnWriteArraySet<>();
    private final AtomicLong generation = new AtomicLong();
    private volatile CustomerDetailState current = CustomerDetailState.empty();

    public CustomerDetailController(
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

    public void subscribe(CustomerDetailStateListener listener) {
        CustomerDetailStateListener required = Objects.requireNonNull(
                listener,
                "Customer detail listener is required.");
        listeners.add(required);
        CustomerDetailState snapshot = current;
        mainExecutor.execute(() -> required.onCustomerDetailStateChanged(snapshot));
    }

    public void unsubscribe(CustomerDetailStateListener listener) {
        listeners.remove(listener);
    }

    public CustomerDetailState currentState() {
        return current;
    }

    public void load(long customerId) {
        if (customerId < MINIMUM_CUSTOMER_ID) {
            publish(CustomerDetailState.error(new CustomerException(
                    CustomerFailureKind.INVALID_REQUEST,
                    "Customer ID is invalid.")));
            return;
        }
        long operationGeneration = generation.incrementAndGet();
        publish(CustomerDetailState.loading());
        workerExecutor.execute(() -> executeLoad(
                customerId,
                operationGeneration));
    }

    public void close() {
        generation.incrementAndGet();
        publish(CustomerDetailState.closed());
        listeners.clear();
    }

    private void executeLoad(long customerId, long operationGeneration) {
        try {
            complete(operationGeneration, CustomerDetailState.ready(
                    repository.detail(customerId)));
        } catch (CustomerException exception) {
            complete(
                    operationGeneration,
                    CustomerDetailState.error(exception));
        }
    }

    private void complete(
            long operationGeneration,
            CustomerDetailState state) {
        if (generation.get() == operationGeneration
                && current.phase() != CustomerDetailState.Phase.CLOSED) {
            publish(state);
        }
    }

    private void publish(CustomerDetailState next) {
        current = next;
        mainExecutor.execute(() -> {
            for (CustomerDetailStateListener listener : listeners) {
                listener.onCustomerDetailStateChanged(next);
            }
        });
    }
}
