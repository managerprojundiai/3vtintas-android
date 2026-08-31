package br.com.tresvtintas.mobile.data.dashboard;

import java.util.OptionalLong;

public record DashboardAccountScope(
        long userId,
        String authorizationRevision,
        OptionalLong organizationId) {

    public DashboardAccountScope {
        organizationId = organizationId == null
                ? OptionalLong.empty()
                : organizationId;
        if (userId < 1
                || authorizationRevision == null
                || !authorizationRevision.matches("^[a-f0-9]{64}$")
                || organizationId.isPresent()
                && organizationId.orElseThrow() < 1) {
            throw new IllegalArgumentException(
                    "Dashboard account scope is invalid.");
        }
    }
}
