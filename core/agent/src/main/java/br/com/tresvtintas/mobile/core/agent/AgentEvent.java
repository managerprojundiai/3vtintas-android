package br.com.tresvtintas.mobile.core.agent;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public record AgentEvent(
        String turnId,
        int sequence,
        AgentEventKind kind,
        Instant createdAt,
        Optional<String> textDelta,
        boolean resetPartialResponse,
        Optional<String> failureCode,
        boolean requiresHuman,
        boolean blocked) {
    private static final int MAXIMUM_DELTA_CHARS = 8_000;
    private static final int MAXIMUM_FAILURE_CODE_CHARS = 80;
    private static final int MINIMUM_SEQUENCE = 1;

    public AgentEvent {
        turnId = AgentIdentifiers.requireUuid(
                turnId,
                "Agent event turn ID is invalid.");
        if (sequence < MINIMUM_SEQUENCE) {
            throw new IllegalArgumentException(
                    "Agent event sequence is invalid.");
        }
        kind = Objects.requireNonNull(
                kind,
                "Agent event kind is required.");
        createdAt = Objects.requireNonNull(
                createdAt,
                "Agent event creation is required.");
        textDelta = Objects.requireNonNull(
                textDelta,
                "Agent event text delta is required.");
        failureCode = Objects.requireNonNull(
                failureCode,
                "Agent event failure code is required.");
        textDelta.ifPresent(value -> {
            if (value.isEmpty()
                    || value.length() > MAXIMUM_DELTA_CHARS) {
                throw new IllegalArgumentException(
                        "Agent event text delta is invalid.");
            }
        });
        failureCode.ifPresent(value -> {
            if (value.isBlank()
                    || value.length() > MAXIMUM_FAILURE_CODE_CHARS
                    || !value.matches("^[a-z0-9_]+$")) {
                throw new IllegalArgumentException(
                        "Agent event failure code is invalid.");
            }
        });
        if ((kind == AgentEventKind.TEXT_DELTA)
                != textDelta.isPresent()) {
            throw new IllegalArgumentException(
                    "Agent event text payload is inconsistent.");
        }
        if ((kind == AgentEventKind.FAILED)
                != failureCode.isPresent()) {
            throw new IllegalArgumentException(
                    "Agent event failure payload is inconsistent.");
        }
        if (resetPartialResponse
                && kind != AgentEventKind.STARTED) {
            throw new IllegalArgumentException(
                    "Agent event reset marker is inconsistent.");
        }
        if ((requiresHuman || blocked)
                && kind != AgentEventKind.COMPLETED) {
            throw new IllegalArgumentException(
                    "Agent event completion payload is inconsistent.");
        }
    }

    public String cursor() {
        return Integer.toString(sequence);
    }
}
