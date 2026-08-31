package br.com.tresvtintas.mobile.core.notifications;

public record FirebaseClientConfiguration(
        boolean configured,
        String applicationId,
        String projectId,
        String apiKey,
        String senderId) {

    public FirebaseClientConfiguration {
        applicationId = normalize(applicationId);
        projectId = normalize(projectId);
        apiKey = normalize(apiKey);
        senderId = normalize(senderId);
        boolean complete = !applicationId.isBlank()
                && !projectId.isBlank()
                && !apiKey.isBlank()
                && !senderId.isBlank();
        if (configured != complete) {
            throw new IllegalArgumentException(
                    "Firebase client configuration is incomplete.");
        }
    }

    private static String normalize(String value) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.length() > 512
                || normalized.indexOf('\n') >= 0
                || normalized.indexOf('\r') >= 0) {
            throw new IllegalArgumentException(
                    "Firebase client configuration is invalid.");
        }
        return normalized;
    }
}
