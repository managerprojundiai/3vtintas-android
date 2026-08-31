package br.com.tresvtintas.mobile.core.network.dto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.Test;

public final class AgentOrderDtoTest {
    private static final String EXPIRY =
            "2026-08-01T12:00:00Z";
    private static final String PENDING = "pending";

    @Test
    public void validatesTheCompleteOrderConfirmationEnvelope() {
        AgentActionDto action = new AgentActionDto(
                "11111111-1111-4111-8111-111111111111",
                "order_confirm",
                PENDING,
                "Confirmar pedido",
                summary(confirmSummary()),
                null,
                EXPIRY);

        assertEquals(
                "The reviewed revision must survive transport.",
                8,
                action.summary().order().expectedRevision());
        assertThrows(
                "The wire kind cannot diverge from the reviewed operation.",
                IllegalArgumentException.class,
                () -> new AgentActionDto(
                        action.id(),
                        "order_complete",
                        action.status(),
                        "Concluir pedido",
                        action.summary(),
                        action.result(),
                        action.expiresAt()));
    }

    @Test
    public void validatesTheMinimalTerminalOrderResult() {
        AgentActionDto action = new AgentActionDto(
                "22222222-2222-4222-8222-222222222222",
                "order_confirm",
                "executed",
                "Confirmar pedido",
                summary(confirmSummary()),
                result(new AgentOrderResultDto(
                        901,
                        "confirmed",
                        9,
                        PENDING,
                        true)),
                EXPIRY);

        assertEquals(
                "The terminal order revision must remain explicit.",
                9,
                action.result().order().revision());
    }

    @Test
    public void exposesNoPrivateOrderFieldsInThePublicDto() {
        Set<String> fields = Arrays.stream(
                        AgentOrderSummaryDto.class.getRecordComponents())
                .map(component -> component.getName())
                .collect(Collectors.toSet());

        assertEquals(
                "The public order preview surface must stay allowlisted.",
                Set.of(
                        "orderId",
                        "operation",
                        "orderType",
                        "organizationName",
                        "customerName",
                        "total",
                        "itemCount",
                        "expectedRevision",
                        "before",
                        "after",
                        "accessAndRevisionWillBeRevalidated"),
                fields);
        assertTrue(
                "Contacts, addresses, notes and line items must stay private.",
                fields.stream().noneMatch(field -> Set.of(
                                "phone",
                                "address",
                                "notes",
                                "items")
                        .contains(field)));
    }

    @Test
    public void rejectsAnUnpublishedConfirmationTransition() {
        assertThrows(
                "Confirmation cannot jump directly to delivered.",
                IllegalArgumentException.class,
                () -> new AgentOrderSummaryDto(
                        901,
                        "confirm",
                        "material",
                        "3V Tintas — Loja Centro",
                        "Cliente 3V",
                        "450.00",
                        3,
                        8,
                        new AgentOrderSnapshotDto(
                                PENDING,
                                PENDING),
                        new AgentOrderSnapshotDto(
                                "delivered",
                                PENDING),
                        true));
    }

    private static AgentOrderSummaryDto confirmSummary() {
        return new AgentOrderSummaryDto(
                901,
                "confirm",
                "material",
                "3V Tintas — Loja Centro",
                "Cliente 3V",
                "450.00",
                3,
                8,
                new AgentOrderSnapshotDto(
                        PENDING,
                        PENDING),
                new AgentOrderSnapshotDto(
                        "confirmed",
                        PENDING),
                true);
    }

    private static AgentActionSummaryDto summary(
            AgentOrderSummaryDto order) {
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
                null,
                order);
    }

    private static AgentActionResultDto result(
            AgentOrderResultDto order) {
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
                null,
                order);
    }
}
