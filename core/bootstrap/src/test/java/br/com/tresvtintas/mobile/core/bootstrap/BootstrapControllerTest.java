package br.com.tresvtintas.mobile.core.bootstrap;

import static org.junit.Assert.assertEquals;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;

public final class BootstrapControllerTest {
    @Test
    public void loadsOncePerSessionAndAllowsExplicitRefresh() {
        AtomicInteger calls = new AtomicInteger();
        BootstrapRemote remote = () -> {
            calls.incrementAndGet();
            return BootstrapTestFixtures.response();
        };
        BootstrapController controller = controller(remote, Runnable::run);

        controller.load(BootstrapTestFixtures.expected());
        controller.load(BootstrapTestFixtures.expected());
        assertEquals(
                "First load must reach ready.",
                BootstrapState.Phase.READY,
                controller.currentState().phase());
        assertEquals("Same session must only load once.", 1, calls.get());

        controller.refresh(BootstrapTestFixtures.expected());
        assertEquals("Explicit refresh must load again.", 2, calls.get());
    }

    @Test
    public void replaysReadyStateWhenSameSessionRequestsCachedBootstrap() {
        AtomicInteger calls = new AtomicInteger();
        BootstrapController controller = controller(() -> {
            calls.incrementAndGet();
            return BootstrapTestFixtures.response();
        }, Runnable::run);
        List<BootstrapState.Phase> observed = new ArrayList<>();
        controller.subscribe(state -> observed.add(state.phase()));

        controller.load(BootstrapTestFixtures.expected());
        int observationsAfterFirstLoad = observed.size();
        controller.load(BootstrapTestFixtures.expected());

        assertEquals(
                "Cached bootstrap must not issue another request.",
                1,
                calls.get());
        assertEquals(
                "Cached READY state must be replayed for a resumed screen.",
                observationsAfterFirstLoad + 1,
                observed.size());
        assertEquals(
                "Replayed state must remain READY.",
                BootstrapState.Phase.READY,
                observed.get(observed.size() - 1));
    }

    @Test
    public void clearInvalidatesQueuedResultFromPreviousAccount() {
        QueuedExecutor worker = new QueuedExecutor();
        BootstrapController controller = controller(
                BootstrapTestFixtures::response,
                worker);

        controller.load(BootstrapTestFixtures.expected());
        assertEquals(
                "Queued load must expose loading.",
                BootstrapState.Phase.LOADING,
                controller.currentState().phase());
        controller.clear();
        worker.runNext();

        assertEquals(
                "Cleared controller must ignore stale result.",
                BootstrapState.Phase.EMPTY,
                controller.currentState().phase());
    }

    @Test
    public void publishesTypedFailureWithoutInventingAuthorization() {
        BootstrapController controller = controller(
                () -> {
                    throw new BootstrapException(
                            BootstrapFailureKind.SERVICE_UNAVAILABLE,
                            "offline");
                },
                Runnable::run);

        controller.load(BootstrapTestFixtures.expected());

        assertEquals(
                "Typed failure must expose error.",
                BootstrapState.Phase.ERROR,
                controller.currentState().phase());
        assertEquals(
                "Failure kind must be preserved.",
                BootstrapFailureKind.SERVICE_UNAVAILABLE,
                controller.currentState().failure().orElseThrow());
    }

    private static BootstrapController controller(
            BootstrapRemote remote,
            Executor worker) {
        return new BootstrapController(
                new BootstrapRepository(remote, BootstrapTestFixtures.compatibility()),
                worker,
                Runnable::run);
    }

    private static final class QueuedExecutor implements Executor {
        private final Queue<Runnable> queued = new ArrayDeque<>();

        @Override
        public void execute(Runnable command) {
            queued.add(command);
        }

        void runNext() {
            queued.remove().run();
        }
    }
}
