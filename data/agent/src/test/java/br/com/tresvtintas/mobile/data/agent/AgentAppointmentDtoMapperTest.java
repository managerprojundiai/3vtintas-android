package br.com.tresvtintas.mobile.data.agent;

import static org.junit.Assert.assertEquals;

import br.com.tresvtintas.mobile.core.agent.AgentAction;
import br.com.tresvtintas.mobile.core.agent.AgentAppointmentOperation;
import br.com.tresvtintas.mobile.core.agent.AgentAppointmentResult;
import br.com.tresvtintas.mobile.core.agent.AgentAppointmentSummary;
import br.com.tresvtintas.mobile.core.network.dto.AgentActionDto;
import br.com.tresvtintas.mobile.core.network.dto.AgentActionResultDto;
import br.com.tresvtintas.mobile.core.network.dto.AgentActionSummaryDto;
import br.com.tresvtintas.mobile.core.network.dto.AgentAppointmentResultDto;
import br.com.tresvtintas.mobile.core.network.dto.AgentAppointmentSnapshotDto;
import br.com.tresvtintas.mobile.core.network.dto.AgentAppointmentSummaryDto;
import java.util.List;
import org.junit.Test;

public final class AgentAppointmentDtoMapperTest {
    @Test
    public void mapsThePublishedAppointmentIntoDedicatedDomainTypes() {
        AgentAppointmentSnapshotDto before =
                new AgentAppointmentSnapshotDto(
                        "confirmed",
                        "2026-08-03T13:30:00Z",
                        60,
                        "Loja Centro");
        AgentAppointmentSnapshotDto after =
                new AgentAppointmentSnapshotDto(
                        "confirmed",
                        "2026-08-04T17:00:00Z",
                        90,
                        "Loja Centro");
        AgentAppointmentSummaryDto appointment =
                new AgentAppointmentSummaryDto(
                        71L,
                        "reschedule",
                        "Visita técnica",
                        "general",
                        "Carlos Pereira",
                        "Loja Centro",
                        "Cliente 3V",
                        4,
                        before,
                        after,
                        true);
        AgentAction action = AgentDtoMapper.action(new AgentActionDto(
                "eeeeeeee-eeee-4eee-8eee-eeeeeeeeeeee",
                "appointment_reschedule",
                "executed",
                "Reagendar compromisso",
                summary(appointment),
                new AgentActionResultDto(
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        new AgentAppointmentResultDto(
                                71,
                                "confirmed",
                                5,
                                true)),
                "2026-07-29T12:10:00Z"));

        AgentAppointmentSummary mapped =
                (AgentAppointmentSummary) action.summary();
        assertEquals(
                "The operation cannot change while mapping.",
                AgentAppointmentOperation.RESCHEDULE,
                mapped.operation());
        assertEquals(
                "The former schedule must remain available for review.",
                before.scheduledAt(),
                mapped.before().orElseThrow().scheduledAt().toString());
        assertEquals(
                "The result cannot become a quote result.",
                AgentAppointmentResult.class,
                action.result().orElseThrow().getClass());
    }

    private static AgentActionSummaryDto summary(
            AgentAppointmentSummaryDto appointment) {
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
                appointment);
    }
}
