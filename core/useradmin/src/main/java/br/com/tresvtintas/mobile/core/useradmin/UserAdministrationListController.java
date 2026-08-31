package br.com.tresvtintas.mobile.core.useradmin;

import br.com.tresvtintas.mobile.core.useradmin.UserAdministrationListState.Snapshot;
import br.com.tresvtintas.mobile.core.useradmin.UserAdministrationModels.Page;
import br.com.tresvtintas.mobile.core.useradmin.UserAdministrationModels.User;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public final class UserAdministrationListController {
    @FunctionalInterface
    public interface Listener {
        void onUserAdministrationStateChanged(UserAdministrationListState state);
    }

    private final UserAdministrationRepository repository;
    private final Executor worker;
    private final Executor main;
    private final Set<Listener> listeners = new CopyOnWriteArraySet<>();
    private final AtomicBoolean busy = new AtomicBoolean();
    private final AtomicLong generation = new AtomicLong();
    private volatile UserAdministrationListState current =
            UserAdministrationListState.empty();

    public UserAdministrationListController(
            UserAdministrationRepository repository,
            Executor worker,
            Executor main) {
        this.repository = Objects.requireNonNull(repository, "Repository is required.");
        this.worker = Objects.requireNonNull(worker, "Worker is required.");
        this.main = Objects.requireNonNull(main, "Main executor is required.");
    }

    public void subscribe(Listener listener) {
        Listener required = Objects.requireNonNull(listener, "Listener is required.");
        listeners.add(required);
        UserAdministrationListState snapshot = current;
        main.execute(() -> required.onUserAdministrationStateChanged(snapshot));
    }

    public void unsubscribe(Listener listener) {
        listeners.remove(listener);
    }

    public void open() {
        load(UserAdministrationQuery.initial(), false);
    }

    public void refresh() {
        load(current.snapshot()
                .map(Snapshot::query)
                .orElseGet(UserAdministrationQuery::initial), false);
    }

    public void apply(UserAdministrationQuery query) {
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
        publish(UserAdministrationListState.closed());
        listeners.clear();
    }

    private void load(UserAdministrationQuery query, boolean more) {
        if (!busy.compareAndSet(false, true)) {
            return;
        }
        Optional<Snapshot> previous = current.snapshot();
        long operation = generation.incrementAndGet();
        publish(previous.map(value -> UserAdministrationListState.busy(value, more))
                .orElseGet(UserAdministrationListState::loading));
        worker.execute(() -> execute(operation, query, previous, more));
    }

    private void execute(
            long operation,
            UserAdministrationQuery query,
            Optional<Snapshot> previous,
            boolean more) {
        try {
            Optional<String> cursor = more
                    ? previous.orElseThrow().nextCursor()
                    : Optional.empty();
            Page page = repository.users(query, cursor);
            List<User> users = new ArrayList<>();
            if (more) {
                users.addAll(previous.orElseThrow().users());
            }
            users.addAll(page.items());
            UserAdministrationModels.Options options = more
                    ? previous.orElseThrow().options()
                    : repository.options();
            complete(operation, UserAdministrationListState.ready(
                    new Snapshot(options, query, users, page.nextCursor())));
        } catch (UserAdministrationException failure) {
            UserAdministrationListState next = previous.isPresent()
                    && transientFailure(failure.kind())
                    ? UserAdministrationListState.stale(previous.orElseThrow(), failure)
                    : UserAdministrationListState.error(failure);
            complete(operation, next);
        }
    }

    private void complete(long operation, UserAdministrationListState state) {
        if (generation.get() != operation
                || current.phase() == UserAdministrationListState.Phase.CLOSED) {
            return;
        }
        busy.set(false);
        publish(state);
    }

    private void publish(UserAdministrationListState state) {
        current = state;
        main.execute(() -> listeners.forEach(
                listener -> listener.onUserAdministrationStateChanged(state)));
    }

    private static boolean transientFailure(UserAdministrationFailureKind kind) {
        return kind == UserAdministrationFailureKind.NETWORK
                || kind == UserAdministrationFailureKind.RATE_LIMITED
                || kind == UserAdministrationFailureKind.SERVICE_UNAVAILABLE;
    }
}
