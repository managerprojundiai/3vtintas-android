package br.com.tresvtintas.mobile.core.agent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import br.com.tresvtintas.mobile.core.delivery.DeliveryStatus;
import br.com.tresvtintas.mobile.core.order.OrderStatus;
import java.time.Instant;
import java.util.Optional;
import org.junit.Test;

public final class AgentDeliverySummaryTest {
    private static final Instant EXPIRY =
            Instant.parse("2026-08-01T12:00:00Z");

    @Test
    public void bindsTheReviewedStartTransitionToTheActionKind() {
        AgentDeliverySummary summary = startSummary();
        AgentAction action = new AgentAction(
                "11111111-1111-4111-8111-111111111111",
                AgentActionKind.DELIVERY_START,
                AgentActionStatus.PENDING,
                AgentAction.DELIVERY_START_TITLE,
                summary,
                Optional.empty(),
                EXPIRY);

        assertEquals(
                "The reviewed assignment revision must remain explicit.",
                8,
                summary.expectedOrderRevision());
        assertEquals(
                "The action must preserve the dedicated delivery summary.",
                AgentDeliverySummary.class,
                action.summary().getClass());
        assertThrows(
                "A complete action cannot reuse a start transition.",
                IllegalArgumentException.class,
                () -> new AgentAction(
                        action.id(),
                        AgentActionKind.DELIVERY_COMPLETE,
                        action.status(),
                        AgentAction.DELIVERY_COMPLETE_TITLE,
                        summary,
                        action.result(),
                        action.expiresAt()));
    }

    @Test
    public void rejectsAnyTransitionThatWasNotPublishedForTheOperation() {
        assertThrows(
                "Starting cannot jump directly to delivered.",
                IllegalArgumentException.class,
                () -> new AgentDeliverySummary(
                        81,
                        AgentDeliveryOperation.START,
                        901,
                        Optional.of("3V Tintas — Loja Centro"),
                        Optional.of("Cliente 3V"),
                        "João Entregador",
                        Optional.empty(),
                        3,
                        8,
                        new AgentDeliverySnapshot(
                                DeliveryStatus.PENDING,
                                OrderStatus.CONFIRMED),
                        new AgentDeliverySnapshot(
                                DeliveryStatus.DELIVERED,
                                OrderStatus.DELIVERED),
                        true));
    }

    @Test
    public void retainsOnlyTheMinimalTerminalDeliveryResult() {
        AgentAction action = new AgentAction(
                "22222222-2222-4222-8222-222222222222",
                AgentActionKind.DELIVERY_COMPLETE,
                AgentActionStatus.EXECUTED,
                AgentAction.DELIVERY_COMPLETE_TITLE,
                completeSummary(),
                Optional.of(new AgentDeliveryResult(
                        81,
                        DeliveryStatus.DELIVERED,
                        901,
                        OrderStatus.DELIVERED,
                        10,
                        true)),
                EXPIRY);

        assertEquals(
                "The terminal result cannot become another action type.",
                AgentDeliveryResult.class,
                action.result().orElseThrow().getClass());
    }

    private static AgentDeliverySummary startSummary() {
        return summary(
                AgentDeliveryOperation.START,
                new AgentDeliverySnapshot(
                        DeliveryStatus.PENDING,
                        OrderStatus.CONFIRMED),
                new AgentDeliverySnapshot(
                        DeliveryStatus.IN_TRANSIT,
                        OrderStatus.IN_PROGRESS));
    }

    private static AgentDeliverySummary completeSummary() {
        return summary(
                AgentDeliveryOperation.COMPLETE,
                new AgentDeliverySnapshot(
                        DeliveryStatus.IN_TRANSIT,
                        OrderStatus.IN_PROGRESS),
                new AgentDeliverySnapshot(
                        DeliveryStatus.DELIVERED,
                        OrderStatus.DELIVERED));
    }

    private static AgentDeliverySummary summary(
            AgentDeliveryOperation operation,
            AgentDeliverySnapshot before,
            AgentDeliverySnapshot after) {
        return new AgentDeliverySummary(
                81,
                operation,
                901,
                Optional.of("3V Tintas — Loja Centro"),
                Optional.of("Cliente 3V"),
                "João Entregador",
                Optional.of(Instant.parse("2026-08-05T13:00:00Z")),
                3,
                8,
                before,
                after,
                true);
    }
}
