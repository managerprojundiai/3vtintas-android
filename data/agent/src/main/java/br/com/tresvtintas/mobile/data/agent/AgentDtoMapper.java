package br.com.tresvtintas.mobile.data.agent;

import br.com.tresvtintas.mobile.core.agent.AgentConversation;
import br.com.tresvtintas.mobile.core.agent.AgentAction;
import br.com.tresvtintas.mobile.core.agent.AgentAppointmentOperation;
import br.com.tresvtintas.mobile.core.agent.AgentAppointmentResult;
import br.com.tresvtintas.mobile.core.agent.AgentAppointmentSnapshot;
import br.com.tresvtintas.mobile.core.agent.AgentAppointmentSummary;
import br.com.tresvtintas.mobile.core.agent.AgentAttendanceChannel;
import br.com.tresvtintas.mobile.core.agent.AgentAttendanceDeliveryState;
import br.com.tresvtintas.mobile.core.agent.AgentAttendanceLatestMessage;
import br.com.tresvtintas.mobile.core.agent.AgentAttendanceMessageDirection;
import br.com.tresvtintas.mobile.core.agent.AgentAttendanceReplyResult;
import br.com.tresvtintas.mobile.core.agent.AgentAttendanceReplySummary;
import br.com.tresvtintas.mobile.core.agent.AgentCommissionOperation;
import br.com.tresvtintas.mobile.core.agent.AgentCommissionResult;
import br.com.tresvtintas.mobile.core.agent.AgentCommissionSummary;
import br.com.tresvtintas.mobile.core.agent.AgentDeliveryOperation;
import br.com.tresvtintas.mobile.core.agent.AgentDeliveryResult;
import br.com.tresvtintas.mobile.core.agent.AgentDeliverySnapshot;
import br.com.tresvtintas.mobile.core.agent.AgentDeliverySummary;
import br.com.tresvtintas.mobile.core.agent.AgentFinanceOperation;
import br.com.tresvtintas.mobile.core.agent.AgentFinanceResult;
import br.com.tresvtintas.mobile.core.agent.AgentFinanceScope;
import br.com.tresvtintas.mobile.core.agent.AgentFinanceSummary;
import br.com.tresvtintas.mobile.core.agent.AgentOrderOperation;
import br.com.tresvtintas.mobile.core.agent.AgentOrderResult;
import br.com.tresvtintas.mobile.core.agent.AgentOrderSnapshot;
import br.com.tresvtintas.mobile.core.agent.AgentOrderSummary;
import br.com.tresvtintas.mobile.core.agent.AgentActionKind;
import br.com.tresvtintas.mobile.core.agent.AgentActionStatus;
import br.com.tresvtintas.mobile.core.agent.AgentActionTargetStatus;
import br.com.tresvtintas.mobile.core.agent.AgentConversationPage;
import br.com.tresvtintas.mobile.core.agent.AgentConversationStatus;
import br.com.tresvtintas.mobile.core.agent.AgentDocument;
import br.com.tresvtintas.mobile.core.agent.AgentDocumentType;
import br.com.tresvtintas.mobile.core.agent.AgentMessage;
import br.com.tresvtintas.mobile.core.agent.AgentMessagePage;
import br.com.tresvtintas.mobile.core.agent.AgentMessageRole;
import br.com.tresvtintas.mobile.core.agent.AgentMaterialQuoteSendSummary;
import br.com.tresvtintas.mobile.core.agent.AgentActionResult;
import br.com.tresvtintas.mobile.core.agent.AgentActionSummary;
import br.com.tresvtintas.mobile.core.agent.AgentMaterialQuoteCreateItem;
import br.com.tresvtintas.mobile.core.agent.AgentMaterialQuoteCreateResult;
import br.com.tresvtintas.mobile.core.agent.AgentMaterialQuoteCreateSummary;
import br.com.tresvtintas.mobile.core.agent.AgentMaterialQuoteAmendChange;
import br.com.tresvtintas.mobile.core.agent.AgentMaterialQuoteAmendItem;
import br.com.tresvtintas.mobile.core.agent.AgentMaterialQuoteAmendOperation;
import br.com.tresvtintas.mobile.core.agent.AgentMaterialQuoteAmendResult;
import br.com.tresvtintas.mobile.core.agent.AgentMaterialQuoteAmendSnapshot;
import br.com.tresvtintas.mobile.core.agent.AgentMaterialQuoteAmendSummary;
import br.com.tresvtintas.mobile.core.agent.AgentMaterialQuoteSendResult;
import br.com.tresvtintas.mobile.core.agent.AgentTurn;
import br.com.tresvtintas.mobile.core.agent.AgentTurnStatus;
import br.com.tresvtintas.mobile.core.appointment.AppointmentKind;
import br.com.tresvtintas.mobile.core.appointment.AppointmentStatus;
import br.com.tresvtintas.mobile.core.commission.CommissionKind;
import br.com.tresvtintas.mobile.core.commission.CommissionPaymentMethod;
import br.com.tresvtintas.mobile.core.commission.CommissionRecipientRole;
import br.com.tresvtintas.mobile.core.commission.CommissionStatus;
import br.com.tresvtintas.mobile.core.delivery.DeliveryStatus;
import br.com.tresvtintas.mobile.core.finance.FinanceEntryStatus;
import br.com.tresvtintas.mobile.core.finance.FinanceEntryType;
import br.com.tresvtintas.mobile.core.finance.FinancePaymentMethod;
import br.com.tresvtintas.mobile.core.order.OrderPaymentStatus;
import br.com.tresvtintas.mobile.core.order.OrderStatus;
import br.com.tresvtintas.mobile.core.order.OrderType;
import br.com.tresvtintas.mobile.core.network.dto.AgentConversationDto;
import br.com.tresvtintas.mobile.core.network.dto.AgentActionDto;
import br.com.tresvtintas.mobile.core.network.dto.AgentConversationPageDto;
import br.com.tresvtintas.mobile.core.network.dto.AgentMessageDto;
import br.com.tresvtintas.mobile.core.network.dto.AgentMessagePageDto;
import br.com.tresvtintas.mobile.core.network.dto.AgentTurnDto;
import java.time.Instant;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.Locale;
import java.util.Optional;

final class AgentDtoMapper {
    private AgentDtoMapper() {
        throw new AssertionError("No instances.");
    }

    static AgentConversationPage conversations(
            AgentConversationPageDto source) {
        return new AgentConversationPage(
                source.items().stream()
                        .map(AgentDtoMapper::conversation)
                        .toList(),
                Optional.ofNullable(source.nextCursor()));
    }

    static AgentConversation conversation(AgentConversationDto source) {
        return new AgentConversation(
                source.id(),
                source.title(),
                AgentConversationStatus.fromWireValue(source.status()),
                source.createdOnThisDevice(),
                Instant.parse(source.lastActivityAt()),
                Instant.parse(source.createdAt()),
                Instant.parse(source.updatedAt()));
    }

    static AgentMessagePage messages(AgentMessagePageDto source) {
        return new AgentMessagePage(
                source.conversationId(),
                source.items().stream()
                        .map(AgentDtoMapper::message)
                        .toList(),
                Optional.ofNullable(source.nextCursor()));
    }

    static AgentMessage message(AgentMessageDto source) {
        return new AgentMessage(
                source.id(),
                AgentMessageRole.fromWireValue(source.role()),
                source.content(),
                source.documents().stream()
                        .map(document -> new AgentDocument(
                                AgentDocumentType.fromWireValue(
                                        document.type()),
                                document.quoteId(),
                                document.filename()))
                        .toList(),
                source.actions().stream()
                        .map(AgentDtoMapper::action)
                        .toList(),
                Optional.ofNullable(source.turnId()),
                Instant.parse(source.createdAt()));
    }

    static AgentAction action(AgentActionDto source) {
        AgentActionKind kind =
                AgentActionKind.fromWireValue(source.kind());
        return new AgentAction(
                source.id(),
                kind,
                AgentActionStatus.fromWireValue(source.status()),
                source.title(),
                summary(source, kind),
                Optional.ofNullable(source.result())
                        .map(result -> mapResult(result, kind)),
                source.requiresStepUp(),
                Instant.parse(source.expiresAt()));
    }

    private static AgentActionSummary summary(
            AgentActionDto source,
            AgentActionKind kind) {
        if (kind == AgentActionKind.MATERIAL_QUOTE_SEND) {
            return new AgentMaterialQuoteSendSummary(
                    source.summary().quoteId(),
                    source.summary().quoteTitle(),
                    source.summary().customerName(),
                    new BigDecimal(source.summary().total()),
                    AgentActionTargetStatus.fromWireValue(
                            source.summary().targetStatus()),
                    source.summary().pricingWillBeRevalidated());
        }
        if (kind == AgentActionKind.MATERIAL_QUOTE_CREATE) {
            return new AgentMaterialQuoteCreateSummary(
                    source.summary().quoteTitle(),
                    source.summary().customerName(),
                    Optional.ofNullable(source.summary().notes()),
                    Optional.ofNullable(source.summary().validUntil())
                            .map(LocalDate::parse),
                    source.summary().items().stream()
                            .map(item -> new AgentMaterialQuoteCreateItem(
                                    item.productId(),
                                    item.description(),
                                    new BigDecimal(item.quantity()),
                                    item.unit(),
                                    new BigDecimal(item.unitPrice()),
                                    new BigDecimal(item.total())))
                            .toList(),
                    new BigDecimal(source.summary().subtotal()),
                    new BigDecimal(source.summary().total()),
                    AgentActionTargetStatus.fromWireValue(
                            source.summary().targetStatus()),
                    source.summary().pricingWillBeRevalidated());
        }
        if (kind == AgentActionKind.ATTENDANCE_REPLY) {
            return new AgentAttendanceReplySummary(
                    source.summary().conversationId(),
                    AgentAttendanceChannel.fromWireValue(
                            source.summary().channel()),
                    source.summary().customerName(),
                    Optional.ofNullable(
                            source.summary().organizationName()),
                    source.summary().expectedRevision(),
                    source.summary().content(),
                    Optional.ofNullable(
                                    source.summary().latestMessage())
                            .map(message ->
                                    new AgentAttendanceLatestMessage(
                                            AgentAttendanceMessageDirection
                                                    .fromWireValue(
                                                            message.direction()),
                                            message.preview(),
                                            Instant.parse(
                                                    message.createdAt()))),
                    source.summary()
                            .accessAndRevisionWillBeRevalidated());
        }
        if (isAppointment(kind)) {
            var appointment = source.summary().appointment();
            return new AgentAppointmentSummary(
                    Optional.ofNullable(appointment.appointmentId()),
                    AgentAppointmentOperation.fromWireValue(
                            appointment.operation()),
                    appointment.title(),
                    enumValue(
                            AppointmentKind.class,
                            appointment.kind()),
                    appointment.responsibleName(),
                    Optional.ofNullable(
                            appointment.organizationName()),
                    Optional.ofNullable(
                            appointment.customerName()),
                    Optional.ofNullable(
                            appointment.expectedRevision()),
                    Optional.ofNullable(appointment.before())
                            .map(AgentDtoMapper::appointmentSnapshot),
                    appointmentSnapshot(appointment.after()),
                    appointment
                            .accessAndAvailabilityWillBeRevalidated());
        }
        if (isDelivery(kind)) {
            var delivery = source.summary().delivery();
            return new AgentDeliverySummary(
                    delivery.deliveryId(),
                    AgentDeliveryOperation.fromWireValue(
                            delivery.operation()),
                    delivery.orderId(),
                    Optional.ofNullable(
                            delivery.organizationName()),
                    Optional.ofNullable(
                            delivery.customerName()),
                    delivery.assignedDriverName(),
                    Optional.ofNullable(delivery.scheduledAt())
                            .map(Instant::parse),
                    delivery.itemCount(),
                    delivery.expectedOrderRevision(),
                    deliverySnapshot(delivery.before()),
                    deliverySnapshot(delivery.after()),
                    delivery
                            .assignmentAndRevisionWillBeRevalidated());
        }
        if (isOrder(kind)) {
            var order = source.summary().order();
            return new AgentOrderSummary(
                    order.orderId(),
                    AgentOrderOperation.fromWireValue(
                            order.operation()),
                    enumValue(
                            OrderType.class,
                            order.orderType()),
                    Optional.ofNullable(
                            order.organizationName()),
                    Optional.ofNullable(
                            order.customerName()),
                    new BigDecimal(order.total()),
                    order.itemCount(),
                    order.expectedRevision(),
                    orderSnapshot(order.before()),
                    orderSnapshot(order.after()),
                    order.accessAndRevisionWillBeRevalidated());
        }
        if (isFinance(kind)) {
            var finance = source.summary().finance();
            return new AgentFinanceSummary(
                    AgentFinanceScope.fromWireValue(finance.scope()),
                    AgentFinanceOperation.fromWireValue(
                            finance.operation()),
                    Optional.ofNullable(finance.entryId()),
                    Optional.ofNullable(finance.organizationId()),
                    Optional.ofNullable(finance.organizationName()),
                    Optional.ofNullable(finance.customerId()),
                    Optional.ofNullable(finance.customerName()),
                    enumValue(FinanceEntryType.class, finance.type()),
                    finance.title(),
                    new BigDecimal(finance.amount()),
                    Optional.ofNullable(finance.dueAt())
                            .map(Instant::parse),
                    Optional.ofNullable(finance.notes()),
                    Optional.ofNullable(finance.beforeStatus())
                            .map(value -> enumValue(
                                    FinanceEntryStatus.class,
                                    value)),
                    enumValue(
                            FinanceEntryStatus.class,
                            finance.afterStatus()),
                    Optional.ofNullable(finance.paymentMethod())
                            .map(value -> enumValue(
                                    FinancePaymentMethod.class,
                                    value)),
                    Optional.ofNullable(finance.paymentReference()),
                    finance.accessAndStateWillBeRevalidated());
        }
        if (isCommission(kind)) {
            var commission = source.summary().commission();
            return new AgentCommissionSummary(
                    commission.commissionId(),
                    AgentCommissionOperation.fromWireValue(
                            commission.operation()),
                    enumValue(CommissionKind.class, commission.kind()),
                    enumValue(
                            CommissionRecipientRole.class,
                            commission.recipientRole()),
                    commission.recipientName(),
                    Optional.ofNullable(
                            commission.organizationName()),
                    Optional.ofNullable(commission.orderId()),
                    Optional.ofNullable(
                                    commission.orderPaymentStatus())
                            .map(value -> enumValue(
                                    OrderPaymentStatus.class,
                                    value)),
                    new BigDecimal(commission.amount()),
                    commission.expectedRevision(),
                    enumValue(
                            CommissionStatus.class,
                            commission.beforeStatus()),
                    enumValue(
                            CommissionStatus.class,
                            commission.afterStatus()),
                    Optional.ofNullable(
                            commission.cancellationReason()),
                    Optional.ofNullable(commission.paymentMethod())
                            .map(value -> enumValue(
                                    CommissionPaymentMethod.class,
                                    value)),
                    Optional.ofNullable(
                            commission.paymentReference()),
                    commission
                            .accessRevisionAndEligibilityWillBeRevalidated());
        }
        return new AgentMaterialQuoteAmendSummary(
                source.summary().quoteId(),
                source.summary().quoteTitle(),
                source.summary().customerName(),
                source.summary().expectedRevision(),
                source.summary().changes().stream()
                        .map(change -> new AgentMaterialQuoteAmendChange(
                                AgentMaterialQuoteAmendOperation
                                        .fromWireValue(
                                                change.operation()),
                                change.productId(),
                                change.description(),
                                Optional.ofNullable(
                                                change.beforeQuantity())
                                        .map(BigDecimal::new),
                                Optional.ofNullable(
                                                change.afterQuantity())
                                        .map(BigDecimal::new)))
                        .toList(),
                snapshot(source.summary().before()),
                snapshot(source.summary().after()),
                source.summary().pricingWillBeRevalidated());
    }

    private static AgentActionResult mapResult(
            br.com.tresvtintas.mobile.core.network.dto.AgentActionResultDto
                    source,
            AgentActionKind kind) {
        if (kind == AgentActionKind.MATERIAL_QUOTE_SEND) {
            return new AgentMaterialQuoteSendResult(
                    source.quoteId(),
                    source.revision(),
                    source.pricingChanged());
        }
        if (kind == AgentActionKind.MATERIAL_QUOTE_CREATE) {
            return new AgentMaterialQuoteCreateResult(
                    source.quoteId(),
                    source.revision(),
                    new BigDecimal(source.total()));
        }
        if (kind == AgentActionKind.ATTENDANCE_REPLY) {
            return new AgentAttendanceReplyResult(
                    source.conversationId(),
                    AgentAttendanceDeliveryState.fromWireValue(
                            source.deliveryState()),
                    source.messageId());
        }
        if (isAppointment(kind)) {
            var appointment = source.appointment();
            return new AgentAppointmentResult(
                    appointment.appointmentId(),
                    enumValue(
                            AppointmentStatus.class,
                            appointment.status()),
                    appointment.revision(),
                    appointment.changed());
        }
        if (isDelivery(kind)) {
            var delivery = source.delivery();
            return new AgentDeliveryResult(
                    delivery.deliveryId(),
                    enumValue(
                            DeliveryStatus.class,
                            delivery.deliveryStatus()),
                    delivery.orderId(),
                    enumValue(
                            OrderStatus.class,
                            delivery.orderStatus()),
                    delivery.orderRevision(),
                    delivery.changed());
        }
        if (isOrder(kind)) {
            var order = source.order();
            return new AgentOrderResult(
                    order.orderId(),
                    enumValue(
                            OrderStatus.class,
                            order.status()),
                    order.revision(),
                    enumValue(
                            OrderPaymentStatus.class,
                            order.paymentStatus()),
                    order.changed());
        }
        if (isFinance(kind)) {
            var finance = source.finance();
            return new AgentFinanceResult(
                    AgentFinanceScope.fromWireValue(finance.scope()),
                    finance.entryId(),
                    enumValue(
                            FinanceEntryStatus.class,
                            finance.status()),
                    finance.changed());
        }
        if (isCommission(kind)) {
            var commission = source.commission();
            return new AgentCommissionResult(
                    commission.commissionId(),
                    enumValue(
                            CommissionStatus.class,
                            commission.status()),
                    commission.revision(),
                    commission.changed());
        }
        return new AgentMaterialQuoteAmendResult(
                source.quoteId(),
                source.revision(),
                new BigDecimal(source.total()));
    }

    private static AgentMaterialQuoteAmendSnapshot snapshot(
            br.com.tresvtintas.mobile.core.network.dto
                            .AgentActionSnapshotDto
                    source) {
        return new AgentMaterialQuoteAmendSnapshot(
                source.items().stream()
                        .map(item -> new AgentMaterialQuoteAmendItem(
                                item.productId(),
                                item.description(),
                                new BigDecimal(item.quantity()),
                                item.unit(),
                                new BigDecimal(item.unitPrice()),
                                new BigDecimal(item.total())))
                        .toList(),
                new BigDecimal(source.subtotal()),
                new BigDecimal(source.discount()),
                new BigDecimal(source.total()));
    }

    private static AgentAppointmentSnapshot appointmentSnapshot(
            br.com.tresvtintas.mobile.core.network.dto
                            .AgentAppointmentSnapshotDto
                    source) {
        return new AgentAppointmentSnapshot(
                enumValue(
                        AppointmentStatus.class,
                        source.status()),
                Instant.parse(source.scheduledAt()),
                source.durationMinutes(),
                Optional.ofNullable(source.location()));
    }

    private static boolean isAppointment(AgentActionKind kind) {
        return kind == AgentActionKind.APPOINTMENT_CREATE
                || kind == AgentActionKind.APPOINTMENT_RESCHEDULE
                || kind == AgentActionKind.APPOINTMENT_CANCEL;
    }

    private static AgentDeliverySnapshot deliverySnapshot(
            br.com.tresvtintas.mobile.core.network.dto
                            .AgentDeliverySnapshotDto
                    source) {
        return new AgentDeliverySnapshot(
                enumValue(
                        DeliveryStatus.class,
                        source.deliveryStatus()),
                enumValue(
                        OrderStatus.class,
                        source.orderStatus()));
    }

    private static boolean isDelivery(AgentActionKind kind) {
        return kind == AgentActionKind.DELIVERY_START
                || kind == AgentActionKind.DELIVERY_COMPLETE;
    }

    private static AgentOrderSnapshot orderSnapshot(
            br.com.tresvtintas.mobile.core.network.dto
                            .AgentOrderSnapshotDto
                    source) {
        return new AgentOrderSnapshot(
                enumValue(
                        OrderStatus.class,
                        source.status()),
                enumValue(
                        OrderPaymentStatus.class,
                        source.paymentStatus()));
    }

    private static boolean isOrder(AgentActionKind kind) {
        return kind == AgentActionKind.ORDER_CONFIRM
                || kind == AgentActionKind.ORDER_START_FULFILLMENT
                || kind == AgentActionKind.ORDER_COMPLETE;
    }

    private static boolean isFinance(AgentActionKind kind) {
        return switch (kind) {
            case PERSONAL_FINANCE_CREATE,
                    PERSONAL_FINANCE_SETTLE,
                    PERSONAL_FINANCE_CANCEL,
                    CORPORATE_FINANCE_CREATE,
                    CORPORATE_FINANCE_SETTLE,
                    CORPORATE_FINANCE_CANCEL -> true;
            default -> false;
        };
    }

    private static boolean isCommission(AgentActionKind kind) {
        return kind == AgentActionKind.COMMISSION_APPROVE
                || kind == AgentActionKind.COMMISSION_CANCEL
                || kind == AgentActionKind.COMMISSION_PAY;
    }

    static AgentTurn turn(AgentTurnDto source) {
        return new AgentTurn(
                source.id(),
                source.conversationId(),
                AgentTurnStatus.fromWireValue(source.status()),
                Instant.parse(source.createdAt()),
                instant(source.startedAt()),
                instant(source.completedAt()));
    }

    private static Optional<Instant> instant(String value) {
        return Optional.ofNullable(value).map(Instant::parse);
    }

    private static <T extends Enum<T>> T enumValue(
            Class<T> type,
            String value) {
        return Enum.valueOf(
                type,
                value.toUpperCase(Locale.ROOT));
    }
}
