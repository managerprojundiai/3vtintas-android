package br.com.tresvtintas.mobile.data.systemconfiguration;

public record SystemConfigurationAccountScope(
        long actorUserId,
        String authorizationRevision) {
    public SystemConfigurationAccountScope {
        if (actorUserId < 1 || authorizationRevision == null
                || authorizationRevision.isBlank()) {
            throw new IllegalArgumentException(
                    "System configuration account scope is invalid.");
        }
    }
}
