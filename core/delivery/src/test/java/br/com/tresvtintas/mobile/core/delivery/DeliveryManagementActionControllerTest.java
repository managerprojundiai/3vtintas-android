package br.com.tresvtintas.mobile.core.delivery;

import static org.junit.Assert.assertEquals;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import org.junit.Test;

public final class DeliveryManagementActionControllerTest {
    private static final String KEY =
            "00000000-0000-4000-8000-000000000902";
    private static final String NOT_USED = "Not used.";

    @Test
    public void delegatesExactScheduleScopeRevisionAndKey() {
        FakeRepository repository = new FakeRepository();
        DeliveryManagementActionController controller =
                new DeliveryManagementActionController(
                        repository,
                        Runnable::run,
                        Runnable::run);
        Instant schedule = Instant.parse("2026-07-27T12:00:00Z");

        controller.execute(
                new DeliveryManagementCommand.Schedule(
                        7,
                        501,
                        3,
                        schedule,
                        90),
                KEY);

        assertEquals(
                "The schedule action must be delegated.",
                DeliveryManagementAction.SCHEDULE,
                repository.action);
        assertEquals(
                "The exact organization must be delegated.",
                7,
                repository.organizationId);
        assertEquals(
                "The exact order must be delegated.",
                501,
                repository.orderId);
        assertEquals(
                "The exact revision must be delegated.",
                3,
                repository.revision);
        assertEquals(
                "The exact schedule must be delegated.",
                schedule,
                repository.scheduledAt);
        assertEquals(
                "The duration must be delegated.",
                90,
                repository.duration);
        assertEquals(
                "The idempotency key must remain unchanged.",
                KEY,
                repository.key);
        assertEquals(
                "A successful command must publish success.",
                DeliveryManagementActionState.Phase.SUCCESS,
                state(controller).phase());
    }

    @Test
    public void suppressesConcurrentManagementMutation() {
        FakeRepository repository = new FakeRepository();
        List<Runnable> work = new ArrayList<>();
        DeliveryManagementActionController controller =
                new DeliveryManagementActionController(
                        repository,
                        work::add,
                        Runnable::run);

        controller.execute(
                new DeliveryManagementCommand.Assignment(
                        7,
                        501,
                        3,
                        OptionalLong.of(21)),
                KEY);
        controller.execute(
                new DeliveryManagementCommand.Completion(7, 501, 3),
                KEY);

        assertEquals(
                "Only one management mutation may be queued.",
                1,
                work.size());
        work.get(0).run();
        assertEquals(
                "Only the first mutation may reach the repository.",
                1,
                repository.calls);
    }

    private static DeliveryManagementActionState state(
            DeliveryManagementActionController controller) {
        DeliveryManagementActionState[] state =
                new DeliveryManagementActionState[1];
        controller.subscribe(value -> state[0] = value);
        return state[0];
    }

    private static final class FakeRepository
            implements DeliveryManagementRepository {
        private int calls;
        private long organizationId;
        private long orderId;
        private int revision;
        private Instant scheduledAt;
        private int duration;
        private String key;
        private DeliveryManagementAction action;

        @Override
        public List<DeliveryManagementOrganization> organizations() {
            throw new AssertionError(NOT_USED);
        }

        @Override
        public List<DeliveryManagementDriver> drivers(long requestedId) {
            throw new AssertionError(NOT_USED);
        }

        @Override
        public DeliveryManagementPage page(
                DeliveryManagementQuery query,
                Optional<String> cursor) {
            throw new AssertionError(NOT_USED);
        }

        @Override
        public DeliveryManagementDetail detail(
                long requestedOrganizationId,
                long requestedOrderId) {
            throw new AssertionError(NOT_USED);
        }

        @Override
        public DeliveryManagementMutationResult schedule(
                long requestedOrganizationId,
                long requestedOrderId,
                int expectedOrderRevision,
                Instant requestedSchedule,
                int durationMinutes,
                String idempotencyKey) {
            scheduledAt = requestedSchedule;
            duration = durationMinutes;
            return result(
                    DeliveryManagementAction.SCHEDULE,
                    requestedOrganizationId,
                    requestedOrderId,
                    expectedOrderRevision,
                    idempotencyKey);
        }

        @Override
        public DeliveryManagementMutationResult assign(
                long requestedOrganizationId,
                long requestedOrderId,
                int expectedOrderRevision,
                OptionalLong driverUserId,
                String idempotencyKey) {
            return result(
                    driverUserId.isPresent()
                            ? DeliveryManagementAction.ASSIGN
                            : DeliveryManagementAction.UNASSIGN,
                    requestedOrganizationId,
                    requestedOrderId,
                    expectedOrderRevision,
                    idempotencyKey);
        }

        @Override
        public DeliveryManagementMutationResult complete(
                long requestedOrganizationId,
                long requestedOrderId,
                int expectedOrderRevision,
                String idempotencyKey) {
            return result(
                    DeliveryManagementAction.COMPLETE,
                    requestedOrganizationId,
                    requestedOrderId,
                    expectedOrderRevision,
                    idempotencyKey);
        }

        private DeliveryManagementMutationResult result(
                DeliveryManagementAction requestedAction,
                long requestedOrganizationId,
                long requestedOrderId,
                int expectedOrderRevision,
                String idempotencyKey) {
            calls++;
            action = requestedAction;
            organizationId = requestedOrganizationId;
            orderId = requestedOrderId;
            revision = expectedOrderRevision;
            key = idempotencyKey;
            return new DeliveryManagementMutationResult(
                    requestedAction,
                    requestedOrderId,
                    OptionalLong.of(801),
                    "confirmed",
                    expectedOrderRevision + 1,
                    Optional.ofNullable(scheduledAt),
                    OptionalLong.empty(),
                    true,
                    false);
        }
    }
}
