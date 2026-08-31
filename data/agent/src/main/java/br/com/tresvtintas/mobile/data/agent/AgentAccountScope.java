package br.com.tresvtintas.mobile.data.agent;

public record AgentAccountScope(
        long userId,
        String authorizationRevision) {
    public AgentAccountScope {
        if (userId < 1
                || authorizationRevision == null
                || !authorizationRevision.matches(
                        "^[A-Za-z0-9._:-]{1,160}$")) {
            throw new IllegalArgumentException(
                    "Agent account scope is invalid.");
        }
    }
}
