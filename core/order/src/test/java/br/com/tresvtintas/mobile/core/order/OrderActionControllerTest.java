package br.com.tresvtintas.mobile.core.order;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.Test;

public final class OrderActionControllerTest {
    private static final String KEY =
            "00000000-0000-4000-8000-000000000701";

    @Test
    public void delegatesConfirmedSequentialStatusAndPublishesResult() {
        FakeRepository repository = new FakeRepository();
        OrderActionController controller = new OrderActionController(
                repository,
                Runnable::run,
                Runnable::run);

        controller.transitionStatus(
                701,
                3,
                OrderStatus.IN_PROGRESS,
                KEY);

        assertEquals(
                "The exact expected revision must reach the repository.",
                3,
                repository.revision);
        assertEquals(
                "The requested sequential status must reach the repository.",
                OrderStatus.IN_PROGRESS,
                repository.status);
        assertEquals(
                "The logical attempt key must remain unchanged.",
                KEY,
                repository.key);
        assertEquals(
                "A successful command must publish its result.",
                OrderActionState.Phase.SUCCESS,
                controllerState(controller).phase());
    }

    @Test
    public void normalizesOptionalCancellationReason() {
        FakeRepository repository = new FakeRepository();
        OrderActionController controller = new OrderActionController(
                repository,
                Runnable::run,
                Runnable::run);

        controller.cancel(701, 4, Optional.of("  cliente solicitou  "), KEY);

        assertEquals(
                "Cancellation reason must be normalized once.",
                Optional.of("cliente solicitou"),
                repository.reason);
    }

    @Test
    public void suppressesSecondCommandWhileFirstIsRunning() {
        FakeRepository repository = new FakeRepository();
        List<Runnable> work = new ArrayList<>();
        OrderActionController controller = new OrderActionController(
                repository,
                work::add,
                Runnable::run);

        controller.transitionStatus(701, 1, OrderStatus.CONFIRMED, KEY);
        controller.cancel(701, 1, Optional.empty(), KEY);

        assertEquals(
                "Only one mutation may be scheduled at a time.",
                1,
                work.size());
        work.get(0).run();
        assertEquals(
                "Only the first mutation may reach the repository.",
                1,
                repository.calls);
    }

    private static OrderActionState controllerState(
            OrderActionController controller) {
        OrderActionState[] state = new OrderActionState[1];
        controller.subscribe(value -> state[0] = value);
        return state[0];
    }

    private static final class FakeRepository implements OrderRepository {
        private int calls;
        private int revision;
        private OrderStatus status;
        private String key;
        private Optional<String> reason = Optional.empty();

        @Override
        public OrderPage page(
                OrderQuery query,
                Optional<String> cursor) {
            throw new AssertionError("Not used.");
        }

        @Override
        public OrderDetail detail(long orderId) {
            throw new AssertionError("Not used.");
        }

        @Override
        public OrderConversionResult convertMaterialQuote(
                long quoteId,
                int expectedRevision,
                String idempotencyKey) {
            throw new AssertionError("Not used.");
        }

        @Override
        public OrderActionResult transitionStatus(
                long orderId,
                int expectedRevision,
                OrderStatus target,
                String idempotencyKey) {
            calls++;
            revision = expectedRevision;
            status = target;
            key = idempotencyKey;
            return result(
                    target == OrderStatus.CONFIRMED
                            ? OrderAction.CONFIRM
                            : OrderAction.START_FULFILLMENT,
                    target);
        }

        @Override
        public OrderActionResult cancel(
                long orderId,
                int expectedRevision,
                Optional<String> cancellationReason,
                String idempotencyKey) {
            calls++;
            revision = expectedRevision;
            reason = cancellationReason;
            key = idempotencyKey;
            return result(OrderAction.CANCEL, OrderStatus.CANCELLED);
        }

        @Override
        public OrderActionResult recordPayment(
                long orderId,
                int expectedRevision,
                OrderPaymentMethod method,
                Optional<String> reference,
                String idempotencyKey) {
            calls++;
            return new OrderActionResult(
                    OrderAction.RECORD_PAYMENT,
                    701,
                    expectedRevision + 1,
                    OrderStatus.CONFIRMED,
                    OrderPaymentStatus.RECEIVED,
                    true,
                    false);
        }

        private static OrderActionResult result(
                OrderAction action,
                OrderStatus status) {
            return new OrderActionResult(
                    action,
                    701,
                    2,
                    status,
                    OrderPaymentStatus.PENDING,
                    true,
                    false);
        }
    }
}
