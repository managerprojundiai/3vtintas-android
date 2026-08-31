package br.com.tresvtintas.mobile.data.accountaccess;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;

import br.com.tresvtintas.mobile.core.accountaccess.AccountAccessException;
import br.com.tresvtintas.mobile.core.accountaccess.AccountAccessFailureKind;
import br.com.tresvtintas.mobile.core.accountaccess.AccountAccessPage;
import br.com.tresvtintas.mobile.core.accountaccess.AccountAccessView;
import br.com.tresvtintas.mobile.core.accountaccess.AccountDevice;
import br.com.tresvtintas.mobile.core.accountaccess.AccountSession;
import br.com.tresvtintas.mobile.core.network.MobileApiFactory;
import br.com.tresvtintas.mobile.core.network.NetworkConfiguration;
import java.io.IOException;
import java.util.Optional;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public final class RemoteManagedAccountAccessReaderTest {
    private static final long TARGET_USER_ID = 42L;
    private static final String CONTENT_TYPE = "Content-Type";
    private MockWebServer server;
    private RemoteManagedAccountAccessReader reader;

    @Before
    public void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        reader = new RemoteManagedAccountAccessReader(
                new ManagedAccountAccessScope(
                        21,
                        TARGET_USER_ID,
                        "b".repeat(64)),
                MobileApiFactory.create(
                        new NetworkConfiguration(
                                "http://localhost:"
                                        + server.getPort()
                                        + "/api/mobile/v1/",
                                "0.51.0-security-admin",
                                59,
                                true),
                        () -> Optional.of("a".repeat(80))));
    }

    @After
    public void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    public void readsExactTargetWithoutSubmittingAuthority() throws Exception {
        server.enqueue(json(devicePage(TARGET_USER_ID)));

        AccountAccessPage page = reader.page(
                AccountAccessView.DEVICES,
                Optional.of("opaque-admin-cursor"),
                30);
        RecordedRequest request = server.takeRequest();
        AccountDevice device = (AccountDevice) page.items().get(0);

        assertEquals(
                "The route must bind the selected public user identifier.",
                "/api/mobile/v1/user-administration/users/42/security/devices"
                        + "?cursor=opaque-admin-cursor&limit=30",
                request.getPath());
        assertEquals(
                "The managed route must use the protected bearer stack.",
                "Bearer " + "a".repeat(80),
                request.getHeader("Authorization"));
        assertFalse(
                "The client must not claim a role or organization scope.",
                request.getPath().matches(".*(role|organizationId)=.*"));
        assertFalse(
                "A third-party device can never be marked as this access.",
                device.current());
    }

    @Test
    public void readsSessionsFromSeparateBoundCursor() throws Exception {
        server.enqueue(json(sessionPage(TARGET_USER_ID)));

        AccountAccessPage page = reader.page(
                AccountAccessView.SESSIONS,
                Optional.empty(),
                30);
        RecordedRequest request = server.takeRequest();
        AccountSession session = (AccountSession) page.items().get(0);

        assertEquals(
                "Session browsing must use the dedicated read-only route.",
                "/api/mobile/v1/user-administration/users/42/security/sessions"
                        + "?limit=30",
                request.getPath());
        assertEquals(
                "The sanitized session status must survive mapping.",
                "ACTIVE",
                session.status().name());
    }

    @Test
    public void acceptsCanonicalUserRoleForUnassignedAccounts()
            throws Exception {
        server.enqueue(json(devicePage(TARGET_USER_ID).replace(
                "\"role\":\"salesperson\"",
                "\"role\":\"user\"")));

        AccountAccessPage page = reader.page(
                AccountAccessView.DEVICES,
                Optional.empty(),
                30);

        assertEquals(
                "The Android contract must accept every canonical app role.",
                1,
                page.items().size());
    }

    @Test
    public void changedTargetAndUnexpectedFieldFailClosed() {
        server.enqueue(json(devicePage(43)));
        server.enqueue(json(devicePage(TARGET_USER_ID).replace(
                "\"nextCursor\":null",
                "\"nextCursor\":null,\"accessToken\":\"secret\"")));

        AccountAccessException changedTarget = assertThrows(
                AccountAccessException.class,
                () -> reader.page(
                        AccountAccessView.DEVICES,
                        Optional.empty(),
                        30));
        AccountAccessException unexpectedField = assertThrows(
                AccountAccessException.class,
                () -> reader.page(
                        AccountAccessView.DEVICES,
                        Optional.empty(),
                        30));

        assertEquals(
                "A changed target must fail as a protocol violation.",
                AccountAccessFailureKind.PROTOCOL,
                changedTarget.kind());
        assertEquals(
                "Unexpected response fields must fail closed.",
                AccountAccessFailureKind.PROTOCOL,
                unexpectedField.kind());
    }

    @Test
    public void closedReaderCannotIssueAnotherRequest() {
        reader.close();

        AccountAccessException failure = assertThrows(
                AccountAccessException.class,
                () -> reader.page(
                        AccountAccessView.DEVICES,
                        Optional.empty(),
                        30));

        assertEquals(
                "A closed reader must behave as revoked local access.",
                AccountAccessFailureKind.ACCESS_REVOKED,
                failure.kind());
        assertEquals(
                "A closed reader must not reach the network.",
                0,
                server.getRequestCount());
    }

    @Test
    public void scopeRejectsInvalidActorsTargetsAndRevisions() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new ManagedAccountAccessScope(
                        0,
                        TARGET_USER_ID,
                        "b".repeat(64)));
        assertThrows(
                IllegalArgumentException.class,
                () -> new ManagedAccountAccessScope(
                        21,
                        0,
                        "b".repeat(64)));
        assertThrows(
                IllegalArgumentException.class,
                () -> new ManagedAccountAccessScope(
                        21,
                        TARGET_USER_ID,
                        "weak"));
    }

    private static MockResponse json(String body) {
        return new MockResponse()
                .setResponseCode(200)
                .setHeader(CONTENT_TYPE, "application/json")
                .setBody(body);
    }

    private static String devicePage(long targetUserId) {
        return """
                {
                  "target":{"id":__TARGET__,"name":"Usuário alvo","role":"salesperson","status":"active"},
                  "items":[{
                    "id":"10000000-0000-4000-8000-000000000001",
                    "displayName":"Aparelho corporativo",
                    "manufacturer":"Samsung",
                    "model":"SM-X",
                    "androidApi":35,
                    "appVersion":"0.50.0",
                    "status":"active",
                    "registeredAt":"2026-07-30T08:00:00Z",
                    "lastSeenAt":"2026-07-30T09:00:00Z",
                    "revokedAt":null,
                    "current":false,
                    "revision":7
                  }],
                  "nextCursor":null
                }
                """.replace(
                        "__TARGET__",
                        Long.toString(targetUserId));
    }

    private static String sessionPage(long targetUserId) {
        return """
                {
                  "target":{"id":__TARGET__,"name":"Usuário alvo","role":"salesperson","status":"active"},
                  "items":[{
                    "id":"20000000-0000-4000-8000-000000000001",
                    "deviceId":"10000000-0000-4000-8000-000000000001",
                    "authMethod":"google",
                    "status":"active",
                    "issuedAt":"2026-07-30T08:00:00Z",
                    "lastSeenAt":"2026-07-30T09:00:00Z",
                    "idleExpiresAt":"2026-07-30T10:00:00Z",
                    "absoluteExpiresAt":"2026-08-30T08:00:00Z",
                    "endedAt":null,
                    "current":false,
                    "revision":9
                  }],
                  "nextCursor":null
                }
                """.replace(
                        "__TARGET__",
                        Long.toString(targetUserId));
    }
}
