package br.com.tresvtintas.mobile.core.delivery;

import static org.junit.Assert.assertEquals;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.Test;

public final class DeliveryListControllerTest {
    @Test
    public void appendsOpaqueNextPageWithoutReplacingFirstPage() {
        FakeRepository repository = new FakeRepository();
        DeliveryListController controller =
                new DeliveryListController(
                        repository,
                        Runnable::run,
                        Runnable::run);

        controller.open(DeliveryQuery.initial());
        controller.loadMore();

        assertEquals(
                "Pagination must preserve the first page.",
                List.of(1L, 2L),
                state(controller).snapshot()
                        .orElseThrow()
                        .items()
                        .stream()
                        .map(DeliverySummary::id)
                        .toList());
        assertEquals(
                "The opaque cursor must reach the repository unchanged.",
                Optional.of("next"),
                repository.cursor);
    }

    private static DeliveryListState state(
            DeliveryListController controller) {
        DeliveryListState[] state = new DeliveryListState[1];
        controller.subscribe(value -> state[0] = value);
        return state[0];
    }

    private static DeliverySummary delivery(long id) {
        return new DeliverySummary(
                id,
                DeliveryStatus.PENDING,
                Set.of(),
                new DeliverySummary.Order(700 + id, "confirmed", 1, 1),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Instant.EPOCH);
    }

    private static final class FakeRepository
            implements DeliveryRepository {
        private Optional<String> cursor = Optional.empty();

        @Override
        public DeliveryPage page(
                DeliveryQuery query,
                Optional<String> requestedCursor) {
            cursor = requestedCursor;
            return requestedCursor.isEmpty()
                    ? new DeliveryPage(
                            List.of(delivery(1)),
                            Optional.of("next"))
                    : new DeliveryPage(
                            List.of(delivery(2)),
                            Optional.empty());
        }

        @Override
        public DeliveryDetail detail(long deliveryId) {
            throw new AssertionError("Not used.");
        }

        @Override
        public DeliveryMutationResult start(
                long deliveryId,
                int expectedOrderRevision,
                String idempotencyKey) {
            throw new AssertionError("Not used.");
        }

        @Override
        public DeliveryMutationResult complete(
                long deliveryId,
                int expectedOrderRevision,
                String idempotencyKey) {
            throw new AssertionError("Not used.");
        }
    }
}
