package br.com.tresvtintas.mobile.core.dashboard;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertThrows;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;

public final class DashboardControllerTest {
    @Test
    public void loadsAuthorizedSnapshotAndPublishesReadyState() {
        QueueExecutor worker = new QueueExecutor();
        List<DashboardState> states = new ArrayList<>();
        DashboardController controller = new DashboardController(
                DashboardControllerTest::snapshot,
                worker,
                Runnable::run);
        controller.subscribe(states::add);

        controller.load();
        worker.runNext();

        assertEquals(
                "An initial load must publish empty, loading and ready.",
                List.of(
                        DashboardState.Phase.EMPTY,
                        DashboardState.Phase.LOADING,
                        DashboardState.Phase.READY),
                states.stream().map(DashboardState::phase).toList());
        assertEquals(
                "The ready state must preserve the authorized server snapshot.",
                "Loja Centro",
                states.get(2).snapshot().orElseThrow()
                        .context().organization().orElseThrow().name());
    }

    @Test
    public void transientRefreshKeepsLastAuthorizedSnapshot() {
        AtomicInteger calls = new AtomicInteger();
        DashboardRepository repository = () -> {
            if (calls.getAndIncrement() == 0) {
                return snapshot();
            }
            throw new DashboardException(
                    DashboardFailureKind.NETWORK,
                    "Temporary network failure.");
        };
        QueueExecutor worker = new QueueExecutor();
        List<DashboardState> states = new ArrayList<>();
        DashboardController controller = new DashboardController(
                repository,
                worker,
                Runnable::run);
        controller.subscribe(states::add);

        controller.load();
        worker.runNext();
        controller.refresh();
        worker.runNext();

        DashboardState stale = states.get(states.size() - 1);
        assertEquals(
                "A transient refresh failure must retain the ready phase.",
                DashboardState.Phase.READY,
                stale.phase());
        assertTrue(
                "The last authorized snapshot must remain visible offline.",
                stale.snapshot().isPresent());
        assertEquals(
                "The stale state must expose a non-sensitive failure kind.",
                Optional.of(DashboardFailureKind.NETWORK),
                stale.failure());
    }

    @Test
    public void closeInvalidatesQueuedResult() {
        QueueExecutor worker = new QueueExecutor();
        List<DashboardState> states = new ArrayList<>();
        DashboardController controller = new DashboardController(
                DashboardControllerTest::snapshot,
                worker,
                Runnable::run);
        controller.subscribe(states::add);

        controller.load();
        controller.close();
        worker.runNext();

        assertEquals(
                "A result queued before close must never revive the screen.",
                DashboardState.Phase.CLOSED,
                states.get(states.size() - 1).phase());
    }

    @Test
    public void rejectsInvalidAggregates() {
        assertThrows(
                "Negative counts must fail closed.",
                IllegalArgumentException.class,
                () -> new DashboardCountAmount(
                        -1,
                        new BigDecimal("10.00")));
        assertThrows(
                "Money with an unexpected scale must fail closed.",
                IllegalArgumentException.class,
                () -> new DashboardCountAmount(
                        1,
                        new BigDecimal("10.0")));
        assertThrows(
                "Negative workload counts must fail closed.",
                IllegalArgumentException.class,
                () -> new DashboardWorkload.Orders(1, -1, 0));
    }

    private static DashboardSnapshot snapshot() {
        return new DashboardSnapshot(
                Instant.parse("2026-07-28T12:00:00Z"),
                new DashboardContext(
                        DashboardVisibility.TEAM,
                        Optional.of(new DashboardContext.Organization(
                                9,
                                "Loja Centro"))),
                new DashboardWorkload(
                        Optional.of(new DashboardWorkload.Orders(8, 3, 2)),
                        Optional.empty(),
                        Optional.empty()),
                Optional.empty(),
                Optional.empty(),
                Optional.empty());
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
