package br.com.tresvtintas.mobile.core.catalog;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.Executor;
import org.junit.Test;

public final class CatalogControllerTest {
    @Test
    public void publishesWarmCacheThenReconcilesWithNetwork() {
        FakeRepository repository = new FakeRepository();
        repository.cached = Optional.of(CatalogTestFixtures.snapshot(
                List.of(CatalogTestFixtures.product(1, "Cache")),
                true,
                true));
        repository.refreshed = networkSnapshot(
                List.of(CatalogTestFixtures.product(1, "Servidor")),
                false);
        CatalogController controller = controller(repository, Runnable::run);
        List<CatalogState> states = new ArrayList<>();
        controller.subscribe(states::add);

        controller.open(CatalogQuery.initial());

        assertEquals(
                "Final projection must come from the server.",
                "Servidor",
                controller.currentState().snapshot().orElseThrow()
                        .items().get(0).name());
        assertFalse(
                "Reconciled projection must be fresh.",
                controller.currentState().snapshot().orElseThrow().stale());
        assertTrue(
                "Warm cache must be rendered before reconciliation.",
                states.stream().anyMatch(state ->
                        state.phase() == CatalogState.Phase.READY
                                && state.snapshot().orElseThrow().stale()));
    }

    @Test
    public void keepsWarmCacheVisibleDuringUnstableNetwork() {
        FakeRepository repository = new FakeRepository();
        repository.cached = Optional.of(CatalogTestFixtures.snapshot(
                List.of(CatalogTestFixtures.product(1, "Cache")),
                false,
                true));
        repository.refreshFailure = new CatalogException(
                CatalogFailureKind.NETWORK,
                "offline");
        CatalogController controller = controller(repository, Runnable::run);

        controller.open(CatalogQuery.initial());

        assertEquals(
                "Network failure with cache remains a usable ready state.",
                CatalogState.Phase.READY,
                controller.currentState().phase());
        assertEquals(
                "Offline warning must be explicit.",
                CatalogFailureKind.NETWORK,
                controller.currentState().failure().orElseThrow());
        assertTrue(
                "Offline data must be marked stale.",
                controller.currentState().snapshot().orElseThrow().stale());
    }

    @Test
    public void hidesCacheWhenServerRevokesCapability() {
        FakeRepository repository = new FakeRepository();
        repository.cached = Optional.of(CatalogTestFixtures.snapshot(
                List.of(CatalogTestFixtures.product(1, "Cache")),
                false,
                true));
        repository.refreshFailure = new CatalogException(
                CatalogFailureKind.FORBIDDEN,
                "revoked");
        CatalogController controller = controller(repository, Runnable::run);

        controller.open(CatalogQuery.initial());

        assertEquals(
                "Authorization revocation must fail closed.",
                CatalogState.Phase.ERROR,
                controller.currentState().phase());
        assertTrue(
                "Revoked projection must not retain cached products.",
                controller.currentState().snapshot().isEmpty());
    }

    @Test
    public void staleSearchCannotOverwriteNewerQuery() {
        QueuedExecutor worker = new QueuedExecutor();
        FakeRepository repository = new FakeRepository();
        CatalogController controller = controller(repository, worker);

        controller.open(CatalogQuery.initial().withSearch("antiga"));
        controller.open(CatalogQuery.initial().withSearch("nova"));
        worker.runNext();
        assertEquals(
                "Old response must remain suppressed.",
                CatalogState.Phase.LOADING,
                controller.currentState().phase());
        worker.runNext();

        assertEquals(
                "Only the newest query may reach the UI.",
                "nova",
                controller.currentState().snapshot().orElseThrow()
                        .items().get(0).name());
    }

    @Test
    public void loadsNextPageOnlyWhenCursorExists() {
        FakeRepository repository = new FakeRepository();
        repository.refreshed = networkSnapshot(
                List.of(CatalogTestFixtures.product(1, "Primeiro")),
                true);
        repository.more = networkSnapshot(
                List.of(
                        CatalogTestFixtures.product(1, "Primeiro"),
                        CatalogTestFixtures.product(2, "Segundo")),
                false);
        CatalogController controller = controller(repository, Runnable::run);
        controller.open(CatalogQuery.initial());

        controller.loadMore();
        controller.loadMore();

        assertEquals("Only one next page must be loaded.", 1, repository.loadMoreCalls);
        assertEquals(
                "Both unique products must be visible.",
                2,
                controller.currentState().snapshot().orElseThrow().items().size());
    }

    private static CatalogController controller(
            FakeRepository repository,
            Executor worker) {
        return new CatalogController(repository, worker, Runnable::run);
    }

    private static CatalogSnapshot networkSnapshot(
            List<CatalogProduct> items,
            boolean hasMore) {
        return new CatalogSnapshot(
                items,
                hasMore,
                false,
                CatalogSource.NETWORK,
                java.time.Instant.parse("2026-07-25T12:00:00Z"));
    }

    private static final class FakeRepository implements CatalogRepository {
        private Optional<CatalogSnapshot> cached = Optional.empty();
        private CatalogSnapshot refreshed = networkSnapshot(
                List.of(CatalogTestFixtures.product(1, "nova")),
                false);
        private CatalogSnapshot more = refreshed;
        private CatalogException refreshFailure;
        private int loadMoreCalls;

        @Override
        public Optional<CatalogSnapshot> cached(CatalogQuery query) {
            return cached;
        }

        @Override
        public CatalogSnapshot refresh(CatalogQuery query) throws CatalogException {
            if (refreshFailure != null) {
                throw refreshFailure;
            }
            if (query.search().isPresent()) {
                return networkSnapshot(
                        List.of(CatalogTestFixtures.product(
                                1,
                                query.search().orElseThrow())),
                        false);
            }
            return refreshed;
        }

        @Override
        public CatalogSnapshot loadMore(CatalogQuery query) {
            loadMoreCalls++;
            return more;
        }

        @Override
        public void clearAccount() {
            cached = Optional.empty();
        }
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
}
