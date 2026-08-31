package br.com.tresvtintas.mobile.core.organizationadmin;

import br.com.tresvtintas.mobile.core.organizationadmin.OrganizationAdministrationListState.Snapshot;
import br.com.tresvtintas.mobile.core.organizationadmin.OrganizationAdministrationModels.Organization;
import br.com.tresvtintas.mobile.core.organizationadmin.OrganizationAdministrationModels.Page;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public final class OrganizationAdministrationListController {
    @FunctionalInterface
    public interface Listener {
        void onOrganizationAdministrationStateChanged(
                OrganizationAdministrationListState state);
    }

    private final OrganizationAdministrationRepository repository;
    private final Executor worker;
    private final Executor main;
    private final Set<Listener> listeners = new CopyOnWriteArraySet<>();
    private final AtomicBoolean busy = new AtomicBoolean();
    private final AtomicLong generation = new AtomicLong();
    private volatile OrganizationAdministrationListState current =
            OrganizationAdministrationListState.empty();

    public OrganizationAdministrationListController(
            OrganizationAdministrationRepository repository,
            Executor worker,
            Executor main) {
        this.repository = Objects.requireNonNull(repository, "Repository is required.");
        this.worker = Objects.requireNonNull(worker, "Worker is required.");
        this.main = Objects.requireNonNull(main, "Main executor is required.");
    }

    public void subscribe(Listener listener) {
        Listener required = Objects.requireNonNull(listener, "Listener is required.");
        listeners.add(required);
        OrganizationAdministrationListState snapshot = current;
        main.execute(() -> required.onOrganizationAdministrationStateChanged(snapshot));
    }

    public void unsubscribe(Listener listener) {
        listeners.remove(listener);
    }

    public void open() {
        load(OrganizationAdministrationQuery.initial(), false);
    }

    public void refresh() {
        load(current.snapshot()
                .map(Snapshot::query)
                .orElseGet(OrganizationAdministrationQuery::initial), false);
    }

    public void apply(OrganizationAdministrationQuery query) {
        load(Objects.requireNonNull(query, "Query is required."), false);
    }

    public void loadMore() {
        Snapshot snapshot = current.snapshot().orElse(null);
        if (snapshot != null && snapshot.nextCursor().isPresent()) {
            load(snapshot.query(), true);
        }
    }

    public void close() {
        generation.incrementAndGet();
        busy.set(false);
        publish(OrganizationAdministrationListState.closed());
        listeners.clear();
    }

    private void load(OrganizationAdministrationQuery query, boolean more) {
        if (!busy.compareAndSet(false, true)) {
            return;
        }
        Optional<Snapshot> previous = current.snapshot();
        long operation = generation.incrementAndGet();
        publish(previous.map(value ->
                        OrganizationAdministrationListState.busy(value, more))
                .orElseGet(OrganizationAdministrationListState::loading));
        worker.execute(() -> execute(operation, query, previous, more));
    }

    private void execute(
            long operation,
            OrganizationAdministrationQuery query,
            Optional<Snapshot> previous,
            boolean more) {
        try {
            Optional<String> cursor = more
                    ? previous.orElseThrow().nextCursor()
                    : Optional.empty();
            Page page = repository.organizations(query, cursor);
            List<Organization> organizations = new ArrayList<>();
            if (more) {
                organizations.addAll(previous.orElseThrow().organizations());
            }
            organizations.addAll(page.items());
            complete(operation, OrganizationAdministrationListState.ready(
                    new Snapshot(query, organizations, page.nextCursor())));
        } catch (OrganizationAdministrationException failure) {
            OrganizationAdministrationListState next = previous.isPresent()
                    && transientFailure(failure.kind())
                    ? OrganizationAdministrationListState.stale(
                            previous.orElseThrow(), failure)
                    : OrganizationAdministrationListState.error(failure);
            complete(operation, next);
        }
    }

    private void complete(
            long operation,
            OrganizationAdministrationListState state) {
        if (generation.get() != operation
                || current.phase()
                        == OrganizationAdministrationListState.Phase.CLOSED) {
            return;
        }
        busy.set(false);
        publish(state);
    }

    private void publish(OrganizationAdministrationListState state) {
        current = state;
        main.execute(() -> listeners.forEach(listener ->
                listener.onOrganizationAdministrationStateChanged(state)));
    }

    private static boolean transientFailure(
            OrganizationAdministrationFailureKind kind) {
        return kind == OrganizationAdministrationFailureKind.NETWORK
                || kind == OrganizationAdministrationFailureKind.RATE_LIMITED
                || kind == OrganizationAdministrationFailureKind.SERVICE_UNAVAILABLE;
    }
}
