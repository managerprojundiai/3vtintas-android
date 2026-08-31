package br.com.tresvtintas.mobile.core.delivery;

import static org.junit.Assert.assertEquals;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import org.junit.Test;

public final class DeliveryManagementListControllerTest {
    private static final String NOT_USED = "Not used.";

    @Test
    public void loadsDirectoryAndAppendsOpaqueNextPage() {
        FakeRepository repository = new FakeRepository();
        DeliveryManagementListController controller =
                new DeliveryManagementListController(
                        repository,
                        Runnable::run,
                        Runnable::run);

        controller.open();
        controller.loadMore();

        DeliveryManagementListState.Snapshot snapshot =
                state(controller).snapshot().orElseThrow();
        assertEquals(
                "The first authorized organization must be selected.",
                7,
                snapshot.selectedOrganization().id());
        assertEquals(
                "Pagination must preserve the first page.",
                List.of(501L, 502L),
                snapshot.items().stream()
                        .map(item -> item.order().id())
                        .toList());
        assertEquals(
                "The opaque cursor must reach the repository unchanged.",
                Optional.of("opaque_next"),
                repository.cursor);
    }

    @Test
    public void selectingOrganizationBuildsAnExactScopedQuery() {
        FakeRepository repository = new FakeRepository();
        DeliveryManagementListController controller =
                new DeliveryManagementListController(
                        repository,
                        Runnable::run,
                        Runnable::run);

        controller.open();
        controller.selectOrganization(8);

        assertEquals(
                "The selected organization must be the only query scope.",
                8,
                repository.query.organizationId());
        assertEquals(
                "The selected organization must be reflected in state.",
                8,
                state(controller).snapshot()
                        .orElseThrow()
                        .selectedOrganization()
                        .id());
    }

    private static DeliveryManagementListState state(
            DeliveryManagementListController controller) {
        DeliveryManagementListState[] state =
                new DeliveryManagementListState[1];
        controller.subscribe(value -> state[0] = value);
        return state[0];
    }

    private static DeliveryManagementSummary summary(
            long orderId,
            long organizationId) {
        return new DeliveryManagementSummary(
                new DeliveryManagementSummary.Order(
                        orderId,
                        "confirmed",
                        3,
                        2,
                        Instant.EPOCH,
                        Instant.EPOCH),
                new DeliveryManagementOrganization(
                        organizationId,
                        "Loja " + organizationId),
                Optional.empty(),
                Optional.empty(),
                Set.of(DeliveryManagementAction.SCHEDULE));
    }

    private static final class FakeRepository
            implements DeliveryManagementRepository {
        private Optional<String> cursor = Optional.empty();
        private DeliveryManagementQuery query =
                DeliveryManagementQuery.initial(7);

        @Override
        public List<DeliveryManagementOrganization> organizations() {
            return List.of(
                    new DeliveryManagementOrganization(7, "Centro"),
                    new DeliveryManagementOrganization(8, "Norte"));
        }

        @Override
        public List<DeliveryManagementDriver> drivers(long organizationId) {
            throw new AssertionError(NOT_USED);
        }

        @Override
        public DeliveryManagementPage page(
                DeliveryManagementQuery requestedQuery,
                Optional<String> requestedCursor) {
            query = requestedQuery;
            cursor = requestedCursor;
            if (requestedCursor.isPresent()) {
                return new DeliveryManagementPage(
                        List.of(summary(502, requestedQuery.organizationId())),
                        Optional.empty());
            }
            return new DeliveryManagementPage(
                    List.of(summary(501, requestedQuery.organizationId())),
                    Optional.of("opaque_next"));
        }

        @Override
        public DeliveryManagementDetail detail(
                long organizationId,
                long orderId) {
            throw new AssertionError(NOT_USED);
        }

        @Override
        public DeliveryManagementMutationResult schedule(
                long organizationId,
                long orderId,
                int expectedOrderRevision,
                Instant scheduledAt,
                int durationMinutes,
                String idempotencyKey) {
            throw new AssertionError(NOT_USED);
        }

        @Override
        public DeliveryManagementMutationResult assign(
                long organizationId,
                long orderId,
                int expectedOrderRevision,
                OptionalLong driverUserId,
                String idempotencyKey) {
            throw new AssertionError(NOT_USED);
        }

        @Override
        public DeliveryManagementMutationResult complete(
                long organizationId,
                long orderId,
                int expectedOrderRevision,
                String idempotencyKey) {
            throw new AssertionError(NOT_USED);
        }
    }
}
