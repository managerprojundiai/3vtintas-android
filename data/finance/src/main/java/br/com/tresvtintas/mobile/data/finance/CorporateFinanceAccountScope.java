package br.com.tresvtintas.mobile.data.finance;

import java.util.Objects;
import java.util.OptionalLong;

public record CorporateFinanceAccountScope(
        long userId,
        String authorizationRevision,
        OptionalLong organizationId,
        boolean globalAccess) {
    private static final long MINIMUM_USER_ID = 1L;

    public CorporateFinanceAccountScope {
        if (userId < MINIMUM_USER_ID) {
            throw new IllegalArgumentException(
                    "Corporate finance scope user ID is invalid.");
        }
        authorizationRevision = Objects.requireNonNull(
                authorizationRevision,
                "Corporate finance authorization revision is required.");
        organizationId = organizationId == null
                ? OptionalLong.empty()
                : organizationId;
        if (!authorizationRevision.matches("^[0-9a-f]{64}$")
                || (organizationId.isPresent()
                        && organizationId.getAsLong() < 1)) {
            throw new IllegalArgumentException(
                    "Corporate finance account scope is invalid.");
        }
    }
}
