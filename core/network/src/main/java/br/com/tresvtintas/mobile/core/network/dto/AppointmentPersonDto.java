package br.com.tresvtintas.mobile.core.network.dto;

import java.util.Set;

public record AppointmentPersonDto(long id, String name, String role) {
    private static final Set<String> ROLES = Set.of(
            "master_admin",
            "manager",
            "salesperson",
            "delivery_driver",
            "painter",
            "customer",
            "user");

    public AppointmentPersonDto {
        id = DtoValidation.requirePositive(id, "Appointment person ID");
        name = DtoValidation.optionalText(
                name,
                "Appointment person name",
                500);
        role = DtoValidation.requireText(
                role,
                "Appointment person role",
                40);
        if (!ROLES.contains(role)) {
            throw new IllegalArgumentException(
                    "Appointment person role is invalid.");
        }
    }
}
