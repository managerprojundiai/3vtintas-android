package br.com.tresvtintas.mobile.data.attendance;

import java.util.Objects;
import java.util.OptionalLong;

public record AttendanceAccountScope(
        long userId,
        String authorizationRevision,
        OptionalLong organizationId) {
    public AttendanceAccountScope {
        organizationId = Objects.requireNonNull(
                organizationId,
                "Attendance organization scope is required.");
        if (userId < 1
                || authorizationRevision == null
                || !authorizationRevision.matches("^[0-9a-f]{64}$")
                || (organizationId.isPresent()
                        && organizationId.getAsLong() < 1)) {
            throw new IllegalArgumentException(
                    "Attendance account scope is invalid.");
        }
    }
}
