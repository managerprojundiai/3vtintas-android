package br.com.tresvtintas.mobile.core.corporatefinance;

import br.com.tresvtintas.mobile.core.finance.FinanceException;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public final class CorporateFinanceOrganizationController {
    private static final int PAGE_SIZE = 30;
    private static final int MAXIMUM_SEARCH_LENGTH = 100;
    private final CorporateFinanceOrganizationRepository repository;
    private final Executor worker;
    private final Executor main;
    private final Set<CorporateFinanceOrganizationStateListener> listeners =
            new CopyOnWriteArraySet<>();
    private final AtomicBoolean busy = new AtomicBoolean();
    private final AtomicLong generation = new AtomicLong();
    private volatile CorporateFinanceOrganizationState current =
            CorporateFinanceOrganizationState.empty();
    private volatile Optional<String> search = Optional.empty();

    public CorporateFinanceOrganizationController(
            CorporateFinanceOrganizationRepository repository,
            Executor worker,
            Executor main) {
        this.repository = Objects.requireNonNull(
                repository,
                "Corporate finance organization repository is required.");
        this.worker = Objects.requireNonNull(worker, "Worker is required.");
        this.main = Objects.requireNonNull(main, "Main executor is required.");
    }

    public void subscribe(
            CorporateFinanceOrganizationStateListener listener) {
        CorporateFinanceOrganizationStateListener required =
                Objects.requireNonNull(listener, "Listener is required.");
        listeners.add(required);
        CorporateFinanceOrganizationState snapshot = current;
        main.execute(() ->
                required.onCorporateFinanceOrganizationStateChanged(snapshot));
    }

    public void unsubscribe(
            CorporateFinanceOrganizationStateListener listener) {
        listeners.remove(listener);
    }

    public void open(String value) {
        search = normalize(value);
        long operation = generation.incrementAndGet();
        busy.set(true);
        publish(CorporateFinanceOrganizationState.loading());
        worker.execute(() -> first(operation));
    }

    public void loadMore() {
        Optional<CorporateFinanceOrganizationSnapshot> snapshot =
                current.snapshot();
        if (snapshot.isEmpty()
                || !snapshot.orElseThrow().hasMore()
                || !busy.compareAndSet(false, true)) {
            return;
        }
        long operation = generation.incrementAndGet();
        CorporateFinanceOrganizationSnapshot value =
                snapshot.orElseThrow();
        publish(CorporateFinanceOrganizationState.loadingMore(value));
        worker.execute(() -> next(operation, value));
    }

    public void close() {
        generation.incrementAndGet();
        busy.set(false);
        publish(CorporateFinanceOrganizationState.closed());
        listeners.clear();
    }

    private void first(long operation) {
        try {
            ready(
                    operation,
                    CorporateFinanceOrganizationSnapshot.from(
                            repository.page(
                                    search,
                                    Optional.empty(),
                                    PAGE_SIZE)));
        } catch (FinanceException failure) {
            failed(operation, failure);
        }
    }

    private void next(
            long operation,
            CorporateFinanceOrganizationSnapshot snapshot) {
        try {
            ready(
                    operation,
                    snapshot.append(repository.page(
                            search,
                            snapshot.nextCursor(),
                            PAGE_SIZE)));
        } catch (FinanceException failure) {
            failed(operation, failure);
        }
    }

    private void ready(
            long operation,
            CorporateFinanceOrganizationSnapshot snapshot) {
        if (!isCurrent(operation)) {
            return;
        }
        busy.set(false);
        publish(CorporateFinanceOrganizationState.ready(snapshot));
    }

    private void failed(long operation, FinanceException failure) {
        if (!isCurrent(operation)) {
            return;
        }
        busy.set(false);
        publish(CorporateFinanceOrganizationState.error(failure));
    }

    private boolean isCurrent(long operation) {
        return generation.get() == operation
                && current.phase()
                        != CorporateFinanceOrganizationState.Phase.CLOSED;
    }

    private void publish(CorporateFinanceOrganizationState state) {
        current = state;
        main.execute(() -> listeners.forEach(listener ->
                listener.onCorporateFinanceOrganizationStateChanged(state)));
    }

    private static Optional<String> normalize(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if (normalized.length() > MAXIMUM_SEARCH_LENGTH) {
            throw new IllegalArgumentException(
                    "Corporate finance organization search is invalid.");
        }
        return Optional.of(normalized);
    }
}
