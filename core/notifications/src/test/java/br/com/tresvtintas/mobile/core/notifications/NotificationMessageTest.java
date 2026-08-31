package br.com.tresvtintas.mobile.core.notifications;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.Test;

public final class NotificationMessageTest {
    private static final String ATTENDANCE = "attendance";
    private static final UUID EVENT_ID =
            UUID.fromString("e7fd42a0-d083-43fc-b5e4-23e43e09ea8b");

    @Test
    public void acceptsOnlyTheVersionedAllowlistedEnvelope() {
        NotificationMessage message = NotificationMessage.parse(Map.of(
                        "schema", "3v.push.v1",
                        "eventId", EVENT_ID.toString(),
                        "category", ATTENDANCE,
                        "route", ATTENDANCE))
                .orElseThrow();

        assertEquals(
                "The opaque event identifier must be preserved.",
                EVENT_ID,
                message.eventId());
        assertEquals(
                "The category must be resolved from its wire value.",
                NotificationCategory.ATTENDANCE,
                message.category());
        assertEquals(
                "The route must be allowlisted.",
                NotificationRoute.ATTENDANCE,
                message.route());
    }

    @Test
    public void rejectsUnknownFieldsAndUnsupportedSchemas() {
        Map<String, String> unexpected = new HashMap<>(validPayload());
        unexpected.put("message", "Sensitive server-authored text");
        Map<String, String> unsupported = new HashMap<>(validPayload());
        unsupported.put("schema", "3v.push.v2");

        assertTrue(
                "Unexpected payload fields must fail closed.",
                NotificationMessage.parse(unexpected).isEmpty());
        assertTrue(
                "Unsupported schemas must fail closed.",
                NotificationMessage.parse(unsupported).isEmpty());
    }

    @Test
    public void rejectsInvalidIdentifiersAndCategoryRouteMismatch() {
        Map<String, String> invalidEvent = new HashMap<>(validPayload());
        invalidEvent.put("eventId", "not-an-event-id");
        Map<String, String> mismatched = new HashMap<>(validPayload());
        mismatched.put("route", "orders");

        assertTrue(
                "Invalid event identifiers must not reach navigation.",
                NotificationMessage.parse(invalidEvent).isEmpty());
        assertTrue(
                "A category cannot redirect to another feature.",
                NotificationMessage.parse(mismatched).isEmpty());
    }

    @Test
    public void constructorAlsoEnforcesCategoryRouteBinding() {
        assertThrows(
                "Direct construction must not bypass route binding.",
                IllegalArgumentException.class,
                () -> new NotificationMessage(
                        EVENT_ID,
                        NotificationCategory.ATTENDANCE,
                        NotificationRoute.ORDERS));
    }

    private static Map<String, String> validPayload() {
        return Map.of(
                "schema", "3v.push.v1",
                "eventId", EVENT_ID.toString(),
                "category", ATTENDANCE,
                "route", ATTENDANCE);
    }
}
