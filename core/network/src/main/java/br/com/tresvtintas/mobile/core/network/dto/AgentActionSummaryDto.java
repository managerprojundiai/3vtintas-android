package br.com.tresvtintas.mobile.core.network.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public record AgentActionSummaryDto(
        Long quoteId,
        String quoteTitle,
        String customerName,
        String notes,
        String validUntil,
        Integer itemCount,
        List<AgentActionItemDto> items,
        String subtotal,
        String total,
        String targetStatus,
        boolean pricingWillBeRevalidated,
        Integer expectedRevision,
        List<AgentActionChangeDto> changes,
        AgentActionSnapshotDto before,
        AgentActionSnapshotDto after,
        String conversationId,
        String channel,
        String organizationName,
        String content,
        AgentAttendanceLatestMessageDto latestMessage,
        Boolean accessAndRevisionWillBeRevalidated,
        AgentAppointmentSummaryDto appointment,
        AgentDeliverySummaryDto delivery,
        AgentOrderSummaryDto order,
        AgentFinanceSummaryDto finance,
        AgentCommissionSummaryDto commission) {
    private static final String MATERIAL_QUOTE_SEND =
            "material_quote_send";
    private static final String MATERIAL_QUOTE_CREATE =
            "material_quote_create";
    private static final String MATERIAL_QUOTE_AMEND =
            "material_quote_amend";
    private static final String ATTENDANCE_REPLY =
            "attendance_reply";
    private static final Set<String> ATTENDANCE_CHANNELS =
            Set.of("whatsapp", "site_chat");
    private static final Pattern ATTENDANCE_CONVERSATION =
            Pattern.compile(
                    "^(?:whatsapp:[1-9]\\d*|site_chat:[^/\\u0000-\\u001f\\u007f]{1,160})$");

    public AgentActionSummaryDto(
            Long quoteId,
            String quoteTitle,
            String customerName,
            String notes,
            String validUntil,
            Integer itemCount,
            List<AgentActionItemDto> items,
            String subtotal,
            String total,
            String targetStatus,
            boolean pricingWillBeRevalidated,
            Integer expectedRevision,
            List<AgentActionChangeDto> changes,
            AgentActionSnapshotDto before,
            AgentActionSnapshotDto after) {
        this(
                quoteId,
                quoteTitle,
                customerName,
                notes,
                validUntil,
                itemCount,
                items,
                subtotal,
                total,
                targetStatus,
                pricingWillBeRevalidated,
                expectedRevision,
                changes,
                before,
                after,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null);
    }

    public AgentActionSummaryDto(
            Long quoteId,
            String quoteTitle,
            String customerName,
            String notes,
            String validUntil,
            Integer itemCount,
            List<AgentActionItemDto> items,
            String subtotal,
            String total,
            String targetStatus,
            boolean pricingWillBeRevalidated,
            Integer expectedRevision,
            List<AgentActionChangeDto> changes,
            AgentActionSnapshotDto before,
            AgentActionSnapshotDto after,
            String conversationId,
            String channel,
            String organizationName,
            String content,
            AgentAttendanceLatestMessageDto latestMessage,
            Boolean accessAndRevisionWillBeRevalidated) {
        this(
                quoteId,
                quoteTitle,
                customerName,
                notes,
                validUntil,
                itemCount,
                items,
                subtotal,
                total,
                targetStatus,
                pricingWillBeRevalidated,
                expectedRevision,
                changes,
                before,
                after,
                conversationId,
                channel,
                organizationName,
                content,
                latestMessage,
                accessAndRevisionWillBeRevalidated,
                null,
                null,
                null);
    }

    public AgentActionSummaryDto(
            Long quoteId,
            String quoteTitle,
            String customerName,
            String notes,
            String validUntil,
            Integer itemCount,
            List<AgentActionItemDto> items,
            String subtotal,
            String total,
            String targetStatus,
            boolean pricingWillBeRevalidated,
            Integer expectedRevision,
            List<AgentActionChangeDto> changes,
            AgentActionSnapshotDto before,
            AgentActionSnapshotDto after,
            String conversationId,
            String channel,
            String organizationName,
            String content,
            AgentAttendanceLatestMessageDto latestMessage,
            Boolean accessAndRevisionWillBeRevalidated,
            AgentAppointmentSummaryDto appointment) {
        this(
                quoteId,
                quoteTitle,
                customerName,
                notes,
                validUntil,
                itemCount,
                items,
                subtotal,
                total,
                targetStatus,
                pricingWillBeRevalidated,
                expectedRevision,
                changes,
                before,
                after,
                conversationId,
                channel,
                organizationName,
                content,
                latestMessage,
                accessAndRevisionWillBeRevalidated,
                appointment,
                null,
                null);
    }

    public AgentActionSummaryDto(
            Long quoteId,
            String quoteTitle,
            String customerName,
            String notes,
            String validUntil,
            Integer itemCount,
            List<AgentActionItemDto> items,
            String subtotal,
            String total,
            String targetStatus,
            boolean pricingWillBeRevalidated,
            Integer expectedRevision,
            List<AgentActionChangeDto> changes,
            AgentActionSnapshotDto before,
            AgentActionSnapshotDto after,
            String conversationId,
            String channel,
            String organizationName,
            String content,
            AgentAttendanceLatestMessageDto latestMessage,
            Boolean accessAndRevisionWillBeRevalidated,
            AgentAppointmentSummaryDto appointment,
            AgentDeliverySummaryDto delivery) {
        this(
                quoteId,
                quoteTitle,
                customerName,
                notes,
                validUntil,
                itemCount,
                items,
                subtotal,
                total,
                targetStatus,
                pricingWillBeRevalidated,
                expectedRevision,
                changes,
                before,
                after,
                conversationId,
                channel,
                organizationName,
                content,
                latestMessage,
                accessAndRevisionWillBeRevalidated,
                appointment,
                delivery,
                null);
    }

    public AgentActionSummaryDto(
            Long quoteId,
            String quoteTitle,
            String customerName,
            String notes,
            String validUntil,
            Integer itemCount,
            List<AgentActionItemDto> items,
            String subtotal,
            String total,
            String targetStatus,
            boolean pricingWillBeRevalidated,
            Integer expectedRevision,
            List<AgentActionChangeDto> changes,
            AgentActionSnapshotDto before,
            AgentActionSnapshotDto after,
            String conversationId,
            String channel,
            String organizationName,
            String content,
            AgentAttendanceLatestMessageDto latestMessage,
            Boolean accessAndRevisionWillBeRevalidated,
            AgentAppointmentSummaryDto appointment,
            AgentDeliverySummaryDto delivery,
            AgentOrderSummaryDto order) {
        this(
                quoteId,
                quoteTitle,
                customerName,
                notes,
                validUntil,
                itemCount,
                items,
                subtotal,
                total,
                targetStatus,
                pricingWillBeRevalidated,
                expectedRevision,
                changes,
                before,
                after,
                conversationId,
                channel,
                organizationName,
                content,
                latestMessage,
                accessAndRevisionWillBeRevalidated,
                appointment,
                delivery,
                order,
                null,
                null);
    }

    public AgentActionSummaryDto {
        quoteId = DtoValidation.optionalPositive(
                quoteId,
                "Agent action quote ID");
        quoteTitle = DtoValidation.optionalText(
                quoteTitle,
                "Agent action quote title",
                200);
        customerName = DtoValidation.optionalText(
                customerName,
                "Agent action customer",
                500);
        notes = DtoValidation.optionalText(
                notes,
                "Agent action notes",
                4_000);
        if (items == null) {
            items = List.of();
        } else {
            items = List.copyOf(items);
        }
        if (changes == null) {
            changes = List.of();
        } else {
            changes = List.copyOf(changes);
        }
        organizationName = DtoValidation.optionalText(
                organizationName,
                "Agent attendance organization",
                200);
        content = DtoValidation.optionalText(
                content,
                "Agent attendance content",
                3_500);
        if (expectedRevision != null && expectedRevision < 1) {
            throw new IllegalArgumentException(
                    "Agent action summary is invalid.");
        }
    }

    boolean validFor(String kind) {
        if (MATERIAL_QUOTE_SEND.equals(kind)) {
            return quoteId != null
                    && validQuoteCommon()
                    && notes == null
                    && validUntil == null
                    && itemCount == null
                    && items.isEmpty()
                    && subtotal == null
                    && "sent".equals(targetStatus)
                    && expectedRevision == null
                    && changes.isEmpty()
                    && before == null
                    && after == null
                    && noAttendanceFields()
                    && noAppointmentFields()
                    && noDeliveryFields()
                    && noOrderFields()
                    && noFinancialFields();
        }
        if (MATERIAL_QUOTE_CREATE.equals(kind)) {
            return quoteId == null
                    && validQuoteCommon()
                    && validDate(validUntil)
                    && validItems()
                    && validMoney(subtotal)
                    && subtotal.equals(total)
                    && "draft".equals(targetStatus)
                    && expectedRevision == null
                    && changes.isEmpty()
                    && before == null
                    && after == null
                    && noAttendanceFields()
                    && noAppointmentFields()
                    && noDeliveryFields()
                    && noOrderFields()
                    && noFinancialFields();
        }
        if (MATERIAL_QUOTE_AMEND.equals(kind)) {
            return quoteId != null
                    && validQuoteCommon()
                    && notes == null
                    && validUntil == null
                    && itemCount == null
                    && items.isEmpty()
                    && subtotal == null
                    && targetStatus == null
                    && expectedRevision != null
                    && !changes.isEmpty()
                    && changes.size() <= 100
                    && before != null
                    && after != null
                    && total.equals(after.total())
                    && noAttendanceFields()
                    && noAppointmentFields()
                    && noDeliveryFields()
                    && noOrderFields()
                    && noFinancialFields();
        }
        if (ATTENDANCE_REPLY.equals(kind)) {
            return customerName != null
                    && quoteId == null
                    && quoteTitle == null
                    && notes == null
                    && validUntil == null
                    && itemCount == null
                    && items.isEmpty()
                    && subtotal == null
                    && total == null
                    && targetStatus == null
                    && !pricingWillBeRevalidated
                    && expectedRevision != null
                    && changes.isEmpty()
                    && before == null
                    && after == null
                    && conversationId != null
                    && ATTENDANCE_CONVERSATION
                            .matcher(conversationId)
                            .matches()
                    && ATTENDANCE_CHANNELS.contains(channel)
                    && content != null
                    && !content.isBlank()
                    && Boolean.TRUE.equals(
                            accessAndRevisionWillBeRevalidated)
                    && noAppointmentFields()
                    && noDeliveryFields()
                    && noOrderFields()
                    && noFinancialFields();
        }
        if (Set.of(
                        "appointment_create",
                        "appointment_reschedule",
                        "appointment_cancel")
                .contains(kind)
                && quoteId == null
                && quoteTitle == null
                && customerName == null
                && notes == null
                && validUntil == null
                && itemCount == null
                && items.isEmpty()
                && subtotal == null
                && total == null
                && targetStatus == null
                && !pricingWillBeRevalidated
                && expectedRevision == null
                && changes.isEmpty()
                && before == null
                && after == null
                && noAttendanceFields()
                && appointment != null
                && appointment.operation().equals(
                        kind.substring("appointment_".length()))
                && noDeliveryFields()
                && noOrderFields()
                && noFinancialFields()) {
            return true;
        }
        if (Set.of("delivery_start", "delivery_complete")
                        .contains(kind)
                && quoteId == null
                && quoteTitle == null
                && customerName == null
                && notes == null
                && validUntil == null
                && itemCount == null
                && items.isEmpty()
                && subtotal == null
                && total == null
                && targetStatus == null
                && !pricingWillBeRevalidated
                && expectedRevision == null
                && changes.isEmpty()
                && before == null
                && after == null
                && noAttendanceFields()
                && noAppointmentFields()
                && delivery != null
                && delivery.operation().equals(
                        kind.substring("delivery_".length()))
                && noOrderFields()
                && noFinancialFields()) {
            return true;
        }
        if (Set.of(
                        "order_confirm",
                        "order_start_fulfillment",
                        "order_complete")
                .contains(kind)
                && quoteId == null
                && quoteTitle == null
                && customerName == null
                && notes == null
                && validUntil == null
                && itemCount == null
                && items.isEmpty()
                && subtotal == null
                && total == null
                && targetStatus == null
                && !pricingWillBeRevalidated
                && expectedRevision == null
                && changes.isEmpty()
                && before == null
                && after == null
                && noAttendanceFields()
                && noAppointmentFields()
                && noDeliveryFields()
                && order != null
                && order.operation().equals(
                        kind.substring("order_".length()))
                && noFinancialFields()) {
            return true;
        }
        if (Set.of(
                        "personal_finance_create",
                        "personal_finance_settle",
                        "personal_finance_cancel",
                        "corporate_finance_create",
                        "corporate_finance_settle",
                        "corporate_finance_cancel")
                .contains(kind)) {
            return noLegacyActionFields()
                    && finance != null
                    && commission == null
                    && kind.equals(
                            finance.scope()
                                    + "_finance_"
                                    + finance.operation());
        }
        return Set.of(
                        "commission_approve",
                        "commission_cancel",
                        "commission_pay")
                .contains(kind)
                && noLegacyActionFields()
                && finance == null
                && commission != null
                && kind.equals(
                        "commission_" + commission.operation());
    }

    private boolean validQuoteCommon() {
        return quoteTitle != null
                && !quoteTitle.isBlank()
                && customerName != null
                && customerName.length() <= 200
                && validMoney(total)
                && pricingWillBeRevalidated;
    }

    private boolean noAttendanceFields() {
        return conversationId == null
                && channel == null
                && organizationName == null
                && content == null
                && latestMessage == null
                && accessAndRevisionWillBeRevalidated == null;
    }

    private boolean noAppointmentFields() {
        return appointment == null;
    }

    private boolean noDeliveryFields() {
        return delivery == null;
    }

    private boolean noOrderFields() {
        return order == null;
    }

    private boolean noFinancialFields() {
        return finance == null && commission == null;
    }

    private boolean noLegacyActionFields() {
        return quoteId == null
                && quoteTitle == null
                && customerName == null
                && notes == null
                && validUntil == null
                && itemCount == null
                && items.isEmpty()
                && subtotal == null
                && total == null
                && targetStatus == null
                && !pricingWillBeRevalidated
                && expectedRevision == null
                && changes.isEmpty()
                && before == null
                && after == null
                && noAttendanceFields()
                && noAppointmentFields()
                && noDeliveryFields()
                && noOrderFields();
    }

    @Override
    public List<AgentActionItemDto> items() {
        return List.copyOf(items);
    }

    @Override
    public List<AgentActionChangeDto> changes() {
        return List.copyOf(changes);
    }

    private boolean validItems() {
        if (itemCount == null
                || itemCount < 1
                || itemCount > 100
                || items == null
                || items.size() != itemCount
                || new HashSet<>(items.stream()
                        .map(AgentActionItemDto::productId)
                        .toList()).size()
                        != items.size()) {
            return false;
        }
        BigDecimal lines = items.stream()
                .map(item -> new BigDecimal(item.total()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return lines.compareTo(new BigDecimal(total)) == 0;
    }

    private static boolean validDate(String value) {
        if (value == null) {
            return true;
        }
        try {
            return LocalDate.parse(value).toString().equals(value);
        } catch (DateTimeParseException failure) {
            return false;
        }
    }

    private static boolean validMoney(String value) {
        return value != null
                && value.matches("^\\d{1,8}\\.\\d{2}$");
    }
}
