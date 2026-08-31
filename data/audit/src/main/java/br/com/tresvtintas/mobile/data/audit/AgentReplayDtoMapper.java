package br.com.tresvtintas.mobile.data.audit;

import br.com.tresvtintas.mobile.core.audit.AgentReplayModels;
import br.com.tresvtintas.mobile.core.audit.AgentReplayModels.Page;
import br.com.tresvtintas.mobile.core.audit.AgentReplayModels.Step;
import br.com.tresvtintas.mobile.core.audit.AgentReplayModels.Turn;
import br.com.tresvtintas.mobile.core.network.dto.AgentReplayPageDto;
import java.time.Instant;
import java.util.Optional;

final class AgentReplayDtoMapper {
    private AgentReplayDtoMapper() {
        throw new AssertionError("No instances.");
    }

    static Page page(AgentReplayPageDto value) {
        return AgentReplayModels.page(
                value.items().stream().map(AgentReplayDtoMapper::turn).toList(),
                Optional.ofNullable(value.nextCursor()));
    }

    private static Turn turn(AgentReplayPageDto.Turn value) {
        return new Turn(
                AgentReplayModels.Channel.fromWireValue(value.channel()),
                Instant.parse(value.startedAt()),
                Instant.parse(value.endedAt()),
                AgentReplayModels.Outcome.fromWireValue(value.outcome()),
                value.steps().stream().map(AgentReplayDtoMapper::step).toList(),
                value.stepsTruncated());
    }

    private static Step step(AgentReplayPageDto.Step value) {
        return new Step(
                AgentReplayModels.Phase.fromWireValue(value.phase()),
                AgentReplayModels.StepOutcome.fromWireValue(value.outcome()),
                Instant.parse(value.occurredAt()));
    }
}
