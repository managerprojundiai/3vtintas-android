package br.com.tresvtintas.mobile.core.catalog;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public final class CatalogController {
    private final CatalogRepository repository;
    private final Executor workerExecutor;
    private final Executor mainExecutor;
    private final Set<CatalogStateListener> listeners = new CopyOnWriteArraySet<>();
    private final Set<CatalogPricingContextListener> pricingListeners =
            new CopyOnWriteArraySet<>();
    private final AtomicBoolean operationInProgress = new AtomicBoolean();
    private final AtomicLong generation = new AtomicLong();
    private volatile CatalogState current = CatalogState.empty();
    private volatile CatalogQuery query = CatalogQuery.initial();
    private volatile Optional<CatalogPricingContext> pricingContext = Optional.empty();

    public CatalogController(
            CatalogRepository repository,
            Executor workerExecutor,
            Executor mainExecutor) {
        this.repository = Objects.requireNonNull(repository, "Catalog repository is required.");
        this.workerExecutor = Objects.requireNonNull(workerExecutor, "Worker executor is required.");
        this.mainExecutor = Objects.requireNonNull(mainExecutor, "Main executor is required.");
    }

    public void subscribe(CatalogStateListener listener) {
        CatalogStateListener required = Objects.requireNonNull(
                listener, "Catalog listener is required.");
        listeners.add(required);
        CatalogState snapshot = current;
        mainExecutor.execute(() -> required.onCatalogStateChanged(snapshot));
    }

    public void unsubscribe(CatalogStateListener listener) {
        listeners.remove(listener);
    }

    public void subscribePricing(CatalogPricingContextListener listener) {
        CatalogPricingContextListener required = Objects.requireNonNull(
                listener, "Catalog pricing listener is required.");
        pricingListeners.add(required);
        Optional<CatalogPricingContext> snapshot = pricingContext;
        mainExecutor.execute(() -> required.onCatalogPricingContextChanged(snapshot));
    }

    public void unsubscribePricing(CatalogPricingContextListener listener) {
        pricingListeners.remove(listener);
    }

    public CatalogState currentState() {
        return current;
    }

    public CatalogQuery currentQuery() {
        return query;
    }

    public Optional<CatalogPricingContext> currentPricingContext() {
        return pricingContext;
    }

    public void open(CatalogQuery requestedQuery) {
        query = Objects.requireNonNull(requestedQuery, "Catalog query is required.");
        long operationGeneration = generation.incrementAndGet();
        operationInProgress.set(true);
        publish(CatalogState.loading());
        workerExecutor.execute(() -> executeInitial(requestedQuery, operationGeneration));
    }

    public void refresh() {
        CatalogState state = current;
        if (state.phase() == CatalogState.Phase.CLOSED) {
            return;
        }
        if (!operationInProgress.compareAndSet(false, true)) {
            return;
        }
        long operationGeneration = generation.incrementAndGet();
        state.snapshot().ifPresentOrElse(
                snapshot -> publish(CatalogState.refreshing(snapshot)),
                () -> publish(CatalogState.loading()));
        workerExecutor.execute(() -> executeRefresh(query, operationGeneration));
    }

    public void loadMore() {
        CatalogState state = current;
        Optional<CatalogSnapshot> snapshot = state.snapshot();
        if (snapshot.isEmpty()
                || !snapshot.orElseThrow().hasMore()
                || !operationInProgress.compareAndSet(false, true)) {
            return;
        }
        long operationGeneration = generation.incrementAndGet();
        publish(CatalogState.loadingMore(snapshot.orElseThrow()));
        workerExecutor.execute(() -> executeLoadMore(query, operationGeneration));
    }

    public void selectPriceList(String versionPublicId) {
        Objects.requireNonNull(versionPublicId, "Catalog table selection is required.");
        long operationGeneration = generation.incrementAndGet();
        operationInProgress.set(true);
        publish(CatalogState.loading());
        workerExecutor.execute(() -> {
            try {
                repository.selectPriceList(versionPublicId);
                Optional<CatalogPricingContext> updated = repository.pricingContext();
                if (!isCurrent(operationGeneration)) {
                    return;
                }
                publishPricing(updated);
                executeNetworkRefresh(query, operationGeneration, Optional.empty());
            } catch (CatalogException exception) {
                completeFailure(operationGeneration, Optional.empty(), exception);
            }
        });
    }

    public void close() {
        generation.incrementAndGet();
        operationInProgress.set(false);
        publish(CatalogState.closed());
    }

    private void executeInitial(
            CatalogQuery requestedQuery,
            long operationGeneration) {
        Optional<CatalogSnapshot> cached = Optional.empty();
        try {
            Optional<CatalogPricingContext> context = repository.pricingContext();
            if (!isCurrent(operationGeneration)) {
                return;
            }
            publishPricing(context);
            if (context.map(CatalogPricingContext::needsSelection).orElse(false)) {
                operationInProgress.set(false);
                publish(CatalogState.priceSelectionRequired());
                return;
            }
            cached = repository.cached(requestedQuery);
            if (!isCurrent(operationGeneration)) {
                return;
            }
            cached.ifPresent(snapshot -> publish(CatalogState.ready(
                    snapshot.asStale(),
                    Optional.empty(),
                    Optional.empty())));
            executeNetworkRefresh(requestedQuery, operationGeneration, cached);
        } catch (CatalogException exception) {
            completeFailure(operationGeneration, cached, exception);
        }
    }

    private void executeRefresh(
            CatalogQuery requestedQuery,
            long operationGeneration) {
        executeNetworkRefresh(
                requestedQuery,
                operationGeneration,
                repository.allowsStalePrices()
                        ? current.snapshot()
                        : Optional.empty());
    }

    private void executeNetworkRefresh(
            CatalogQuery requestedQuery,
            long operationGeneration,
            Optional<CatalogSnapshot> fallback) {
        try {
            CatalogSnapshot fresh = repository.refresh(requestedQuery);
            completeReady(operationGeneration, fresh);
        } catch (CatalogException exception) {
            completeFailure(operationGeneration, fallback, exception);
        }
    }

    private void executeLoadMore(
            CatalogQuery requestedQuery,
            long operationGeneration) {
        Optional<CatalogSnapshot> fallback = repository.allowsStalePrices()
                ? current.snapshot()
                : Optional.empty();
        try {
            CatalogSnapshot page = repository.loadMore(requestedQuery);
            completeReady(operationGeneration, page);
        } catch (CatalogException exception) {
            completeFailure(operationGeneration, fallback, exception);
        }
    }

    private void completeReady(
            long operationGeneration,
            CatalogSnapshot snapshot) {
        if (!isCurrent(operationGeneration)) {
            return;
        }
        operationInProgress.set(false);
        publish(CatalogState.ready(
                snapshot,
                Optional.empty(),
                Optional.empty()));
    }

    private void completeFailure(
            long operationGeneration,
            Optional<CatalogSnapshot> fallback,
            CatalogException exception) {
        if (!isCurrent(operationGeneration)) {
            return;
        }
        operationInProgress.set(false);
        if (fallback.isPresent() && mayExposeFallback(exception.kind())) {
            publish(CatalogState.ready(
                    fallback.orElseThrow().asStale(),
                    Optional.of(exception.kind()),
                    exception.requestId()));
        } else {
            publish(CatalogState.error(exception));
        }
    }

    private boolean isCurrent(long operationGeneration) {
        return generation.get() == operationGeneration
                && current.phase() != CatalogState.Phase.CLOSED;
    }

    private void publish(CatalogState next) {
        current = next;
        mainExecutor.execute(() -> {
            for (CatalogStateListener listener : listeners) {
                listener.onCatalogStateChanged(next);
            }
        });
    }

    private void publishPricing(Optional<CatalogPricingContext> next) {
        pricingContext = next;
        mainExecutor.execute(() -> {
            for (CatalogPricingContextListener listener : pricingListeners) {
                listener.onCatalogPricingContextChanged(next);
            }
        });
    }

    private static boolean mayExposeFallback(CatalogFailureKind kind) {
        return switch (kind) {
            case NETWORK, RATE_LIMITED, SERVICE_UNAVAILABLE -> true;
            default -> false;
        };
    }
}
