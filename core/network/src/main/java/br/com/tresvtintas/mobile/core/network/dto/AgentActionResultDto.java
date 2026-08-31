package br.com.tresvtintas.mobile.core.network.dto;

import java.util.Set;

public record AgentActionResultDto(
        Long quoteId,
        String status,
        Integer revision,
        Boolean pricingChanged,
        String total,
        String conversationId,
        String deliveryState,
        String messageId,
        AgentAppointmentResultDto appointment,
        AgentDeliveryResultDto delivery,
        AgentOrderResultDto order,
        AgentFinanceResultDto finance,
        AgentCommissionResultDto commission) {
    private static final String MATERIAL_QUOTE_SEND =
            "material_quote_send";
    private static final String MATERIAL_QUOTE_CREATE =
            "material_quote_create";
    private static final String MATERIAL_QUOTE_AMEND =
            "material_quote_amend";
    private static final String ATTENDANCE_REPLY =
            "attendance_reply";
    private static final Set<String> STATUSES = Set.of("sent", "draft");
    private static final Set<String> DELIVERY_STATES =
            Set.of("available", "queued");

    public AgentActionResultDto(
            long quoteId,
            String status,
            int revision,
            Boolean pricingChanged,
            String total) {
        this(
                quoteId,
                status,
                revision,
                pricingChanged,
                total,
                null,
                null,
                null,
                null,
                null,
                null);
    }

    public AgentActionResultDto(
            Long quoteId,
            String status,
            Integer revision,
            Boolean pricingChanged,
            String total,
            String conversationId,
            String deliveryState,
            String messageId) {
        this(
                quoteId,
                status,
                revision,
                pricingChanged,
                total,
                conversationId,
                deliveryState,
                messageId,
                null,
                null,
                null);
    }

    public AgentActionResultDto(
            Long quoteId,
            String status,
            Integer revision,
            Boolean pricingChanged,
            String total,
            String conversationId,
            String deliveryState,
            String messageId,
            AgentAppointmentResultDto appointment) {
        this(
                quoteId,
                status,
                revision,
                pricingChanged,
                total,
                conversationId,
                deliveryState,
                messageId,
                appointment,
                null,
                null);
    }

    public AgentActionResultDto(
            Long quoteId,
            String status,
            Integer revision,
            Boolean pricingChanged,
            String total,
            String conversationId,
            String deliveryState,
            String messageId,
            AgentAppointmentResultDto appointment,
            AgentDeliveryResultDto delivery) {
        this(
                quoteId,
                status,
                revision,
                pricingChanged,
                total,
                conversationId,
                deliveryState,
                messageId,
                appointment,
                delivery,
                null);
    }

    public AgentActionResultDto(
            Long quoteId,
            String status,
            Integer revision,
            Boolean pricingChanged,
            String total,
            String conversationId,
            String deliveryState,
            String messageId,
            AgentAppointmentResultDto appointment,
            AgentDeliveryResultDto delivery,
            AgentOrderResultDto order) {
        this(
                quoteId,
                status,
                revision,
                pricingChanged,
                total,
                conversationId,
                deliveryState,
                messageId,
                appointment,
                delivery,
                order,
                null,
                null);
    }

    public AgentActionResultDto {
        quoteId = DtoValidation.optionalPositive(
                quoteId,
                "Agent action result quote ID");
        if (revision != null) {
            revision = Math.toIntExact(DtoValidation.requirePositive(
                    revision,
                    "Agent action result revision"));
        }
        conversationId = DtoValidation.optionalText(
                conversationId,
                "Agent attendance result conversation",
                180);
        messageId = DtoValidation.optionalText(
                messageId,
                "Agent attendance result message",
                400);
        if (status != null && !STATUSES.contains(status)) {
            throw new IllegalArgumentException(
                    "Agent action result status is invalid.");
        }
    }

    boolean validFor(String kind) {
        if (MATERIAL_QUOTE_SEND.equals(kind)) {
            return quoteId != null
                    && revision != null
                    && "sent".equals(status)
                    && pricingChanged != null
                    && total == null
                    && noAttendanceFields()
                    && noAppointmentFields()
                    && noDeliveryFields()
                    && noOrderFields()
                    && noFinancialFields();
        }
        if ((MATERIAL_QUOTE_CREATE.equals(kind)
                        || MATERIAL_QUOTE_AMEND.equals(kind))
                && quoteId != null
                && revision != null
                && "draft".equals(status)
                && pricingChanged == null
                && validMoney(total)
                && noAttendanceFields()
                && noAppointmentFields()
                && noDeliveryFields()
                && noOrderFields()
                && noFinancialFields()) {
            return true;
        }
        if (ATTENDANCE_REPLY.equals(kind)) {
            return quoteId == null
                    && revision == null
                    && status == null
                    && pricingChanged == null
                    && total == null
                    && conversationId != null
                    && conversationId.matches(
                            "^(?:whatsapp:[1-9]\\d*|site_chat:[^/\\u0000-\\u001f\\u007f]{1,160})$")
                    && DELIVERY_STATES.contains(deliveryState)
                    && messageId != null
                    && !messageId.isBlank()
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
                && revision == null
                && status == null
                && pricingChanged == null
                && total == null
                && noAttendanceFields()
                && appointment != null
                && noDeliveryFields()
                && noOrderFields()
                && noFinancialFields()) {
            return true;
        }
        if (Set.of("delivery_start", "delivery_complete")
                        .contains(kind)
                && quoteId == null
                && revision == null
                && status == null
                && pricingChanged == null
                && total == null
                && noAttendanceFields()
                && noAppointmentFields()
                && delivery != null
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
                && revision == null
                && status == null
                && pricingChanged == null
                && total == null
                && noAttendanceFields()
                && noAppointmentFields()
                && noDeliveryFields()
                && order != null
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
            return noLegacyResultFields()
                    && finance != null
                    && commission == null
                    && kind.startsWith(
                            finance.scope() + "_finance_");
        }
        return Set.of(
                        "commission_approve",
                        "commission_cancel",
                        "commission_pay")
                .contains(kind)
                && noLegacyResultFields()
                && finance == null
                && commission != null;
    }

    private boolean noAttendanceFields() {
        return conversationId == null
                && deliveryState == null
                && messageId == null;
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

    private boolean noLegacyResultFields() {
        return quoteId == null
                && revision == null
                && status == null
                && pricingChanged == null
                && total == null
                && noAttendanceFields()
                && noAppointmentFields()
                && noDeliveryFields()
                && noOrderFields();
    }

    private static boolean validMoney(String value) {
        return value != null
                && value.matches("^\\d{1,8}\\.\\d{2}$");
    }
}
