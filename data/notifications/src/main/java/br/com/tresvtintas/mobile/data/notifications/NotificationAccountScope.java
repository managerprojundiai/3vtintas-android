package br.com.tresvtintas.mobile.data.notifications;

public record NotificationAccountScope(
        long userId,
        String authorizationRevision) {

    public NotificationAccountScope {
        if (userId < 1
                || authorizationRevision == null
                || !authorizationRevision.matches("^[a-f0-9]{64}$")) {
            throw new IllegalArgumentException(
                    "Notification account scope is invalid.");
        }
    }
}
