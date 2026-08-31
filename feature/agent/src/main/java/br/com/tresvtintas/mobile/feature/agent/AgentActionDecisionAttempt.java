package br.com.tresvtintas.mobile.feature.agent;

import br.com.tresvtintas.mobile.core.agent.AgentActionDecision;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

final class AgentActionDecisionAttempt {
    private String key = "";
    private String actionId = "";
    private Optional<AgentActionDecision> decision = Optional.empty();

    static AgentActionDecisionAttempt restored(
            String key,
            String actionId,
            String decisionName) {
        AgentActionDecisionAttempt result =
                new AgentActionDecisionAttempt();
        if (!validUuid(key) || !validUuid(actionId)) {
            return result;
        }
        try {
            result.decision = Optional.of(
                    AgentActionDecision.valueOf(decisionName));
            result.key = key;
            result.actionId = actionId.toLowerCase(Locale.ROOT);
            return result;
        } catch (IllegalArgumentException failure) {
            return new AgentActionDecisionAttempt();
        }
    }

    String keyFor(
            String requestedActionId,
            AgentActionDecision requestedDecision) {
        String requiredActionId = UUID.fromString(requestedActionId)
                .toString()
                .toLowerCase(Locale.ROOT);
        if (!key.isEmpty()
                && requiredActionId.equals(actionId)
                && decision.filter(value ->
                        value != requestedDecision).isPresent()) {
            throw new IllegalStateException(
                    "A pending action decision cannot be changed.");
        }
        if (key.isEmpty() || !requiredActionId.equals(actionId)) {
            key = UUID.randomUUID().toString();
            actionId = requiredActionId;
            decision = Optional.of(requestedDecision);
        }
        return key;
    }

    Optional<AgentActionDecision> decisionFor(
            String requestedActionId) {
        return actionId.equalsIgnoreCase(requestedActionId)
                ? decision
                : Optional.empty();
    }

    String key() {
        return key;
    }

    String actionId() {
        return actionId;
    }

    String decisionName() {
        return decision.map(Enum::name).orElse("");
    }

    void reset() {
        key = "";
        actionId = "";
        decision = Optional.empty();
    }

    private static boolean validUuid(String value) {
        if (value == null) {
            return false;
        }
        try {
            return value.equalsIgnoreCase(
                    UUID.fromString(value).toString());
        } catch (IllegalArgumentException failure) {
            return false;
        }
    }
}
