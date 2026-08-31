package br.com.tresvtintas.mobile.data.notifications;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import br.com.tresvtintas.mobile.core.network.MobileApiFactory;
import br.com.tresvtintas.mobile.core.network.NetworkConfiguration;
import br.com.tresvtintas.mobile.core.notifications.NotificationCategory;
import br.com.tresvtintas.mobile.core.notifications.NotificationException;
import br.com.tresvtintas.mobile.core.notifications.NotificationFailureKind;
import br.com.tresvtintas.mobile.core.notifications.NotificationPermissionState;
import br.com.tresvtintas.mobile.core.notifications.NotificationPreferences;
import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public final class RemoteNotificationRepositoryTest {
    private static final String CONTENT_TYPE = "Content-Type";
    private static final String FIREBASE_INSTALLATION_ID =
            "cdefghijklmnopqrstuvwx";
    private MockWebServer server;
    private RemoteNotificationRepository repository;

    @Before
    public void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        repository = repository();
    }

    @After
    public void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    public void readsDeviceScopedPreferencesWithoutClientAuthorizationClaims()
            throws Exception {
        server.enqueue(json(preferencesJson("granted", true, 4)));

        NotificationPreferences preferences = repository.load();
        RecordedRequest request = server.takeRequest();

        assertEquals(
                "The endpoint must derive user and device from the session.",
                "/api/mobile/v1/notifications/preferences",
                request.getPath());
        assertFalse(
                "The client must never assert a user identifier.",
                request.getPath().contains("userId="));
        assertFalse(
                "The client must never assert a device identifier.",
                request.getPath().contains("deviceId="));
        assertEquals(
                "The server permission decision must be preserved.",
                NotificationPermissionState.GRANTED,
                preferences.permissionState());
        assertTrue(
                "Essential security alerts must remain enabled.",
                preferences.enabled(NotificationCategory.SECURITY));
    }

    @Test
    public void updatesOneCompletePreferenceDocumentWithRevision()
            throws Exception {
        server.enqueue(json(preferencesJson("denied", false, 5)));

        NotificationPreferences saved = repository.update(
                NotificationPermissionState.DENIED,
                false,
                categories(false),
                4);
        RecordedRequest request = server.takeRequest();
        String body = request.getBody().readUtf8();

        assertEquals(
                "Preference updates must use the protected PUT endpoint.",
                "PUT /api/mobile/v1/notifications/preferences HTTP/1.1",
                request.getRequestLine());
        assertTrue(
                "Optimistic concurrency revision must be explicit.",
                body.contains("\"expectedRevision\":4"));
        assertTrue(
                "Every notification category must be submitted explicitly.",
                body.contains("\"attendance\":false")
                        && body.contains("\"agent\":false"));
        assertFalse(
                "The body must not contain client-authored identity.",
                body.contains("userId") || body.contains("deviceId"));
        assertEquals(
                "The authoritative server revision must replace the old one.",
                5,
                saved.revision());
    }

    @Test
    public void registrationTransmitsFidOnlyToTheProtectedEndpoint()
            throws Exception {
        server.enqueue(json(registrationJson(true, true)));

        repository.register(FIREBASE_INSTALLATION_ID);
        RecordedRequest request = server.takeRequest();
        String body = request.getBody().readUtf8();

        assertEquals(
                "Registration must use the device-scoped PUT endpoint.",
                "PUT /api/mobile/v1/notifications/registration HTTP/1.1",
                request.getRequestLine());
        assertTrue(
                "The validated Firebase Installation ID must be transmitted.",
                body.contains(FIREBASE_INSTALLATION_ID));
        assertFalse(
                "Registration must not invent user or device claims.",
                body.contains("userId") || body.contains("deviceId"));
    }

    @Test
    public void unregistersCurrentDeviceWithoutSendingATargetBody()
            throws Exception {
        server.enqueue(json(registrationJson(false, true)));

        repository.unregister();
        RecordedRequest request = server.takeRequest();

        assertEquals(
                "Revocation must target only the authenticated device.",
                "DELETE /api/mobile/v1/notifications/registration HTTP/1.1",
                request.getRequestLine());
        assertEquals(
                "Revocation must not accept a client-authored target.",
                0L,
                request.getBodySize());
    }

    @Test
    public void rejectsUnknownFieldsAndPreservesConflictTrace() {
        server.enqueue(json(preferencesJson("granted", true, 4)
                .replace(
                        "\"revision\":4",
                        "\"revision\":4,\"internalToken\":\"hidden\"")));
        String requestId = UUID.randomUUID().toString();
        server.enqueue(new MockResponse()
                .setResponseCode(409)
                .setHeader(CONTENT_TYPE, "application/problem+json")
                .setBody(problemJson(requestId)));

        NotificationException protocolFailure = assertThrows(
                "Unexpected sensitive fields must fail closed.",
                NotificationException.class,
                repository::load);
        NotificationException conflict = assertThrows(
                "Concurrent preference updates must be typed.",
                NotificationException.class,
                () -> repository.update(
                        NotificationPermissionState.GRANTED,
                        true,
                        categories(true),
                        4));

        assertEquals(
                "An unexpected response field is a protocol failure.",
                NotificationFailureKind.PROTOCOL,
                protocolFailure.kind());
        assertEquals(
                "The API conflict must map to optimistic concurrency.",
                NotificationFailureKind.CONFLICT,
                conflict.kind());
        assertEquals(
                "Support correlation must survive error mapping.",
                requestId,
                conflict.requestId().orElseThrow());
    }

    @Test
    public void revokedAccountScopeCannotIssueAnotherRequest() {
        repository.close();

        NotificationException failure = assertThrows(
                "A closed account scope must reject all network operations.",
                NotificationException.class,
                repository::load);

        assertEquals(
                "A revoked account must fail with a typed access error.",
                NotificationFailureKind.ACCESS_REVOKED,
                failure.kind());
        assertEquals(
                "A revoked account must not issue a network request.",
                0,
                server.getRequestCount());
    }

    private RemoteNotificationRepository repository() {
        return new RemoteNotificationRepository(
                new NotificationAccountScope(21, "b".repeat(64)),
                MobileApiFactory.create(
                        new NetworkConfiguration(
                                "http://localhost:" + server.getPort()
                                        + "/api/mobile/v1/",
                                "0.30.0-notifications",
                                33,
                                true),
                        () -> Optional.of("a".repeat(80))));
    }

    private static Map<NotificationCategory, Boolean> categories(
            boolean enabled) {
        Map<NotificationCategory, Boolean> result =
                new EnumMap<>(NotificationCategory.class);
        for (NotificationCategory category : NotificationCategory.values()) {
            if (category.configurable()) {
                result.put(category, enabled);
            }
        }
        return result;
    }

    private static MockResponse json(String body) {
        return new MockResponse()
                .setResponseCode(200)
                .setHeader(CONTENT_TYPE, "application/json")
                .setBody(body);
    }

    private static String preferencesJson(
            String permissionState,
            boolean operationalEnabled,
            int revision) {
        return ("{\"permissionState\":\"%s\","
                + "\"operationalEnabled\":%s,"
                + "\"categories\":{"
                + "\"attendance\":true,\"orders\":true,"
                + "\"deliveries\":true,\"quotes\":true,"
                + "\"commissions\":true,\"appointments\":true,"
                + "\"finance\":true,\"agent\":true},"
                + "\"essentialSecurityAlerts\":true,"
                + "\"revision\":%d,"
                + "\"updatedAt\":\"2026-07-28T12:00:00.000Z\"}")
                .formatted(permissionState, operationalEnabled, revision);
    }

    private static String registrationJson(
            boolean registered,
            boolean changed) {
        return ("{\"provider\":\"fcm\","
                + "\"registered\":%s,\"changed\":%s,"
                + "\"updatedAt\":\"2026-07-28T12:00:00.000Z\"}")
                .formatted(registered, changed);
    }

    private static String problemJson(String requestId) {
        return ("{\"type\":\"https://www.3vtintas.com.br/problems/conflict\","
                + "\"title\":\"Conflict\",\"status\":409,"
                + "\"detail\":\"The preference document changed.\","
                + "\"instance\":\"urn:3v:request:%s\","
                + "\"code\":\"RESOURCE_CONFLICT\",\"requestId\":\"%s\"}")
                .formatted(requestId, requestId);
    }
}
