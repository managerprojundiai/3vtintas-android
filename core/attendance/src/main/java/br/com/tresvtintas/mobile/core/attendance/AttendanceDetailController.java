package br.com.tresvtintas.mobile.core.attendance;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public final class AttendanceDetailController {
    private static final int PAGE_SIZE = 50;

    @FunctionalInterface
    public interface Listener {
        void onAttendanceDetailStateChanged(AttendanceDetailState state);
    }

    private final AttendanceRepository repository;
    private final Executor worker;
    private final Executor main;
    private final Set<Listener> listeners = new CopyOnWriteArraySet<>();
    private final AtomicBoolean busy = new AtomicBoolean();
    private final AtomicLong generation = new AtomicLong();
    private volatile AttendanceDetailState current =
            AttendanceDetailState.empty();

    public AttendanceDetailController(
            AttendanceRepository repository,
            Executor worker,
            Executor main) {
        this.repository = Objects.requireNonNull(
                repository,
                "Attendance repository is required.");
        this.worker = Objects.requireNonNull(
                worker,
                "Attendance worker is required.");
        this.main = Objects.requireNonNull(
                main,
                "Attendance main executor is required.");
    }

    public void subscribe(Listener listener) {
        Listener required = Objects.requireNonNull(
                listener,
                "Attendance listener is required.");
        listeners.add(required);
        AttendanceDetailState snapshot = current;
        main.execute(() ->
                required.onAttendanceDetailStateChanged(snapshot));
    }

    public void unsubscribe(Listener listener) {
        listeners.remove(listener);
    }

    public void open(String conversationId) {
        String required = requiredConversationId(conversationId);
        busy.set(true);
        long operation = generation.incrementAndGet();
        publish(AttendanceDetailState.loading());
        worker.execute(() -> first(
                required,
                operation,
                Optional.empty()));
    }

    public void refresh() {
        Optional<AttendanceDetailState.Snapshot> snapshot =
                current.snapshot();
        if (snapshot.isEmpty()
                || !busy.compareAndSet(false, true)) {
            return;
        }
        AttendanceDetailState.Snapshot fallback =
                snapshot.orElseThrow();
        long operation = generation.incrementAndGet();
        publish(AttendanceDetailState.refreshing(fallback));
        worker.execute(() -> first(
                fallback.conversation().id(),
                operation,
                Optional.of(fallback)));
    }

    public void loadMore() {
        Optional<AttendanceDetailState.Snapshot> snapshot =
                current.snapshot();
        if (snapshot.isEmpty()
                || !snapshot.orElseThrow().hasMore()
                || !busy.compareAndSet(false, true)) {
            return;
        }
        AttendanceDetailState.Snapshot value = snapshot.orElseThrow();
        long operation = generation.incrementAndGet();
        publish(AttendanceDetailState.loadingMore(value));
        worker.execute(() -> next(operation, value));
    }

    public void close() {
        generation.incrementAndGet();
        busy.set(false);
        publish(AttendanceDetailState.closed());
        listeners.clear();
    }

    private void first(
            String conversationId,
            long operation,
            Optional<AttendanceDetailState.Snapshot> fallback) {
        try {
            AttendanceConversation conversation =
                    repository.conversation(conversationId);
            AttendanceMessagePage page = repository.messagePage(
                    conversationId,
                    Optional.empty(),
                    PAGE_SIZE);
            if (!conversation.id().equals(page.conversationId())) {
                throw new AttendanceException(
                        AttendanceFailureKind.PROTOCOL,
                        "Attendance response identifiers do not match.");
            }
            AttendanceDetailState.Snapshot loaded =
                    new AttendanceDetailState.Snapshot(
                    conversation,
                    page.items(),
                    page.nextCursor());
            try {
                ready(operation, markDisplayedRead(loaded));
            } catch (AttendanceException failure) {
                if (isTransient(failure.kind())) {
                    warning(operation, loaded, failure);
                } else {
                    failed(operation, fallback, failure);
                }
            }
        } catch (AttendanceException failure) {
            failed(operation, fallback, failure);
        }
    }

    private void next(
            long operation,
            AttendanceDetailState.Snapshot snapshot) {
        try {
            AttendanceMessagePage page = repository.messagePage(
                    snapshot.conversation().id(),
                    snapshot.nextCursor(),
                    PAGE_SIZE);
            if (!snapshot.conversation().id().equals(
                    page.conversationId())) {
                throw new AttendanceException(
                        AttendanceFailureKind.PROTOCOL,
                        "Attendance response identifiers do not match.");
            }
            ready(operation, snapshot.append(page));
        } catch (AttendanceException failure) {
            failed(operation, Optional.of(snapshot), failure);
        }
    }

    private void ready(
            long operation,
            AttendanceDetailState.Snapshot snapshot) {
        if (!isCurrent(operation)) {
            return;
        }
        busy.set(false);
        publish(AttendanceDetailState.ready(snapshot));
    }

    private void failed(
            long operation,
            Optional<AttendanceDetailState.Snapshot> fallback,
            AttendanceException failure) {
        if (!isCurrent(operation)) {
            return;
        }
        busy.set(false);
        if (fallback.isPresent() && isTransient(failure.kind())) {
            publish(AttendanceDetailState.warning(
                    fallback.orElseThrow(),
                    failure));
        } else {
            publish(AttendanceDetailState.error(failure));
        }
    }

    private AttendanceDetailState.Snapshot markDisplayedRead(
            AttendanceDetailState.Snapshot snapshot)
            throws AttendanceException {
        Optional<AttendanceMessage> newest = snapshot.messages().stream()
                .max((left, right) -> {
                    int created = left.createdAt().compareTo(
                            right.createdAt());
                    if (created != 0) {
                        return created;
                    }
                    return Integer.compare(
                            Integer.parseInt(left.sourceId()),
                            Integer.parseInt(right.sourceId()));
                });
        if (newest.isEmpty()) {
            return snapshot;
        }
        AttendanceReadCursorResult result = repository.markRead(
                snapshot.conversation().id(),
                newest.orElseThrow().id());
        if (!snapshot.conversation().id().equals(
                result.conversationId())) {
            throw new AttendanceException(
                    AttendanceFailureKind.PROTOCOL,
                    "Attendance read cursor belongs to another conversation.");
        }
        return new AttendanceDetailState.Snapshot(
                snapshot.conversation().withUnreadCount(
                        result.unreadCount()),
                snapshot.messages(),
                snapshot.nextCursor());
    }

    private void warning(
            long operation,
            AttendanceDetailState.Snapshot snapshot,
            AttendanceException failure) {
        if (!isCurrent(operation)) {
            return;
        }
        busy.set(false);
        publish(AttendanceDetailState.warning(snapshot, failure));
    }

    private boolean isCurrent(long operation) {
        return generation.get() == operation
                && current.phase() != AttendanceDetailState.Phase.CLOSED;
    }

    private static boolean isTransient(AttendanceFailureKind failure) {
        return failure == AttendanceFailureKind.NETWORK
                || failure == AttendanceFailureKind.RATE_LIMITED
                || failure == AttendanceFailureKind.SERVICE_UNAVAILABLE;
    }

    private void publish(AttendanceDetailState state) {
        current = state;
        main.execute(() -> listeners.forEach(listener ->
                listener.onAttendanceDetailStateChanged(state)));
    }

    private static String requiredConversationId(String value) {
        return AttendanceConversationId.require(value);
    }
}
