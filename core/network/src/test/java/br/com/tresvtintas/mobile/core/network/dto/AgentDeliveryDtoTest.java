package br.com.tresvtintas.mobile.core.network.dto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.Test;

public final class AgentDeliveryDtoTest {
    private static final String EXPIRY =
            "2026-08-01T12:00:00Z";
    private static final String DELIVERED = "delivered";

    @Test
    public void validatesTheCompleteDeliveryStartEnvelope() {
        AgentDeliverySummaryDto delivery = startSummary();
        AgentActionDto action = new AgentActionDto(
                "11111111-1111-4111-8111-111111111111",
                "delivery_start",
                "pending",
                "Iniciar entrega",
                summary(delivery),
                null,
                EXPIRY);

        assertEquals(
                "The assigned driver must survive transport.",
                "João Entregador",
                action.summary().delivery().assignedDriverName());
        assertTrue(
                "The confirmation must explicitly promise revalidation.",
                action.summary()
                        .delivery()
                        .assignmentAndRevisionWillBeRevalidated());
        assertThrows(
                "The wire kind cannot diverge from the reviewed operation.",
                IllegalArgumentException.class,
                () -> new AgentActionDto(
                        action.id(),
                        "delivery_complete",
                        action.status(),
                        "Concluir entrega",
                        action.summary(),
                        action.result(),
                        action.expiresAt()));
    }

    @Test
    public void validatesTheMinimalTerminalDeliveryResult() {
        AgentActionDto action = new AgentActionDto(
                "22222222-2222-4222-8222-222222222222",
                "delivery_complete",
                "executed",
                "Concluir entrega",
                summary(completeSummary()),
                result(new AgentDeliveryResultDto(
                        81,
                        DELIVERED,
                        901,
                        DELIVERED,
                        10,
                        true)),
                EXPIRY);

        assertEquals(
                "The terminal order revision must remain explicit.",
                10,
                action.result().delivery().orderRevision());
    }

    @Test
    public void exposesNoPrivateDeliveryFieldsInThePublicDto() {
        Set<String> fields = Arrays.stream(
                        AgentDeliverySummaryDto.class
                                .getRecordComponents())
                .map(component -> component.getName())
                .collect(Collectors.toSet());

        assertEquals(
                "The public delivery preview surface must stay allowlisted.",
                Set.of(
                        "deliveryId",
                        "operation",
                        "orderId",
                        "organizationName",
                        "customerName",
                        "assignedDriverName",
                        "scheduledAt",
                        "itemCount",
                        "expectedOrderRevision",
                        "before",
                        "after",
                        "assignmentAndRevisionWillBeRevalidated"),
                fields);
        assertTrue(
                "Phone, address, instructions, tracking and line items "
                        + "must not exist in the public preview.",
                fields.stream().noneMatch(field -> Set.of(
                                "phone",
                                "address",
                                "instructions",
                                "trackingCode",
                                "items")
                        .contains(field)));
    }

    @Test
    public void rejectsAnUnpublishedStartTransition() {
        assertThrows(
                "Starting cannot publish a delivered terminal state.",
                IllegalArgumentException.class,
                () -> new AgentDeliverySummaryDto(
                        81,
                        "start",
                        901,
                        "3V Tintas — Loja Centro",
                        "Cliente 3V",
                        "João Entregador",
                        null,
                        3,
                        8,
                        new AgentDeliverySnapshotDto(
                                "pending",
                                "confirmed"),
                        new AgentDeliverySnapshotDto(
                                DELIVERED,
                                DELIVERED),
                        true));
    }

    private static AgentDeliverySummaryDto startSummary() {
        return delivery(
                "start",
                new AgentDeliverySnapshotDto(
                        "pending",
                        "confirmed"),
                new AgentDeliverySnapshotDto(
                        "in_transit",
                        "in_progress"));
    }

    private static AgentDeliverySummaryDto completeSummary() {
        return delivery(
                "complete",
                new AgentDeliverySnapshotDto(
                        "in_transit",
                        "in_progress"),
                new AgentDeliverySnapshotDto(
                        DELIVERED,
                        DELIVERED));
    }

    private static AgentDeliverySummaryDto delivery(
            String operation,
            AgentDeliverySnapshotDto before,
            AgentDeliverySnapshotDto after) {
        return new AgentDeliverySummaryDto(
                81,
                operation,
                901,
                "3V Tintas — Loja Centro",
                "Cliente 3V",
                "João Entregador",
                "2026-08-05T13:00:00Z",
                3,
                8,
                before,
                after,
                true);
    }

    private static AgentActionSummaryDto summary(
            AgentDeliverySummaryDto delivery) {
        return new AgentActionSummaryDto(
                null,
                null,
                null,
                null,
                null,
                null,
                List.of(),
                null,
                null,
                null,
                false,
                null,
                List.of(),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                delivery);
    }

    private static AgentActionResultDto result(
            AgentDeliveryResultDto delivery) {
        return new AgentActionResultDto(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                delivery);
    }
}
