package br.com.tresvtintas.mobile.data.useradmin;

public record UserAdministrationAccountScope(
        long userId,
        String authorizationRevision) {
    public UserAdministrationAccountScope {
        if (userId < 1
                || authorizationRevision == null
                || !authorizationRevision.matches("^[a-f0-9]{64}$")) {
            throw new IllegalArgumentException("User administration scope is invalid.");
        }
    }
}
