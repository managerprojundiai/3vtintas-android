package br.com.tresvtintas.mobile.data.team;

import java.util.OptionalLong;

public record TeamAccountScope(
        long userId,
        String authorizationRevision,
        OptionalLong organizationId) {
    public TeamAccountScope {
        organizationId = organizationId == null
                ? OptionalLong.empty()
                : organizationId;
        if (userId < 1
                || authorizationRevision == null
                || !authorizationRevision.matches("^[a-f0-9]{64}$")
                || organizationId.isPresent()
                && organizationId.orElseThrow() < 1) {
            throw new IllegalArgumentException(
                    "Team account scope is invalid.");
        }
    }
}
