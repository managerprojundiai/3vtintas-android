package br.com.tresvtintas.mobile.data.agent;

import static org.junit.Assert.assertEquals;

import br.com.tresvtintas.mobile.core.agent.AgentAction;
import br.com.tresvtintas.mobile.core.agent.AgentOrderOperation;
import br.com.tresvtintas.mobile.core.agent.AgentOrderResult;
import br.com.tresvtintas.mobile.core.agent.AgentOrderSummary;
import br.com.tresvtintas.mobile.core.network.dto.AgentActionDto;
import br.com.tresvtintas.mobile.core.network.dto.AgentActionResultDto;
import br.com.tresvtintas.mobile.core.network.dto.AgentActionSummaryDto;
import br.com.tresvtintas.mobile.core.network.dto.AgentOrderResultDto;
import br.com.tresvtintas.mobile.core.network.dto.AgentOrderSnapshotDto;
import br.com.tresvtintas.mobile.core.network.dto.AgentOrderSummaryDto;
import br.com.tresvtintas.mobile.core.order.OrderPaymentStatus;
import br.com.tresvtintas.mobile.core.order.OrderStatus;
import br.com.tresvtintas.mobile.core.order.OrderType;
import java.util.List;
import org.junit.Test;

public final class AgentOrderDtoMapperTest {
    @Test
    public void mapsThePublishedOrderIntoDedicatedDomainTypes() {
        AgentAction action = AgentDtoMapper.action(new AgentActionDto(
                "22222222-2222-4222-8222-222222222222",
                "order_start_fulfillment",
                "executed",
                "Iniciar separação do pedido",
                summary(new AgentOrderSummaryDto(
                        901,
                        "start_fulfillment",
                        "material",
                        "3V Tintas — Loja Centro",
                        "Cliente 3V",
                        "450.00",
                        3,
                        9,
                        new AgentOrderSnapshotDto(
                                "confirmed",
                                "pending"),
                        new AgentOrderSnapshotDto(
                                "in_progress",
                                "pending"),
                        true)),
                result(new AgentOrderResultDto(
                        901,
                        "in_progress",
                        10,
                        "pending",
                        true)),
                "2026-08-01T12:00:00Z"));

        AgentOrderSummary summary =
                (AgentOrderSummary) action.summary();
        AgentOrderResult result =
                (AgentOrderResult) action.result().orElseThrow();
        assertEquals(
                "The operation cannot change while mapping.",
                AgentOrderOperation.START_FULFILLMENT,
                summary.operation());
        assertEquals(
                "The order type must remain typed.",
                OrderType.MATERIAL,
                summary.orderType());
        assertEquals(
                "The terminal state must remain typed.",
                OrderStatus.IN_PROGRESS,
                result.status());
        assertEquals(
                "The payment state must remain typed.",
                OrderPaymentStatus.PENDING,
                result.paymentStatus());
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
