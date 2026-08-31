package br.com.tresvtintas.mobile.data.agent;

import static org.junit.Assert.assertEquals;

import br.com.tresvtintas.mobile.core.agent.AgentAction;
import br.com.tresvtintas.mobile.core.agent.AgentAttendanceReplyResult;
import br.com.tresvtintas.mobile.core.agent.AgentAttendanceReplySummary;
import br.com.tresvtintas.mobile.core.network.dto.AgentActionDto;
import br.com.tresvtintas.mobile.core.network.dto.AgentActionResultDto;
import br.com.tresvtintas.mobile.core.network.dto.AgentActionSummaryDto;
import br.com.tresvtintas.mobile.core.network.dto.AgentAttendanceLatestMessageDto;
import java.util.List;
import org.junit.Test;

public final class AgentAttendanceDtoMapperTest {
    @Test
    public void mapsThePublishedAttendanceActionIntoDedicatedDomainTypes() {
        AgentAction action = AgentDtoMapper.action(new AgentActionDto(
                "dddddddd-dddd-4ddd-8ddd-dddddddddddd",
                "attendance_reply",
                "executed",
                "Enviar resposta de atendimento",
                new AgentActionSummaryDto(
                        null,
                        null,
                        "Cliente 3V",
                        null,
                        null,
                        null,
                        List.of(),
                        null,
                        null,
                        null,
                        false,
                        7,
                        List.of(),
                        null,
                        null,
                        "whatsapp:701",
                        "whatsapp",
                        "Loja Centro",
                        "Sim, entregamos hoje.",
                        new AgentAttendanceLatestMessageDto(
                                "inbound",
                                "Vocês entregam hoje?",
                                "2026-07-29T12:00:00Z"),
                        true),
                new AgentActionResultDto(
                        null,
                        null,
                        null,
                        null,
                        null,
                        "whatsapp:701",
                        "queued",
                        "whatsapp:701:801"),
                "2026-07-29T12:10:00Z"));

        assertEquals(
                "Attendance summaries cannot be mistaken for quote summaries.",
                AgentAttendanceReplySummary.class,
                action.summary().getClass());
        assertEquals(
                "The exact reply must survive transport mapping.",
                "Sim, entregamos hoje.",
                ((AgentAttendanceReplySummary) action.summary()).content());
        assertEquals(
                "Attendance results cannot become navigable quote results.",
                AgentAttendanceReplyResult.class,
                action.result().orElseThrow().getClass());
    }
}
