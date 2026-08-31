package br.com.tresvtintas.mobile.core.agent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import br.com.tresvtintas.mobile.core.order.OrderPaymentStatus;
import br.com.tresvtintas.mobile.core.order.OrderStatus;
import br.com.tresvtintas.mobile.core.order.OrderType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import org.junit.Test;

public final class AgentOrderSummaryTest {
    private static final Instant EXPIRY =
            Instant.parse("2026-08-01T12:00:00Z");

    @Test
    public void bindsTheReviewedTransitionToTheActionKind() {
        AgentOrderSummary summary = confirmSummary();
        AgentAction action = new AgentAction(
                "11111111-1111-4111-8111-111111111111",
                AgentActionKind.ORDER_CONFIRM,
                AgentActionStatus.PENDING,
                AgentAction.ORDER_CONFIRM_TITLE,
                summary,
                Optional.empty(),
                EXPIRY);

        assertEquals(
                "The reviewed order revision must remain explicit.",
                8,
                summary.expectedRevision());
        assertThrows(
                "A completion action cannot reuse a confirmation preview.",
                IllegalArgumentException.class,
                () -> new AgentAction(
                        action.id(),
                        AgentActionKind.ORDER_COMPLETE,
                        action.status(),
                        AgentAction.ORDER_COMPLETE_TITLE,
                        summary,
                        action.result(),
                        action.expiresAt()));
    }

    @Test
    public void rejectsATransitionThatChangesPaymentState() {
        assertThrows(
                "A status transition cannot silently alter payment.",
                IllegalArgumentException.class,
                () -> new AgentOrderSummary(
                        901,
                        AgentOrderOperation.CONFIRM,
                        OrderType.MATERIAL,
                        Optional.of("3V Tintas — Loja Centro"),
                        Optional.of("Cliente 3V"),
                        new BigDecimal("450.00"),
                        3,
                        8,
                        new AgentOrderSnapshot(
                                OrderStatus.PENDING,
                                OrderPaymentStatus.PENDING),
                        new AgentOrderSnapshot(
                                OrderStatus.CONFIRMED,
                                OrderPaymentStatus.RECEIVED),
                        true));
    }

    @Test
    public void retainsOnlyTheMinimalTerminalOrderResult() {
        AgentAction action = new AgentAction(
                "22222222-2222-4222-8222-222222222222",
                AgentActionKind.ORDER_CONFIRM,
                AgentActionStatus.EXECUTED,
                AgentAction.ORDER_CONFIRM_TITLE,
                confirmSummary(),
                Optional.of(new AgentOrderResult(
                        901,
                        OrderStatus.CONFIRMED,
                        9,
                        OrderPaymentStatus.PENDING,
                        true)),
                EXPIRY);

        assertEquals(
                "The terminal result cannot become another action type.",
                AgentOrderResult.class,
                action.result().orElseThrow().getClass());
    }

    private static AgentOrderSummary confirmSummary() {
        return new AgentOrderSummary(
                901,
                AgentOrderOperation.CONFIRM,
                OrderType.MATERIAL,
                Optional.of("3V Tintas — Loja Centro"),
                Optional.of("Cliente 3V"),
                new BigDecimal("450.00"),
                3,
                8,
                new AgentOrderSnapshot(
                        OrderStatus.PENDING,
                        OrderPaymentStatus.PENDING),
                new AgentOrderSnapshot(
                        OrderStatus.CONFIRMED,
                        OrderPaymentStatus.PENDING),
                true);
    }
}
