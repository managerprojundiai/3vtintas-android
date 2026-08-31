package br.com.tresvtintas.mobile.core.audit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import br.com.tresvtintas.mobile.core.audit.AgentReplayModels.Channel;
import br.com.tresvtintas.mobile.core.audit.AgentReplayModels.Outcome;
import br.com.tresvtintas.mobile.core.audit.AgentReplayModels.Phase;
import br.com.tresvtintas.mobile.core.audit.AgentReplayModels.Step;
import br.com.tresvtintas.mobile.core.audit.AgentReplayModels.StepOutcome;
import br.com.tresvtintas.mobile.core.audit.AgentReplayModels.Turn;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.Test;

public final class AgentReplayModelsTest {
    private static final Instant START = Instant.parse("2026-07-31T12:00:00Z");
    private static final Instant END = Instant.parse("2026-07-31T12:00:03Z");

    @Test
    public void acceptsOnlyTheSanitizedChronologicalProjection() {
        Turn turn = turn(List.of(
                new Step(Phase.RECEIVED, StepOutcome.OK, START),
                new Step(Phase.COMPLETED, StepOutcome.OK, END)));

        assertEquals("The public channel must remain exact.",
                Channel.SITE_CHAT, turn.channel());
        assertEquals("Only sanitized phases may compose the replay.",
                2, turn.steps().size());
        assertEquals("Wire values must map case-insensitively.",
                Channel.MOBILE_APP, Channel.fromWireValue("mobile_app"));
    }

    @Test
    public void rejectsOutOfRangeStepsAndUnsafeCursors() {
        Step late = new Step(
                Phase.COMPLETED,
                StepOutcome.OK,
                END.plusSeconds(1));

        assertThrows(
                "Steps outside the public turn envelope must fail closed.",
                IllegalArgumentException.class,
                () -> turn(List.of(late)));
        assertThrows(
                "Opaque cursors may not carry query or control characters.",
                IllegalArgumentException.class,
                () -> AgentReplayModels.page(
                        List.of(turn(List.of())),
                        Optional.of("cursor?internal=true")));
        assertThrows(
                "Unknown internal phases cannot enter the UI model.",
                IllegalArgumentException.class,
                () -> Phase.fromWireValue("raw_prompt_saved"));
    }

    private static Turn turn(List<Step> steps) {
        return new Turn(
                Channel.SITE_CHAT,
                START,
                END,
                Outcome.COMPLETED,
                steps,
                false);
    }
}
