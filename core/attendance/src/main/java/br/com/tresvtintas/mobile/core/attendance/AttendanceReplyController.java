package br.com.tresvtintas.mobile.core.attendance;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public final class AttendanceReplyController {
    @FunctionalInterface
    public interface Listener {
        void onAttendanceReplyStateChanged(AttendanceReplyState state);
    }

    private final AttendanceRepository repository;
    private final Executor worker;
    private final Executor main;
    private final Set<Listener> listeners = new CopyOnWriteArraySet<>();
    private final AtomicBoolean busy = new AtomicBoolean();
    private final AtomicLong generation = new AtomicLong();
    private volatile AttendanceReplyState current =
            AttendanceReplyState.idle();

    public AttendanceReplyController(
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
                "Attendance reply listener is required.");
        listeners.add(required);
        AttendanceReplyState snapshot = current;
        main.execute(() ->
                required.onAttendanceReplyStateChanged(snapshot));
    }

    public void unsubscribe(Listener listener) {
        listeners.remove(listener);
    }

    public void send(
            String conversationId,
            String content,
            String idempotencyKey) {
        final String requiredConversationId;
        final String normalizedContent;
        try {
            requiredConversationId =
                    AttendanceConversationId.require(conversationId);
            normalizedContent = AttendanceReplyContent.normalize(content);
            AttendanceIdempotencyKey.require(idempotencyKey);
        } catch (IllegalArgumentException failure) {
            publish(AttendanceReplyState.error(new AttendanceException(
                    AttendanceFailureKind.INVALID_REQUEST,
                    "Attendance reply request is invalid.",
                    failure)));
            return;
        }
        if (!busy.compareAndSet(false, true)) {
            return;
        }
        long operation = generation.incrementAndGet();
        publish(AttendanceReplyState.sending());
        worker.execute(() -> {
            try {
                complete(operation, AttendanceReplyState.success(
                        repository.reply(
                                requiredConversationId,
                                normalizedContent,
                                idempotencyKey)));
            } catch (AttendanceException failure) {
                complete(
                        operation,
                        AttendanceReplyState.error(failure));
            }
        });
    }

    public void close() {
        generation.incrementAndGet();
        busy.set(false);
        publish(AttendanceReplyState.closed());
        listeners.clear();
    }

    private void complete(
            long operation,
            AttendanceReplyState state) {
        if (generation.get() != operation
                || current.phase() == AttendanceReplyState.Phase.CLOSED) {
            return;
        }
        busy.set(false);
        publish(state);
    }

    private void publish(AttendanceReplyState state) {
        current = state;
        main.execute(() -> listeners.forEach(listener ->
                listener.onAttendanceReplyStateChanged(state)));
    }

}
