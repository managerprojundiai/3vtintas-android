package br.com.tresvtintas.mobile.core.agent;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class AgentConversationDetailState {
    public enum Phase {
        EMPTY,
        LOADING,
        READY,
        REFRESHING,
        LOADING_MORE,
        WARNING,
        ERROR,
        CLOSED
    }

    public record Snapshot(
            AgentConversation conversation,
            List<AgentMessage> messages,
            Optional<String> nextCursor) {
        public Snapshot {
            conversation = Objects.requireNonNull(
                    conversation,
                    "Agent conversation is required.");
            if (messages == null
                    || messages.stream().anyMatch(Objects::isNull)) {
                throw new IllegalArgumentException(
                        "Agent messages are invalid.");
            }
            messages = List.copyOf(messages);
            nextCursor = Objects.requireNonNull(
                    nextCursor,
                    "Agent message cursor is required.");
        }

        public boolean hasMore() {
            return nextCursor.isPresent();
        }

        public Optional<String> pendingTurnId() {
            return AgentPendingTurn.from(messages);
        }

        public Snapshot prepend(AgentMessagePage page) {
            if (!conversation.id().equals(page.conversationId())) {
                throw new IllegalArgumentException(
                        "Agent message page belongs to another conversation.");
            }
            Map<Long, AgentMessage> merged = new LinkedHashMap<>();
            page.items().forEach(item -> merged.put(item.id(), item));
            messages.forEach(item -> merged.put(item.id(), item));
            return new Snapshot(
                    conversation,
                    new ArrayList<>(merged.values()),
                    page.nextCursor());
        }
    }

    private final Phase phase;
    private final Optional<Snapshot> snapshot;
    private final Optional<AgentFailureKind> failure;
    private final Optional<String> requestId;

    private AgentConversationDetailState(
            Phase phase,
            Optional<Snapshot> snapshot,
            Optional<AgentFailureKind> failure,
            Optional<String> requestId) {
        this.phase = Objects.requireNonNull(
                phase,
                "Agent detail phase is required.");
        this.snapshot = Objects.requireNonNull(
                snapshot,
                "Agent detail snapshot is required.");
        this.failure = Objects.requireNonNull(
                failure,
                "Agent detail failure is required.");
        this.requestId = Objects.requireNonNull(
                requestId,
                "Agent detail request ID is required.");
    }

    public static AgentConversationDetailState empty() {
        return value(Phase.EMPTY, Optional.empty());
    }

    public static AgentConversationDetailState loading() {
        return value(Phase.LOADING, Optional.empty());
    }

    public static AgentConversationDetailState ready(Snapshot snapshot) {
        return value(Phase.READY, Optional.of(snapshot));
    }

    public static AgentConversationDetailState refreshing(
            Snapshot snapshot) {
        return value(Phase.REFRESHING, Optional.of(snapshot));
    }

    public static AgentConversationDetailState loadingMore(
            Snapshot snapshot) {
        return value(Phase.LOADING_MORE, Optional.of(snapshot));
    }

    public static AgentConversationDetailState warning(
            Snapshot snapshot,
            AgentException failure) {
        return failed(Phase.WARNING, Optional.of(snapshot), failure);
    }

    public static AgentConversationDetailState error(
            AgentException failure) {
        return failed(Phase.ERROR, Optional.empty(), failure);
    }

    public static AgentConversationDetailState closed() {
        return value(Phase.CLOSED, Optional.empty());
    }

    public Phase phase() {
        return phase;
    }

    public Optional<Snapshot> snapshot() {
        return snapshot;
    }

    public Optional<AgentFailureKind> failure() {
        return failure;
    }

    public Optional<String> requestId() {
        return requestId;
    }

    private static AgentConversationDetailState value(
            Phase phase,
            Optional<Snapshot> snapshot) {
        return new AgentConversationDetailState(
                phase,
                snapshot,
                Optional.empty(),
                Optional.empty());
    }

    private static AgentConversationDetailState failed(
            Phase phase,
            Optional<Snapshot> snapshot,
            AgentException failure) {
        AgentException required = Objects.requireNonNull(
                failure,
                "Agent detail failure is required.");
        return new AgentConversationDetailState(
                phase,
                snapshot,
                Optional.of(required.kind()),
                required.requestId());
    }
}
