package br.com.tresvtintas.mobile.data.finance;

import java.util.Objects;

public record FinanceAccountScope(long userId, String authorizationRevision) {
    private static final long MINIMUM_USER_ID = 1L;

    public FinanceAccountScope {
        if (userId < MINIMUM_USER_ID) {
            throw new IllegalArgumentException("Finance scope user ID is invalid.");
        }
        authorizationRevision = Objects.requireNonNull(
                authorizationRevision,
                "Finance authorization revision is required.");
        if (!authorizationRevision.matches("^[0-9a-f]{64}$")) {
            throw new IllegalArgumentException(
                    "Finance authorization revision is invalid.");
        }
    }
}
