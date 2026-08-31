package br.com.tresvtintas.mobile.core.order;

import static org.junit.Assert.assertEquals;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import org.junit.Test;

public final class OrderListControllerTest {
    private static final String NOT_USED = "Not used.";
    @Test
    public void appendsOpaqueNextPageWithoutReplacingFirstPage() {
        FakeRepository repository = new FakeRepository();
        OrderListController controller = new OrderListController(repository, Runnable::run, Runnable::run);
        controller.open(OrderQuery.initial());
        controller.loadMore();

        assertEquals(
                "Pagination must preserve the first page.",
                List.of(1L, 2L),
                controllerStateIds(controller));
        assertEquals(
                "The opaque cursor must be forwarded without interpretation.",
                Optional.of("next"),
                repository.receivedCursor);
    }

    private static List<Long> controllerStateIds(OrderListController controller) {
        final OrderListState[] state = new OrderListState[1];
        controller.subscribe(value -> state[0] = value);
        return state[0].snapshot().orElseThrow().items().stream().map(OrderSummary::id).toList();
    }

    private static final class FakeRepository implements OrderRepository {
        private Optional<String> receivedCursor = Optional.empty();
        @Override public OrderPage page(OrderQuery query, Optional<String> cursor) {
            receivedCursor = cursor;
            return cursor.isEmpty() ? new OrderPage(List.of(order(1)), Optional.of("next"))
                    : new OrderPage(List.of(order(2)), Optional.empty());
        }
        @Override public OrderDetail detail(long orderId) { throw new AssertionError(NOT_USED); }
        @Override public OrderConversionResult convertMaterialQuote(long quoteId, int expectedRevision,
                String idempotencyKey) { throw new AssertionError(NOT_USED); }
        @Override public OrderActionResult transitionStatus(long orderId, int expectedRevision,
                OrderStatus status, String idempotencyKey) {
            throw new AssertionError(NOT_USED);
        }
        @Override public OrderActionResult cancel(long orderId, int expectedRevision,
                Optional<String> reason, String idempotencyKey) {
            throw new AssertionError(NOT_USED);
        }
        @Override public OrderActionResult recordPayment(long orderId, int expectedRevision,
                OrderPaymentMethod method, Optional<String> reference, String idempotencyKey) {
            throw new AssertionError(NOT_USED);
        }
        private static OrderSummary order(long id) {
            return new OrderSummary(id, OrderType.MATERIAL, OrderStatus.PENDING, 1,
                    OrderPaymentStatus.PENDING, Set.of(), Optional.of(new BigDecimal("10.00")), 1,
                    OptionalLong.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                    Optional.empty(), Instant.EPOCH, Instant.EPOCH);
        }
    }
}
