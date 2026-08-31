package br.com.tresvtintas.mobile.data.quote;

public record MaterialQuoteAccountScope(
        long userId,
        String authorizationRevision) {
    public MaterialQuoteAccountScope {
        if (userId < 1
                || authorizationRevision == null
                || !authorizationRevision.matches("^[0-9a-f]{64}$")) {
            throw new IllegalArgumentException("Quote account scope is invalid.");
        }
    }

    public String accountKey() {
        return userId + ":" + authorizationRevision;
    }
}
