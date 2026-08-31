package br.com.tresvtintas.mobile.core.agent;

import java.util.Objects;
import java.util.Optional;

public record AgentActionDecisionState(
        Phase phase,
        Optional<AgentAction> action,
        Optional<AgentActionDecision> decision,
        Optional<AgentActionDecisionResult> result,
        Optional<AgentFailureKind> failure,
        Optional<String> requestId) {
    public enum Phase {
        IDLE,
        RUNNING,
        SUCCESS,
        ERROR,
        CLOSED
    }

    public AgentActionDecisionState {
        phase = Objects.requireNonNull(
                phase,
                "Agent action decision phase is required.");
        action = Objects.requireNonNull(
                action,
                "Agent action is required.");
        decision = Objects.requireNonNull(
                decision,
                "Agent action decision is required.");
        result = Objects.requireNonNull(
                result,
                "Agent action decision result is required.");
        failure = Objects.requireNonNull(
                failure,
                "Agent action decision failure is required.");
        requestId = Objects.requireNonNull(
                requestId,
                "Agent action decision request ID is required.");
    }

    public static AgentActionDecisionState idle() {
        return value(
                Phase.IDLE,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty());
    }

    public static AgentActionDecisionState running(
            AgentAction action,
            AgentActionDecision decision) {
        return value(
                Phase.RUNNING,
                Optional.of(action),
                Optional.of(decision),
                Optional.empty(),
                Optional.empty(),
                Optional.empty());
    }

    public static AgentActionDecisionState success(
            AgentAction action,
            AgentActionDecision decision,
            AgentActionDecisionResult result) {
        return value(
                Phase.SUCCESS,
                Optional.of(action),
                Optional.of(decision),
                Optional.of(result),
                Optional.empty(),
                Optional.empty());
    }

    public static AgentActionDecisionState error(
            AgentAction action,
            AgentActionDecision decision,
            AgentException failure) {
        AgentException required = Objects.requireNonNull(
                failure,
                "Agent action decision failure is required.");
        return value(
                Phase.ERROR,
                Optional.of(action),
                Optional.of(decision),
                Optional.empty(),
                Optional.of(required.kind()),
                required.requestId());
    }

    public static AgentActionDecisionState closed() {
        return value(
                Phase.CLOSED,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty());
    }

    private static AgentActionDecisionState value(
            Phase phase,
            Optional<AgentAction> action,
            Optional<AgentActionDecision> decision,
            Optional<AgentActionDecisionResult> result,
            Optional<AgentFailureKind> failure,
            Optional<String> requestId) {
        return new AgentActionDecisionState(
                phase,
                action,
                decision,
                result,
                failure,
                requestId);
    }
}
