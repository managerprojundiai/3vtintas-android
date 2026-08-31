package br.com.tresvtintas.mobile.core.catalogadmin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import br.com.tresvtintas.mobile.core.catalogadmin.CatalogAdministrationListState.Phase;
import br.com.tresvtintas.mobile.core.catalogadmin.CatalogAdministrationModels.Category;
import br.com.tresvtintas.mobile.core.catalogadmin.CatalogAdministrationModels.CategoryMutation;
import br.com.tresvtintas.mobile.core.catalogadmin.CatalogAdministrationModels.KnowledgeDraft;
import br.com.tresvtintas.mobile.core.catalogadmin.CatalogAdministrationModels.Mutation;
import br.com.tresvtintas.mobile.core.catalogadmin.CatalogAdministrationModels.Page;
import br.com.tresvtintas.mobile.core.catalogadmin.CatalogAdministrationModels.Product;
import br.com.tresvtintas.mobile.core.catalogadmin.CatalogAdministrationModels.ProductDraft;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.Executor;
import org.junit.Test;

public final class CatalogAdministrationListControllerTest {
    @Test
    public void appendsOnlyTheOpaqueNextPage() {
        FakeRepository repository = new FakeRepository();
        List<CatalogAdministrationListState> states = new ArrayList<>();
        CatalogAdministrationListController controller =
                controller(repository, Runnable::run);
        controller.subscribe(states::add);

        controller.open(CatalogAdministrationQuery.initial());
        controller.loadMore();
        controller.loadMore();

        CatalogAdministrationListState state = states.get(states.size() - 1);
        assertEquals(
                "Both authorized pages must be visible.",
                2,
                state.products().size());
        assertTrue(
                "The final opaque cursor must disappear.",
                state.nextCursor().isEmpty());
        assertEquals(
                "A missing cursor must prevent an extra request.",
                2,
                repository.calls);
    }

    @Test
    public void transientRefreshKeepsTheLastAuthorizedPage() {
        FakeRepository repository = new FakeRepository();
        List<CatalogAdministrationListState> states = new ArrayList<>();
        CatalogAdministrationListController controller =
                controller(repository, Runnable::run);
        controller.subscribe(states::add);
        controller.open(CatalogAdministrationQuery.initial());
        repository.failure = new CatalogAdministrationException(
                CatalogAdministrationFailureKind.NETWORK,
                "Offline.");

        controller.refresh();

        CatalogAdministrationListState state = states.get(states.size() - 1);
        assertEquals(
                "A transient refresh must be explicitly stale.",
                Phase.STALE,
                state.phase());
        assertEquals(
                "The last authorized product must remain visible.",
                1,
                state.products().size());
    }

    @Test
    public void authorizationFailureClearsPreviouslyVisibleProducts() {
        FakeRepository repository = new FakeRepository();
        List<CatalogAdministrationListState> states = new ArrayList<>();
        CatalogAdministrationListController controller =
                controller(repository, Runnable::run);
        controller.subscribe(states::add);
        controller.open(CatalogAdministrationQuery.initial());
        repository.failure = new CatalogAdministrationException(
                CatalogAdministrationFailureKind.FORBIDDEN,
                "Revoked.");

        controller.refresh();

        CatalogAdministrationListState state = states.get(states.size() - 1);
        assertEquals(
                "Authorization revocation must fail closed.",
                Phase.ERROR,
                state.phase());
        assertTrue(
                "Revoked products cannot remain rendered.",
                state.products().isEmpty());
    }

    @Test
    public void newerSearchSuppressesAQueuedOlderResult() {
        FakeRepository repository = new FakeRepository();
        QueueExecutor worker = new QueueExecutor();
        List<CatalogAdministrationListState> states = new ArrayList<>();
        CatalogAdministrationListController controller =
                controller(repository, worker);
        controller.subscribe(states::add);

        controller.open(CatalogAdministrationQuery.initial().withSearch("old"));
        controller.open(CatalogAdministrationQuery.initial().withSearch("new"));
        worker.runNext();
        assertEquals(
                "The stale completion must not replace the loading state.",
                Phase.LOADING,
                states.get(states.size() - 1).phase());
        worker.runNext();

        assertEquals(
                "Only the latest search may reach the screen.",
                "new",
                states.get(states.size() - 1).products().get(0).name());
    }

    @Test
    public void closeSuppressesDelayedWork() {
        QueueExecutor worker = new QueueExecutor();
        List<CatalogAdministrationListState> states = new ArrayList<>();
        CatalogAdministrationListController controller =
                controller(new FakeRepository(), worker);
        controller.subscribe(states::add);
        controller.open(CatalogAdministrationQuery.initial());

        controller.close();
        worker.runNext();

        assertEquals(
                "A closed account scope cannot be revived.",
                Phase.CLOSED,
                states.get(states.size() - 1).phase());
    }

    private static CatalogAdministrationListController controller(
            CatalogAdministrationRepository repository,
            Executor worker) {
        return new CatalogAdministrationListController(
                repository,
                worker,
                Runnable::run);
    }

    private static Product product(long id, String name) {
        Instant now = Instant.parse("2026-07-30T12:00:00Z");
        return new Product(
                id,
                Optional.empty(),
                name,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                "10.00",
                3,
                Optional.empty(),
                true,
                1,
                CatalogAdministrationModels.emptyKnowledge(),
                now,
                now);
    }

    private static final class QueueExecutor implements Executor {
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
            implements CatalogAdministrationRepository {
        private static final String UNUSED = "Not used.";
        private int calls;
        private CatalogAdministrationException failure;

        @Override
        public Page products(
                CatalogAdministrationQuery query,
                Optional<String> cursor) throws CatalogAdministrationException {
            if (failure != null) {
                throw failure;
            }
            calls++;
            String name = query.search().orElse("Product " + calls);
            return new Page(
                    List.of(CatalogAdministrationListControllerTest.product(
                            calls,
                            name)),
                    calls == 1 ? Optional.of("opaque_next") : Optional.empty());
        }

        @Override
        public Product product(long productId) {
            throw new AssertionError(UNUSED);
        }

        @Override
        public List<Category> categories() {
            throw new AssertionError(UNUSED);
        }

        @Override
        public Mutation create(ProductDraft product, String key) {
            throw new AssertionError(UNUSED);
        }

        @Override
        public Mutation update(
                long productId,
                int revision,
                ProductDraft product,
                String key) {
            throw new AssertionError(UNUSED);
        }

        @Override
        public Mutation setActive(
                long productId,
                int revision,
                boolean active,
                String key) {
            throw new AssertionError(UNUSED);
        }

        @Override
        public Mutation updateKnowledge(
                long productId,
                int revision,
                KnowledgeDraft knowledge,
                String key) {
            throw new AssertionError(UNUSED);
        }

        @Override
        public CategoryMutation createCategory(
                String name,
                Optional<String> description,
                String key) {
            throw new AssertionError(UNUSED);
        }
    }
}
