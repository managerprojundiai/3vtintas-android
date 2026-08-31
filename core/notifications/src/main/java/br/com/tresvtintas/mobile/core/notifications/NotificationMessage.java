package br.com.tresvtintas.mobile.core.notifications;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public record NotificationMessage(
        UUID eventId,
        NotificationCategory category,
        NotificationRoute route) {
    private static final String SCHEMA = "3v.push.v1";
    private static final Set<String> EXPECTED_KEYS =
            Set.of("schema", "eventId", "category", "route");

    public NotificationMessage {
        if (eventId == null
                || category == null
                || route == null
                || category.route() != route) {
            throw new IllegalArgumentException(
                    "Notification message is invalid.");
        }
    }

    public static Optional<NotificationMessage> parse(Map<String, String> data) {
        if (data == null
                || !data.keySet().equals(EXPECTED_KEYS)
                || !SCHEMA.equals(data.get("schema"))) {
            return Optional.empty();
        }
        String event = data.get("eventId");
        String categoryValue = data.get("category");
        String routeValue = data.get("route");
        if (event == null || categoryValue == null || routeValue == null) {
            return Optional.empty();
        }
        try {
            UUID eventId = UUID.fromString(event);
            NotificationCategory category =
                    NotificationCategory.fromWireValue(categoryValue)
                            .orElseThrow();
            NotificationRoute route =
                    NotificationRoute.fromWireValue(routeValue)
                            .orElseThrow();
            return Optional.of(new NotificationMessage(eventId, category, route));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }
}
