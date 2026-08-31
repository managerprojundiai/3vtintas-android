package br.com.tresvtintas.mobile.core.notifications;

public interface NotificationPermissionController {
    NotificationPermissionState currentState();

    boolean requiresRuntimeRequest();

    void recordDecision(boolean granted);
}
