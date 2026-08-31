package br.com.tresvtintas.mobile.data.systemconfiguration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import br.com.tresvtintas.mobile.core.network.MobileApiFactory;
import br.com.tresvtintas.mobile.core.network.NetworkConfiguration;
import br.com.tresvtintas.mobile.core.systemconfiguration.SystemConfigurationException;
import br.com.tresvtintas.mobile.core.systemconfiguration.SystemConfigurationFailureKind;
import br.com.tresvtintas.mobile.core.systemconfiguration.SystemConfigurationModels.Mutation;
import br.com.tresvtintas.mobile.core.systemconfiguration.SystemConfigurationModels.Snapshot;
import br.com.tresvtintas.mobile.core.systemconfiguration.SystemConfigurationModels.Values;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public final class RemoteSystemConfigurationRepositoryTest {
    private static final String CONTENT_TYPE = "Content-Type";
    private static final String IDEMPOTENCY_KEY =
            "00000000-0000-4000-8000-000000000714";
    private static final Values VALUES = new Values(
            "3V Tintas Jundiaí",
            "(11) 4000-0000",
            "Rua das Tintas, 3",
            "Bellarte Pinturas",
            "(11) 4999-0000",
            "3.00",
            false);

    private MockWebServer server;
    private RemoteSystemConfigurationRepository repository;

    @Before
    public void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        repository = new RemoteSystemConfigurationRepository(
                new SystemConfigurationAccountScope(21, "b".repeat(64)),
                MobileApiFactory.create(
                        new NetworkConfiguration(
                                "http://localhost:"
                                        + server.getPort()
                                        + "/api/mobile/v1/",
                                "0.53.0-system-config",
                                61,
                                true),
                        () -> Optional.of("a".repeat(80))));
    }

    @After
    public void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    public void loadsOnlyTheAuthenticatedMasterSnapshot() throws Exception {
        server.enqueue(json(configurationJson()));

        Snapshot snapshot = repository.load();
        RecordedRequest request = server.takeRequest();

        assertEquals("The exact safe route must be used.",
                "GET /api/mobile/v1/system-configuration",
                request.getMethod() + " " + request.getPath());
        assertEquals("The protected stack must attach Bearer authentication.",
                "Bearer " + "a".repeat(80),
                request.getHeader("Authorization"));
        assertFalse("Authority must never be submitted by the client.",
                request.getPath().matches(
                        ".*(role|organizationId|userId)=.*"));
        assertEquals("The server revision must be preserved.",
                7, snapshot.revision());
        assertEquals("Only allowlisted values may be mapped.",
                VALUES, snapshot.values());
    }

    @Test
    public void updatesAFullConfirmedSnapshotWithIdempotency() throws Exception {
        server.enqueue(json("{\"configuration\":"
                + configurationJson(8)
                + ",\"changed\":true}")
                .setHeader("X-Idempotency-Replayed", "false"));

        Mutation mutation = repository.update(VALUES, 7, IDEMPOTENCY_KEY);
        RecordedRequest request = server.takeRequest();
        String body = request.getBody().readUtf8();

        assertEquals("The update must use the dedicated action route.",
                "POST /api/mobile/v1/system-configuration/actions",
                request.getMethod() + " " + request.getPath());
        assertEquals("The exact idempotency key must cross the boundary.",
                IDEMPOTENCY_KEY,
                request.getHeader("Idempotency-Key"));
        assertTrue("The visual confirmation must be explicit in the command.",
                body.contains("\"confirmed\":true"));
        assertTrue("The displayed revision must bind the update.",
                body.contains("\"expectedRevision\":7"));
        assertFalse("Role, store and actor scope must remain server-derived.",
                body.matches(".*(role|organizationId|userId).*"));
        assertTrue("The server changed result must be preserved.",
                mutation.changed());
        assertFalse("A first execution must not be marked as replay.",
                mutation.replayed());
        assertEquals("The committed revision must be returned.",
                8, mutation.configuration().revision());
    }

    @Test
    public void unexpectedSecretFieldFailsClosed() {
        server.enqueue(json(configurationJson().replace(
                "\"revision\":7",
                "\"revision\":7,\"openaiApiKey\":\"must-not-leak\"")));

        SystemConfigurationException failure = assertThrows(
                SystemConfigurationException.class,
                repository::load);

        assertEquals("Unknown sensitive fields must never be ignored.",
                SystemConfigurationFailureKind.PROTOCOL,
                failure.kind());
    }

    @Test
    public void conflictPreservesTheSupportCorrelation() {
        String requestId = UUID.randomUUID().toString();
        server.enqueue(new MockResponse()
                .setResponseCode(409)
                .setHeader(CONTENT_TYPE, "application/problem+json")
                .setBody(problemJson(requestId)));

        SystemConfigurationException failure = assertThrows(
                SystemConfigurationException.class,
                () -> repository.update(VALUES, 7, IDEMPOTENCY_KEY));

        assertEquals("A stale revision must remain a conflict.",
                SystemConfigurationFailureKind.CONFLICT,
                failure.kind());
        assertEquals("The support request ID must cross the boundary.",
                requestId,
                failure.requestId().orElseThrow());
    }

    @Test
    public void closedScopeNeverIssuesAnotherRequest() {
        repository.close();

        SystemConfigurationException failure = assertThrows(
                SystemConfigurationException.class,
                repository::load);

        assertEquals("A replaced account scope must fail as revoked.",
                SystemConfigurationFailureKind.ACCESS_REVOKED,
                failure.kind());
        assertEquals("A revoked scope must not reach the network.",
                0, server.getRequestCount());
    }

    private static MockResponse json(String body) {
        return new MockResponse()
                .setResponseCode(200)
                .setHeader(CONTENT_TYPE, "application/json")
                .setBody(body);
    }

    private static String configurationJson() {
        return configurationJson(7);
    }

    private static String configurationJson(int revision) {
        return """
                {
                  "storeName":"3V Tintas Jundiaí",
                  "storePhone":"(11) 4000-0000",
                  "storeAddress":"Rua das Tintas, 3",
                  "laborCompanyName":"Bellarte Pinturas",
                  "laborCompanyContact":"(11) 4999-0000",
                  "defaultCommissionRate":"3.00",
                  "autoApprovePainters":false,
                  "revision":__REVISION__,
                  "updatedAt":"2026-07-31T12:00:00Z"
                }
                """.replace(
                        "__REVISION__",
                        Integer.toString(revision));
    }

    private static String problemJson(String requestId) {
        return ("{\"type\":\"https://www.3vtintas.com.br/problems/conflict\","
                + "\"title\":\"Conflict\",\"status\":409,"
                + "\"detail\":\"Resource changed.\","
                + "\"instance\":\"urn:3v:request:%s\","
                + "\"code\":\"RESOURCE_CONFLICT\",\"requestId\":\"%s\"}")
                .formatted(requestId, requestId);
    }
}
