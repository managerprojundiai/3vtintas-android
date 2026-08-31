package br.com.tresvtintas.mobile.core.delivery;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.Test;

public final class DeliveryActionControllerTest {
    private static final String KEY =
            "00000000-0000-4000-8000-000000000801";

    @Test
    public void delegatesExactRevisionAndStableAttemptKey() {
        FakeRepository repository = new FakeRepository();
        DeliveryActionController controller =
                new DeliveryActionController(
                        repository,
                        Runnable::run,
                        Runnable::run);

        controller.execute(DeliveryAction.START, 801, 4, KEY);

        assertEquals(
                "The selected action must reach the repository.",
                DeliveryAction.START,
                repository.action);
        assertEquals(
                "The exact delivery revision must reach the repository.",
                4,
                repository.revision);
        assertEquals(
                "The idempotency key must remain unchanged.",
                KEY,
                repository.key);
        assertEquals(
                "A successful command must publish success.",
                DeliveryActionState.Phase.SUCCESS,
                state(controller).phase());
    }

    @Test
    public void suppressesSecondCommandWhileFirstIsRunning() {
        FakeRepository repository = new FakeRepository();
        List<Runnable> work = new ArrayList<>();
        DeliveryActionController controller =
                new DeliveryActionController(
                        repository,
                        work::add,
                        Runnable::run);

        controller.execute(DeliveryAction.START, 801, 4, KEY);
        controller.execute(DeliveryAction.COMPLETE, 801, 4, KEY);

        assertEquals(
                "Only one mutation may be queued at a time.",
                1,
                work.size());
        work.get(0).run();
        assertEquals(
                "Only the first command may reach the repository.",
                1,
                repository.calls);
    }

    private static DeliveryActionState state(
            DeliveryActionController controller) {
        DeliveryActionState[] state = new DeliveryActionState[1];
        controller.subscribe(value -> state[0] = value);
        return state[0];
    }

    private static final class FakeRepository
            implements DeliveryRepository {
        private int calls;
        private int revision;
        private String key;
        private DeliveryAction action;

        @Override
        public DeliveryPage page(
                DeliveryQuery query,
                Optional<String> cursor) {
            throw new AssertionError("Not used.");
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
            return result(
                    DeliveryAction.START,
                    deliveryId,
                    expectedOrderRevision,
                    idempotencyKey);
        }

        @Override
        public DeliveryMutationResult complete(
                long deliveryId,
                int expectedOrderRevision,
                String idempotencyKey) {
            return result(
                    DeliveryAction.COMPLETE,
                    deliveryId,
                    expectedOrderRevision,
                    idempotencyKey);
        }

        private DeliveryMutationResult result(
                DeliveryAction requestedAction,
                long deliveryId,
                int expectedOrderRevision,
                String idempotencyKey) {
            calls++;
            action = requestedAction;
            revision = expectedOrderRevision;
            key = idempotencyKey;
            return new DeliveryMutationResult(
                    requestedAction,
                    deliveryId,
                    requestedAction == DeliveryAction.START
                            ? DeliveryStatus.IN_TRANSIT
                            : DeliveryStatus.DELIVERED,
                    701,
                    requestedAction == DeliveryAction.START
                            ? "in_progress"
                            : "delivered",
                    expectedOrderRevision + 1,
                    true,
                    false);
        }
    }
}
