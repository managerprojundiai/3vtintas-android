package br.com.tresvtintas.mobile.core.attendance;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;

public record AttendanceConversation(
        String id,
        AttendanceChannel channel,
        String sourceId,
        int revision,
        Optional<Organization> organization,
        Customer customer,
        Optional<AssignedUser> assignedUser,
        AttendanceFolder folder,
        AttendancePriority priority,
        AttendanceHandlingMode handlingMode,
        String state,
        Stats stats,
        Optional<LastMessage> lastMessage,
        Instant activityAt,
        Instant updatedAt) {
    private static final long MINIMUM_ID = 1;
    private static final int MINIMUM_REVISION = 1;

    public AttendanceConversation {
        id = required(id, "Attendance ID", 200);
        Objects.requireNonNull(channel, "Attendance channel is required.");
        sourceId = required(sourceId, "Attendance source ID", 160);
        if (revision < MINIMUM_REVISION) {
            throw new IllegalArgumentException(
                    "Attendance revision is invalid.");
        }
        organization = Objects.requireNonNull(
                organization,
                "Attendance organization is required.");
        Objects.requireNonNull(customer, "Attendance customer is required.");
        assignedUser = Objects.requireNonNull(
                assignedUser,
                "Attendance assigned user is required.");
        Objects.requireNonNull(folder, "Attendance folder is required.");
        Objects.requireNonNull(priority, "Attendance priority is required.");
        Objects.requireNonNull(
                handlingMode,
                "Attendance handling mode is required.");
        state = required(state, "Attendance state", 80);
        Objects.requireNonNull(stats, "Attendance statistics are required.");
        lastMessage = Objects.requireNonNull(
                lastMessage,
                "Attendance last message is required.");
        Objects.requireNonNull(
                activityAt,
                "Attendance activity is required.");
        Objects.requireNonNull(
                updatedAt,
                "Attendance update is required.");
    }

    public AttendanceConversation withUnreadCount(int unreadCount) {
        return new AttendanceConversation(
                id,
                channel,
                sourceId,
                revision,
                organization,
                customer,
                assignedUser,
                folder,
                priority,
                handlingMode,
                state,
                new Stats(
                        stats.inboundCount(),
                        stats.outboundCount(),
                        unreadCount),
                lastMessage,
                activityAt,
                updatedAt);
    }

    public record Organization(long id, String name) {
        public Organization {
            if (id < MINIMUM_ID) {
                throw new IllegalArgumentException(
                        "Attendance organization ID is invalid.");
            }
            name = required(
                    name,
                    "Attendance organization name",
                    200);
        }
    }

    public record Customer(
            OptionalLong id,
            Optional<String> displayName) {
        public Customer {
            id = Objects.requireNonNull(
                    id,
                    "Attendance customer ID is required.");
            if (id.isPresent()
                    && id.getAsLong() < MINIMUM_ID) {
                throw new IllegalArgumentException(
                        "Attendance customer ID is invalid.");
            }
            displayName = boundedOptional(
                    displayName,
                    "Attendance customer name",
                    500);
        }
    }

    public record AssignedUser(
            long id,
            Optional<String> name,
            boolean assignedToCurrentActor) {
        public AssignedUser {
            if (id < MINIMUM_ID) {
                throw new IllegalArgumentException(
                        "Attendance assigned user ID is invalid.");
            }
            name = boundedOptional(
                    name,
                    "Attendance assigned user name",
                    500);
        }
    }

    public record Stats(
            int inboundCount,
            int outboundCount,
            int unreadCount) {
        public Stats {
            if (inboundCount < 0
                    || outboundCount < 0
                    || unreadCount < 0) {
                throw new IllegalArgumentException(
                        "Attendance statistics are invalid.");
            }
        }
    }

    public record LastMessage(
            AttendanceMessageDirection direction,
            String type,
            String preview,
            Optional<String> status,
            Instant createdAt) {
        public LastMessage {
            Objects.requireNonNull(
                    direction,
                    "Attendance message direction is required.");
            type = required(type, "Attendance message type", 80);
            if (preview == null || preview.length() > 240) {
                throw new IllegalArgumentException(
                        "Attendance message preview is invalid.");
            }
            status = boundedOptional(
                    status,
                    "Attendance message status",
                    80);
            Objects.requireNonNull(
                    createdAt,
                    "Attendance message creation is required.");
        }
    }

    private static String required(
            String value,
            String field,
            int maximumLength) {
        if (value == null
                || value.isBlank()
                || value.length() > maximumLength) {
            throw new IllegalArgumentException(field + " is invalid.");
        }
        return value;
    }

    private static Optional<String> boundedOptional(
            Optional<String> value,
            String field,
            int maximumLength) {
        Optional<String> required = Objects.requireNonNull(
                value,
                field + " is required.");
        if (required.map(String::length).orElse(0) > maximumLength) {
            throw new IllegalArgumentException(field + " is invalid.");
        }
        return required;
    }
}
