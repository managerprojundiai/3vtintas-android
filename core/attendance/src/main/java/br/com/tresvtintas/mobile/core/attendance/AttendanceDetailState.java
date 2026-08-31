package br.com.tresvtintas.mobile.core.attendance;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record AttendanceDetailState(
        Phase phase,
        Optional<Snapshot> snapshot,
        Optional<AttendanceFailureKind> failure,
        Optional<String> requestId) {
    public enum Phase {
        EMPTY,
        LOADING,
        REFRESHING,
        LOADING_MORE,
        READY,
        ERROR,
        CLOSED
    }

    public AttendanceDetailState {
        Objects.requireNonNull(phase, "Attendance phase is required.");
        snapshot = Objects.requireNonNull(
                snapshot,
                "Attendance snapshot is required.");
        failure = Objects.requireNonNull(
                failure,
                "Attendance failure is required.");
        requestId = Objects.requireNonNull(
                requestId,
                "Attendance request ID is required.");
    }

    public static AttendanceDetailState empty() {
        return value(
                Phase.EMPTY,
                Optional.empty(),
                Optional.empty(),
                Optional.empty());
    }

    public static AttendanceDetailState loading() {
        return value(
                Phase.LOADING,
                Optional.empty(),
                Optional.empty(),
                Optional.empty());
    }

    public static AttendanceDetailState refreshing(Snapshot snapshot) {
        return value(
                Phase.REFRESHING,
                Optional.of(snapshot),
                Optional.empty(),
                Optional.empty());
    }

    public static AttendanceDetailState loadingMore(Snapshot snapshot) {
        return value(
                Phase.LOADING_MORE,
                Optional.of(snapshot),
                Optional.empty(),
                Optional.empty());
    }

    public static AttendanceDetailState ready(Snapshot snapshot) {
        return value(
                Phase.READY,
                Optional.of(snapshot),
                Optional.empty(),
                Optional.empty());
    }

    public static AttendanceDetailState warning(
            Snapshot snapshot,
            AttendanceException failure) {
        return value(
                Phase.READY,
                Optional.of(snapshot),
                Optional.of(failure.kind()),
                failure.requestId());
    }

    public static AttendanceDetailState error(
            AttendanceException failure) {
        return value(
                Phase.ERROR,
                Optional.empty(),
                Optional.of(failure.kind()),
                failure.requestId());
    }

    public static AttendanceDetailState closed() {
        return value(
                Phase.CLOSED,
                Optional.empty(),
                Optional.empty(),
                Optional.empty());
    }

    private static AttendanceDetailState value(
            Phase phase,
            Optional<Snapshot> snapshot,
            Optional<AttendanceFailureKind> failure,
            Optional<String> requestId) {
        return new AttendanceDetailState(
                phase,
                snapshot,
                failure,
                requestId);
    }

    public record Snapshot(
            AttendanceConversation conversation,
            List<AttendanceMessage> messages,
            Optional<String> nextCursor) {
        public Snapshot {
            Objects.requireNonNull(
                    conversation,
                    "Attendance conversation is required.");
            if (messages == null
                    || messages.stream().anyMatch(Objects::isNull)) {
                throw new IllegalArgumentException(
                        "Attendance messages are invalid.");
            }
            messages = List.copyOf(messages);
            nextCursor = Objects.requireNonNull(
                    nextCursor,
                    "Attendance message cursor is required.");
        }

        public Snapshot append(AttendanceMessagePage page) {
            if (!conversation.id().equals(page.conversationId())) {
                throw new IllegalArgumentException(
                        "Attendance message page belongs to another conversation.");
            }
            List<AttendanceMessage> combined =
                    new ArrayList<>(messages);
            combined.addAll(page.items());
            return new Snapshot(
                    conversation,
                    combined,
                    page.nextCursor());
        }

        public boolean hasMore() {
            return nextCursor.isPresent();
        }
    }
}
