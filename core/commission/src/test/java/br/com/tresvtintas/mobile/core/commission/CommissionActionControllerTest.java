package br.com.tresvtintas.mobile.core.commission;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.Test;

public final class CommissionActionControllerTest {
    private static final String KEY = "commission-action-key-0001";

    @Test
    public void sendsTheExactCommandAndIdempotencyKey() {
        FakeRepository repository = new FakeRepository();
        CommissionActionController controller = new CommissionActionController(
                repository,
                Runnable::run,
                Runnable::run);
        CommissionMutationCommand command = CommissionMutationCommand.pay(
                701,
                2,
                CommissionPaymentMethod.PIX,
                "PIX-123");

        controller.execute(command, KEY);

        CommissionActionState state = state(controller);
        assertEquals(
                "A valid mutation must finish successfully.",
                CommissionActionState.Phase.SUCCESS,
                state.phase());
        assertEquals(
                "The repository must receive the immutable command.",
                command,
                repository.command);
        assertEquals(
                "A logical retry key must reach the repository unchanged.",
                KEY,
                repository.key);
    }

    @Test
    public void rejectsInvalidKeysBeforeTheRepository() {
        FakeRepository repository = new FakeRepository();
        CommissionActionController controller = new CommissionActionController(
                repository,
                Runnable::run,
                Runnable::run);

        controller.execute(CommissionMutationCommand.approve(701, 1), "short");

        assertEquals(
                "Invalid idempotency evidence must fail locally.",
                CommissionActionState.Phase.ERROR,
                state(controller).phase());
        assertEquals(
                "Invalid idempotency evidence must not reach the repository.",
                0,
                repository.calls);
    }

    @Test
    public void fingerprintsNeverContainSensitiveFormValues() {
        CommissionMutationCommand cancellation =
                CommissionMutationCommand.cancel(
                        701,
                        1,
                        "Motivo confidencial da comissão");
        CommissionMutationCommand payment = CommissionMutationCommand.pay(
                701,
                2,
                CommissionPaymentMethod.PIX,
                "referencia-confidencial");

        assertEquals(
                "Fingerprints must use a fixed SHA-256 representation.",
                64,
                cancellation.fingerprint().length());
        assertEquals(
                "Fingerprints must use a fixed SHA-256 representation.",
                64,
                payment.fingerprint().length());
        assertFalse(
                "Cancellation details must never enter saved UI state.",
                cancellation.fingerprint().contains("confidencial"));
        assertFalse(
                "Payment references must never enter saved UI state.",
                payment.fingerprint().contains("confidencial"));
    }

    private static CommissionActionState state(
            CommissionActionController controller) {
        CommissionActionState[] value = new CommissionActionState[1];
        controller.subscribe(state -> value[0] = state);
        return value[0];
    }

    private static final class FakeRepository implements CommissionRepository {
        private int calls;
        private CommissionMutationCommand command;
        private String key;

        @Override
        public CommissionPage page(
                CommissionQuery query,
                Optional<String> cursor) {
            return new CommissionPage(
                    List.of(),
                    new CommissionOverview(
                            new CommissionOverview.Totals(0, BigDecimal.ZERO),
                            new CommissionOverview.Totals(0, BigDecimal.ZERO),
                            new CommissionOverview.Totals(0, BigDecimal.ZERO),
                            new CommissionOverview.Totals(0, BigDecimal.ZERO)),
                    Optional.empty());
        }

        @Override
        public CommissionDetail detail(
                long commissionId,
                CommissionScope scope) {
            throw new AssertionError("Not used.");
        }

        @Override
        public CommissionMutationResult transition(
                CommissionMutationCommand received,
                String idempotencyKey) {
            calls++;
            command = received;
            key = idempotencyKey;
            return new CommissionMutationResult(
                    received.action(),
                    received.commissionId(),
                    CommissionStatus.PAID,
                    received.expectedRevision() + 1,
                    true,
                    false);
        }
    }
}
