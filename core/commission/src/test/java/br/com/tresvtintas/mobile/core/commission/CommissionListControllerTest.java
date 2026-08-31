package br.com.tresvtintas.mobile.core.commission;

import static org.junit.Assert.assertEquals;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.Executor;
import org.junit.Test;

public final class CommissionListControllerTest {
    private static final CommissionOverview OVERVIEW = new CommissionOverview(
            new CommissionOverview.Totals(1, new BigDecimal("10.00")),
            new CommissionOverview.Totals(1, new BigDecimal("20.00")),
            new CommissionOverview.Totals(0, new BigDecimal("0.00")),
            new CommissionOverview.Totals(0, new BigDecimal("0.00")));

    @Test
    public void appendsOpaquePageAndKeepsFullServerOverview() {
        FakeRepository repository = new FakeRepository();
        CommissionListController controller = new CommissionListController(
                repository,
                CommissionQuery.initial(CommissionScope.SELF),
                Runnable::run,
                Runnable::run);

        controller.open(CommissionQuery.initial(CommissionScope.SELF));
        controller.loadMore();

        CommissionListState state = state(controller);
        assertEquals(
                "Pagination must preserve the first page.",
                List.of(1L, 2L),
                state.snapshot().orElseThrow().items().stream()
                        .map(CommissionSummary::id)
                        .toList());
        assertEquals(
                "The opaque cursor must be forwarded without interpretation.",
                Optional.of("next"),
                repository.receivedCursor);
        assertEquals(
                "The server-calculated overview must remain authoritative.",
                OVERVIEW,
                state.snapshot().orElseThrow().overview());
    }

    @Test
    public void keepsLastSnapshotWhenRefreshFailsTransiently() {
        FakeRepository repository = new FakeRepository();
        CommissionListController controller = new CommissionListController(
                repository,
                CommissionQuery.initial(CommissionScope.SELF),
                Runnable::run,
                Runnable::run);
        controller.open(CommissionQuery.initial(CommissionScope.SELF));
        repository.transientFailure = true;

        controller.refresh();

        CommissionListState state = state(controller);
        assertEquals(
                "A transient refresh failure must retain useful data.",
                CommissionListState.Phase.READY,
                state.phase());
        assertEquals(
                "The stale snapshot must remain available.",
                List.of(1L),
                state.snapshot().orElseThrow().items().stream()
                        .map(CommissionSummary::id)
                        .toList());
        assertEquals(
                "The UI must receive a typed stale-data warning.",
                Optional.of(CommissionFailureKind.NETWORK),
                state.failure());
    }

    @Test
    public void ignoresQueuedResultAfterControllerIsClosed() {
        QueueExecutor worker = new QueueExecutor();
        CommissionListController controller = new CommissionListController(
                new FakeRepository(),
                CommissionQuery.initial(CommissionScope.SELF),
                worker,
                Runnable::run);
        controller.open(CommissionQuery.initial(CommissionScope.SELF));

        controller.close();
        worker.runNext();

        assertEquals(
                "Closing the account scope must invalidate queued work.",
                CommissionListState.Phase.CLOSED,
                state(controller).phase());
    }

    private static CommissionListState state(CommissionListController controller) {
        CommissionListState[] value = new CommissionListState[1];
        controller.subscribe(state -> value[0] = state);
        return value[0];
    }

    private static CommissionSummary commission(long id) {
        return new CommissionSummary(
                id,
                CommissionKind.SELLER,
                CommissionStatus.PENDING,
                new CommissionSummary.Recipient(
                        OptionalLong.of(21),
                        CommissionRecipientRole.SALESPERSON,
                        Optional.of("Ana")),
                Optional.of(new CommissionSummary.Organization(9, "Loja Centro")),
                Optional.of(new CommissionSummary.Order(
                        701,
                        "material",
                        "pending")),
                new CommissionSummary.Calculation(
                        "BRL",
                        new BigDecimal("100.00"),
                        new BigDecimal("10.00"),
                        new BigDecimal("10.00"),
                        "seller-v1"),
                1,
                false,
                Set.of(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Instant.parse("2026-07-25T12:00:00Z"),
                Instant.parse("2026-07-25T12:00:00Z"));
    }

    private static final class FakeRepository implements CommissionRepository {
        private Optional<String> receivedCursor = Optional.empty();
        private boolean transientFailure;

        @Override
        public CommissionPage page(
                CommissionQuery query,
                Optional<String> cursor) throws CommissionException {
            receivedCursor = cursor;
            if (transientFailure) {
                throw new CommissionException(
                        CommissionFailureKind.NETWORK,
                        "Network unavailable.");
            }
            return cursor.isEmpty()
                    ? new CommissionPage(
                            List.of(commission(1)),
                            OVERVIEW,
                            Optional.of("next"))
                    : new CommissionPage(
                            List.of(commission(2)),
                            OVERVIEW,
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
                CommissionMutationCommand command,
                String idempotencyKey) {
            throw new AssertionError("Not used.");
        }
    }

    private static final class QueueExecutor implements Executor {
        private final Queue<Runnable> pending = new ArrayDeque<>();

        @Override
        public void execute(Runnable command) {
            pending.add(command);
        }

        void runNext() {
            pending.remove().run();
        }
    }
}
