package br.com.tresvtintas.mobile.core.network.dto;

import java.util.Set;

public record AttendanceConversationDto(
        String id,
        String channel,
        String sourceId,
        int revision,
        Organization organization,
        Customer customer,
        AssignedUser assignedUser,
        String folder,
        String priority,
        String handlingMode,
        String state,
        Stats stats,
        LastMessage lastMessage,
        String activityAt,
        String updatedAt) {
    private static final Set<String> CHANNELS =
            Set.of("whatsapp", "site_chat");
    private static final Set<String> FOLDERS = Set.of(
            "inbox",
            "mine",
            "unassigned",
            "urgent",
            "follow_up",
            "fornecedores",
            "vip",
            "resolved");
    private static final Set<String> PRIORITIES =
            Set.of("low", "normal", "high", "urgent");
    private static final Set<String> HANDLING_MODES =
            Set.of("ai", "human");
    private static final int MINIMUM_REVISION = 1;

    public AttendanceConversationDto {
        id = DtoValidation.requireText(id, "Attendance ID", 200);
        sourceId = DtoValidation.requireText(
                sourceId,
                "Attendance source ID",
                160);
        if (revision < MINIMUM_REVISION) {
            throw new IllegalArgumentException(
                    "Attendance revision is invalid.");
        }
        state = DtoValidation.requireText(
                state,
                "Attendance state",
                80);
        activityAt = DtoValidation.requireInstant(
                activityAt,
                "Attendance activity");
        updatedAt = DtoValidation.requireInstant(
                updatedAt,
                "Attendance update");
        if (!CHANNELS.contains(channel)
                || !FOLDERS.contains(folder)
                || !PRIORITIES.contains(priority)
                || !HANDLING_MODES.contains(handlingMode)
                || customer == null
                || stats == null) {
            throw new IllegalArgumentException(
                    "Attendance conversation is invalid.");
        }
    }

    public record Organization(long id, String name) {
        public Organization {
            id = DtoValidation.requirePositive(
                    id,
                    "Attendance organization ID");
            name = DtoValidation.requireText(
                    name,
                    "Attendance organization name",
                    200);
        }
    }

    public record Customer(Long id, String displayName) {
        public Customer {
            id = DtoValidation.optionalPositive(
                    id,
                    "Attendance customer ID");
            displayName = DtoValidation.optionalText(
                    displayName,
                    "Attendance customer name",
                    500);
        }
    }

    public record AssignedUser(
            long id,
            String name,
            boolean assignedToCurrentActor) {
        public AssignedUser {
            id = DtoValidation.requirePositive(
                    id,
                    "Attendance assigned user ID");
            name = DtoValidation.optionalText(
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
            String direction,
            String type,
            String preview,
            String status,
            String createdAt) {
        public LastMessage {
            if (!Set.of("inbound", "outbound").contains(direction)) {
                throw new IllegalArgumentException(
                        "Attendance message direction is invalid.");
            }
            type = DtoValidation.requireText(
                    type,
                    "Attendance message type",
                    80);
            preview = DtoValidation.optionalText(
                    preview,
                    "Attendance message preview",
                    240);
            if (preview == null) {
                throw new IllegalArgumentException(
                        "Attendance message preview is required.");
            }
            status = DtoValidation.optionalText(
                    status,
                    "Attendance message status",
                    80);
            createdAt = DtoValidation.requireInstant(
                    createdAt,
                    "Attendance message creation");
        }
    }
}
