package br.com.tresvtintas.mobile.data.dashboard;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import br.com.tresvtintas.mobile.core.dashboard.DashboardException;
import br.com.tresvtintas.mobile.core.dashboard.DashboardFailureKind;
import br.com.tresvtintas.mobile.core.dashboard.DashboardSnapshot;
import br.com.tresvtintas.mobile.core.dashboard.DashboardVisibility;
import br.com.tresvtintas.mobile.core.network.MobileApiFactory;
import br.com.tresvtintas.mobile.core.network.NetworkConfiguration;
import java.io.IOException;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public final class RemoteDashboardRepositoryTest {
    private static final String CONTENT_TYPE = "Content-Type";
    private MockWebServer server;
    private RemoteDashboardRepository repository;

    @Before
    public void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        repository = repository(OptionalLong.of(9));
    }

    @After
    public void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    public void readsRoleScopedDashboardWithoutClientAuthorizationClaims()
            throws Exception {
        server.enqueue(json(fullDashboard()));

        DashboardSnapshot snapshot = repository.load();
        RecordedRequest request = server.takeRequest();

        assertEquals(
                "Only the selected organization may be submitted.",
                "/api/mobile/v1/dashboard?organizationId=9",
                request.getPath());
        assertFalse(
                "The client must not assert its role.",
                request.getPath().contains("role="));
        assertFalse(
                "The client must not assert its visibility.",
                request.getPath().contains("scope="));
        assertEquals(
                "Server-derived visibility must be preserved.",
                DashboardVisibility.TEAM,
                snapshot.context().visibility());
        assertEquals(
                "Money must remain an exact decimal.",
                "180.25",
                snapshot.commissions().orElseThrow()
                        .pending().amount().toPlainString());
        assertEquals(
                "Nullable operational counts must map explicitly.",
                Optional.of(4),
                snapshot.workload().deliveries().orElseThrow()
                        .awaitingSchedule());
    }

    @Test
    public void allowsOnlySectionsReturnedForTheAuthorizedRole()
            throws Exception {
        repository = repository(OptionalLong.empty());
        server.enqueue(json(minimalDashboard()));

        DashboardSnapshot snapshot = repository.load();
        RecordedRequest request = server.takeRequest();

        assertEquals(
                "A personal dashboard must not invent an organization query.",
                "/api/mobile/v1/dashboard",
                request.getPath());
        assertTrue(
                "Orders absent from the server response must stay hidden.",
                snapshot.workload().orders().isEmpty());
        assertTrue(
                "Corporate finance absent from the response must stay hidden.",
                snapshot.finance().isEmpty());
    }

    @Test
    public void rejectsUnknownEnumsAndUnexpectedSensitiveFields() {
        server.enqueue(json(fullDashboard().replace(
                "\"visibility\":\"team\"",
                "\"visibility\":\"invented\"")));
        server.enqueue(json(fullDashboard().replace(
                "\"generatedAt\":\"2026-07-28T12:00:00.000Z\"",
                "\"generatedAt\":\"2026-07-28T12:00:00.000Z\","
                        + "\"internalPrompt\":\"do not expose\"")));

        DashboardException enumFailure = assertThrows(
                "Unknown authorization values must fail closed.",
                DashboardException.class,
                repository::load);
        DashboardException fieldFailure = assertThrows(
                "Unexpected internal fields must fail closed.",
                DashboardException.class,
                repository::load);

        assertEquals(
                "Unknown visibility must be a protocol failure.",
                DashboardFailureKind.PROTOCOL,
                enumFailure.kind());
        assertEquals(
                "Unexpected sensitive data must be a protocol failure.",
                DashboardFailureKind.PROTOCOL,
                fieldFailure.kind());
    }

    @Test
    public void mapsForbiddenProblemAndPreservesRequestTrace() {
        String requestId = UUID.randomUUID().toString();
        server.enqueue(new MockResponse()
                .setResponseCode(403)
                .setHeader(CONTENT_TYPE, "application/problem+json")
                .setBody(problemJson(requestId)));

        DashboardException failure = assertThrows(
                "A rejected dashboard must return a typed failure.",
                DashboardException.class,
                repository::load);

        assertEquals(
                "Authorization rejection must map to forbidden.",
                DashboardFailureKind.FORBIDDEN,
                failure.kind());
        assertEquals(
                "Support correlation must be retained.",
                requestId,
                failure.requestId().orElseThrow());
    }

    @Test
    public void revokedScopeCannotIssueAnotherRequest() {
        repository.close();

        DashboardException failure = assertThrows(
                "A closed account scope must reject reads.",
                DashboardException.class,
                repository::load);

        assertEquals(
                "A closed scope must fail as access revoked.",
                DashboardFailureKind.ACCESS_REVOKED,
                failure.kind());
        assertEquals(
                "A revoked scope must not issue a network request.",
                0,
                server.getRequestCount());
    }

    private RemoteDashboardRepository repository(
            OptionalLong organizationId) {
        return new RemoteDashboardRepository(
                new DashboardAccountScope(
                        21,
                        "b".repeat(64),
                        organizationId),
                MobileApiFactory.create(
                        new NetworkConfiguration(
                                "http://localhost:" + server.getPort()
                                        + "/api/mobile/v1/",
                                "0.29.0-dashboard",
                                15,
                                true),
                        () -> Optional.of("a".repeat(80))));
    }

    private static MockResponse json(String body) {
        return new MockResponse()
                .setResponseCode(200)
                .setHeader(CONTENT_TYPE, "application/json")
                .setBody(body);
    }

    private static String fullDashboard() {
        return """
                {
                  "generatedAt":"2026-07-28T12:00:00.000Z",
                  "context":{
                    "visibility":"team",
                    "organization":{"id":9,"name":"Loja Centro"}
                  },
                  "workload":{
                    "orders":{"total":12,"active":7,"awaitingPayment":2},
                    "quotes":{
                      "activeMaterial":5,"activeLabor":3,"expiringSoon":1
                    },
                    "deliveries":{
                      "active":8,"today":3,"inTransit":2,"awaitingSchedule":4
                    }
                  },
                  "commissions":{
                    "scope":"team",
                    "pending":{"count":4,"amount":"180.25"},
                    "approved":{"count":9,"amount":"450.00"}
                  },
                  "appointments":{"scope":"team","today":3,"upcoming":12},
                  "finance":{
                    "scope":"corporate",
                    "pending":{
                      "expense":{"count":2,"amount":"50.00"},
                      "payable":{"count":3,"amount":"90.00"},
                      "receivable":{"count":4,"amount":"120.00"}
                    },
                    "overdue":{
                      "expense":{"count":1,"amount":"10.00"},
                      "payable":{"count":1,"amount":"20.00"},
                      "receivable":{"count":1,"amount":"30.00"}
                    }
                  }
                }
                """;
    }

    private static String minimalDashboard() {
        return """
                {
                  "generatedAt":"2026-07-28T12:00:00.000Z",
                  "context":{"visibility":"self","organization":null},
                  "workload":{
                    "orders":null,
                    "quotes":null,
                    "deliveries":null
                  },
                  "commissions":null,
                  "appointments":{
                    "scope":"self","today":1,"upcoming":2
                  },
                  "finance":null
                }
                """;
    }

    private static String problemJson(String requestId) {
        return ("{\"type\":\"https://www.3vtintas.com.br/problems/forbidden\","
                + "\"title\":\"Forbidden\",\"status\":403,"
                + "\"detail\":\"Access denied.\","
                + "\"instance\":\"urn:3v:request:%s\","
                + "\"code\":\"FORBIDDEN\",\"requestId\":\"%s\"}")
                .formatted(requestId, requestId);
    }
}
