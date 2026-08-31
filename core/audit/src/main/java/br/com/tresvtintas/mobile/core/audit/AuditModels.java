package br.com.tresvtintas.mobile.core.audit;

import br.com.tresvtintas.mobile.core.model.AppRole;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class AuditModels {
    private AuditModels() {
        throw new AssertionError("No instances.");
    }

    public static Page page(List<Event> items, Optional<String> nextCursor) {
        return new Page(items, nextCursor);
    }

    public record Actor(String name, AppRole role) {
        public Actor {
            name = requireText(name, "Audit actor name", 200);
            Objects.requireNonNull(role, "Audit actor role is required.");
        }
    }

    public record Event(
            String action,
            Optional<String> entity,
            Optional<Actor> actor,
            Instant occurredAt) {
        public Event {
            action = requireText(action, "Audit action", 100);
            entity = optionalText(entity, "Audit entity", 100);
            actor = actor == null ? Optional.empty() : actor;
            Objects.requireNonNull(occurredAt, "Audit occurrence is required.");
        }
    }

    public record Page(List<Event> items, Optional<String> nextCursor) {
        public Page {
            if (items == null || items.size() > 100
                    || items.stream().anyMatch(Objects::isNull)) {
                throw new IllegalArgumentException("Audit page is invalid.");
            }
            items = List.copyOf(items);
            nextCursor = optionalText(nextCursor, "Audit cursor", 128);
        }

        @Override
        public List<Event> items() {
            return List.copyOf(items);
        }
    }

    private static String requireText(String value, String label, int maximum) {
        if (value == null || value.isBlank() || value.length() > maximum) {
            throw new IllegalArgumentException(label + " is invalid.");
        }
        return value.strip();
    }

    private static Optional<String> optionalText(
            Optional<String> value,
            String label,
            int maximum) {
        if (value == null || value.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(requireText(value.orElseThrow(), label, maximum));
    }
}
