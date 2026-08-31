package br.com.tresvtintas.mobile.data.catalogadmin;

public record CatalogAdministrationAccountScope(
        long actorUserId,
        String authorizationRevision) {
    public CatalogAdministrationAccountScope {
        if (actorUserId < 1L
                || authorizationRevision == null
                || !authorizationRevision.matches("^[a-f0-9]{64}$")) {
            throw new IllegalArgumentException(
                    "Catalog administration actor is invalid.");
        }
    }
}
