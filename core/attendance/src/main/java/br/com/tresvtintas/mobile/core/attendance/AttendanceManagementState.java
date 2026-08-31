package br.com.tresvtintas.mobile.core.attendance;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;

public record AttendanceManagementState(
        Phase phase,
        Optional<Snapshot> snapshot,
        Optional<AttendanceFailureKind> failure,
        Optional<String> requestId) {
    public AttendanceManagementState {
        Objects.requireNonNull(phase, "Attendance management phase is required.");
        snapshot = Objects.requireNonNull(
                snapshot,
                "Attendance management snapshot is required.");
        failure = Objects.requireNonNull(
                failure,
                "Attendance management failure is required.");
        requestId = Objects.requireNonNull(
                requestId,
                "Attendance management request ID is required.");
        if ((phase == Phase.READY
                        || phase == Phase.SAVING
                        || phase == Phase.SUCCESS)
                && snapshot.isEmpty()) {
            throw new IllegalArgumentException(
                    "Attendance management state has no snapshot.");
        }
        if (phase == Phase.ERROR && failure.isEmpty()) {
            throw new IllegalArgumentException(
                    "Attendance management error has no failure.");
        }
    }

    public static AttendanceManagementState idle() {
        return state(Phase.IDLE);
    }

    public static AttendanceManagementState loading() {
        return state(Phase.LOADING);
    }

    public static AttendanceManagementState ready(Snapshot snapshot) {
        return withSnapshot(Phase.READY, snapshot);
    }

    public static AttendanceManagementState saving(Snapshot snapshot) {
        return withSnapshot(Phase.SAVING, snapshot);
    }

    public static AttendanceManagementState success(Snapshot snapshot) {
        return withSnapshot(Phase.SUCCESS, snapshot);
    }

    public static AttendanceManagementState error(
            Optional<Snapshot> snapshot,
            AttendanceException failure) {
        Objects.requireNonNull(failure, "Attendance failure is required.");
        return new AttendanceManagementState(
                Phase.ERROR,
                snapshot,
                Optional.of(failure.kind()),
                failure.requestId());
    }

    public static AttendanceManagementState closed() {
        return state(Phase.CLOSED);
    }

    private static AttendanceManagementState state(Phase phase) {
        return new AttendanceManagementState(
                phase,
                Optional.empty(),
                Optional.empty(),
                Optional.empty());
    }

    private static AttendanceManagementState withSnapshot(
            Phase phase,
            Snapshot snapshot) {
        return new AttendanceManagementState(
                phase,
                Optional.of(Objects.requireNonNull(
                        snapshot,
                        "Attendance management snapshot is required.")),
                Optional.empty(),
                Optional.empty());
    }

    public enum Phase {
        IDLE,
        LOADING,
        READY,
        SAVING,
        SUCCESS,
        ERROR,
        CLOSED
    }

    public record Snapshot(
            String conversationId,
            int revision,
            AttendanceFolder folder,
            AttendancePriority priority,
            Optional<AttendanceConversation.AssignedUser> assignedUser,
            List<AttendanceAssignee> assignees,
            boolean replayed) {
        public Snapshot {
            conversationId =
                    AttendanceConversationId.require(conversationId);
            if (revision < 1
                    || assignees == null
                    || assignees.stream().anyMatch(Objects::isNull)) {
                throw new IllegalArgumentException(
                        "Attendance management snapshot is invalid.");
            }
            Objects.requireNonNull(folder, "Attendance folder is required.");
            Objects.requireNonNull(
                    priority,
                    "Attendance priority is required.");
            assignedUser = Objects.requireNonNull(
                    assignedUser,
                    "Attendance assigned user is required.");
            assignees = List.copyOf(assignees);
        }

        public static Snapshot from(
                AttendanceConversation conversation,
                List<AttendanceAssignee> assignees) {
            Objects.requireNonNull(
                    conversation,
                    "Attendance conversation is required.");
            return new Snapshot(
                    conversation.id(),
                    conversation.revision(),
                    conversation.folder(),
                    conversation.priority(),
                    conversation.assignedUser(),
                    assignees,
                    false);
        }

        public AttendanceManagementSelection selection() {
            OptionalLong assigned = assignedUser
                    .map(value -> OptionalLong.of(value.id()))
                    .orElseGet(OptionalLong::empty);
            return new AttendanceManagementSelection(
                    folder,
                    priority,
                    assigned);
        }

        public Snapshot managed(AttendanceManagementResult result) {
            Objects.requireNonNull(result, "Attendance result is required.");
            if (!conversationId.equals(result.conversationId())
                    || result.revision() < revision) {
                throw new IllegalArgumentException(
                        "Attendance management result is inconsistent.");
            }
            return new Snapshot(
                    conversationId,
                    result.revision(),
                    result.folder(),
                    result.priority(),
                    result.assignedUser(),
                    assignees,
                    result.replayed());
        }
    }
}
