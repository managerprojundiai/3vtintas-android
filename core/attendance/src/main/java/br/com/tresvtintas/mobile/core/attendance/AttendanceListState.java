package br.com.tresvtintas.mobile.core.attendance;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record AttendanceListState(
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

    public AttendanceListState {
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

    public static AttendanceListState empty() {
        return value(
                Phase.EMPTY,
                Optional.empty(),
                Optional.empty(),
                Optional.empty());
    }

    public static AttendanceListState loading() {
        return value(
                Phase.LOADING,
                Optional.empty(),
                Optional.empty(),
                Optional.empty());
    }

    public static AttendanceListState refreshing(Snapshot snapshot) {
        return value(
                Phase.REFRESHING,
                Optional.of(snapshot),
                Optional.empty(),
                Optional.empty());
    }

    public static AttendanceListState loadingMore(Snapshot snapshot) {
        return value(
                Phase.LOADING_MORE,
                Optional.of(snapshot),
                Optional.empty(),
                Optional.empty());
    }

    public static AttendanceListState ready(Snapshot snapshot) {
        return value(
                Phase.READY,
                Optional.of(snapshot),
                Optional.empty(),
                Optional.empty());
    }

    public static AttendanceListState warning(
            Snapshot snapshot,
            AttendanceException failure) {
        return value(
                Phase.READY,
                Optional.of(snapshot),
                Optional.of(failure.kind()),
                failure.requestId());
    }

    public static AttendanceListState error(
            AttendanceException failure) {
        return value(
                Phase.ERROR,
                Optional.empty(),
                Optional.of(failure.kind()),
                failure.requestId());
    }

    public static AttendanceListState closed() {
        return value(
                Phase.CLOSED,
                Optional.empty(),
                Optional.empty(),
                Optional.empty());
    }

    private static AttendanceListState value(
            Phase phase,
            Optional<Snapshot> snapshot,
            Optional<AttendanceFailureKind> failure,
            Optional<String> requestId) {
        return new AttendanceListState(
                phase,
                snapshot,
                failure,
                requestId);
    }

    public record Snapshot(
            AttendanceQuery query,
            List<AttendanceConversation> items,
            Optional<String> nextCursor) {
        public Snapshot {
            Objects.requireNonNull(query, "Attendance query is required.");
            if (items == null
                    || items.stream().anyMatch(Objects::isNull)) {
                throw new IllegalArgumentException(
                        "Attendance items are invalid.");
            }
            items = List.copyOf(items);
            nextCursor = Objects.requireNonNull(
                    nextCursor,
                    "Attendance cursor is required.");
        }

        public Snapshot append(AttendancePage page) {
            List<AttendanceConversation> combined =
                    new ArrayList<>(items);
            combined.addAll(page.items());
            return new Snapshot(
                    query,
                    combined,
                    page.nextCursor());
        }

        public boolean hasMore() {
            return nextCursor.isPresent();
        }
    }
}
