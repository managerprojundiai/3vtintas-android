package br.com.tresvtintas.mobile.core.accountaccess;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;
import org.junit.Test;

public final class AccountAccessControllerTest {
    private static final String DEVICE_ID =
            "10000000-0000-4000-8000-000000000001";
    private static final String OTHER_DEVICE_ID =
            "10000000-0000-4000-8000-000000000002";

    @Test
    public void opensAndAppendsOpaquePages() {
        FakeRepository repository = new FakeRepository();
        repository.pageOutcomes.add(page(
                List.of(AccountAccessModelsTest.device(
                        DEVICE_ID,
                        true)),
                Optional.of("opaque-next")));
        repository.pageOutcomes.add(page(
                List.of(AccountAccessModelsTest.device(
                        OTHER_DEVICE_ID,
                        false)),
                Optional.empty()));
        List<AccountAccessState> states = new ArrayList<>();
        AccountAccessController controller = controller(
                repository,
                Runnable::run,
                states,
                ignored -> {
                });

        controller.open(AccountAccessView.DEVICES);
        controller.loadMore();

        AccountAccessState ready = latest(states);
        assertEquals(
                "Both pages must be retained in display order.",
                2,
                ready.snapshot().orElseThrow().items().size());
        assertFalse(
                "The last page must remove the pagination affordance.",
                ready.snapshot().orElseThrow().hasMore());
        assertEquals(
                "Pagination must preserve the opaque server cursor.",
                List.of(Optional.empty(), Optional.of("opaque-next")),
                repository.cursors);
    }

    @Test
    public void otherAccessRevocationRefreshesBeforeNotifying() {
        FakeRepository repository = new FakeRepository();
        repository.pageOutcomes.add(page(
                List.of(AccountAccessModelsTest.device(
                        OTHER_DEVICE_ID,
                        false)),
                Optional.empty()));
        repository.pageOutcomes.add(page(
                List.of(),
                Optional.empty()));
        repository.revocation = new AccountRevocation(
                AccountAccessView.DEVICES,
                OTHER_DEVICE_ID,
                true,
                false);
        List<AccountRevocation> revocations = new ArrayList<>();
        List<AccountAccessState> states = new ArrayList<>();
        AccountAccessController controller = controller(
                repository,
                Runnable::run,
                states,
                revocations::add);

        controller.open(AccountAccessView.DEVICES);
        controller.revoke(OTHER_DEVICE_ID);

        assertEquals(
                "The visible list must refresh after a remote revocation.",
                2,
                repository.pageCalls);
        assertTrue(
                "The revoked access must disappear before completion.",
                latest(states)
                        .snapshot()
                        .orElseThrow()
                        .items()
                        .isEmpty());
        assertEquals(
                "Exactly one audited revocation event must be delivered.",
                List.of(repository.revocation),
                revocations);
    }

    @Test
    public void currentRevocationNeverReusesTheRejectedCredential() {
        FakeRepository repository = new FakeRepository();
        repository.pageOutcomes.add(page(
                List.of(AccountAccessModelsTest.device(
                        DEVICE_ID,
                        true)),
                Optional.empty()));
        repository.revocation = new AccountRevocation(
                AccountAccessView.DEVICES,
                DEVICE_ID,
                true,
                true);
        List<AccountRevocation> revocations = new ArrayList<>();
        AccountAccessController controller = controller(
                repository,
                Runnable::run,
                new ArrayList<>(),
                revocations::add);

        controller.open(AccountAccessView.DEVICES);
        controller.revoke(DEVICE_ID);

        assertEquals(
                "A revoked current credential must not make a follow-up request.",
                1,
                repository.pageCalls);
        assertEquals(
                "The application must receive the current-access event.",
                List.of(repository.revocation),
                revocations);
    }

    @Test
    public void viewSwitchCannotSuppressCurrentRevocationCompletion() {
        FakeRepository repository = new FakeRepository();
        repository.pageOutcomes.add(page(
                List.of(AccountAccessModelsTest.device(
                        DEVICE_ID,
                        true)),
                Optional.empty()));
        repository.revocation = new AccountRevocation(
                AccountAccessView.DEVICES,
                DEVICE_ID,
                true,
                true);
        QueuedExecutor worker = new QueuedExecutor();
        List<AccountRevocation> revocations = new ArrayList<>();
        AccountAccessController controller = controller(
                repository,
                worker,
                new ArrayList<>(),
                revocations::add);
        controller.open(AccountAccessView.DEVICES);
        worker.runNext();

        controller.revoke(DEVICE_ID);
        controller.open(AccountAccessView.SESSIONS);
        worker.runNext();

        assertEquals(
                "A view change must be ignored while revocation is committed.",
                AccountAccessView.DEVICES,
                controller.currentView());
        assertEquals(
                "Current revocation must still reach local logout.",
                List.of(repository.revocation),
                revocations);
    }

    @Test
    public void completedRevocationSurvivesRefreshFailure() {
        FakeRepository repository = new FakeRepository();
        repository.pageOutcomes.add(page(
                List.of(AccountAccessModelsTest.device(
                        OTHER_DEVICE_ID,
                        false)),
                Optional.empty()));
        repository.pageOutcomes.add(new AccountAccessException(
                AccountAccessFailureKind.NETWORK,
                "refresh failed"));
        repository.revocation = new AccountRevocation(
                AccountAccessView.DEVICES,
                OTHER_DEVICE_ID,
                true,
                false);
        List<AccountRevocation> revocations = new ArrayList<>();
        List<AccountAccessState> states = new ArrayList<>();
        AccountAccessController controller = controller(
                repository,
                Runnable::run,
                states,
                revocations::add);

        controller.open(AccountAccessView.DEVICES);
        controller.revoke(OTHER_DEVICE_ID);

        AccountAccessState ready = latest(states);
        assertTrue(
                "A server-confirmed revocation must disappear from stale data.",
                ready.snapshot().orElseThrow().items().isEmpty());
        assertEquals(
                "Refresh failure must remain visible as a recoverable warning.",
                Optional.of(AccountAccessFailureKind.NETWORK),
                ready.failure());
        assertEquals(
                "Completion must be emitted even when refresh fails.",
                List.of(repository.revocation),
                revocations);
    }

    @Test
    public void recoverableFailureKeepsLastVerifiedSnapshot() {
        FakeRepository repository = new FakeRepository();
        repository.pageOutcomes.add(page(
                List.of(AccountAccessModelsTest.device(
                        DEVICE_ID,
                        true)),
                Optional.of("next")));
        repository.pageOutcomes.add(new AccountAccessException(
                AccountAccessFailureKind.NETWORK,
                "offline"));
        List<AccountAccessState> states = new ArrayList<>();
        AccountAccessController controller = controller(
                repository,
                Runnable::run,
                states,
                ignored -> {
                });

        controller.open(AccountAccessView.DEVICES);
        controller.loadMore();

        AccountAccessState ready = latest(states);
        assertEquals(
                "Recoverable pagination failure must retain verified data.",
                AccountAccessState.Phase.READY,
                ready.phase());
        assertEquals(
                "The warning must classify the recoverable failure.",
                Optional.of(AccountAccessFailureKind.NETWORK),
                ready.failure());
        assertEquals(
                "The prior item must remain visible.",
                1,
                ready.snapshot().orElseThrow().items().size());
    }

    @Test
    public void closeInvalidatesQueuedWork() {
        FakeRepository repository = new FakeRepository();
        repository.pageOutcomes.add(page(
                List.of(AccountAccessModelsTest.device(
                        DEVICE_ID,
                        true)),
                Optional.empty()));
        QueuedExecutor worker = new QueuedExecutor();
        List<AccountAccessState> states = new ArrayList<>();
        AccountAccessController controller = controller(
                repository,
                worker,
                states,
                ignored -> {
                });

        controller.open(AccountAccessView.DEVICES);
        controller.close();
        worker.runNext();

        assertEquals(
                "Late work must not republish account data after close.",
                AccountAccessState.Phase.CLOSED,
                latest(states).phase());
    }

    @Test
    public void readOnlyControllerNeverInvokesRevocation() {
        FakeRepository repository = new FakeRepository();
        repository.pageOutcomes.add(page(
                List.of(AccountAccessModelsTest.device(
                        OTHER_DEVICE_ID,
                        false)),
                Optional.empty()));
        AccountAccessController controller =
                AccountAccessController.readOnly(
                        repository,
                        Runnable::run,
                        Runnable::run);

        controller.open(AccountAccessView.DEVICES);
        controller.revoke(OTHER_DEVICE_ID);

        assertFalse(
                "Managed access browsing must not expose revocation.",
                controller.canRevoke());
        assertEquals(
                "A read-only attempt must leave the verified item intact.",
                1,
                repository.pageCalls);
    }

    private static AccountAccessController controller(
            FakeRepository repository,
            Executor worker,
            List<AccountAccessState> states,
            AccountRevocationListener revocationListener) {
        AccountAccessController controller =
                new AccountAccessController(
                        repository,
                        worker,
                        Runnable::run,
                        revocationListener);
        controller.subscribe(states::add);
        return controller;
    }

    private static AccountAccessState latest(
            List<AccountAccessState> states) {
        return states.get(states.size() - 1);
    }

    private static AccountAccessPage page(
            List<AccountAccessEntry> entries,
            Optional<String> cursor) {
        return new AccountAccessPage(entries, cursor);
    }

    private static final class FakeRepository
            implements AccountAccessRepository {
        private final Deque<Object> pageOutcomes =
                new ArrayDeque<>();
        private final List<Optional<String>> cursors =
                new ArrayList<>();
        private AccountRevocation revocation;
        private int pageCalls;

        @Override
        public AccountAccessPage page(
                AccountAccessView view,
                Optional<String> cursor,
                int limit) throws AccountAccessException {
            pageCalls++;
            cursors.add(cursor);
            Object outcome = pageOutcomes.removeFirst();
            if (outcome instanceof AccountAccessException failure) {
                throw failure;
            }
            return (AccountAccessPage) outcome;
        }

        @Override
        public AccountRevocation revoke(
                AccountAccessView view,
                String targetId) {
            return revocation;
        }
    }

    private static final class QueuedExecutor implements Executor {
        private final Deque<Runnable> work = new ArrayDeque<>();

        @Override
        public void execute(Runnable command) {
            work.addLast(command);
        }

        void runNext() {
            work.removeFirst().run();
        }
    }
}
