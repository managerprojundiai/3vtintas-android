package br.com.tresvtintas.mobile.core.order;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public final class OrderActionController {
    private static final int MAX_REASON_LENGTH = 1_000;
    private static final int MAX_REFERENCE_LENGTH = 500;

    private final OrderRepository repository;
    private final Executor worker;
    private final Executor main;
    private final Set<OrderActionStateListener> listeners =
            new CopyOnWriteArraySet<>();
    private final AtomicBoolean operationInProgress = new AtomicBoolean();
    private final AtomicLong generation = new AtomicLong();
    private volatile OrderActionState current = OrderActionState.idle();

    public OrderActionController(
            OrderRepository repository,
            Executor worker,
            Executor main) {
        this.repository = Objects.requireNonNull(
                repository,
                "Order repository is required.");
        this.worker = Objects.requireNonNull(
                worker,
                "Order worker is required.");
        this.main = Objects.requireNonNull(
                main,
                "Main executor is required.");
    }

    public void subscribe(OrderActionStateListener listener) {
        OrderActionStateListener required = Objects.requireNonNull(
                listener,
                "Order action listener is required.");
        listeners.add(required);
        OrderActionState snapshot = current;
        main.execute(() -> required.onOrderActionStateChanged(snapshot));
    }

    public void unsubscribe(OrderActionStateListener listener) {
        listeners.remove(listener);
    }

    public void transitionStatus(
            long orderId,
            int expectedRevision,
            OrderStatus status,
            String idempotencyKey) {
        OrderAction action = actionFor(status);
        if (!validRequest(orderId, expectedRevision, idempotencyKey)
                || action == null) {
            invalid(action == null ? OrderAction.CONFIRM : action);
            return;
        }
        execute(
                action,
                () -> repository.transitionStatus(
                        orderId,
                        expectedRevision,
                        status,
                        idempotencyKey));
    }

    public void cancel(
            long orderId,
            int expectedRevision,
            Optional<String> reason,
            String idempotencyKey) {
        Optional<String> normalized = normalize(reason);
        if (!validRequest(orderId, expectedRevision, idempotencyKey)
                || exceeds(reason, MAX_REASON_LENGTH)) {
            invalid(OrderAction.CANCEL);
            return;
        }
        execute(
                OrderAction.CANCEL,
                () -> repository.cancel(
                        orderId,
                        expectedRevision,
                        normalized,
                        idempotencyKey));
    }

    public void recordPayment(
            long orderId,
            int expectedRevision,
            OrderPaymentMethod paymentMethod,
            Optional<String> paymentReference,
            String idempotencyKey) {
        Optional<String> normalized = normalize(paymentReference);
        if (!validRequest(orderId, expectedRevision, idempotencyKey)
                || paymentMethod == null
                || exceeds(paymentReference, MAX_REFERENCE_LENGTH)) {
            invalid(OrderAction.RECORD_PAYMENT);
            return;
        }
        execute(
                OrderAction.RECORD_PAYMENT,
                () -> repository.recordPayment(
                        orderId,
                        expectedRevision,
                        paymentMethod,
                        normalized,
                        idempotencyKey));
    }

    public void close() {
        generation.incrementAndGet();
        operationInProgress.set(false);
        publish(OrderActionState.closed());
        listeners.clear();
    }

    private void execute(
            OrderAction action,
            OrderActionOperation operation) {
        if (!operationInProgress.compareAndSet(false, true)) {
            return;
        }
        long operationGeneration = generation.incrementAndGet();
        publish(OrderActionState.running(action));
        worker.execute(() -> {
            try {
                complete(
                        operationGeneration,
                        OrderActionState.success(operation.run()));
            } catch (OrderException failure) {
                complete(
                        operationGeneration,
                        OrderActionState.error(action, failure));
            }
        });
    }

    private void complete(
            long operationGeneration,
            OrderActionState state) {
        if (generation.get() != operationGeneration
                || current.phase() == OrderActionState.Phase.CLOSED) {
            return;
        }
        operationInProgress.set(false);
        publish(state);
    }

    private void invalid(OrderAction action) {
        publish(OrderActionState.error(
                action,
                new OrderException(
                        OrderFailureKind.INVALID_REQUEST,
                        "Order action request is invalid.")));
    }

    private void publish(OrderActionState state) {
        current = state;
        main.execute(() -> listeners.forEach(
                listener -> listener.onOrderActionStateChanged(state)));
    }

    private static OrderAction actionFor(OrderStatus status) {
        if (status == null) {
            return null;
        }
        return switch (status) {
            case CONFIRMED -> OrderAction.CONFIRM;
            case IN_PROGRESS -> OrderAction.START_FULFILLMENT;
            case DELIVERED -> OrderAction.COMPLETE;
            default -> null;
        };
    }

    private static boolean validRequest(
            long orderId,
            int expectedRevision,
            String idempotencyKey) {
        return orderId > 0
                && expectedRevision > 0
                && idempotencyKey != null
                && idempotencyKey.length() >= 16
                && idempotencyKey.length() <= 255
                && idempotencyKey.matches("^[\\x21-\\x7e]+$");
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
    private interface OrderActionOperation {
        OrderActionResult run() throws OrderException;
    }
}
