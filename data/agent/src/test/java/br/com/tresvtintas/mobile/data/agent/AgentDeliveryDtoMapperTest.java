package br.com.tresvtintas.mobile.data.agent;

import static org.junit.Assert.assertEquals;

import br.com.tresvtintas.mobile.core.agent.AgentAction;
import br.com.tresvtintas.mobile.core.agent.AgentDeliveryOperation;
import br.com.tresvtintas.mobile.core.agent.AgentDeliveryResult;
import br.com.tresvtintas.mobile.core.agent.AgentDeliverySummary;
import br.com.tresvtintas.mobile.core.delivery.DeliveryStatus;
import br.com.tresvtintas.mobile.core.network.dto.AgentActionDto;
import br.com.tresvtintas.mobile.core.network.dto.AgentActionResultDto;
import br.com.tresvtintas.mobile.core.network.dto.AgentActionSummaryDto;
import br.com.tresvtintas.mobile.core.network.dto.AgentDeliveryResultDto;
import br.com.tresvtintas.mobile.core.network.dto.AgentDeliverySnapshotDto;
import br.com.tresvtintas.mobile.core.network.dto.AgentDeliverySummaryDto;
import br.com.tresvtintas.mobile.core.order.OrderStatus;
import java.util.List;
import org.junit.Test;

public final class AgentDeliveryDtoMapperTest {
    private static final String DELIVERED = "delivered";

    @Test
    public void mapsThePublishedDeliveryIntoDedicatedDomainTypes() {
        AgentAction action = AgentDtoMapper.action(new AgentActionDto(
                "22222222-2222-4222-8222-222222222222",
                "delivery_complete",
                "executed",
                "Concluir entrega",
                summary(new AgentDeliverySummaryDto(
                        81,
                        "complete",
                        901,
                        "3V Tintas — Loja Centro",
                        "Cliente 3V",
                        "João Entregador",
                        "2026-08-05T13:00:00Z",
                        3,
                        9,
                        new AgentDeliverySnapshotDto(
                                "in_transit",
                                "in_progress"),
                        new AgentDeliverySnapshotDto(
                                DELIVERED,
                                DELIVERED),
                        true)),
                result(new AgentDeliveryResultDto(
                        81,
                        DELIVERED,
                        901,
                        DELIVERED,
                        10,
                        true)),
                "2026-08-01T12:00:00Z"));

        AgentDeliverySummary summary =
                (AgentDeliverySummary) action.summary();
        AgentDeliveryResult result =
                (AgentDeliveryResult) action.result().orElseThrow();
        assertEquals(
                "The delivery operation cannot change while mapping.",
                AgentDeliveryOperation.COMPLETE,
                summary.operation());
        assertEquals(
                "The reviewed starting status must remain typed.",
                DeliveryStatus.IN_TRANSIT,
                summary.before().deliveryStatus());
        assertEquals(
                "The terminal order state must remain typed.",
                OrderStatus.DELIVERED,
                result.orderStatus());
        assertEquals(
                "The terminal revision must remain explicit.",
                10,
                result.orderRevision());
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
