package br.com.tresvtintas.mobile.core.agent;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public record AgentTurn(
        String id,
        String conversationId,
        AgentTurnStatus status,
        Instant createdAt,
        Optional<Instant> startedAt,
        Optional<Instant> completedAt) {
    public AgentTurn {
        id = AgentIdentifiers.requireUuid(
                id,
                "Agent turn ID is invalid.");
        conversationId = AgentIdentifiers.requireUuid(
                conversationId,
                "Agent turn conversation ID is invalid.");
        status = Objects.requireNonNull(
                status,
                "Agent turn status is required.");
        createdAt = Objects.requireNonNull(
                createdAt,
                "Agent turn creation is required.");
        startedAt = Objects.requireNonNull(
                startedAt,
                "Agent turn start is required.");
        completedAt = Objects.requireNonNull(
                completedAt,
                "Agent turn completion is required.");
        if (status == AgentTurnStatus.QUEUED
                && (startedAt.isPresent() || completedAt.isPresent())) {
            throw new IllegalArgumentException(
                    "Queued agent turn timestamps are inconsistent.");
        }
        if (status == AgentTurnStatus.RUNNING
                && (startedAt.isEmpty() || completedAt.isPresent())) {
            throw new IllegalArgumentException(
                    "Running agent turn timestamps are inconsistent.");
        }
        if (status.terminal() && completedAt.isEmpty()) {
            throw new IllegalArgumentException(
                    "Terminal agent turn completion is missing.");
        }
    }

    public AgentTurn withStatus(
            AgentTurnStatus next,
            Instant occurredAt) {
        AgentTurnStatus required = Objects.requireNonNull(
                next,
                "Agent turn status is required.");
        Instant time = Objects.requireNonNull(
                occurredAt,
                "Agent turn status time is required.");
        return new AgentTurn(
                id,
                conversationId,
                required,
                createdAt,
                required == AgentTurnStatus.RUNNING
                        ? Optional.of(startedAt.orElse(time))
                        : startedAt,
                required.terminal()
                        ? Optional.of(time)
                        : Optional.empty());
    }
}
