package br.com.tresvtintas.mobile.data.order;

import java.util.Objects;

public record OrderAccountScope(long userId, String authorizationRevision) {
    private static final long MIN_USER_ID = 1L;

    public OrderAccountScope {
        if (userId < MIN_USER_ID) {
            throw new IllegalArgumentException("Order scope user ID is invalid.");
        }
        authorizationRevision = Objects.requireNonNull(
                authorizationRevision, "Authorization revision is required.");
        if (!authorizationRevision.matches("^[0-9a-f]{64}$")) {
            throw new IllegalArgumentException("Authorization revision is invalid.");
        }
    }
}
