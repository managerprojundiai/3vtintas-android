package br.com.tresvtintas.mobile.core.audit;

import java.util.Optional;

public record AuditQuery(
        Optional<String> action,
        Optional<String> entity,
        int pageSize) {
    private static final int MAXIMUM_FILTER_LENGTH = 100;
    public AuditQuery {
        action = normalize(action, "Audit action");
        entity = normalize(entity, "Audit entity");
        if (pageSize < 1 || pageSize > 100) {
            throw new IllegalArgumentException("Audit page size is invalid.");
        }
    }

    public static AuditQuery initial() {
        return new AuditQuery(Optional.empty(), Optional.empty(), 30);
    }

    public AuditQuery withAction(String value) {
        return new AuditQuery(Optional.ofNullable(value), entity, pageSize);
    }

    public AuditQuery withEntity(String value) {
        return new AuditQuery(action, Optional.ofNullable(value), pageSize);
    }

    private static Optional<String> normalize(Optional<String> value, String label) {
        if (value == null || value.isEmpty()) {
            return Optional.empty();
        }
        String normalized = value.orElseThrow().strip();
        if (normalized.isEmpty()) {
            return Optional.empty();
        }
        if (normalized.length() > MAXIMUM_FILTER_LENGTH) {
            throw new IllegalArgumentException(label + " is invalid.");
        }
        return Optional.of(normalized);
    }
}
