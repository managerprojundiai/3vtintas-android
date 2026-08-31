package br.com.tresvtintas.mobile.feature.agent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;

import br.com.tresvtintas.mobile.core.agent.AgentActionDecision;
import org.junit.Test;

public final class AgentActionDecisionAttemptTest {
    private static final String ACTION_ID =
            "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa";
    private static final String KEY =
            "bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb";

    @Test
    public void keepsTheSameDecisionAndKeyAcrossRetryAndRotation() {
        AgentActionDecisionAttempt attempt =
                new AgentActionDecisionAttempt();
        String first = attempt.keyFor(
                ACTION_ID,
                AgentActionDecision.CONFIRM);
        String retry = attempt.keyFor(
                ACTION_ID,
                AgentActionDecision.CONFIRM);
        AgentActionDecisionAttempt restored =
                AgentActionDecisionAttempt.restored(
                        first,
                        attempt.actionId(),
                        attempt.decisionName());

        assertEquals(
                "A retry must preserve the logical idempotency key.",
                first,
                retry);
        assertEquals(
                "Rotation must preserve the same logical attempt.",
                first,
                restored.keyFor(
                        ACTION_ID,
                        AgentActionDecision.CONFIRM));
        assertEquals(
                "The pending decision must remain locked.",
                AgentActionDecision.CONFIRM,
                restored.decisionFor(ACTION_ID).orElseThrow());
    }

    @Test
    public void refusesDecisionChangesAfterAnUncertainResponse() {
        AgentActionDecisionAttempt attempt =
                AgentActionDecisionAttempt.restored(
                        KEY,
                        ACTION_ID,
                        AgentActionDecision.CONFIRM.name());

        assertThrows(
                "A possibly executed confirmation cannot become reject.",
                IllegalStateException.class,
                () -> attempt.keyFor(
                        ACTION_ID,
                        AgentActionDecision.REJECT));
        attempt.reset();
        assertFalse(
                "A terminal reconciliation must clear the attempt.",
                attempt.decisionFor(ACTION_ID).isPresent());
    }
}
