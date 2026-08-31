package br.com.tresvtintas.mobile.data.attendance;

import br.com.tresvtintas.mobile.core.attendance.AttendanceChannel;
import br.com.tresvtintas.mobile.core.attendance.AttendanceAssignee;
import br.com.tresvtintas.mobile.core.attendance.AttendanceAssigneePage;
import br.com.tresvtintas.mobile.core.attendance.AttendanceConversation;
import br.com.tresvtintas.mobile.core.attendance.AttendanceFolder;
import br.com.tresvtintas.mobile.core.attendance.AttendanceHandlingMode;
import br.com.tresvtintas.mobile.core.attendance.AttendanceMessage;
import br.com.tresvtintas.mobile.core.attendance.AttendanceMessageDirection;
import br.com.tresvtintas.mobile.core.attendance.AttendanceMessagePage;
import br.com.tresvtintas.mobile.core.attendance.AttendanceManagementResult;
import br.com.tresvtintas.mobile.core.attendance.AttendancePage;
import br.com.tresvtintas.mobile.core.attendance.AttendancePriority;
import br.com.tresvtintas.mobile.core.attendance.AttendanceReplyDeliveryState;
import br.com.tresvtintas.mobile.core.attendance.AttendanceReplyResult;
import br.com.tresvtintas.mobile.core.attendance.AttendanceReadCursorResult;
import br.com.tresvtintas.mobile.core.network.dto.AttendanceConversationDto;
import br.com.tresvtintas.mobile.core.network.dto.AttendanceAssigneePageDto;
import br.com.tresvtintas.mobile.core.network.dto.AttendanceManagementResponse;
import br.com.tresvtintas.mobile.core.network.dto.AttendanceMessageDto;
import br.com.tresvtintas.mobile.core.network.dto.AttendanceMessagePageDto;
import br.com.tresvtintas.mobile.core.network.dto.AttendancePageDto;
import br.com.tresvtintas.mobile.core.network.dto.AttendanceReplyResponse;
import br.com.tresvtintas.mobile.core.network.dto.AttendanceReadCursorResponse;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalLong;

final class AttendanceDtoMapper {
    private AttendanceDtoMapper() {
        throw new AssertionError("No instances.");
    }

    static AttendancePage page(AttendancePageDto value) {
        return new AttendancePage(
                value.items().stream()
                        .map(AttendanceDtoMapper::conversation)
                        .toList(),
                Optional.ofNullable(value.nextCursor()));
    }

    static AttendanceConversation conversation(
            AttendanceConversationDto value) {
        return new AttendanceConversation(
                value.id(),
                enumValue(AttendanceChannel.class, value.channel()),
                value.sourceId(),
                value.revision(),
                Optional.ofNullable(value.organization()).map(item ->
                        new AttendanceConversation.Organization(
                                item.id(),
                                item.name())),
                new AttendanceConversation.Customer(
                        optionalLong(value.customer().id()),
                        Optional.ofNullable(
                                value.customer().displayName())),
                Optional.ofNullable(value.assignedUser()).map(item ->
                        new AttendanceConversation.AssignedUser(
                                item.id(),
                                Optional.ofNullable(item.name()),
                                item.assignedToCurrentActor())),
                enumValue(AttendanceFolder.class, folder(value.folder())),
                enumValue(
                        AttendancePriority.class,
                        value.priority()),
                enumValue(
                        AttendanceHandlingMode.class,
                        value.handlingMode()),
                value.state(),
                new AttendanceConversation.Stats(
                        value.stats().inboundCount(),
                        value.stats().outboundCount(),
                        value.stats().unreadCount()),
                Optional.ofNullable(value.lastMessage()).map(item ->
                        new AttendanceConversation.LastMessage(
                                enumValue(
                                        AttendanceMessageDirection.class,
                                        item.direction()),
                                item.type(),
                                item.preview(),
                                Optional.ofNullable(item.status()),
                                Instant.parse(item.createdAt()))),
                Instant.parse(value.activityAt()),
                Instant.parse(value.updatedAt()));
    }

    static AttendanceAssigneePage assigneePage(
            AttendanceAssigneePageDto value) {
        return new AttendanceAssigneePage(
                value.items().stream()
                        .map(item -> new AttendanceAssignee(
                                item.id(),
                                item.displayName()))
                        .toList(),
                Optional.ofNullable(value.nextCursor()));
    }

    static AttendanceManagementResult management(
            AttendanceManagementResponse value,
            boolean replayed) {
        return new AttendanceManagementResult(
                value.conversationId(),
                value.revision(),
                enumValue(AttendanceFolder.class, folder(value.folder())),
                enumValue(AttendancePriority.class, value.priority()),
                Optional.ofNullable(value.assignedUser()).map(item ->
                        new AttendanceConversation.AssignedUser(
                                item.id(),
                                Optional.ofNullable(item.name()),
                                item.assignedToCurrentActor())),
                Instant.parse(value.updatedAt()),
                replayed);
    }

    static AttendanceMessagePage messagePage(
            AttendanceMessagePageDto value) {
        return new AttendanceMessagePage(
                value.conversationId(),
                value.items().stream()
                        .map(AttendanceDtoMapper::message)
                        .toList(),
                Optional.ofNullable(value.nextCursor()));
    }

    static AttendanceReplyResult reply(
            AttendanceReplyResponse value,
            boolean replayed) {
        return new AttendanceReplyResult(
                value.conversationId(),
                message(value.message()),
                enumValue(
                        AttendanceReplyDeliveryState.class,
                        value.deliveryState()),
                replayed);
    }

    static AttendanceReadCursorResult readCursor(
            AttendanceReadCursorResponse value) {
        return new AttendanceReadCursorResult(
                value.conversationId(),
                value.readThroughMessageId(),
                value.unreadCount(),
                Instant.parse(value.updatedAt()));
    }

    private static AttendanceMessage message(
            AttendanceMessageDto value) {
        return new AttendanceMessage(
                value.id(),
                value.sourceId(),
                enumValue(
                        AttendanceMessageDirection.class,
                        value.direction()),
                value.type(),
                value.content(),
                value.truncated(),
                Optional.ofNullable(value.status()),
                Instant.parse(value.createdAt()));
    }

    private static String folder(String value) {
        return "fornecedores".equals(value)
                ? "suppliers"
                : value;
    }

    private static OptionalLong optionalLong(Long value) {
        return value == null
                ? OptionalLong.empty()
                : OptionalLong.of(value);
    }

    private static <T extends Enum<T>> T enumValue(
            Class<T> type,
            String value) {
        return Enum.valueOf(
                type,
                value.toUpperCase(Locale.ROOT));
    }
}
