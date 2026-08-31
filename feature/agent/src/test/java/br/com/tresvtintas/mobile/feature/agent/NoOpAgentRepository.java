package br.com.tresvtintas.mobile.feature.agent;

import br.com.tresvtintas.mobile.core.agent.AgentActionDecision;
import br.com.tresvtintas.mobile.core.agent.AgentActionDecisionResult;
import br.com.tresvtintas.mobile.core.agent.AgentConversation;
import br.com.tresvtintas.mobile.core.agent.AgentConversationPage;
import br.com.tresvtintas.mobile.core.agent.AgentMessagePage;
import br.com.tresvtintas.mobile.core.agent.AgentRepository;
import br.com.tresvtintas.mobile.core.agent.AgentTurn;
import java.util.Optional;

final class NoOpAgentRepository implements AgentRepository {
    private static final String NOT_USED =
            "Not used by this test.";

    @Override
    public AgentConversationPage conversations(
            Optional<String> cursor,
            int limit) {
        throw new UnsupportedOperationException(NOT_USED);
    }

    @Override
    public AgentConversation createConversation(
            String title,
            String idempotencyKey) {
        throw new UnsupportedOperationException(NOT_USED);
    }

    @Override
    public AgentConversation conversation(String conversationId) {
        throw new UnsupportedOperationException(NOT_USED);
    }

    @Override
    public AgentMessagePage messages(
            String conversationId,
            Optional<String> cursor,
            int limit) {
        throw new UnsupportedOperationException(NOT_USED);
    }

    @Override
    public AgentTurn enqueue(
            String conversationId,
            String message,
            String idempotencyKey) {
        throw new UnsupportedOperationException(NOT_USED);
    }

    @Override
    public AgentTurn turn(String turnId) {
        throw new UnsupportedOperationException(NOT_USED);
    }

    @Override
    public AgentTurn cancel(
            String turnId,
            String idempotencyKey) {
        throw new UnsupportedOperationException(NOT_USED);
    }

    @Override
    public AgentActionDecisionResult decideAction(
            String actionId,
            AgentActionDecision decision,
            String idempotencyKey) {
        throw new UnsupportedOperationException(NOT_USED);
    }
}
