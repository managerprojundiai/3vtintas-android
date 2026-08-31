package br.com.tresvtintas.mobile.feature.agent;

import br.com.tresvtintas.mobile.core.agent.AgentAction;
import br.com.tresvtintas.mobile.core.agent.AgentActionKind;
import br.com.tresvtintas.mobile.core.agent.AgentActionStatus;
import br.com.tresvtintas.mobile.core.agent.AgentActionTargetStatus;
import br.com.tresvtintas.mobile.core.agent.AgentAppointmentOperation;
import br.com.tresvtintas.mobile.core.agent.AgentAppointmentSnapshot;
import br.com.tresvtintas.mobile.core.agent.AgentAppointmentSummary;
import br.com.tresvtintas.mobile.core.agent.AgentAttendanceChannel;
import br.com.tresvtintas.mobile.core.agent.AgentAttendanceLatestMessage;
import br.com.tresvtintas.mobile.core.agent.AgentAttendanceMessageDirection;
import br.com.tresvtintas.mobile.core.agent.AgentAttendanceReplySummary;
import br.com.tresvtintas.mobile.core.agent.AgentDeliveryOperation;
import br.com.tresvtintas.mobile.core.agent.AgentDeliverySnapshot;
import br.com.tresvtintas.mobile.core.agent.AgentDeliverySummary;
import br.com.tresvtintas.mobile.core.agent.AgentMaterialQuoteAmendChange;
import br.com.tresvtintas.mobile.core.agent.AgentMaterialQuoteAmendItem;
import br.com.tresvtintas.mobile.core.agent.AgentMaterialQuoteAmendOperation;
import br.com.tresvtintas.mobile.core.agent.AgentMaterialQuoteAmendSnapshot;
import br.com.tresvtintas.mobile.core.agent.AgentMaterialQuoteAmendSummary;
import br.com.tresvtintas.mobile.core.agent.AgentMaterialQuoteCreateItem;
import br.com.tresvtintas.mobile.core.agent.AgentMaterialQuoteCreateSummary;
import br.com.tresvtintas.mobile.core.agent.AgentMaterialQuoteSendSummary;
import br.com.tresvtintas.mobile.core.agent.AgentOrderOperation;
import br.com.tresvtintas.mobile.core.agent.AgentOrderSnapshot;
import br.com.tresvtintas.mobile.core.agent.AgentOrderSummary;
import br.com.tresvtintas.mobile.core.appointment.AppointmentKind;
import br.com.tresvtintas.mobile.core.appointment.AppointmentStatus;
import br.com.tresvtintas.mobile.core.delivery.DeliveryStatus;
import br.com.tresvtintas.mobile.core.order.OrderPaymentStatus;
import br.com.tresvtintas.mobile.core.order.OrderStatus;
import br.com.tresvtintas.mobile.core.order.OrderType;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Synthetic agent actions shared by debug-only physical-device tests.
 */
final class AgentActionReviewFixtures {
    private static final Instant EXPIRY =
            Instant.parse("2026-07-27T13:00:00Z");
    private static final String PREMIUM_PAINT = "Tinta Premium";
    private static final String TWO_UNITS = "2.00";
    private static final String CUSTOMER_NAME = "Marina Alves";
    private static final String CENTRAL_STORE =
            "3V Tintas — Loja Centro";
    private static final String CENTRAL_ADDRESS =
            "Rua das Flores, 120 — Centro";

    private AgentActionReviewFixtures() {
        throw new AssertionError("No instances.");
    }

    static AgentAction create() {
        return new AgentAction(
                "bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb",
                AgentActionKind.MATERIAL_QUOTE_CREATE,
                AgentActionStatus.PENDING,
                AgentAction.MATERIAL_QUOTE_CREATE_TITLE,
                new AgentMaterialQuoteCreateSummary(
                        "Pintura interna",
                        "Cliente 3V",
                        Optional.of("Parede interna"),
                        Optional.of(LocalDate.of(2026, 8, 3)),
                        List.of(new AgentMaterialQuoteCreateItem(
                                7,
                                PREMIUM_PAINT,
                                new BigDecimal("1.25"),
                                "lata",
                                new BigDecimal("19.99"),
                                new BigDecimal("24.99"))),
                        new BigDecimal("24.99"),
                        new BigDecimal("24.99"),
                        AgentActionTargetStatus.DRAFT,
                        true),
                Optional.empty(),
                EXPIRY);
    }

    static AgentAction send() {
        return new AgentAction(
                "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa",
                AgentActionKind.MATERIAL_QUOTE_SEND,
                AgentActionStatus.PENDING,
                AgentAction.MATERIAL_QUOTE_SEND_TITLE,
                new AgentMaterialQuoteSendSummary(
                        42,
                        "Tinta da fachada",
                        "Cliente 3V",
                        new BigDecimal("1299.90"),
                        AgentActionTargetStatus.SENT,
                        true),
                Optional.empty(),
                EXPIRY);
    }

    static AgentAction amend() {
        AgentMaterialQuoteAmendItem previousPaint = amendItem(
                7,
                PREMIUM_PAINT,
                TWO_UNITS,
                "100.00",
                "200.00");
        AgentMaterialQuoteAmendItem removedPrimer = amendItem(
                8,
                "Fundo preparador",
                "1.00",
                "50.00",
                "50.00");
        AgentMaterialQuoteAmendItem resultingPaint = amendItem(
                7,
                PREMIUM_PAINT,
                "3.00",
                "100.00",
                "300.00");
        AgentMaterialQuoteAmendItem addedTape = amendItem(
                9,
                "Fita profissional",
                TWO_UNITS,
                "25.00",
                "50.00");
        return new AgentAction(
                "cccccccc-cccc-4ccc-8ccc-cccccccccccc",
                AgentActionKind.MATERIAL_QUOTE_AMEND,
                AgentActionStatus.PENDING,
                AgentAction.MATERIAL_QUOTE_AMEND_TITLE,
                new AgentMaterialQuoteAmendSummary(
                        91,
                        "Pintura interna",
                        "Cliente 3V",
                        4,
                        changes(),
                        new AgentMaterialQuoteAmendSnapshot(
                                List.of(previousPaint, removedPrimer),
                                new BigDecimal("250.00"),
                                new BigDecimal("10.00"),
                                new BigDecimal("240.00")),
                        new AgentMaterialQuoteAmendSnapshot(
                                List.of(resultingPaint, addedTape),
                                new BigDecimal("350.00"),
                                new BigDecimal("10.00"),
                                new BigDecimal("340.00")),
                        true),
                Optional.empty(),
                EXPIRY);
    }

    static AgentAction attendanceReply() {
        return new AgentAction(
                "dddddddd-dddd-4ddd-8ddd-dddddddddddd",
                AgentActionKind.ATTENDANCE_REPLY,
                AgentActionStatus.PENDING,
                AgentAction.ATTENDANCE_REPLY_TITLE,
                new AgentAttendanceReplySummary(
                        "whatsapp:9187",
                        AgentAttendanceChannel.WHATSAPP,
                        CUSTOMER_NAME,
                        Optional.of(CENTRAL_STORE),
                        12,
                        "Olá, Marina! Temos a tinta solicitada em estoque. "
                                + "Posso preparar o orçamento para retirada hoje.",
                        Optional.of(new AgentAttendanceLatestMessage(
                                AgentAttendanceMessageDirection.INBOUND,
                                "Vocês têm a tinta premium branca de 18 litros?",
                                Instant.parse("2026-07-29T12:15:00Z"))),
                        true),
                Optional.empty(),
                EXPIRY);
    }

    static AgentAction appointmentCreate() {
        return new AgentAction(
                "eeeeeeee-eeee-4eee-8eee-eeeeeeeeeeee",
                AgentActionKind.APPOINTMENT_CREATE,
                AgentActionStatus.PENDING,
                AgentAction.APPOINTMENT_CREATE_TITLE,
                new AgentAppointmentSummary(
                        Optional.empty(),
                        AgentAppointmentOperation.CREATE,
                        "Visita técnica — Loja Centro",
                        AppointmentKind.GENERAL,
                        "Carlos Pereira",
                        Optional.of(CENTRAL_STORE),
                        Optional.of(CUSTOMER_NAME),
                        Optional.empty(),
                        Optional.empty(),
                        new AgentAppointmentSnapshot(
                                AppointmentStatus.SCHEDULED,
                                Instant.parse("2026-08-03T13:30:00Z"),
                                60,
                                Optional.of(CENTRAL_ADDRESS)),
                        true),
                Optional.empty(),
                EXPIRY);
    }

    static AgentAction appointmentReschedule() {
        return appointmentChange(
                AgentActionKind.APPOINTMENT_RESCHEDULE,
                AgentAction.APPOINTMENT_RESCHEDULE_TITLE,
                AgentAppointmentOperation.RESCHEDULE,
                new AgentAppointmentSnapshot(
                        AppointmentStatus.CONFIRMED,
                        Instant.parse("2026-08-03T13:30:00Z"),
                        60,
                        Optional.of(CENTRAL_ADDRESS)),
                new AgentAppointmentSnapshot(
                        AppointmentStatus.CONFIRMED,
                        Instant.parse("2026-08-04T17:00:00Z"),
                        90,
                        Optional.of(CENTRAL_ADDRESS)));
    }

    static AgentAction appointmentCancel() {
        AgentAppointmentSnapshot before = new AgentAppointmentSnapshot(
                AppointmentStatus.CONFIRMED,
                Instant.parse("2026-08-03T13:30:00Z"),
                60,
                Optional.of(CENTRAL_ADDRESS));
        return appointmentChange(
                AgentActionKind.APPOINTMENT_CANCEL,
                AgentAction.APPOINTMENT_CANCEL_TITLE,
                AgentAppointmentOperation.CANCEL,
                before,
                new AgentAppointmentSnapshot(
                        AppointmentStatus.CANCELLED,
                        before.scheduledAt(),
                        before.durationMinutes(),
                        before.location()));
    }

    static AgentAction deliveryStart() {
        return deliveryAction(
                "11111111-1111-4111-8111-111111111111",
                AgentActionKind.DELIVERY_START,
                AgentAction.DELIVERY_START_TITLE,
                AgentDeliveryOperation.START,
                new AgentDeliverySnapshot(
                        DeliveryStatus.PENDING,
                        OrderStatus.CONFIRMED),
                new AgentDeliverySnapshot(
                        DeliveryStatus.IN_TRANSIT,
                        OrderStatus.IN_PROGRESS));
    }

    static AgentAction deliveryComplete() {
        return deliveryAction(
                "22222222-2222-4222-8222-222222222222",
                AgentActionKind.DELIVERY_COMPLETE,
                AgentAction.DELIVERY_COMPLETE_TITLE,
                AgentDeliveryOperation.COMPLETE,
                new AgentDeliverySnapshot(
                        DeliveryStatus.IN_TRANSIT,
                        OrderStatus.IN_PROGRESS),
                new AgentDeliverySnapshot(
                        DeliveryStatus.DELIVERED,
                        OrderStatus.DELIVERED));
    }

    static AgentAction orderConfirm() {
        return orderAction(
                "33333333-3333-4333-8333-333333333333",
                AgentActionKind.ORDER_CONFIRM,
                AgentAction.ORDER_CONFIRM_TITLE,
                AgentOrderOperation.CONFIRM,
                OrderStatus.PENDING,
                OrderStatus.CONFIRMED,
                12);
    }

    static AgentAction orderStartFulfillment() {
        return orderAction(
                "44444444-4444-4444-8444-444444444444",
                AgentActionKind.ORDER_START_FULFILLMENT,
                AgentAction.ORDER_START_FULFILLMENT_TITLE,
                AgentOrderOperation.START_FULFILLMENT,
                OrderStatus.CONFIRMED,
                OrderStatus.IN_PROGRESS,
                13);
    }

    static AgentAction orderComplete() {
        return orderAction(
                "55555555-5555-4555-8555-555555555555",
                AgentActionKind.ORDER_COMPLETE,
                AgentAction.ORDER_COMPLETE_TITLE,
                AgentOrderOperation.COMPLETE,
                OrderStatus.IN_PROGRESS,
                OrderStatus.DELIVERED,
                14);
    }

    private static AgentAction appointmentChange(
            AgentActionKind kind,
            String actionTitle,
            AgentAppointmentOperation operation,
            AgentAppointmentSnapshot before,
            AgentAppointmentSnapshot after) {
        return new AgentAction(
                "ffffffff-ffff-4fff-8fff-ffffffffffff",
                kind,
                AgentActionStatus.PENDING,
                actionTitle,
                new AgentAppointmentSummary(
                        Optional.of(71L),
                        operation,
                        "Visita técnica — Loja Centro",
                        AppointmentKind.GENERAL,
                        "Carlos Pereira",
                        Optional.of(CENTRAL_STORE),
                        Optional.of(CUSTOMER_NAME),
                        Optional.of(4),
                        Optional.of(before),
                        after,
                        true),
                Optional.empty(),
                EXPIRY);
    }

    private static AgentAction deliveryAction(
            String actionId,
            AgentActionKind kind,
            String title,
            AgentDeliveryOperation operation,
            AgentDeliverySnapshot before,
            AgentDeliverySnapshot after) {
        return new AgentAction(
                actionId,
                kind,
                AgentActionStatus.PENDING,
                title,
                new AgentDeliverySummary(
                        81,
                        operation,
                        901,
                        Optional.of(CENTRAL_STORE),
                        Optional.of(CUSTOMER_NAME),
                        "João Entregador",
                        Optional.of(Instant.parse(
                                "2026-08-05T13:00:00Z")),
                        3,
                        8,
                        before,
                        after,
                        true),
                Optional.empty(),
                EXPIRY);
    }

    private static AgentAction orderAction(
            String actionId,
            AgentActionKind kind,
            String title,
            AgentOrderOperation operation,
            OrderStatus before,
            OrderStatus after,
            int expectedRevision) {
        return new AgentAction(
                actionId,
                kind,
                AgentActionStatus.PENDING,
                title,
                new AgentOrderSummary(
                        902,
                        operation,
                        OrderType.MATERIAL,
                        Optional.of(CENTRAL_STORE),
                        Optional.of(CUSTOMER_NAME),
                        new BigDecimal("489.90"),
                        4,
                        expectedRevision,
                        new AgentOrderSnapshot(
                                before,
                                OrderPaymentStatus.PENDING),
                        new AgentOrderSnapshot(
                                after,
                                OrderPaymentStatus.PENDING),
                        true),
                Optional.empty(),
                EXPIRY);
    }

    private static List<AgentMaterialQuoteAmendChange> changes() {
        return List.of(
                new AgentMaterialQuoteAmendChange(
                        AgentMaterialQuoteAmendOperation.UPDATE_QUANTITY,
                        7,
                        PREMIUM_PAINT,
                        Optional.of(new BigDecimal(TWO_UNITS)),
                        Optional.of(new BigDecimal("3.00"))),
                new AgentMaterialQuoteAmendChange(
                        AgentMaterialQuoteAmendOperation.REMOVE,
                        8,
                        "Fundo preparador",
                        Optional.of(BigDecimal.ONE),
                        Optional.empty()),
                new AgentMaterialQuoteAmendChange(
                        AgentMaterialQuoteAmendOperation.ADD,
                        9,
                        "Fita profissional",
                        Optional.empty(),
                        Optional.of(new BigDecimal(TWO_UNITS))));
    }

    private static AgentMaterialQuoteAmendItem amendItem(
            long productId,
            String description,
            String quantity,
            String unitPrice,
            String total) {
        return new AgentMaterialQuoteAmendItem(
                productId,
                description,
                new BigDecimal(quantity),
                "un",
                new BigDecimal(unitPrice),
                new BigDecimal(total));
    }
}
