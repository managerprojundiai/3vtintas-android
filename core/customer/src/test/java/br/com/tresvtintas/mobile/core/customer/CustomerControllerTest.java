package br.com.tresvtintas.mobile.core.customer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Queue;
import java.util.concurrent.Executor;
import org.junit.Test;

public final class CustomerControllerTest {
    private static final Executor DIRECT = Runnable::run;

    @Test
    public void loadsAndMergesKeysetPagesWithoutDuplicateCustomers()
            throws Exception {
        FakeRepository repository = new FakeRepository();
        repository.pages.add(page(
                List.of(summary(1), summary(2)),
                Optional.of("cursor_2")));
        repository.pages.add(page(
                List.of(summary(2), summary(3)),
                Optional.empty()));
        CustomerListController controller = new CustomerListController(
                repository,
                DIRECT,
                DIRECT);

        controller.open(CustomerQuery.initial());
        controller.loadMore();

        CustomerSnapshot snapshot = controller.currentState()
                .snapshot()
                .orElseThrow();
        assertEquals(
                "Duplicate keyset rows must be replaced, not repeated.",
                List.of(1L, 2L, 3L),
                snapshot.items().stream().map(CustomerSummary::id).toList());
        assertFalse("Final page must close pagination.", snapshot.hasMore());
        assertEquals(
                "Second page must use the opaque server cursor.",
                Optional.of("cursor_2"),
                repository.cursors.get(1));
    }

    @Test
    public void retainsVisibleListOnlyForTransientRefreshFailure()
            throws Exception {
        FakeRepository repository = new FakeRepository();
        repository.pages.add(page(List.of(summary(1)), Optional.empty()));
        CustomerListController controller = new CustomerListController(
                repository,
                DIRECT,
                DIRECT);
        controller.open(CustomerQuery.initial());

        repository.failure = new CustomerException(
                CustomerFailureKind.NETWORK,
                "offline",
                "00000000-0000-4000-8000-000000000099",
                null);
        controller.refresh();

        assertEquals(
                "Transient refresh keeps the current in-memory list.",
                CustomerListState.Phase.READY,
                controller.currentState().phase());
        assertEquals(
                "Warning remains typed.",
                CustomerFailureKind.NETWORK,
                controller.currentState().failure().orElseThrow());

        repository.failure = new CustomerException(
                CustomerFailureKind.FORBIDDEN,
                "revoked");
        controller.refresh();
        assertEquals(
                "Authorization failures must remove visible customer PII.",
                CustomerListState.Phase.ERROR,
                controller.currentState().phase());
        assertTrue(
                "No snapshot survives revocation.",
                controller.currentState().snapshot().isEmpty());
    }

    @Test
    public void closeDiscardsAQueuedCustomerResponse() {
        FakeRepository repository = new FakeRepository();
        repository.pages.add(page(List.of(summary(1)), Optional.empty()));
        QueuedExecutor worker = new QueuedExecutor();
        CustomerListController controller = new CustomerListController(
                repository,
                worker,
                DIRECT);

        controller.open(CustomerQuery.initial());
        controller.close();
        worker.runNext();

        assertEquals(
                "Closed screen must not publish customer data.",
                CustomerListState.Phase.CLOSED,
                controller.currentState().phase());
    }

    @Test
    public void saveControllerSerializesOneLogicalMutation() {
        FakeRepository repository = new FakeRepository();
        QueuedExecutor worker = new QueuedExecutor();
        CustomerSaveController controller = new CustomerSaveController(
                repository,
                worker,
                DIRECT);
        CustomerDraft draft = CustomerDraft.fromRaw(
                "Cliente",
                "",
                "",
                "",
                "",
                "",
                "",
                "");

        controller.create(
                OptionalLong.of(20),
                draft,
                "00000000-0000-4000-8000-000000000071");
        controller.create(
                OptionalLong.of(20),
                draft,
                "00000000-0000-4000-8000-000000000072");
        worker.runNext();

        assertEquals(
                "Only one mutation may run at a time.",
                1,
                repository.createCount);
        assertEquals(
                "Successful result reaches the UI.",
                CustomerSaveState.Phase.SUCCESS,
                controller.currentState().phase());
    }

    private static CustomerPage page(
            List<CustomerSummary> items,
            Optional<String> cursor) {
        return new CustomerPage(items, cursor);
    }

    private static CustomerSummary summary(long id) {
        return new CustomerSummary(
                id,
                OptionalLong.of(20),
                "Cliente " + id,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                OptionalLong.of(30),
                Instant.parse("2026-07-25T10:00:00Z"),
                Instant.parse("2026-07-25T11:00:00Z"));
    }

    private static final class QueuedExecutor implements Executor {
        private final Queue<Runnable> queue = new ArrayDeque<>();

        @Override
        public void execute(Runnable command) {
            queue.add(command);
        }

        void runNext() {
            queue.remove().run();
        }
    }

    private static final class FakeRepository implements CustomerRepository {
        private final Queue<CustomerPage> pages = new ArrayDeque<>();
        private final List<Optional<String>> cursors = new ArrayList<>();
        private CustomerException failure;
        private int createCount;

        @Override
        public CustomerPage page(
                CustomerQuery query,
                Optional<String> cursor) throws CustomerException {
            cursors.add(cursor);
            if (failure != null) {
                throw failure;
            }
            return pages.remove();
        }

        @Override
        public CustomerDetail detail(long customerId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public CustomerMutationResult create(
                OptionalLong organizationId,
                CustomerDraft customer,
                String idempotencyKey) {
            createCount += 1;
            return new CustomerMutationResult(91, true, false);
        }

        @Override
        public CustomerMutationResult update(
                long customerId,
                CustomerDraft customer,
                String idempotencyKey) {
            throw new UnsupportedOperationException();
        }
    }
}
