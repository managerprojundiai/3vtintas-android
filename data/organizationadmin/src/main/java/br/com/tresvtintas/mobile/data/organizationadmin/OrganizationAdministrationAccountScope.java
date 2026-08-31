package br.com.tresvtintas.mobile.data.organizationadmin;

public record OrganizationAdministrationAccountScope(
        long actorUserId,
        String authorizationRevision) {
    public OrganizationAdministrationAccountScope {
        if (actorUserId < 1
                || authorizationRevision == null
                || !authorizationRevision.matches("^[a-f0-9]{64}$")) {
            throw new IllegalArgumentException(
                    "Organization administration actor is invalid.");
        }
    }
}
