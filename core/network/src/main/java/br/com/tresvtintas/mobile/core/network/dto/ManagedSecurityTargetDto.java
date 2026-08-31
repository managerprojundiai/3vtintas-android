package br.com.tresvtintas.mobile.core.network.dto;

import java.util.Set;

public record ManagedSecurityTargetDto(
        long id,
        String name,
        String role,
        String status) {
    private static final long MINIMUM_USER_ID = 1L;
    private static final Set<String> ROLES = Set.of(
            "master_admin",
            "manager",
            "salesperson",
            "painter",
            "delivery_driver",
            "customer",
            "user");
    private static final Set<String> STATUSES = Set.of(
            "active",
            "blocked");

    public ManagedSecurityTargetDto {
        if (id < MINIMUM_USER_ID) {
            throw new IllegalArgumentException(
                    "Managed security target ID is invalid.");
        }
        name = DtoValidation.requireText(
                name,
                "Managed security target name",
                200);
        role = DtoValidation.requireText(
                role,
                "Managed security target role",
                32);
        status = DtoValidation.requireText(
                status,
                "Managed security target status",
                16);
        if (!ROLES.contains(role) || !STATUSES.contains(status)) {
            throw new IllegalArgumentException(
                    "Managed security target authority is invalid.");
        }
    }
}
