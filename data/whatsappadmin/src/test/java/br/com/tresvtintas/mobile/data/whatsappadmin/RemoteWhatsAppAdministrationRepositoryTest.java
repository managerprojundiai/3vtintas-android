package br.com.tresvtintas.mobile.data.whatsappadmin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import br.com.tresvtintas.mobile.core.network.MobileApiFactory;
import br.com.tresvtintas.mobile.core.network.NetworkConfiguration;
import br.com.tresvtintas.mobile.core.whatsappadmin.WhatsAppAdministrationException;
import br.com.tresvtintas.mobile.core.whatsappadmin.WhatsAppAdministrationFailureKind;
import br.com.tresvtintas.mobile.core.whatsappadmin.WhatsAppAdministrationModels.ActionResult;
import br.com.tresvtintas.mobile.core.whatsappadmin.WhatsAppAdministrationModels.EphemeralQr;
import br.com.tresvtintas.mobile.core.whatsappadmin.WhatsAppAdministrationModels.Snapshot;
import br.com.tresvtintas.mobile.core.whatsappadmin.WhatsAppChannelMode;
import java.io.IOException;
import java.util.Optional;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public final class RemoteWhatsAppAdministrationRepositoryTest {
    private static final String CONTENT_TYPE = "Content-Type";
    private static final String IDEMPOTENCY_KEY =
            "00000000-0000-4000-8000-000000000715";
    private static final long STORE_ID = 31L;
    private static final long CONNECTION_ID = 77L;

    private MockWebServer server;
    private RemoteWhatsAppAdministrationRepository repository;

    @Before
    public void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        repository = new RemoteWhatsAppAdministrationRepository(
                new WhatsAppAdministrationAccountScope(21L, "b".repeat(64)),
                MobileApiFactory.create(
                        new NetworkConfiguration(
                                "http://localhost:"
                                        + server.getPort()
                                        + "/api/mobile/v1/",
                                "0.54.0-whatsapp-admin",
                                62,
                                true),
                        () -> Optional.of("a".repeat(80))));
    }

    @After
    public void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    public void loadsTheSafeMasterOnlyProjection() throws Exception {
        server.enqueue(json(snapshotJson()));

        Snapshot snapshot = repository.load();
        RecordedRequest request = server.takeRequest();

        assertEquals(
                "The exact administration route must be used.",
                "GET /api/mobile/v1/whatsapp-administration/stores",
                request.getMethod() + " " + request.getPath());
        assertEquals(
                "The protected stack must attach authentication.",
                "Bearer " + "a".repeat(80),
                request.getHeader("Authorization"));
        assertFalse(
                "Client requests must not submit an authority scope.",
                request.getPath().matches(".*(role|userId|organizationId)=.*"));
        assertEquals(
                "All safe stores must map.",
                1,
                snapshot.stores().size());
        assertEquals(
                "The public mode must map.",
                WhatsAppChannelMode.EVOLUTION,
                snapshot.stores().get(0).mode());
    }

    @Test
    public void configuresMetaWithConfirmationAndIdempotency() throws Exception {
        server.enqueue(json(actionJson("configure_meta", null, null))
                .setHeader("X-Idempotency-Replayed", "false"));

        ActionResult result = repository.configureMeta(
                STORE_ID,
                "12345678901",
                Optional.of("5511999999999"),
                IDEMPOTENCY_KEY);
        RecordedRequest request = server.takeRequest();
        String body = request.getBody().readUtf8();

        assertEquals(
                "Meta configuration must use the store action route.",
                "POST /api/mobile/v1/whatsapp-administration/stores/31/actions",
                request.getMethod() + " " + request.getPath());
        assertEquals(
                "The exact idempotency key must cross the boundary.",
                IDEMPOTENCY_KEY,
                request.getHeader("Idempotency-Key"));
        assertTrue(
                "Visual confirmation must be explicit.",
                body.contains("\"confirmed\":true"));
        assertTrue(
                "Only the public Meta identifier may be sent.",
                body.contains("\"phoneNumberId\":\"12345678901\""));
        assertFalse(
                "Tokens and secrets must never be serialized.",
                body.matches(".*(?i)(token|secret|apiKey|webhook).*"));
        assertFalse("First execution must not be replayed.", result.replayed());
    }

    @Test
    public void qrUsesTheDedicatedNonIdempotentRoute() throws Exception {
        server.enqueue(json(actionJson(
                "request_evolution_qr",
                "data:image/png;base64,AAAA",
                "PAIR-123")));

        EphemeralQr qr = repository.requestQr(CONNECTION_ID);
        RecordedRequest request = server.takeRequest();

        assertEquals(
                "QR must use its dedicated endpoint.",
                "POST /api/mobile/v1/whatsapp-administration/connections/77/qr",
                request.getMethod() + " " + request.getPath());
        assertNull(
                "QR retrieval must not create an idempotency record.",
                request.getHeader("Idempotency-Key"));
        assertTrue(
                "QR retrieval must still require confirmation.",
                request.getBody().readUtf8().contains("\"confirmed\":true"));
        assertEquals(
                "Pairing code must remain ephemeral.",
                "PAIR-123",
                qr.pairingCode().orElseThrow());
    }

    @Test
    public void mutationFailsClosedIfQrMaterialAppears() {
        server.enqueue(json(actionJson(
                "provision_evolution",
                "data:image/png;base64,AAAA",
                null))
                .setHeader("X-Idempotency-Replayed", "false"));

        WhatsAppAdministrationException failure = assertThrows(
                "Mutation must reject leaked QR material.",
                WhatsAppAdministrationException.class,
                () -> repository.provisionEvolution(STORE_ID, IDEMPOTENCY_KEY));

        assertEquals(
                "Credential leakage must fail as a protocol error.",
                WhatsAppAdministrationFailureKind.PROTOCOL,
                failure.kind());
    }

    @Test
    public void unknownSecretFieldFailsClosed() {
        server.enqueue(json(snapshotJson().replace(
                "\"hasError\":false",
                "\"hasError\":false,\"apiToken\":\"must-not-leak\"")));

        WhatsAppAdministrationException failure = assertThrows(
                "Unknown sensitive fields must not be ignored.",
                WhatsAppAdministrationException.class,
                repository::load);

        assertEquals(
                "Unknown fields must fail as a protocol error.",
                WhatsAppAdministrationFailureKind.PROTOCOL,
                failure.kind());
    }

    @Test
    public void closedScopeNeverIssuesAnotherRequest() {
        repository.close();

        WhatsAppAdministrationException failure = assertThrows(
                "Closed account scope must reject work.",
                WhatsAppAdministrationException.class,
                repository::load);

        assertEquals(
                "Closed account scope must be reported as revoked.",
                WhatsAppAdministrationFailureKind.ACCESS_REVOKED,
                failure.kind());
        assertEquals(
                "Closed scope must not reach the network.",
                0,
                server.getRequestCount());
    }

    private static MockResponse json(String body) {
        return new MockResponse()
                .setResponseCode(200)
                .setHeader(CONTENT_TYPE, "application/json")
                .setBody(body);
    }

    private static String snapshotJson() {
        return """
                {
                  "stores":[{
                    "id":31,
                    "slug":"jundiai",
                    "name":"3V Tintas Jundiaí",
                    "status":"active",
                    "mode":"evolution",
                    "connections":[{
                      "id":77,
                      "organizationId":31,
                      "provider":"evolution",
                      "label":"WhatsApp Evolution",
                      "status":"connected",
                      "inboundEnabled":true,
                      "outboundEnabled":true,
                      "phoneNumber":"5511999999999",
                      "phoneNumberId":null,
                      "evolutionInstanceName":"store-31",
                      "hasError":false,
                      "lastConnectedAt":"2026-08-01T11:00:00Z",
                      "lastSeenAt":"2026-08-01T11:05:00Z",
                      "updatedAt":"2026-08-01T11:05:00Z"
                    }]
                  }]
                }
                """;
    }

    private static String actionJson(
            String action,
            String qrCode,
            String pairingCode) {
        return ("""
                {
                  "action":"__ACTION__",
                  "organizationId":31,
                  "connectionId":77,
                  "mode":"evolution",
                  "status":"connecting",
                  "phoneNumber":null,
                  "qrCode":__QR__,
                  "pairingCode":__PAIRING__
                }
                """)
                .replace("__ACTION__", action)
                .replace("__QR__", nullableJson(qrCode))
                .replace("__PAIRING__", nullableJson(pairingCode));
    }

    private static String nullableJson(String value) {
        return value == null ? "null" : "\"" + value + "\"";
    }
}
