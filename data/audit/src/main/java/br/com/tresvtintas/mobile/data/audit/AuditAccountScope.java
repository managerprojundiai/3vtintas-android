package br.com.tresvtintas.mobile.data.audit;

public record AuditAccountScope(long userId, String authorizationRevision) {
    public AuditAccountScope {
        if (userId < 1
                || authorizationRevision == null
                || !authorizationRevision.matches("^[a-f0-9]{64}$")) {
            throw new IllegalArgumentException("Audit account scope is invalid.");
        }
    }
}
