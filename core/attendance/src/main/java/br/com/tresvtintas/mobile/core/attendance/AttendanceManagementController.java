package br.com.tresvtintas.mobile.core.attendance;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public final class AttendanceManagementController {
    private static final int DIRECTORY_PAGE_SIZE = 100;
    private static final int MAXIMUM_DIRECTORY_PAGES = 10;

    @FunctionalInterface
    public interface Listener {
        void onAttendanceManagementStateChanged(
                AttendanceManagementState state);
    }

    private final AttendanceRepository repository;
    private final Executor worker;
    private final Executor main;
    private final Set<Listener> listeners = new CopyOnWriteArraySet<>();
    private final AtomicBoolean busy = new AtomicBoolean();
    private final AtomicLong generation = new AtomicLong();
    private volatile AttendanceManagementState current =
            AttendanceManagementState.idle();

    public AttendanceManagementController(
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
                "Attendance management listener is required.");
        listeners.add(required);
        AttendanceManagementState snapshot = current;
        main.execute(() ->
                required.onAttendanceManagementStateChanged(snapshot));
    }

    public void unsubscribe(Listener listener) {
        listeners.remove(listener);
    }

    public void open(AttendanceConversation conversation) {
        AttendanceConversation required = Objects.requireNonNull(
                conversation,
                "Attendance conversation is required.");
        busy.set(true);
        long operation = generation.incrementAndGet();
        publish(AttendanceManagementState.loading());
        worker.execute(() -> loadDirectory(required, operation));
    }

    public void save(
            AttendanceManagementSelection selection,
            String idempotencyKey) {
        Optional<AttendanceManagementState.Snapshot> snapshot =
                current.snapshot();
        if (snapshot.isEmpty()) {
            publish(AttendanceManagementState.error(
                    Optional.empty(),
                    invalidRequest("Attendance management is not ready.")));
            return;
        }
        if (selection == null) {
            publish(AttendanceManagementState.error(
                    snapshot,
                    invalidRequest(
                            "Attendance management request is invalid.")));
            return;
        }
        final AttendanceManagementSelection required;
        final String key;
        try {
            required = selection;
            key = AttendanceIdempotencyKey.require(idempotencyKey);
        } catch (IllegalArgumentException failure) {
            publish(AttendanceManagementState.error(
                    snapshot,
                    invalidRequest(
                            "Attendance management request is invalid.",
                            failure)));
            return;
        }
        if (!busy.compareAndSet(false, true)) {
            return;
        }
        AttendanceManagementState.Snapshot value =
                snapshot.orElseThrow();
        long operation = generation.incrementAndGet();
        publish(AttendanceManagementState.saving(value));
        worker.execute(() -> save(
                operation,
                value,
                required,
                key));
    }

    public void close() {
        generation.incrementAndGet();
        busy.set(false);
        publish(AttendanceManagementState.closed());
        listeners.clear();
    }

    private void loadDirectory(
            AttendanceConversation conversation,
            long operation) {
        try {
            List<AttendanceAssignee> items = new ArrayList<>();
            Set<Long> identifiers = new HashSet<>();
            Set<String> cursors = new HashSet<>();
            Optional<String> cursor = Optional.empty();
            for (int pageIndex = 0;
                    pageIndex < MAXIMUM_DIRECTORY_PAGES;
                    pageIndex++) {
                AttendanceAssigneePage page = repository.assignees(
                        conversation.id(),
                        Optional.empty(),
                        cursor,
                        DIRECTORY_PAGE_SIZE);
                append(items, identifiers, page.items());
                if (page.nextCursor().isEmpty()) {
                    ready(operation, AttendanceManagementState.Snapshot.from(
                            conversation,
                            items));
                    return;
                }
                String next = page.nextCursor().orElseThrow();
                if (!cursors.add(next)
                        || cursor.filter(next::equals).isPresent()) {
                    throw protocol(
                            "Attendance assignee pagination did not progress.");
                }
                cursor = Optional.of(next);
            }
            throw protocol(
                    "Attendance assignee directory exceeds the safe limit.");
        } catch (AttendanceException failure) {
            failed(operation, Optional.empty(), failure);
        } catch (IllegalArgumentException failure) {
            failed(
                    operation,
                    Optional.empty(),
                    protocol(
                            "Attendance assignee response is invalid.",
                            failure));
        }
    }

    private void save(
            long operation,
            AttendanceManagementState.Snapshot snapshot,
            AttendanceManagementSelection selection,
            String key) {
        try {
            AttendanceManagementResult result = repository.manage(
                    snapshot.conversationId(),
                    snapshot.revision(),
                    selection,
                    key);
            ready(operation, snapshot.managed(result), true);
        } catch (AttendanceException failure) {
            failed(operation, Optional.of(snapshot), failure);
        } catch (IllegalArgumentException failure) {
            failed(
                    operation,
                    Optional.of(snapshot),
                    protocol(
                            "Attendance management response is invalid.",
                            failure));
        }
    }

    private void ready(
            long operation,
            AttendanceManagementState.Snapshot snapshot) {
        ready(operation, snapshot, false);
    }

    private void ready(
            long operation,
            AttendanceManagementState.Snapshot snapshot,
            boolean saved) {
        if (!isCurrent(operation)) {
            return;
        }
        busy.set(false);
        publish(saved
                ? AttendanceManagementState.success(snapshot)
                : AttendanceManagementState.ready(snapshot));
    }

    private void failed(
            long operation,
            Optional<AttendanceManagementState.Snapshot> fallback,
            AttendanceException failure) {
        if (!isCurrent(operation)) {
            return;
        }
        busy.set(false);
        publish(AttendanceManagementState.error(fallback, failure));
    }

    private boolean isCurrent(long operation) {
        return generation.get() == operation
                && current.phase()
                        != AttendanceManagementState.Phase.CLOSED;
    }

    private void publish(AttendanceManagementState state) {
        current = state;
        main.execute(() -> listeners.forEach(listener ->
                listener.onAttendanceManagementStateChanged(state)));
    }

    private static void append(
            List<AttendanceAssignee> target,
            Set<Long> identifiers,
            List<AttendanceAssignee> items) throws AttendanceException {
        for (AttendanceAssignee item : items) {
            if (!identifiers.add(item.id())) {
                throw protocol(
                        "Attendance assignee directory contains duplicates.");
            }
            target.add(item);
        }
    }

    private static AttendanceException invalidRequest(String message) {
        return new AttendanceException(
                AttendanceFailureKind.INVALID_REQUEST,
                message);
    }

    private static AttendanceException invalidRequest(
            String message,
            Throwable cause) {
        return new AttendanceException(
                AttendanceFailureKind.INVALID_REQUEST,
                message,
                cause);
    }

    private static AttendanceException protocol(String message) {
        return new AttendanceException(
                AttendanceFailureKind.PROTOCOL,
                message);
    }

    private static AttendanceException protocol(
            String message,
            Throwable cause) {
        return new AttendanceException(
                AttendanceFailureKind.PROTOCOL,
                message,
                cause);
    }
}
