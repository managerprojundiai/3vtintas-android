package br.com.tresvtintas.mobile.core.network.dto;

public record AuthenticatedUser(long id, String name, String email, String role) {
    private static final int MINIMUM_USER_ID = 1;

    public AuthenticatedUser {
        if (id < MINIMUM_USER_ID) {
            throw new IllegalArgumentException("User ID must be positive.");
        }
        if (name != null && name.length() > 200) {
            throw new IllegalArgumentException("User name is too long.");
        }
        if (email != null && email.length() > 320) {
            throw new IllegalArgumentException("User email is too long.");
        }
        role = DtoValidation.requireText(role, "User role", 40);
    }
}
