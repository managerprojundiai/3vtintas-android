package br.com.tresvtintas.mobile.data.accountaccess;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import br.com.tresvtintas.mobile.core.accountaccess.AccountAccessException;
import br.com.tresvtintas.mobile.core.accountaccess.AccountAccessFailureKind;
import br.com.tresvtintas.mobile.core.accountaccess.AccountAccessPage;
import br.com.tresvtintas.mobile.core.accountaccess.AccountAccessView;
import br.com.tresvtintas.mobile.core.accountaccess.AccountDevice;
import br.com.tresvtintas.mobile.core.accountaccess.AccountRevocation;
import br.com.tresvtintas.mobile.core.accountaccess.AccountSession;
import br.com.tresvtintas.mobile.core.network.MobileApiFactory;
import br.com.tresvtintas.mobile.core.network.NetworkConfiguration;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public final class RemoteAccountAccessRepositoryTest {
    private static final String CONTENT_TYPE = "Content-Type";
    private static final String DEVICE_ID =
            "10000000-0000-4000-8000-000000000001";
    private static final String SESSION_ID =
            "20000000-0000-4000-8000-000000000001";
    private MockWebServer server;
    private RemoteAccountAccessRepository repository;

    @Before
    public void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        repository = new RemoteAccountAccessRepository(
                new AccountAccessScope(
                        21,
                        SESSION_ID,
                        DEVICE_ID,
                        "b".repeat(64)),
                MobileApiFactory.create(
                        new NetworkConfiguration(
                                "http://localhost:"
                                        + server.getPort()
                                        + "/api/mobile/v1/",
                                "0.30.0-account-security",
                                31,
                                true),
                        () -> Optional.of("a".repeat(80))));
    }

    @After
    public void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    public void listsOnlyAuthenticatedUsersOwnDevices() throws Exception {
        server.enqueue(json(devicePage()));

        AccountAccessPage page = repository.page(
                AccountAccessView.DEVICES,
                Optional.of("opaque-device-cursor"),
                30);
        RecordedRequest request = server.takeRequest();
        AccountDevice device = (AccountDevice) page.items().get(0);

        assertEquals(
                "The current device must remain identifiable locally.",
                DEVICE_ID,
                device.id());
        assertTrue(
                "The server, not a client parameter, marks the current device.",
                device.current());
        assertEquals(
                "Only pagination data may be sent for a self-scoped list.",
                "/api/mobile/v1/devices"
                        + "?cursor=opaque-device-cursor&limit=30",
                request.getPath());
        assertFalse(
                "The app must never submit role, store, or user authority.",
                request.getPath().matches(
                        ".*(role|organizationId|userId)=.*"));
        assertEquals(
                "The endpoint must use the protected bearer stack.",
                "Bearer " + "a".repeat(80),
                request.getHeader("Authorization"));
    }

    @Test
    public void listsSessionsAndPreservesNaturalExpiry() throws Exception {
        server.enqueue(json(sessionPage()));

        AccountAccessPage page = repository.page(
                AccountAccessView.SESSIONS,
                Optional.empty(),
                30);
        RecordedRequest request = server.takeRequest();
        AccountSession session =
                (AccountSession) page.items().get(0);

        assertEquals(
                "Expired status must be mapped without inventing revocation.",
                "EXPIRED",
                session.status().name());
        assertTrue(
                "Natural expiry may have no end timestamp.",
                session.endedAt().isEmpty());
        assertEquals(
                "The session route must remain self-scoped.",
                "/api/mobile/v1/sessions?limit=30",
                request.getPath());
    }

    @Test
    public void revokesDeviceAndSessionWithExactOpaqueTargets()
            throws Exception {
        server.enqueue(json(
                "{\"deviceId\":\""
                        + DEVICE_ID
                        + "\",\"changed\":true,\"current\":false}"));
        server.enqueue(json(
                "{\"sessionId\":\""
                        + SESSION_ID
                        + "\",\"changed\":false,\"current\":true}"));

        AccountRevocation device = repository.revoke(
                AccountAccessView.DEVICES,
                DEVICE_ID);
        RecordedRequest deviceRequest = server.takeRequest();
        AccountRevocation session = repository.revoke(
                AccountAccessView.SESSIONS,
                SESSION_ID);
        RecordedRequest sessionRequest = server.takeRequest();

        assertTrue(
                "Changed must preserve the server's idempotent result.",
                device.changed());
        assertTrue(
                "Current-session revocation must reach local logout handling.",
                session.current());
        assertEquals(
                "Device revocation must use DELETE on the opaque identifier.",
                "DELETE /api/mobile/v1/devices/" + DEVICE_ID,
                deviceRequest.getMethod()
                        + " "
                        + deviceRequest.getPath());
        assertEquals(
                "Session revocation must use DELETE on the opaque identifier.",
                "DELETE /api/mobile/v1/sessions/" + SESSION_ID,
                sessionRequest.getMethod()
                        + " "
                        + sessionRequest.getPath());
    }

    @Test
    public void changedTargetAndUnexpectedFieldFailClosed() {
        server.enqueue(json(
                "{\"deviceId\":\""
                        + "10000000-0000-4000-8000-000000000009"
                        + "\",\"changed\":true,\"current\":false}"));
        server.enqueue(json(devicePage().replace(
                "\"nextCursor\":null",
                "\"nextCursor\":null,\"internalToken\":\"secret\"")));

        AccountAccessException changedTarget = assertThrows(
                AccountAccessException.class,
                () -> repository.revoke(
                        AccountAccessView.DEVICES,
                        DEVICE_ID));
        AccountAccessException unexpectedField = assertThrows(
                AccountAccessException.class,
                () -> repository.page(
                        AccountAccessView.DEVICES,
                        Optional.empty(),
                        30));

        assertEquals(
                "A response for another resource must be rejected.",
                AccountAccessFailureKind.PROTOCOL,
                changedTarget.kind());
        assertEquals(
                "Unexpected sensitive fields must never be ignored.",
                AccountAccessFailureKind.PROTOCOL,
                unexpectedField.kind());
    }

    @Test
    public void hiddenResourceDoesNotEnableEnumeration() {
        String requestId = UUID.randomUUID().toString();
        server.enqueue(new MockResponse()
                .setResponseCode(404)
                .setHeader(
                        CONTENT_TYPE,
                        "application/problem+json")
                .setBody(problemJson(requestId)));

        AccountAccessException failure = assertThrows(
                AccountAccessException.class,
                () -> repository.revoke(
                        AccountAccessView.SESSIONS,
                        SESSION_ID));

        assertEquals(
                "Absent and unauthorized resources must look identical.",
                AccountAccessFailureKind.NOT_FOUND,
                failure.kind());
        assertEquals(
                "The support correlation must survive the boundary.",
                requestId,
                failure.requestId().orElseThrow());
    }

    @Test
    public void closedScopeNeverIssuesAnotherRequest() {
        repository.close();

        AccountAccessException failure = assertThrows(
                AccountAccessException.class,
                () -> repository.page(
                        AccountAccessView.DEVICES,
                        Optional.empty(),
                        30));

        assertEquals(
                "A replaced account scope must fail as revoked.",
                AccountAccessFailureKind.ACCESS_REVOKED,
                failure.kind());
        assertEquals(
                "A revoked scope must not reach the network.",
                0,
                server.getRequestCount());
    }

    @Test
    public void scopeRequiresExactAuthenticatedIdentities() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new AccountAccessScope(
                        0,
                        SESSION_ID,
                        DEVICE_ID,
                        "b".repeat(64)));
        assertThrows(
                IllegalArgumentException.class,
                () -> new AccountAccessScope(
                        21,
                        "not-a-session",
                        DEVICE_ID,
                        "b".repeat(64)));
        assertThrows(
                IllegalArgumentException.class,
                () -> new AccountAccessScope(
                        21,
                        SESSION_ID,
                        DEVICE_ID,
                        "weak-revision"));
    }

    private static MockResponse json(String body) {
        return new MockResponse()
                .setResponseCode(200)
                .setHeader(CONTENT_TYPE, "application/json")
                .setBody(body);
    }

    private static String devicePage() {
        return """
                {
                  "items":[{
                    "id":"10000000-0000-4000-8000-000000000001",
                    "displayName":"Tablet atual",
                    "manufacturer":"Samsung",
                    "model":"SM-X",
                    "androidApi":35,
                    "appVersion":"0.30.0",
                    "status":"active",
                    "registeredAt":"2026-07-27T08:00:00Z",
                    "lastSeenAt":"2026-07-27T09:00:00Z",
                    "revokedAt":null,
                    "current":true,
                    "revision":3
                  }],
                  "nextCursor":null
                }
                """;
    }

    private static String sessionPage() {
        return """
                {
                  "items":[{
                    "id":"20000000-0000-4000-8000-000000000001",
                    "deviceId":"10000000-0000-4000-8000-000000000001",
                    "authMethod":"google",
                    "status":"expired",
                    "issuedAt":"2026-07-27T08:00:00Z",
                    "lastSeenAt":"2026-07-27T09:00:00Z",
                    "idleExpiresAt":"2026-07-27T10:00:00Z",
                    "absoluteExpiresAt":"2026-08-27T08:00:00Z",
                    "endedAt":null,
                    "current":false,
                    "revision":5
                  }],
                  "nextCursor":null
                }
                """;
    }

    private static String problemJson(String requestId) {
        return ("{\"type\":\"https://www.3vtintas.com.br/problems/not-found\","
                + "\"title\":\"Not found\",\"status\":404,"
                + "\"detail\":\"Resource not found.\","
                + "\"instance\":\"urn:3v:request:%s\","
                + "\"code\":\"NOT_FOUND\",\"requestId\":\"%s\"}")
                .formatted(requestId, requestId);
    }
}
