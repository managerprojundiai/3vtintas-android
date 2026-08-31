package br.com.tresvtintas.mobile.core.finance;

import static org.junit.Assert.assertEquals;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.Test;

public final class FinanceListControllerTest {
    private static final String UNUSED_OPERATION = "Operation is not used.";
    private static final FinanceOverview OVERVIEW = overview();

    @Test
    public void appendsOpaquePageAndKeepsAuthoritativeOverview() {
        FakeRepository repository = new FakeRepository();
        FinanceListController controller = new FinanceListController(
                repository,
                FinanceQuery.initial(),
                Runnable::run,
                Runnable::run);

        controller.open(FinanceQuery.initial());
        controller.loadMore();

        FinanceListState state = state(controller);
        assertEquals(
                "Keyset pagination must append without dropping prior rows.",
                List.of(1L, 2L),
                state.snapshot().orElseThrow().items().stream()
                        .map(FinanceSummary::id)
                        .toList());
        assertEquals(
                "Opaque cursor must be forwarded unchanged.",
                Optional.of("next"),
                repository.receivedCursor);
        assertEquals(
                "Full-filter server overview must remain authoritative.",
                OVERVIEW,
                state.snapshot().orElseThrow().overview());
    }

    @Test
    public void retainsSnapshotOnTransientRefreshFailure() {
        FakeRepository repository = new FakeRepository();
        FinanceListController controller = new FinanceListController(
                repository,
                FinanceQuery.initial(),
                Runnable::run,
                Runnable::run);
        controller.open(FinanceQuery.initial());
        repository.transientFailure = true;

        controller.refresh();

        FinanceListState state = state(controller);
        assertEquals(
                "Transient failure must keep the usable ready state.",
                FinanceListState.Phase.READY,
                state.phase());
        assertEquals(
                "Typed stale warning must reach the UI.",
                Optional.of(FinanceFailureKind.NETWORK),
                state.failure());
        assertEquals(
                "In-memory rows must remain visible.",
                1,
                state.snapshot().orElseThrow().items().size());
    }

    private static FinanceListState state(FinanceListController controller) {
        FinanceListState[] value = new FinanceListState[1];
        controller.subscribe(state -> value[0] = state);
        return value[0];
    }

    private static FinanceSummary entry(long id) {
        return new FinanceSummary(
                id,
                FinanceEntryType.EXPENSE,
                FinanceEntryStatus.PENDING,
                FinanceEntrySource.MANUAL,
                "Combustível",
                new BigDecimal("50.00"),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Set.of(FinanceAction.SETTLE, FinanceAction.CANCEL),
                Instant.parse("2026-07-26T12:00:00Z"),
                Instant.parse("2026-07-26T12:00:00Z"));
    }

    private static FinanceOverview overview() {
        FinanceOverview.MoneyTotal zero =
                new FinanceOverview.MoneyTotal(0, new BigDecimal("0.00"));
        FinanceOverview.TypeTotals empty =
                new FinanceOverview.TypeTotals(zero, zero, zero);
        return new FinanceOverview(
                new FinanceOverview.TypeTotals(
                        new FinanceOverview.MoneyTotal(
                                1,
                                new BigDecimal("50.00")),
                        zero,
                        zero),
                empty,
                empty,
                empty);
    }

    private static final class FakeRepository implements FinanceRepository {
        private Optional<String> receivedCursor = Optional.empty();
        private boolean transientFailure;

        @Override
        public FinancePage page(
                FinanceQuery query,
                Optional<String> cursor) throws FinanceException {
            receivedCursor = cursor;
            if (transientFailure) {
                throw new FinanceException(
                        FinanceFailureKind.NETWORK,
                        "Network unavailable.");
            }
            return cursor.isEmpty()
                    ? new FinancePage(
                            List.of(entry(1)),
                            OVERVIEW,
                            Optional.of("next"))
                    : new FinancePage(
                            List.of(entry(2)),
                            OVERVIEW,
                            Optional.empty());
        }

        @Override
        public FinanceDetail detail(long entryId) {
            throw new AssertionError(UNUSED_OPERATION);
        }

        @Override
        public FinanceMutationResult create(
                FinanceDraft draft,
                String idempotencyKey) {
            throw new AssertionError(UNUSED_OPERATION);
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
            throw new AssertionError(UNUSED_OPERATION);
        }
    }
}
