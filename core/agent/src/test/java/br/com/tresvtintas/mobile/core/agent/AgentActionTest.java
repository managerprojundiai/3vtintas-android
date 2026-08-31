package br.com.tresvtintas.mobile.core.agent;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import br.com.tresvtintas.mobile.core.appointment.AppointmentKind;
import br.com.tresvtintas.mobile.core.appointment.AppointmentStatus;
import br.com.tresvtintas.mobile.core.commission.CommissionKind;
import br.com.tresvtintas.mobile.core.commission.CommissionRecipientRole;
import br.com.tresvtintas.mobile.core.commission.CommissionStatus;
import br.com.tresvtintas.mobile.core.finance.FinanceEntryStatus;
import br.com.tresvtintas.mobile.core.finance.FinanceEntryType;
import br.com.tresvtintas.mobile.core.finance.FinancePaymentMethod;
import br.com.tresvtintas.mobile.core.order.OrderPaymentStatus;
import org.junit.Test;

public final class AgentActionTest {
    private static final Instant NOW =
            Instant.parse("2026-07-27T12:00:00Z");
    private static final BigDecimal TWO_HUNDRED =
            new BigDecimal("200.00");
    private static final BigDecimal UNIT_PRICE =
            new BigDecimal("100.00");
    private static final String CUSTOMER_NAME = "Cliente 3V";
    private static final String PREMIUM_PAINT = "Tinta Premium";
    private static final String STORE = "Loja Centro";

    @Test
    public void allowsOnlyPendingUnexpiredActionsToBeDecided() {
        AgentAction pending = action(
                AgentActionStatus.PENDING,
                NOW.plusSeconds(60));
        AgentAction executing = action(
                AgentActionStatus.EXECUTING,
                NOW.plusSeconds(60));

        assertTrue(
                "A pending action inside its TTL must be reviewable.",
                pending.canDecide(NOW));
        assertFalse(
                "An executing action cannot receive another decision.",
                executing.canDecide(NOW));
        assertFalse(
                "The exact expiry instant must close the decision.",
                pending.canDecide(NOW.plusSeconds(60)));
    }

    @Test
    public void rejectsActionInjectionOnUserMessages() {
        AgentAction action = action(
                AgentActionStatus.PENDING,
                NOW.plusSeconds(60));

        assertThrows(
                "A user message cannot inject a server action.",
                IllegalArgumentException.class,
                () -> new AgentMessage(
                        1,
                        AgentMessageRole.USER,
                        "Confirmar",
                        List.of(),
                        List.of(action),
                        Optional.empty(),
                        NOW));
    }

    static AgentAction action(
            AgentActionStatus status,
            Instant expiresAt) {
        return new AgentAction(
                "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa",
                AgentActionKind.MATERIAL_QUOTE_SEND,
                status,
                AgentAction.MATERIAL_QUOTE_SEND_TITLE,
                new AgentMaterialQuoteSendSummary(
                        42,
                        "Tinta da fachada",
                        CUSTOMER_NAME,
                        new BigDecimal("1299.90"),
                        AgentActionTargetStatus.SENT,
                        true),
                status == AgentActionStatus.EXECUTED
                        ? Optional.of(new AgentMaterialQuoteSendResult(
                                42,
                                2,
                                false))
                        : Optional.empty(),
                expiresAt);
    }

    @Test
    public void retainsTheCreatedQuoteResultOnlyAfterExecution() {
        AgentAction action = new AgentAction(
                "bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb",
                AgentActionKind.MATERIAL_QUOTE_CREATE,
                AgentActionStatus.EXECUTED,
                AgentAction.MATERIAL_QUOTE_CREATE_TITLE,
                new AgentMaterialQuoteCreateSummary(
                        "Pintura interna",
                        CUSTOMER_NAME,
                        Optional.of("Parede interna"),
                        Optional.empty(),
                        List.of(new AgentMaterialQuoteCreateItem(
                                7,
                                PREMIUM_PAINT,
                                new BigDecimal("2.00"),
                                "un",
                                UNIT_PRICE,
                                TWO_HUNDRED)),
                        TWO_HUNDRED,
                        TWO_HUNDRED,
                        AgentActionTargetStatus.DRAFT,
                        true),
                Optional.of(new AgentMaterialQuoteCreateResult(
                        91,
                        1,
                        TWO_HUNDRED)),
                NOW.plusSeconds(60));

        assertEquals(
                "The created quote ID must remain navigable.",
                91,
                ((AgentMaterialQuoteCreateResult)
                                action.result().orElseThrow())
                        .quoteId());
    }

    @Test
    public void acceptsTheSameHalfUpLineRoundingAsTheServer() {
        AgentMaterialQuoteCreateItem item =
                new AgentMaterialQuoteCreateItem(
                        7,
                        PREMIUM_PAINT,
                        new BigDecimal("1.25"),
                        "lata",
                        new BigDecimal("19.99"),
                        new BigDecimal("24.99"));

        assertEquals(
                "The reviewed amount must match server-side cent rounding.",
                new BigDecimal("24.99"),
                item.total());
    }

    @Test
    public void retainsTheCompleteAmendmentBeforeExecution() {
        AgentMaterialQuoteAmendSummary summary = amendmentSummary(
                new BigDecimal("3.00"));
        AgentAction action = new AgentAction(
                "cccccccc-cccc-4ccc-8ccc-cccccccccccc",
                AgentActionKind.MATERIAL_QUOTE_AMEND,
                AgentActionStatus.PENDING,
                AgentAction.MATERIAL_QUOTE_AMEND_TITLE,
                summary,
                Optional.empty(),
                NOW.plusSeconds(60));

        assertEquals(
                "The expected revision must remain bound to review.",
                4,
                summary.expectedRevision());
        assertEquals(
                "The visual comparison must expose the previous total.",
                new BigDecimal("190.00"),
                summary.before().total());
        assertTrue(
                "A pending amendment must require an explicit decision.",
                action.canDecide(NOW));
    }

    @Test
    public void rejectsChangesThatDoNotMatchTheReviewedSnapshots() {
        assertThrows(
                "The requested quantity cannot diverge from the after view.",
                IllegalArgumentException.class,
                () -> amendmentSummary(new BigDecimal("4.00")));
    }

    @Test
    public void retainsARevisionBoundAttendanceReply() {
        AgentAttendanceReplySummary summary =
                new AgentAttendanceReplySummary(
                        "whatsapp:701",
                        AgentAttendanceChannel.WHATSAPP,
                        CUSTOMER_NAME,
                        Optional.of(STORE),
                        7,
                        "Sim, entregamos hoje.",
                        Optional.of(new AgentAttendanceLatestMessage(
                                AgentAttendanceMessageDirection.INBOUND,
                                "Vocês entregam hoje?",
                                NOW)),
                        true);
        AgentAction action = new AgentAction(
                "dddddddd-dddd-4ddd-8ddd-dddddddddddd",
                AgentActionKind.ATTENDANCE_REPLY,
                AgentActionStatus.EXECUTED,
                AgentAction.ATTENDANCE_REPLY_TITLE,
                summary,
                Optional.of(new AgentAttendanceReplyResult(
                        "whatsapp:701",
                        AgentAttendanceDeliveryState.QUEUED,
                        "whatsapp:701:801")),
                NOW.plusSeconds(60));

        assertEquals(
                "The reviewed attendance revision must be retained.",
                7,
                summary.expectedRevision());
        assertEquals(
                "The exact outgoing text must be retained.",
                "Sim, entregamos hoje.",
                summary.content());
        assertEquals(
                "The terminal result must remain attendance-specific.",
                AgentAttendanceReplyResult.class,
                action.result().orElseThrow().getClass());
    }

    @Test
    public void bindsAppointmentOperationRevisionAndTerminalResult() {
        AgentAppointmentSnapshot before = new AgentAppointmentSnapshot(
                AppointmentStatus.CONFIRMED,
                NOW.plusSeconds(3_600),
                60,
                Optional.of(STORE));
        AgentAppointmentSnapshot after = new AgentAppointmentSnapshot(
                AppointmentStatus.CONFIRMED,
                NOW.plusSeconds(7_200),
                90,
                Optional.of(STORE));
        AgentAppointmentSummary summary = new AgentAppointmentSummary(
                Optional.of(71L),
                AgentAppointmentOperation.RESCHEDULE,
                "Visita técnica",
                AppointmentKind.GENERAL,
                "Carlos Pereira",
                Optional.of(STORE),
                Optional.of(CUSTOMER_NAME),
                Optional.of(4),
                Optional.of(before),
                after,
                true);
        AgentAction action = new AgentAction(
                "eeeeeeee-eeee-4eee-8eee-eeeeeeeeeeee",
                AgentActionKind.APPOINTMENT_RESCHEDULE,
                AgentActionStatus.EXECUTED,
                AgentAction.APPOINTMENT_RESCHEDULE_TITLE,
                summary,
                Optional.of(new AgentAppointmentResult(
                        71,
                        AppointmentStatus.CONFIRMED,
                        5,
                        true)),
                NOW.plusSeconds(60));

        assertEquals(
                "The reviewed revision must remain bound.",
                4,
                summary.expectedRevision().orElseThrow().intValue());
        assertEquals(
                "The terminal result must remain appointment-specific.",
                AgentAppointmentResult.class,
                action.result().orElseThrow().getClass());
        assertThrows(
                "A reschedule summary cannot be presented as cancellation.",
                IllegalArgumentException.class,
                () -> new AgentAction(
                        action.id(),
                        AgentActionKind.APPOINTMENT_CANCEL,
                        action.status(),
                        AgentAction.APPOINTMENT_CANCEL_TITLE,
                        summary,
                        action.result(),
                        action.expiresAt()));
    }

    @Test
    public void marksFinancialMutationsAsStepUpProtected() {
        AgentFinanceSummary summary = new AgentFinanceSummary(
                AgentFinanceScope.PERSONAL,
                AgentFinanceOperation.SETTLE,
                Optional.of(31L),
                Optional.empty(),
                Optional.empty(),
                Optional.of(18L),
                Optional.of(CUSTOMER_NAME),
                FinanceEntryType.EXPENSE,
                "Compra de material",
                new BigDecimal("120.00"),
                Optional.empty(),
                Optional.empty(),
                Optional.of(FinanceEntryStatus.PENDING),
                FinanceEntryStatus.SETTLED,
                Optional.of(FinancePaymentMethod.PIX),
                Optional.of("Pagamento confirmado"),
                true);
        AgentAction action = new AgentAction(
                "ffffffff-ffff-4fff-8fff-ffffffffffff",
                AgentActionKind.PERSONAL_FINANCE_SETTLE,
                AgentActionStatus.PENDING,
                AgentAction.PERSONAL_FINANCE_SETTLE_TITLE,
                summary,
                Optional.empty(),
                true,
                NOW.plusSeconds(60));

        assertTrue(
                "A financial mutation must require explicit step-up.",
                action.requiresStepUp());
        assertThrows(
                "A server response cannot downgrade step-up policy.",
                IllegalArgumentException.class,
                () -> new AgentAction(
                        action.id(),
                        action.kind(),
                        action.status(),
                        action.title(),
                        action.summary(),
                        action.result(),
                        false,
                        action.expiresAt()));
    }

    @Test
    public void bindsCommissionRevisionAndEligibilityToTheReview() {
        AgentCommissionSummary summary = new AgentCommissionSummary(
                73,
                AgentCommissionOperation.APPROVE,
                CommissionKind.SELLER,
                CommissionRecipientRole.SALESPERSON,
                "Vendedor 3V",
                Optional.of(STORE),
                Optional.of(91L),
                Optional.of(OrderPaymentStatus.RECEIVED),
                new BigDecimal("45.90"),
                4,
                CommissionStatus.PENDING,
                CommissionStatus.APPROVED,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                true);
        AgentAction action = new AgentAction(
                "99999999-9999-4999-8999-999999999999",
                AgentActionKind.COMMISSION_APPROVE,
                AgentActionStatus.EXECUTED,
                AgentAction.COMMISSION_APPROVE_TITLE,
                summary,
                Optional.of(new AgentCommissionResult(
                        73,
                        CommissionStatus.APPROVED,
                        5,
                        true)),
                true,
                NOW.plusSeconds(60));

        assertEquals(
                "The reviewed commission revision must remain bound.",
                4,
                summary.expectedRevision());
        assertEquals(
                "The terminal commission revision must remain explicit.",
                5,
                ((AgentCommissionResult)
                                action.result().orElseThrow())
                        .revision());
    }

    private static AgentMaterialQuoteAmendSummary amendmentSummary(
            BigDecimal afterQuantity) {
        AgentMaterialQuoteAmendItem previous =
                new AgentMaterialQuoteAmendItem(
                        7,
                        PREMIUM_PAINT,
                        new BigDecimal("2.00"),
                        "un",
                        UNIT_PRICE,
                        TWO_HUNDRED);
        AgentMaterialQuoteAmendItem next =
                new AgentMaterialQuoteAmendItem(
                        7,
                        PREMIUM_PAINT,
                        afterQuantity,
                        "un",
                        UNIT_PRICE,
                        afterQuantity.multiply(UNIT_PRICE)
                                .setScale(2));
        return new AgentMaterialQuoteAmendSummary(
                91,
                "Pintura interna",
                CUSTOMER_NAME,
                4,
                List.of(new AgentMaterialQuoteAmendChange(
                        AgentMaterialQuoteAmendOperation.UPDATE_QUANTITY,
                        7,
                        PREMIUM_PAINT,
                        Optional.of(new BigDecimal("2.00")),
                        Optional.of(new BigDecimal("3.00")))),
                new AgentMaterialQuoteAmendSnapshot(
                        List.of(previous),
                        TWO_HUNDRED,
                        new BigDecimal("10.00"),
                        new BigDecimal("190.00")),
                new AgentMaterialQuoteAmendSnapshot(
                        List.of(next),
                        next.total(),
                        new BigDecimal("10.00"),
                        next.total().subtract(
                                new BigDecimal("10.00"))),
                true);
    }
}
