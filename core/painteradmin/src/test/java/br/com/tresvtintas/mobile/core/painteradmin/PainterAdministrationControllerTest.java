package br.com.tresvtintas.mobile.core.painteradmin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import br.com.tresvtintas.mobile.core.painteradmin.PainterAdministrationListState.Phase;
import br.com.tresvtintas.mobile.core.painteradmin.PainterAdministrationModels.AccessRequest;
import br.com.tresvtintas.mobile.core.painteradmin.PainterAdministrationModels.AccessRequestDetail;
import br.com.tresvtintas.mobile.core.painteradmin.PainterAdministrationModels.Manager;
import br.com.tresvtintas.mobile.core.painteradmin.PainterAdministrationModels.Mutation;
import br.com.tresvtintas.mobile.core.painteradmin.PainterAdministrationModels.Options;
import br.com.tresvtintas.mobile.core.painteradmin.PainterAdministrationModels.Organization;
import br.com.tresvtintas.mobile.core.painteradmin.PainterAdministrationModels.Page;
import br.com.tresvtintas.mobile.core.painteradmin.PainterAdministrationModels.Painter;
import br.com.tresvtintas.mobile.core.painteradmin.PainterAdministrationModels.PainterDetail;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Queue;
import java.util.concurrent.Executor;
import org.junit.Test;

public final class PainterAdministrationControllerTest {
    private static final String UNUSED_OPERATION = "Not used.";

    @Test
    public void loadsAuthorizedSnapshotAndAppendsOpaquePages() {
        FakeRepository repository = new FakeRepository();
        List<PainterAdministrationListState> states = new ArrayList<>();
        PainterAdministrationListController controller =
                new PainterAdministrationListController(
                        repository,
                        Runnable::run,
                        Runnable::run);
        controller.subscribe(states::add);

        controller.open();
        controller.loadMorePainters();
        controller.loadMoreRequests();

        PainterAdministrationListState state =
                states.get(states.size() - 1);
        assertEquals(
                "Pagination must finish in the ready state.",
                Phase.READY,
                state.phase());
        assertEquals(
                "Painter pages must be appended without replacing prior data.",
                2,
                state.snapshot().orElseThrow().painters().size());
        assertEquals(
                "Request pages must be appended without replacing prior data.",
                2,
                state.snapshot().orElseThrow().requests().size());
        assertTrue(
                "Opaque cursors must disappear after the final page.",
                state.snapshot().orElseThrow().painterCursor().isEmpty());
    }

    @Test
    public void keepsLastSnapshotWhenRefreshFailsTransiently() {
        FakeRepository repository = new FakeRepository();
        List<PainterAdministrationListState> states = new ArrayList<>();
        PainterAdministrationListController controller =
                new PainterAdministrationListController(
                        repository,
                        Runnable::run,
                        Runnable::run);
        controller.subscribe(states::add);
        controller.open();
        repository.failReads = true;

        controller.refresh();

        PainterAdministrationListState state =
                states.get(states.size() - 1);
        assertEquals(
                "Transient failures must retain visible data.",
                Phase.STALE,
                state.phase());
        assertEquals(
                "The prior painter must remain available while stale.",
                1,
                state.snapshot().orElseThrow().painters().size());
        assertEquals(
                "The failure kind must remain actionable by the UI.",
                PainterAdministrationFailureKind.NETWORK,
                state.failure().orElseThrow().kind());
    }

    @Test
    public void ignoresCompletionAfterControllerIsClosed() {
        FakeRepository repository = new FakeRepository();
        QueuedExecutor worker = new QueuedExecutor();
        List<PainterAdministrationListState> states = new ArrayList<>();
        PainterAdministrationListController controller =
                new PainterAdministrationListController(
                        repository,
                        worker,
                        Runnable::run);
        controller.subscribe(states::add);
        controller.open();

        controller.close();
        worker.runNext();

        assertEquals(
                "A closed screen must not receive a delayed result.",
                Phase.CLOSED,
                states.get(states.size() - 1).phase());
    }

    @Test
    public void taskControllerDropsConcurrentSubmission() {
        QueuedExecutor worker = new QueuedExecutor();
        List<PainterAdministrationTaskState<String>> states =
                new ArrayList<>();
        PainterAdministrationTaskController<String> controller =
                new PainterAdministrationTaskController<>(
                        worker,
                        Runnable::run);
        controller.subscribe(states::add);

        controller.submit(() -> "first");
        controller.submit(() -> "second");
        worker.runNext();

        assertEquals(
                "Only the accepted task may complete.",
                Optional.of("first"),
                states.get(states.size() - 1).result());
        assertTrue(
                "The concurrent submission must not enqueue extra work.",
                worker.isEmpty());
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

        boolean isEmpty() {
            return tasks.isEmpty();
        }
    }

    private static final class FakeRepository
            implements PainterAdministrationRepository {
        private int painterPage;
        private int requestPage;
        private boolean failReads;

        @Override
        public Options options() {
            return new Options(
                    List.of(new Organization(7, "Jundiaí")),
                    List.of(new Manager(
                            19,
                            "Gerente",
                            OptionalLong.of(7))));
        }

        @Override
        public Page<Painter> painters(
                PainterAdministrationQuery query,
                Optional<String> cursor)
                throws PainterAdministrationException {
            if (failReads) {
                throw new PainterAdministrationException(
                        PainterAdministrationFailureKind.NETWORK,
                        "Offline.");
            }
            painterPage++;
            return new Page<>(
                    List.of(painter(painterPage)),
                    painterPage == 1
                            ? Optional.of("painters_next")
                            : Optional.empty());
        }

        @Override
        public PainterDetail painter(long painterId) {
            throw new AssertionError(UNUSED_OPERATION);
        }

        @Override
        public Page<AccessRequest> accessRequests(
                Optional<String> cursor,
                int pageSize) {
            requestPage++;
            return new Page<>(
                    List.of(request(requestPage)),
                    requestPage == 1
                            ? Optional.of("requests_next")
                            : Optional.empty());
        }

        @Override
        public AccessRequestDetail accessRequest(long requestId) {
            throw new AssertionError(UNUSED_OPERATION);
        }

        @Override
        public Mutation create(PainterDraft draft, String idempotencyKey) {
            throw new AssertionError(UNUSED_OPERATION);
        }

        @Override
        public Mutation updateStatus(
                long painterId,
                int expectedRevision,
                PainterAdministrationStatus status,
                String idempotencyKey) {
            throw new AssertionError(UNUSED_OPERATION);
        }

        @Override
        public Mutation updateCommission(
                long painterId,
                int expectedRevision,
                BigDecimal commissionRate,
                String idempotencyKey) {
            throw new AssertionError(UNUSED_OPERATION);
        }

        @Override
        public Mutation updateManager(
                long painterId,
                int expectedRevision,
                OptionalLong managerUserId,
                String idempotencyKey) {
            throw new AssertionError(UNUSED_OPERATION);
        }

        @Override
        public Mutation approvePainter(
                long requestId,
                int expectedRevision,
                long organizationId,
                OptionalLong managerUserId,
                BigDecimal commissionRate,
                String idempotencyKey) {
            throw new AssertionError(UNUSED_OPERATION);
        }

        @Override
        public Mutation approveManager(
                long requestId,
                int expectedRevision,
                long organizationId,
                String idempotencyKey) {
            throw new AssertionError(UNUSED_OPERATION);
        }

        @Override
        public Mutation reject(
                long requestId,
                int expectedRevision,
                String reason,
                String idempotencyKey) {
            throw new AssertionError(UNUSED_OPERATION);
        }

        private Painter painter(int suffix) {
            return new Painter(
                    suffix,
                    100 + suffix,
                    "Pintor " + suffix,
                    Optional.of("painter@example.com"),
                    Optional.empty(),
                    Optional.of("Pintura"),
                    PainterAdministrationStatus.ACTIVE,
                    new BigDecimal("4.00"),
                    options().organizations().get(0),
                    Optional.of(options().managers().get(0)),
                    1,
                    Instant.parse("2026-07-30T12:00:00Z"));
        }

        private static AccessRequest request(int suffix) {
            return new AccessRequest(
                    suffix,
                    200 + suffix,
                    "Solicitante " + suffix,
                    Optional.of("request@example.com"),
                    Optional.empty(),
                    Optional.of("Pintura"),
                    1,
                    Instant.parse("2026-07-30T11:00:00Z"),
                    Instant.parse("2026-07-30T12:00:00Z"));
        }
    }
}
