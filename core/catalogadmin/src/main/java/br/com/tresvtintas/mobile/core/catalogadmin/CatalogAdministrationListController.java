package br.com.tresvtintas.mobile.core.catalogadmin;

import br.com.tresvtintas.mobile.core.catalogadmin.CatalogAdministrationModels.Page;
import br.com.tresvtintas.mobile.core.catalogadmin.CatalogAdministrationModels.Product;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public final class CatalogAdministrationListController {
    @FunctionalInterface
    public interface Listener {
        void onCatalogAdministrationStateChanged(
                CatalogAdministrationListState state);
    }

    private final CatalogAdministrationRepository repository;
    private final Executor worker;
    private final Executor main;
    private final Set<Listener> listeners = new CopyOnWriteArraySet<>();
    private final AtomicBoolean busy = new AtomicBoolean();
    private final AtomicLong generation = new AtomicLong();
    private volatile CatalogAdministrationListState current =
            CatalogAdministrationListState.empty();
    private volatile CatalogAdministrationQuery query =
            CatalogAdministrationQuery.initial();

    public CatalogAdministrationListController(
            CatalogAdministrationRepository repository,
            Executor worker,
            Executor main) {
        this.repository = Objects.requireNonNull(repository, "Repository is required.");
        this.worker = Objects.requireNonNull(worker, "Worker is required.");
        this.main = Objects.requireNonNull(main, "Main executor is required.");
    }

    public void subscribe(Listener listener) {
        Listener required = Objects.requireNonNull(listener, "Listener is required.");
        listeners.add(required);
        main.execute(() -> required.onCatalogAdministrationStateChanged(current));
    }

    public void unsubscribe(Listener listener) {
        listeners.remove(listener);
    }

    public void open(CatalogAdministrationQuery requested) {
        query = Objects.requireNonNull(requested, "Query is required.");
        generation.incrementAndGet();
        busy.set(false);
        load(false, true);
    }

    public void refresh() {
        load(false, false);
    }

    public void loadMore() {
        if (current.nextCursor().isPresent()) {
            load(true, false);
        }
    }

    public void close() {
        generation.incrementAndGet();
        busy.set(false);
        publish(new CatalogAdministrationListState(
                CatalogAdministrationListState.Phase.CLOSED,
                List.of(),
                Optional.empty(),
                Optional.empty(),
                false));
        listeners.clear();
    }

    private void load(boolean more, boolean replace) {
        if (!busy.compareAndSet(false, true)) {
            return;
        }
        CatalogAdministrationListState previous = replace
                ? CatalogAdministrationListState.empty()
                : current;
        CatalogAdministrationQuery requested = query;
        long operation = generation.incrementAndGet();
        publish(new CatalogAdministrationListState(
                CatalogAdministrationListState.Phase.LOADING,
                previous.products(),
                previous.nextCursor(),
                Optional.empty(),
                more));
        worker.execute(() -> execute(operation, requested, previous, more));
    }

    private void execute(
            long operation,
            CatalogAdministrationQuery requested,
            CatalogAdministrationListState previous,
            boolean more) {
        try {
            Optional<String> cursor = more
                    ? previous.nextCursor()
                    : Optional.empty();
            Page page = repository.products(requested, cursor);
            List<Product> products = new ArrayList<>();
            if (more) {
                products.addAll(previous.products());
            }
            products.addAll(page.items());
            complete(operation, new CatalogAdministrationListState(
                    CatalogAdministrationListState.Phase.READY,
                    products,
                    page.nextCursor(),
                    Optional.empty(),
                    false));
        } catch (CatalogAdministrationException failure) {
            boolean stale = !previous.products().isEmpty()
                    && transientFailure(failure.kind());
            complete(operation, new CatalogAdministrationListState(
                    stale
                            ? CatalogAdministrationListState.Phase.STALE
                            : CatalogAdministrationListState.Phase.ERROR,
                    stale ? previous.products() : List.of(),
                    stale ? previous.nextCursor() : Optional.empty(),
                    Optional.of(failure),
                    false));
        }
    }

    private void complete(long operation, CatalogAdministrationListState state) {
        if (generation.get() != operation
                || current.phase() == CatalogAdministrationListState.Phase.CLOSED) {
            return;
        }
        busy.set(false);
        publish(state);
    }

    private void publish(CatalogAdministrationListState state) {
        current = state;
        main.execute(() -> listeners.forEach(listener ->
                listener.onCatalogAdministrationStateChanged(state)));
    }

    private static boolean transientFailure(
            CatalogAdministrationFailureKind kind) {
        return kind == CatalogAdministrationFailureKind.NETWORK
                || kind == CatalogAdministrationFailureKind.RATE_LIMITED
                || kind == CatalogAdministrationFailureKind.SERVICE_UNAVAILABLE;
    }
}
