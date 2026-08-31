package br.com.tresvtintas.mobile.data.commission;

import java.util.Objects;

public record CommissionAccountScope(
        long userId,
        String authorizationRevision) {
    private static final long MIN_USER_ID = 1L;

    public CommissionAccountScope {
        if (userId < MIN_USER_ID) {
            throw new IllegalArgumentException(
                    "Commission scope user ID is invalid.");
        }
        authorizationRevision = Objects.requireNonNull(
                authorizationRevision,
                "Authorization revision is required.");
        if (!authorizationRevision.matches("^[0-9a-f]{64}$")) {
            throw new IllegalArgumentException(
                    "Authorization revision is invalid.");
        }
    }
}
