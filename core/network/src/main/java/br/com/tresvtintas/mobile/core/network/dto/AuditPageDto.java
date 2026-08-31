package br.com.tresvtintas.mobile.core.network.dto;

import java.util.List;

public record AuditPageDto(List<Event> items, String nextCursor) {
    public AuditPageDto {
        if (items == null || items.size() > 100
                || items.stream().anyMatch(value -> value == null)) {
            throw new IllegalArgumentException("Audit events are invalid.");
        }
        items = List.copyOf(items);
        nextCursor = DtoValidation.optionalText(nextCursor, "Audit cursor", 128);
    }

    @Override
    public List<Event> items() {
        return List.copyOf(items);
    }

    public record Event(
            String action,
            String entity,
            Actor actor,
            String occurredAt) {
        public Event {
            action = DtoValidation.requireText(action, "Audit action", 100);
            entity = DtoValidation.optionalText(entity, "Audit entity", 100);
            occurredAt = DtoValidation.requireInstant(
                    occurredAt,
                    "Audit occurrence");
        }
    }

    public record Actor(String name, String role) {
        public Actor {
            name = DtoValidation.requireText(name, "Audit actor name", 200);
            role = DtoValidation.requireText(role, "Audit actor role", 40);
        }
    }
}
