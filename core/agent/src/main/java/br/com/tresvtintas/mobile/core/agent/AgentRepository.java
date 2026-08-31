package br.com.tresvtintas.mobile.core.agent;

import java.util.Objects;
import java.util.Optional;

public interface AgentRepository {
    AgentConversationPage conversations(
            Optional<String> cursor,
            int limit) throws AgentException;

    AgentConversation createConversation(
            String title,
            String idempotencyKey) throws AgentException;

    AgentConversation conversation(
            String conversationId) throws AgentException;

    AgentMessagePage messages(
            String conversationId,
            Optional<String> cursor,
            int limit) throws AgentException;

    AgentTurn enqueue(
            String conversationId,
            String message,
            String idempotencyKey) throws AgentException;

    AgentTurn turn(String turnId) throws AgentException;

    AgentTurn cancel(
            String turnId,
            String idempotencyKey) throws AgentException;

    AgentActionDecisionResult decideAction(
            String actionId,
            AgentActionDecision decision,
            String idempotencyKey) throws AgentException;

    default AgentActionDecisionResult decideAction(
            String actionId,
            AgentActionDecision decision,
            String idempotencyKey,
            Optional<String> stepUpToken) throws AgentException {
        Objects.requireNonNull(
                stepUpToken,
                "Agent step-up token is required.");
        if (stepUpToken.isPresent()) {
            throw new AgentException(
                    AgentFailureKind.PROTOCOL,
                    "Agent repository does not support step-up.");
        }
        return decideAction(actionId, decision, idempotencyKey);
    }

    default AgentStepUpChallenge requestStepUp(
            String actionId) throws AgentException {
        throw new AgentException(
                AgentFailureKind.PROTOCOL,
                "Agent repository does not support step-up.");
    }

    default AgentStepUpGrant verifyStepUp(
            String actionId,
            String challengeId,
            String credential) throws AgentException {
        throw new AgentException(
                AgentFailureKind.PROTOCOL,
                "Agent repository does not support step-up.");
    }
}
