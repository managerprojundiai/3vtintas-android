package br.com.tresvtintas.mobile.data.accountaccess;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import br.com.tresvtintas.mobile.core.accountaccess.AccountAccessException;
import br.com.tresvtintas.mobile.core.accountaccess.AccountAccessFailureKind;
import br.com.tresvtintas.mobile.core.accountaccess.AccountAccessStatus;
import br.com.tresvtintas.mobile.core.accountaccess.AccountAccessView;
import br.com.tresvtintas.mobile.core.accountaccess.AccountDevice;
import br.com.tresvtintas.mobile.core.accountaccess.ManagedAccountRevocationChallenge;
import br.com.tresvtintas.mobile.core.accountaccess.ManagedAccountRevocationGrant;
import br.com.tresvtintas.mobile.core.accountaccess.ManagedAccountRevocationPreview;
import br.com.tresvtintas.mobile.core.accountaccess.ManagedAccountRevocationResult;
import br.com.tresvtintas.mobile.core.network.MobileApiFactory;
import br.com.tresvtintas.mobile.core.network.NetworkConfiguration;
import java.io.IOException;
import java.time.Instant;
import java.util.Optional;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public final class RemoteManagedAccountRevocationRepositoryTest {
    private static final long TARGET_USER_ID = 42L;
    private static final String ACTION_ID =
            "30000000-0000-4000-8000-000000000001";
    private static final String DEVICE_ID =
            "10000000-0000-4000-8000-000000000001";
    private static final String CHALLENGE_ID =
            "40000000-0000-4000-8000-000000000001";
    private static final String TOKEN = "3vsu1_" + "t".repeat(43);
    private static final String CONTENT_TYPE = "Content-Type";
    private MockWebServer server;
    private RemoteManagedAccountRevocationRepository repository;

    @Before
    public void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        repository = new RemoteManagedAccountRevocationRepository(
                new ManagedAccountAccessScope(
                        21,
                        TARGET_USER_ID,
                        "b".repeat(64)),
                MobileApiFactory.create(
                        new NetworkConfiguration(
                                "http://localhost:"
                                        + server.getPort()
                                        + "/api/mobile/v1/",
                                "0.52.0-security-revoke",
                                60,
                                true),
                        () -> Optional.of("a".repeat(80))));
    }

    @After
    public void tearDown() throws IOException {
        repository.close();
        server.shutdown();
    }

    @Test
    public void executesBoundDeviceFlowWithoutClientAuthority()
            throws Exception {
        server.enqueue(json(201, preview(DEVICE_ID, 7, TARGET_USER_ID)));
        server.enqueue(json(201, challenge()));
        server.enqueue(json(200, grant()));
        server.enqueue(json(200, result(TARGET_USER_ID, DEVICE_ID, 8)));

        ManagedAccountRevocationPreview preview =
                repository.prepare(device());
        ManagedAccountRevocationChallenge challenge =
                repository.challenge(preview.actionId());
        ManagedAccountRevocationGrant grant = repository.verify(
                preview.actionId(),
                challenge.id(),
                "credential".repeat(10));
        ManagedAccountRevocationResult result = repository.execute(
                preview.actionId(),
                grant.token(),
                "50000000-0000-4000-8000-000000000001");

        RecordedRequest prepareRequest = server.takeRequest();
        RecordedRequest challengeRequest = server.takeRequest();
        RecordedRequest verifyRequest = server.takeRequest();
        RecordedRequest executeRequest = server.takeRequest();
        assertEquals(
                "Prepare must bind only the selected user and device.",
                "/api/mobile/v1/user-administration/users/42/security/devices/"
                        + DEVICE_ID
                        + "/revocations/prepare",
                prepareRequest.getPath());
        assertTrue(
                "Prepare must carry the displayed security revision.",
                prepareRequest.getBody().readUtf8()
                        .contains("\"expectedRevision\":7"));
        assertFalse(
                "The Android client must not claim a role or store.",
                challengeRequest.getBody().readUtf8()
                        .matches(".*(role|organization|store).*"));
        assertTrue(
                "Step-up verification must carry the bound challenge.",
                verifyRequest.getBody().readUtf8()
                        .contains(CHALLENGE_ID));
        assertEquals(
                "Execution retry identity must be explicit.",
                "50000000-0000-4000-8000-000000000001",
                executeRequest.getHeader("Idempotency-Key"));
        assertEquals(
                "The successful mutation must advance the revision.",
                8,
                result.revision());
        assertEquals(
                "The result must remain a device revocation.",
                AccountAccessView.DEVICES,
                result.view());
    }

    @Test
    public void changedPreviewScopeFailsClosed() {
        server.enqueue(json(201, preview(DEVICE_ID, 8, TARGET_USER_ID)));

        AccountAccessException failure = assertThrows(
                AccountAccessException.class,
                () -> repository.prepare(device()));

        assertEquals(
                "A changed revision must be rejected as protocol data.",
                AccountAccessFailureKind.PROTOCOL,
                failure.kind());
    }

    @Test
    public void changedExecutionTargetFailsClosed() {
        server.enqueue(json(200, result(43, DEVICE_ID, 8)));

        AccountAccessException failure = assertThrows(
                AccountAccessException.class,
                () -> repository.execute(
                        ACTION_ID,
                        TOKEN,
                        "50000000-0000-4000-8000-000000000001"));

        assertEquals(
                "A changed target must be rejected as protocol data.",
                AccountAccessFailureKind.PROTOCOL,
                failure.kind());
    }

    @Test
    public void closedRepositoryCannotReachNetwork() {
        repository.close();

        AccountAccessException failure = assertThrows(
                AccountAccessException.class,
                () -> repository.prepare(device()));

        assertEquals(
                "A closed administrative scope is locally revoked.",
                AccountAccessFailureKind.ACCESS_REVOKED,
                failure.kind());
        assertEquals(
                "Closed scope must not issue a request.",
                0,
                server.getRequestCount());
    }

    private static AccountDevice device() {
        return new AccountDevice(
                DEVICE_ID,
                "Aparelho corporativo",
                Optional.of("Samsung"),
                Optional.of("SM-X"),
                35,
                "0.52.0",
                AccountAccessStatus.ACTIVE,
                Instant.parse("2026-07-31T08:00:00Z"),
                Instant.parse("2026-07-31T09:00:00Z"),
                Optional.empty(),
                false,
                7);
    }

    private static MockResponse json(int status, String body) {
        return new MockResponse()
                .setResponseCode(status)
                .setHeader(CONTENT_TYPE, "application/json")
                .setBody(body);
    }

    private static String preview(
            String resourceId,
            int revision,
            long targetUserId) {
        return """
                {
                  "actionId":"__ACTION__",
                  "kind":"device",
                  "target":{"id":__TARGET__,"name":"Usuário alvo"},
                  "resource":{"id":"__RESOURCE__","label":"Aparelho corporativo","revision":__REVISION__},
                  "consequence":"Todas as sessões serão encerradas.",
                  "expiresAt":"2026-07-31T13:00:00Z"
                }
                """
                .replace("__ACTION__", ACTION_ID)
                .replace("__TARGET__", Long.toString(targetUserId))
                .replace("__RESOURCE__", resourceId)
                .replace("__REVISION__", Integer.toString(revision));
    }

    private static String challenge() {
        return """
                {
                  "challengeId":"__CHALLENGE__",
                  "nonce":"__NONCE__",
                  "googleServerClientId":"473842962788-example.apps.googleusercontent.com",
                  "expiresAt":"2026-07-31T12:55:00Z"
                }
                """
                .replace("__CHALLENGE__", CHALLENGE_ID)
                .replace("__NONCE__", "3vn1_" + "n".repeat(43));
    }

    private static String grant() {
        return """
                {
                  "stepUpToken":"__TOKEN__",
                  "expiresAt":"2026-07-31T12:52:00Z"
                }
                """.replace("__TOKEN__", TOKEN);
    }

    private static String result(
            long targetUserId,
            String resourceId,
            int revision) {
        return """
                {
                  "actionId":"__ACTION__",
                  "kind":"device",
                  "targetUserId":__TARGET__,
                  "resourceId":"__RESOURCE__",
                  "changed":true,
                  "revision":__REVISION__
                }
                """
                .replace("__ACTION__", ACTION_ID)
                .replace("__TARGET__", Long.toString(targetUserId))
                .replace("__RESOURCE__", resourceId)
                .replace("__REVISION__", Integer.toString(revision));
    }
}
