package br.com.tresvtintas.mobile.core.bootstrap;

public record ExpectedBootstrapIdentity(long userId, String sessionId, String deviceId) {
    private static final long MINIMUM_IDENTIFIER = 1L;

    public ExpectedBootstrapIdentity {
        if (userId < MINIMUM_IDENTIFIER) {
            throw new IllegalArgumentException("Expected user ID must be positive.");
        }
        if (sessionId == null || sessionId.isBlank()
                || deviceId == null || deviceId.isBlank()) {
            throw new IllegalArgumentException("Expected session identity is required.");
        }
    }

    public String stableKey() {
        return userId + ":" + sessionId + ":" + deviceId;
    }
}
