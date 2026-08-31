package br.com.tresvtintas.mobile.core.agent;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class AgentConversationListState {
    public enum Phase {
        EMPTY,
        LOADING,
        READY,
        REFRESHING,
        LOADING_MORE,
        CREATING,
        WARNING,
        ERROR,
        CLOSED
    }

    public record Snapshot(
            List<AgentConversation> items,
            Optional<String> nextCursor) {
        public Snapshot {
            if (items == null
                    || items.stream().anyMatch(Objects::isNull)) {
                throw new IllegalArgumentException(
                        "Agent conversation snapshot is invalid.");
            }
            items = List.copyOf(items);
            nextCursor = Objects.requireNonNull(
                    nextCursor,
                    "Agent conversation cursor is required.");
        }

        public boolean hasMore() {
            return nextCursor.isPresent();
        }

        public Snapshot append(AgentConversationPage page) {
            Map<String, AgentConversation> merged =
                    new LinkedHashMap<>();
            items.forEach(item -> merged.put(item.id(), item));
            page.items().forEach(item -> merged.put(item.id(), item));
            return new Snapshot(
                    new ArrayList<>(merged.values()),
                    page.nextCursor());
        }

        public Snapshot prepend(AgentConversation conversation) {
            List<AgentConversation> merged = new ArrayList<>();
            merged.add(conversation);
            items.stream()
                    .filter(item -> !item.id().equals(conversation.id()))
                    .forEach(merged::add);
            return new Snapshot(merged, nextCursor);
        }
    }

    private final Phase phase;
    private final Optional<Snapshot> snapshot;
    private final Optional<AgentFailureKind> failure;
    private final Optional<String> requestId;
    private final Optional<AgentConversation> created;

    private AgentConversationListState(
            Phase phase,
            Optional<Snapshot> snapshot,
            Optional<AgentFailureKind> failure,
            Optional<String> requestId,
            Optional<AgentConversation> created) {
        this.phase = Objects.requireNonNull(
                phase,
                "Agent conversation phase is required.");
        this.snapshot = Objects.requireNonNull(
                snapshot,
                "Agent conversation snapshot is required.");
        this.failure = Objects.requireNonNull(
                failure,
                "Agent conversation failure is required.");
        this.requestId = Objects.requireNonNull(
                requestId,
                "Agent conversation request ID is required.");
        this.created = Objects.requireNonNull(
                created,
                "Created agent conversation is required.");
    }

    public static AgentConversationListState empty() {
        return value(Phase.EMPTY, Optional.empty());
    }

    public static AgentConversationListState loading() {
        return value(Phase.LOADING, Optional.empty());
    }

    public static AgentConversationListState ready(Snapshot snapshot) {
        return value(Phase.READY, Optional.of(snapshot));
    }

    public static AgentConversationListState refreshing(
            Snapshot snapshot) {
        return value(Phase.REFRESHING, Optional.of(snapshot));
    }

    public static AgentConversationListState loadingMore(
            Snapshot snapshot) {
        return value(Phase.LOADING_MORE, Optional.of(snapshot));
    }

    public static AgentConversationListState creating(
            Snapshot snapshot) {
        return value(Phase.CREATING, Optional.of(snapshot));
    }

    public static AgentConversationListState created(
            Snapshot snapshot,
            AgentConversation conversation) {
        return new AgentConversationListState(
                Phase.READY,
                Optional.of(snapshot),
                Optional.empty(),
                Optional.empty(),
                Optional.of(conversation));
    }

    public static AgentConversationListState warning(
            Snapshot snapshot,
            AgentException failure) {
        return failed(Phase.WARNING, Optional.of(snapshot), failure);
    }

    public static AgentConversationListState error(
            AgentException failure) {
        return failed(Phase.ERROR, Optional.empty(), failure);
    }

    public static AgentConversationListState closed() {
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

    public Optional<AgentConversation> created() {
        return created;
    }

    private static AgentConversationListState value(
            Phase phase,
            Optional<Snapshot> snapshot) {
        return new AgentConversationListState(
                phase,
                snapshot,
                Optional.empty(),
                Optional.empty(),
                Optional.empty());
    }

    private static AgentConversationListState failed(
            Phase phase,
            Optional<Snapshot> snapshot,
            AgentException failure) {
        AgentException required = Objects.requireNonNull(
                failure,
                "Agent conversation failure is required.");
        return new AgentConversationListState(
                phase,
                snapshot,
                Optional.of(required.kind()),
                required.requestId(),
                Optional.empty());
    }
}
