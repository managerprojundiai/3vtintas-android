package br.com.tresvtintas.mobile.data.whatsappadmin;

public record WhatsAppAdministrationAccountScope(
        long actorUserId,
        String authorizationRevision) {
    public WhatsAppAdministrationAccountScope {
        if (actorUserId < 1 || authorizationRevision == null
                || authorizationRevision.isBlank()) {
            throw new IllegalArgumentException(
                    "WhatsApp administration account scope is invalid.");
        }
    }
}
