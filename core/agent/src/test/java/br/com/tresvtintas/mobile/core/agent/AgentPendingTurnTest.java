package br.com.tresvtintas.mobile.core.agent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.Test;

public final class AgentPendingTurnTest {
    private static final String TURN_ONE =
            "11111111-1111-4111-8111-111111111111";
    private static final String TURN_TWO =
            "22222222-2222-4222-8222-222222222222";

    @Test
    public void findsNewestUnansweredUserTurn() {
        List<AgentMessage> messages = List.of(
                message(1, AgentMessageRole.USER, TURN_ONE),
                message(2, AgentMessageRole.ASSISTANT, TURN_ONE),
                message(3, AgentMessageRole.USER, TURN_TWO));

        assertEquals(
                "Newest unanswered user turn must be recoverable.",
                Optional.of(TURN_TWO),
                AgentPendingTurn.from(messages));
    }

    @Test
    public void returnsEmptyWhenEveryTurnHasPublicResponse() {
        List<AgentMessage> messages = List.of(
                message(1, AgentMessageRole.USER, TURN_ONE),
                message(2, AgentMessageRole.ASSISTANT, TURN_ONE));

        assertTrue(
                "Answered history must not reopen a stream.",
                AgentPendingTurn.from(messages).isEmpty());
    }

    private static AgentMessage message(
            long id,
            AgentMessageRole role,
            String turnId) {
        return new AgentMessage(
                id,
                role,
                role.wireValue(),
                List.of(),
                List.of(),
                Optional.of(turnId),
                Instant.parse("2026-07-27T12:00:00Z")
                        .plusSeconds(id));
    }
}
