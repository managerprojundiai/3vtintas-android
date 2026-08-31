package br.com.tresvtintas.mobile.core.accountaccess;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public final class AccountAccessController {
    private static final int PAGE_SIZE = 30;
    private final AccountAccessReader reader;
    private final Optional<AccountAccessRevoker> revoker;
    private final Executor worker;
    private final Executor main;
    private final AccountRevocationListener revocationListener;
    private final Set<AccountAccessStateListener> listeners =
            new CopyOnWriteArraySet<>();
    private final AtomicBoolean busy = new AtomicBoolean();
    private final AtomicLong generation = new AtomicLong();
    private volatile AccountAccessView view = AccountAccessView.DEVICES;
    private volatile AccountAccessState current =
            AccountAccessState.empty(AccountAccessView.DEVICES);

    public AccountAccessController(
            AccountAccessRepository repository,
            Executor worker,
            Executor main,
            AccountRevocationListener revocationListener) {
        this(
                repository,
                Optional.of(Objects.requireNonNull(
                        repository,
                        "Account access repository is required.")),
                worker,
                main,
                revocationListener);
    }

    private AccountAccessController(
            AccountAccessReader reader,
            Optional<AccountAccessRevoker> revoker,
            Executor worker,
            Executor main,
            AccountRevocationListener revocationListener) {
        this.reader = Objects.requireNonNull(
                reader,
                "Account access reader is required.");
        this.revoker = Objects.requireNonNull(
                revoker,
                "Account access revoker state is required.");
        this.worker = Objects.requireNonNull(
                worker,
                "Account access worker is required.");
        this.main = Objects.requireNonNull(
                main,
                "Account access main executor is required.");
        this.revocationListener = Objects.requireNonNull(
                revocationListener,
                "Account revocation listener is required.");
    }

    public static AccountAccessController readOnly(
            AccountAccessReader reader,
            Executor worker,
            Executor main) {
        return new AccountAccessController(
                reader,
                Optional.empty(),
                worker,
                main,
                ignored -> {
                });
    }

    public boolean canRevoke() {
        return revoker.isPresent();
    }

    public void subscribe(AccountAccessStateListener listener) {
        AccountAccessStateListener required = Objects.requireNonNull(
                listener,
                "Account access listener is required.");
        listeners.add(required);
        AccountAccessState snapshot = current;
        main.execute(() -> required.onAccountAccessStateChanged(snapshot));
    }

    public void unsubscribe(AccountAccessStateListener listener) {
        listeners.remove(listener);
    }

    public AccountAccessView currentView() {
        return view;
    }

    public void open(AccountAccessView requested) {
        if (current.phase() == AccountAccessState.Phase.REVOKING) {
            return;
        }
        view = Objects.requireNonNull(
                requested,
                "Account access view is required.");
        long operation = generation.incrementAndGet();
        busy.set(true);
        publish(AccountAccessState.loading(requested));
        worker.execute(() -> first(
                requested,
                operation,
                Optional.empty()));
    }

    public void refresh() {
        if (current.phase() == AccountAccessState.Phase.CLOSED
                || !busy.compareAndSet(false, true)) {
            return;
        }
        long operation = generation.incrementAndGet();
        AccountAccessView requested = view;
        Optional<AccountAccessSnapshot> fallback =
                current.snapshot();
        fallback.ifPresentOrElse(
                value -> publish(AccountAccessState.refreshing(value)),
                () -> publish(AccountAccessState.loading(requested)));
        worker.execute(() -> first(
                requested,
                operation,
                fallback));
    }

    public void loadMore() {
        Optional<AccountAccessSnapshot> snapshot =
                current.snapshot();
        if (snapshot.isEmpty()
                || !snapshot.orElseThrow().hasMore()
                || !busy.compareAndSet(false, true)) {
            return;
        }
        long operation = generation.incrementAndGet();
        AccountAccessSnapshot value = snapshot.orElseThrow();
        publish(AccountAccessState.loadingMore(value));
        worker.execute(() -> next(operation, value));
    }

    public void revoke(String targetId) {
        if (revoker.isEmpty()) {
            return;
        }
        Optional<AccountAccessSnapshot> snapshot =
                current.snapshot();
        Optional<AccountAccessEntry> target = snapshot.flatMap(
                value -> value.entry(targetId));
        if (snapshot.isEmpty()
                || target.isEmpty()
                || target.orElseThrow().status()
                        != AccountAccessStatus.ACTIVE
                || !busy.compareAndSet(false, true)) {
            return;
        }
        long operation = generation.incrementAndGet();
        AccountAccessSnapshot fallback = snapshot.orElseThrow();
        publish(AccountAccessState.revoking(
                fallback,
                target.orElseThrow().id()));
        worker.execute(() -> revoke(
                operation,
                fallback,
                target.orElseThrow().id()));
    }

    public void close() {
        generation.incrementAndGet();
        busy.set(false);
        publish(AccountAccessState.closed(view));
        listeners.clear();
    }

    private void first(
            AccountAccessView requested,
            long operation,
            Optional<AccountAccessSnapshot> fallback) {
        try {
            ready(
                    operation,
                    AccountAccessSnapshot.from(
                            requested,
                            reader.page(
                                    requested,
                                    Optional.empty(),
                                    PAGE_SIZE)));
        } catch (AccountAccessException failure) {
            failed(operation, requested, fallback, failure);
        }
    }

    private void next(
            long operation,
            AccountAccessSnapshot snapshot) {
        try {
            ready(
                    operation,
                    snapshot.append(reader.page(
                            snapshot.view(),
                            snapshot.nextCursor(),
                            PAGE_SIZE)));
        } catch (AccountAccessException failure) {
            failed(
                    operation,
                    snapshot.view(),
                    Optional.of(snapshot),
                    failure);
        }
    }

    private void revoke(
            long operation,
            AccountAccessSnapshot snapshot,
            String targetId) {
        AccountRevocation result;
        try {
            result = revoker.orElseThrow().revoke(
                    snapshot.view(),
                    targetId);
        } catch (AccountAccessException failure) {
            failed(
                    operation,
                    snapshot.view(),
                    Optional.of(snapshot),
                    failure);
            return;
        }
        if (!isCurrent(operation)) {
            return;
        }
        if (result.current()) {
            busy.set(false);
            publish(AccountAccessState.ready(
                    snapshot,
                    Optional.empty(),
                    Optional.empty()));
            notifyRevocation(result);
            return;
        }
        AccountAccessSnapshot confirmed =
                snapshot.without(targetId);
        try {
            AccountAccessSnapshot refreshed =
                    AccountAccessSnapshot.from(
                            snapshot.view(),
                            reader.page(
                                    snapshot.view(),
                                    Optional.empty(),
                                    PAGE_SIZE));
            if (!isCurrent(operation)) {
                return;
            }
            busy.set(false);
            publish(AccountAccessState.ready(
                    refreshed,
                    Optional.empty(),
                    Optional.empty()));
            notifyRevocation(result);
        } catch (AccountAccessException failure) {
            failed(
                    operation,
                    snapshot.view(),
                    Optional.of(confirmed),
                    failure);
            if (isCurrent(operation)) {
                notifyRevocation(result);
            }
        }
    }

    private void ready(
            long operation,
            AccountAccessSnapshot snapshot) {
        if (!isCurrent(operation)) {
            return;
        }
        busy.set(false);
        publish(AccountAccessState.ready(
                snapshot,
                Optional.empty(),
                Optional.empty()));
    }

    private void failed(
            long operation,
            AccountAccessView requested,
            Optional<AccountAccessSnapshot> fallback,
            AccountAccessException failure) {
        if (!isCurrent(operation)) {
            return;
        }
        busy.set(false);
        if (fallback.isPresent()
                && recoverable(failure.kind())) {
            publish(AccountAccessState.ready(
                    fallback.orElseThrow(),
                    Optional.of(failure.kind()),
                    failure.requestId()));
        } else {
            publish(AccountAccessState.error(
                    requested,
                    failure));
        }
    }

    private boolean isCurrent(long operation) {
        return generation.get() == operation
                && current.phase()
                        != AccountAccessState.Phase.CLOSED;
    }

    private static boolean recoverable(
            AccountAccessFailureKind kind) {
        return kind == AccountAccessFailureKind.NETWORK
                || kind == AccountAccessFailureKind.RATE_LIMITED
                || kind
                        == AccountAccessFailureKind.SERVICE_UNAVAILABLE;
    }

    private void publish(AccountAccessState state) {
        current = state;
        main.execute(() -> listeners.forEach(listener ->
                listener.onAccountAccessStateChanged(state)));
    }

    private void notifyRevocation(AccountRevocation result) {
        main.execute(() ->
                revocationListener.onAccountAccessRevoked(result));
    }
}
