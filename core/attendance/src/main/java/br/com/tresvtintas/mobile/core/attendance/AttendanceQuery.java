package br.com.tresvtintas.mobile.core.attendance;

import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;

public record AttendanceQuery(
        OptionalLong organizationId,
        Optional<String> search,
        Optional<AttendanceChannel> channel,
        Optional<AttendanceFolder> folder,
        Optional<AttendancePriority> priority,
        AttendanceAssignment assignment,
        int pageSize) {
    private static final int DEFAULT_PAGE_SIZE = 30;
    private static final int MAXIMUM_PAGE_SIZE = 100;
    private static final int MAXIMUM_SEARCH_LENGTH = 80;

    public AttendanceQuery {
        organizationId = Objects.requireNonNull(
                organizationId,
                "Attendance organization is required.");
        if ((organizationId.isPresent()
                        && organizationId.getAsLong() < 1)
                || pageSize < 1
                || pageSize > MAXIMUM_PAGE_SIZE) {
            throw new IllegalArgumentException(
                    "Attendance query is invalid.");
        }
        search = Objects.requireNonNull(
                        search,
                        "Attendance search is required.")
                .map(String::trim)
                .filter(value -> !value.isEmpty());
        if (search.map(String::length).orElse(0)
                > MAXIMUM_SEARCH_LENGTH) {
            throw new IllegalArgumentException(
                    "Attendance search is too long.");
        }
        channel = Objects.requireNonNull(
                channel,
                "Attendance channel is required.");
        folder = Objects.requireNonNull(
                folder,
                "Attendance folder is required.");
        priority = Objects.requireNonNull(
                priority,
                "Attendance priority is required.");
        Objects.requireNonNull(
                assignment,
                "Attendance assignment is required.");
    }

    public static AttendanceQuery initial(
            OptionalLong organizationId,
            boolean assignedOnly) {
        return new AttendanceQuery(
                organizationId,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                assignedOnly
                        ? AttendanceAssignment.MINE
                        : AttendanceAssignment.ALL,
                DEFAULT_PAGE_SIZE);
    }

    public AttendanceQuery withSearch(String value) {
        return copy(
                Optional.ofNullable(value),
                channel,
                folder,
                priority,
                assignment);
    }

    public AttendanceQuery withChannel(
            Optional<AttendanceChannel> value) {
        return copy(search, value, folder, priority, assignment);
    }

    public AttendanceQuery withFolder(
            Optional<AttendanceFolder> value) {
        return copy(search, channel, value, priority, assignment);
    }

    public AttendanceQuery withPriority(
            Optional<AttendancePriority> value) {
        return copy(search, channel, folder, value, assignment);
    }

    public AttendanceQuery withAssignment(
            AttendanceAssignment value) {
        return copy(search, channel, folder, priority, value);
    }

    private AttendanceQuery copy(
            Optional<String> nextSearch,
            Optional<AttendanceChannel> nextChannel,
            Optional<AttendanceFolder> nextFolder,
            Optional<AttendancePriority> nextPriority,
            AttendanceAssignment nextAssignment) {
        return new AttendanceQuery(
                organizationId,
                nextSearch,
                nextChannel,
                nextFolder,
                nextPriority,
                nextAssignment,
                pageSize);
    }
}
