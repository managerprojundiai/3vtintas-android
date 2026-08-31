package br.com.tresvtintas.mobile.core.agent;

import java.util.Objects;
import java.util.Optional;

public final class AgentTurnState {
    public enum Phase {
        IDLE,
        SUBMITTING,
        QUEUED,
        RUNNING,
        RECONNECTING,
        CANCELLING,
        COMPLETED,
        FAILED,
        CANCELLED,
        ERROR,
        CLOSED
    }

    private static final int MAXIMUM_PARTIAL_CHARS = 8_000;
    private final Phase phase;
    private final Optional<String> conversationId;
    private final Optional<AgentTurn> turn;
    private final String partialText;
    private final Optional<String> lastEventId;
    private final Optional<AgentFailureKind> failure;
    private final Optional<String> requestId;
    private final Optional<String> runtimeFailureCode;
    private final int reconnectAttempt;
    private final boolean toolActive;
    private final boolean requiresHuman;
    private final boolean blocked;

    private AgentTurnState(
            Phase phase,
            Optional<String> conversationId,
            Optional<AgentTurn> turn,
            String partialText,
            Optional<String> lastEventId,
            Optional<AgentFailureKind> failure,
            Optional<String> requestId,
            Optional<String> runtimeFailureCode,
            int reconnectAttempt,
            boolean toolActive,
            boolean requiresHuman,
            boolean blocked) {
        this.phase = Objects.requireNonNull(
                phase,
                "Agent turn phase is required.");
        this.conversationId = Objects.requireNonNull(
                conversationId,
                "Agent turn conversation is required.");
        this.turn = Objects.requireNonNull(
                turn,
                "Agent turn is required.");
        if (partialText == null
                || partialText.length() > MAXIMUM_PARTIAL_CHARS) {
            throw new IllegalArgumentException(
                    "Agent partial response is invalid.");
        }
        this.partialText = partialText;
        this.lastEventId = Objects.requireNonNull(
                lastEventId,
                "Agent event cursor is required.");
        this.failure = Objects.requireNonNull(
                failure,
                "Agent turn failure is required.");
        this.requestId = Objects.requireNonNull(
                requestId,
                "Agent turn request ID is required.");
        this.runtimeFailureCode = Objects.requireNonNull(
                runtimeFailureCode,
                "Agent runtime failure is required.");
        if (reconnectAttempt < 0) {
            throw new IllegalArgumentException(
                    "Agent reconnect attempt is invalid.");
        }
        this.reconnectAttempt = reconnectAttempt;
        this.toolActive = toolActive;
        this.requiresHuman = requiresHuman;
        this.blocked = blocked;
    }

    public static AgentTurnState idle(String conversationId) {
        return value(
                Phase.IDLE,
                conversation(conversationId),
                Optional.empty(),
                "",
                Optional.empty());
    }

    public static AgentTurnState submitting(String conversationId) {
        return value(
                Phase.SUBMITTING,
                conversation(conversationId),
                Optional.empty(),
                "",
                Optional.empty());
    }

    public static AgentTurnState queued(AgentTurn turn) {
        return queued(turn, Optional.empty());
    }

    public static AgentTurnState queued(
            AgentTurn turn,
            Optional<String> cursor) {
        return active(
                Phase.QUEUED,
                turn,
                "",
                Objects.requireNonNull(
                        cursor,
                        "Agent queued cursor is required."),
                0,
                false);
    }

    public static AgentTurnState running(
            AgentTurn turn,
            String partialText,
            Optional<String> cursor,
            boolean toolActive) {
        return active(
                Phase.RUNNING,
                turn,
                partialText,
                cursor,
                0,
                toolActive);
    }

    public static AgentTurnState reconnecting(
            AgentTurn turn,
            String partialText,
            Optional<String> cursor,
            AgentFailureKind failure,
            int attempt) {
        return new AgentTurnState(
                Phase.RECONNECTING,
                Optional.of(turn.conversationId()),
                Optional.of(turn),
                partialText,
                cursor,
                Optional.of(failure),
                Optional.empty(),
                Optional.empty(),
                attempt,
                false,
                false,
                false);
    }

    public static AgentTurnState cancelling(
            AgentTurnState current) {
        AgentTurn value = current.turn.orElseThrow(() ->
                new IllegalArgumentException(
                        "Agent turn is required for cancellation."));
        return active(
                Phase.CANCELLING,
                value,
                current.partialText,
                current.lastEventId,
                current.reconnectAttempt,
                false);
    }

    public static AgentTurnState terminal(
            AgentTurn turn,
            String partialText,
            Optional<String> cursor,
            Optional<String> runtimeFailureCode,
            boolean requiresHuman,
            boolean blocked) {
        Phase phase = switch (turn.status()) {
            case COMPLETED -> Phase.COMPLETED;
            case FAILED -> Phase.FAILED;
            case CANCELLED -> Phase.CANCELLED;
            default -> throw new IllegalArgumentException(
                    "Agent terminal turn is invalid.");
        };
        return new AgentTurnState(
                phase,
                Optional.of(turn.conversationId()),
                Optional.of(turn),
                partialText,
                cursor,
                Optional.empty(),
                Optional.empty(),
                runtimeFailureCode,
                0,
                false,
                requiresHuman,
                blocked);
    }

    public static AgentTurnState error(
            String conversationId,
            Optional<AgentTurn> turn,
            String partialText,
            Optional<String> cursor,
            AgentException failure) {
        AgentException required = Objects.requireNonNull(
                failure,
                "Agent turn failure is required.");
        return new AgentTurnState(
                Phase.ERROR,
                conversation(conversationId),
                turn,
                partialText,
                cursor,
                Optional.of(required.kind()),
                required.requestId(),
                Optional.empty(),
                0,
                false,
                false,
                false);
    }

    public static AgentTurnState closed() {
        return value(
                Phase.CLOSED,
                Optional.empty(),
                Optional.empty(),
                "",
                Optional.empty());
    }

    public Phase phase() {
        return phase;
    }

    public Optional<String> conversationId() {
        return conversationId;
    }

    public Optional<AgentTurn> turn() {
        return turn;
    }

    public String partialText() {
        return partialText;
    }

    public Optional<String> lastEventId() {
        return lastEventId;
    }

    public Optional<AgentFailureKind> failure() {
        return failure;
    }

    public Optional<String> requestId() {
        return requestId;
    }

    public Optional<String> runtimeFailureCode() {
        return runtimeFailureCode;
    }

    public int reconnectAttempt() {
        return reconnectAttempt;
    }

    public boolean toolActive() {
        return toolActive;
    }

    public boolean requiresHuman() {
        return requiresHuman;
    }

    public boolean blocked() {
        return blocked;
    }

    public boolean canSend() {
        return switch (phase) {
            case IDLE, COMPLETED, FAILED, CANCELLED, ERROR -> true;
            default -> false;
        };
    }

    public boolean canCancel() {
        return switch (phase) {
            case QUEUED, RUNNING, RECONNECTING -> true;
            case ERROR -> turn
                    .map(value -> !value.status().terminal())
                    .orElse(false);
            default -> false;
        };
    }

    public boolean active() {
        return switch (phase) {
            case SUBMITTING, QUEUED, RUNNING, RECONNECTING, CANCELLING -> true;
            default -> false;
        };
    }

    public boolean terminal() {
        return phase == Phase.COMPLETED
                || phase == Phase.FAILED
                || phase == Phase.CANCELLED;
    }

    private static AgentTurnState active(
            Phase phase,
            AgentTurn turn,
            String partialText,
            Optional<String> cursor,
            int reconnectAttempt,
            boolean toolActive) {
        return new AgentTurnState(
                phase,
                Optional.of(turn.conversationId()),
                Optional.of(turn),
                partialText,
                cursor,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                reconnectAttempt,
                toolActive,
                false,
                false);
    }

    private static AgentTurnState value(
            Phase phase,
            Optional<String> conversationId,
            Optional<AgentTurn> turn,
            String partialText,
            Optional<String> cursor) {
        return new AgentTurnState(
                phase,
                conversationId,
                turn,
                partialText,
                cursor,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                0,
                false,
                false,
                false);
    }

    private static Optional<String> conversation(String value) {
        return Optional.of(AgentIdentifiers.requireUuid(
                value,
                "Agent conversation ID is invalid."));
    }
}
