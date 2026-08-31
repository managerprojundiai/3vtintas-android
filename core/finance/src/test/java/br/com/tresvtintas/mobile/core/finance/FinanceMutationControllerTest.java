package br.com.tresvtintas.mobile.core.finance;

import static org.junit.Assert.assertEquals;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.OptionalLong;
import org.junit.Test;

public final class FinanceMutationControllerTest {
    private static final String UNUSED_OPERATION = "Operation is not used.";

    @Test
    public void forwardsStableKeyAndPublishesMutationResult() {
        FakeRepository repository = new FakeRepository();
        FinanceMutationController controller = new FinanceMutationController(
                repository,
                Runnable::run,
                Runnable::run);
        FinanceDraft draft = new FinanceDraft(
                FinanceEntryType.RECEIVABLE,
                "Parcela",
                new BigDecimal("100.00"),
                Optional.empty(),
                OptionalLong.empty(),
                Optional.empty());

        controller.create(draft, "00000000-0000-4000-8000-000000000501");

        FinanceMutationState state = state(controller);
        assertEquals(
                "Successful repository call must publish success.",
                FinanceMutationState.Phase.SUCCESS,
                state.phase());
        assertEquals(
                "Create action must remain explicit.",
                FinanceAction.CREATE,
                state.action().orElseThrow());
        assertEquals(
                "Controller must forward the stable retry key.",
                "00000000-0000-4000-8000-000000000501",
                repository.receivedKey);
    }

    @Test
    public void rejectsInvalidMutationWithoutCallingRepository() {
        FakeRepository repository = new FakeRepository();
        FinanceMutationController controller = new FinanceMutationController(
                repository,
                Runnable::run,
                Runnable::run);

        controller.cancel(0, "short");

        FinanceMutationState state = state(controller);
        assertEquals(
                "Invalid request must fail before I/O.",
                FinanceMutationState.Phase.ERROR,
                state.phase());
        assertEquals(
                "Invalid request must remain typed.",
                FinanceFailureKind.INVALID_REQUEST,
                state.failure().orElseThrow());
        assertEquals(
                "Invalid request must not call the repository.",
                0,
                repository.calls);
    }

    private static FinanceMutationState state(
            FinanceMutationController controller) {
        FinanceMutationState[] value = new FinanceMutationState[1];
        controller.subscribe(state -> value[0] = state);
        return value[0];
    }

    private static final class FakeRepository implements FinanceRepository {
        private int calls;
        private String receivedKey;

        @Override
        public FinancePage page(
                FinanceQuery query,
                Optional<String> cursor) {
            throw new AssertionError(UNUSED_OPERATION);
        }

        @Override
        public FinanceDetail detail(long entryId) {
            throw new AssertionError(UNUSED_OPERATION);
        }

        @Override
        public FinanceMutationResult create(
                FinanceDraft draft,
                String idempotencyKey) {
            calls++;
            receivedKey = idempotencyKey;
            return new FinanceMutationResult(
                    FinanceAction.CREATE,
                    701,
                    FinanceEntryStatus.PENDING,
                    true,
                    false);
        }

        @Override
        public FinanceMutationResult settle(
                long entryId,
                FinancePaymentMethod paymentMethod,
                Optional<String> paymentReference,
                String idempotencyKey) {
            throw new AssertionError(UNUSED_OPERATION);
        }

        @Override
        public FinanceMutationResult cancel(
                long entryId,
                String idempotencyKey) {
            calls++;
            return new FinanceMutationResult(
                    FinanceAction.CANCEL,
                    entryId,
                    FinanceEntryStatus.CANCELLED,
                    true,
                    false);
        }
    }
}
