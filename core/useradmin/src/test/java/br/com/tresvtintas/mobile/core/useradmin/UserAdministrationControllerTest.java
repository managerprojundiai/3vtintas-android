package br.com.tresvtintas.mobile.core.useradmin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import br.com.tresvtintas.mobile.core.model.AppRole;
import br.com.tresvtintas.mobile.core.useradmin.UserAdministrationListState.Phase;
import br.com.tresvtintas.mobile.core.useradmin.UserAdministrationModels.Mutation;
import br.com.tresvtintas.mobile.core.useradmin.UserAdministrationModels.Options;
import br.com.tresvtintas.mobile.core.useradmin.UserAdministrationModels.Organization;
import br.com.tresvtintas.mobile.core.useradmin.UserAdministrationModels.Page;
import br.com.tresvtintas.mobile.core.useradmin.UserAdministrationModels.User;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.Executor;
import org.junit.Test;

public final class UserAdministrationControllerTest {
    @Test
    public void loadsAuthorizedSnapshotAndAppendsOpaquePage() {
        FakeRepository repository = new FakeRepository();
        List<UserAdministrationListState> states = new ArrayList<>();
        UserAdministrationListController controller =
                new UserAdministrationListController(
                        repository,
                        Runnable::run,
                        Runnable::run);
        controller.subscribe(states::add);

        controller.open();
        controller.loadMore();

        UserAdministrationListState state = states.get(states.size() - 1);
        assertEquals(
                "Pagination must finish in the ready state.",
                Phase.READY,
                state.phase());
        assertEquals(
                "The second page must append without replacing users.",
                2,
                state.snapshot().orElseThrow().users().size());
        assertTrue(
                "The final opaque cursor must disappear.",
                state.snapshot().orElseThrow().nextCursor().isEmpty());
    }

    @Test
    public void keepsSnapshotWhenRefreshFailsTransiently() {
        FakeRepository repository = new FakeRepository();
        List<UserAdministrationListState> states = new ArrayList<>();
        UserAdministrationListController controller =
                new UserAdministrationListController(
                        repository,
                        Runnable::run,
                        Runnable::run);
        controller.subscribe(states::add);
        controller.open();
        repository.failReads = true;

        controller.refresh();

        UserAdministrationListState state = states.get(states.size() - 1);
        assertEquals(
                "A transient failure must retain the visible snapshot.",
                Phase.STALE,
                state.phase());
        assertEquals(
                "The prior user must remain visible while stale.",
                1,
                state.snapshot().orElseThrow().users().size());
        assertEquals(
                "The failure kind must remain actionable.",
                UserAdministrationFailureKind.NETWORK,
                state.failure().orElseThrow().kind());
    }

    @Test
    public void ignoresCompletionAfterControllerIsClosed() {
        QueuedExecutor worker = new QueuedExecutor();
        List<UserAdministrationListState> states = new ArrayList<>();
        UserAdministrationListController controller =
                new UserAdministrationListController(
                        new FakeRepository(),
                        worker,
                        Runnable::run);
        controller.subscribe(states::add);
        controller.open();

        controller.close();
        worker.runNext();

        assertEquals(
                "A closed screen must ignore delayed work.",
                Phase.CLOSED,
                states.get(states.size() - 1).phase());
    }

    private static final class QueuedExecutor implements Executor {
        private final Queue<Runnable> tasks = new ArrayDeque<>();

        @Override
        public void execute(Runnable command) {
            tasks.add(command);
        }

        void runNext() {
            tasks.remove().run();
        }
    }

    private static final class FakeRepository
            implements UserAdministrationRepository {
        private static final String UNUSED_OPERATION = "Not used.";
        private int page;
        private boolean failReads;

        @Override
        public Options options() {
            return new Options(
                    List.of(new Organization(7, "Jundiaí", "jundiai")),
                    List.of(AppRole.MANAGER, AppRole.PAINTER),
                    List.of(AppRole.SALESPERSON, AppRole.DELIVERY_DRIVER));
        }

        @Override
        public Page users(UserAdministrationQuery query, Optional<String> cursor)
                throws UserAdministrationException {
            if (failReads) {
                throw new UserAdministrationException(
                        UserAdministrationFailureKind.NETWORK,
                        "Offline.");
            }
            page++;
            return new Page(
                    List.of(user(page)),
                    page == 1 ? Optional.of("users_next") : Optional.empty());
        }

        @Override
        public User user(long userId) {
            throw new AssertionError(UNUSED_OPERATION);
        }

        @Override
        public Mutation assignStandardRole(
                long userId,
                int expectedRevision,
                AppRole role,
                String idempotencyKey) {
            throw new AssertionError(UNUSED_OPERATION);
        }

        @Override
        public Mutation assignOperationalRole(
                long userId,
                int expectedRevision,
                AppRole role,
                long organizationId,
                String idempotencyKey) {
            throw new AssertionError(UNUSED_OPERATION);
        }

        @Override
        public Mutation setBlocked(
                long userId,
                int expectedRevision,
                boolean blocked,
                String idempotencyKey) {
            throw new AssertionError(UNUSED_OPERATION);
        }

        private static User user(int suffix) {
            Instant now = Instant.parse("2026-07-30T12:00:00Z");
            return new User(
                    suffix,
                    "Usuário " + suffix,
                    Optional.of("user@example.com"),
                    AppRole.MANAGER,
                    false,
                    4,
                    List.of(),
                    now,
                    now);
        }
    }
}
