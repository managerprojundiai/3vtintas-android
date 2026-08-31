package br.com.tresvtintas.mobile.data.laborquote;

public record LaborQuoteAccountScope(long userId, String authorizationRevision) {
    public LaborQuoteAccountScope {
        if (userId < 1
                || authorizationRevision == null
                || !authorizationRevision.matches("^[0-9a-f]{64}$")) {
            throw new IllegalArgumentException("Labor quote account scope is invalid.");
        }
    }

    public String accountKey() {
        return userId + ":" + authorizationRevision;
    }
}
