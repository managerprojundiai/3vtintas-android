package br.com.tresvtintas.mobile.core.notifications;

import java.util.Optional;

public record NotificationSettingsState(
        Phase phase,
        Optional<NotificationPreferences> preferences,
        Optional<NotificationFailureKind> failure,
        Optional<String> requestId) {

    public enum Phase {
        EMPTY,
        LOADING,
        READY,
        SAVING,
        ERROR,
        CLOSED
    }

    public NotificationSettingsState {
        if (phase == null) {
            throw new IllegalArgumentException("Notification state phase is required.");
        }
        preferences = preferences == null ? Optional.empty() : preferences;
        failure = failure == null ? Optional.empty() : failure;
        requestId = requestId == null ? Optional.empty() : requestId;
        if ((phase == Phase.READY || phase == Phase.SAVING)
                && preferences.isEmpty()) {
            throw new IllegalArgumentException(
                    "Notification state requires preferences.");
        }
    }

    public static NotificationSettingsState empty() {
        return phase(Phase.EMPTY);
    }

    public static NotificationSettingsState loading() {
        return phase(Phase.LOADING);
    }

    public static NotificationSettingsState ready(NotificationPreferences value) {
        return ready(value, null);
    }

    public static NotificationSettingsState ready(
            NotificationPreferences value,
            NotificationException warning) {
        return new NotificationSettingsState(
                Phase.READY,
                Optional.of(value),
                warning == null
                        ? Optional.empty()
                        : Optional.of(warning.kind()),
                warning == null
                        ? Optional.empty()
                        : warning.requestId());
    }

    public static NotificationSettingsState saving(NotificationPreferences value) {
        return new NotificationSettingsState(
                Phase.SAVING,
                Optional.of(value),
                Optional.empty(),
                Optional.empty());
    }

    public static NotificationSettingsState error(NotificationException failure) {
        return new NotificationSettingsState(
                Phase.ERROR,
                Optional.empty(),
                Optional.of(failure.kind()),
                failure.requestId());
    }

    public static NotificationSettingsState closed() {
        return phase(Phase.CLOSED);
    }

    private static NotificationSettingsState phase(Phase value) {
        return new NotificationSettingsState(
                value,
                Optional.empty(),
                Optional.empty(),
                Optional.empty());
    }
}
