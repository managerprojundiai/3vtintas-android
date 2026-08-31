package br.com.tresvtintas.mobile.data.painteradmin;

public record PainterAdministrationAccountScope(
        long userId,
        String authorizationRevision) {
    public PainterAdministrationAccountScope {
        if (userId < 1
                || authorizationRevision == null
                || !authorizationRevision.matches("^[a-f0-9]{64}$")) {
            throw new IllegalArgumentException(
                    "Painter administration scope is invalid.");
        }
    }
}
