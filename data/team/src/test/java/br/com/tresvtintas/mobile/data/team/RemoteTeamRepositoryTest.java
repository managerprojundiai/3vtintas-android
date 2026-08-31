package br.com.tresvtintas.mobile.data.team;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;

import br.com.tresvtintas.mobile.core.network.MobileApiFactory;
import br.com.tresvtintas.mobile.core.network.NetworkConfiguration;
import br.com.tresvtintas.mobile.core.team.TeamContext;
import br.com.tresvtintas.mobile.core.team.TeamException;
import br.com.tresvtintas.mobile.core.team.TeamFailureKind;
import br.com.tresvtintas.mobile.core.team.TeamSnapshot;
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

public final class RemoteTeamRepositoryTest {
    private static final String CONTENT_TYPE = "Content-Type";
    private MockWebServer server;
    private RemoteTeamRepository repository;

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
    public void readsTeamWithoutClientAuthorizationClaims()
            throws Exception {
        server.enqueue(json(teamJson()));

        TeamSnapshot snapshot = repository.load();
        RecordedRequest request = server.takeRequest();

        assertEquals(
                "Only the selected organization may be submitted.",
                "/api/mobile/v1/team?organizationId=9",
                request.getPath());
        assertFalse(
                "The client must not assert its role.",
                request.getPath().contains("role="));
        assertFalse(
                "The client must not assert its visibility.",
                request.getPath().contains("scope="));
        assertEquals(
                "Server-derived visibility must be preserved.",
                TeamContext.Visibility.TEAM,
                snapshot.context().visibility());
        assertEquals(
                "Money must remain an exact decimal.",
                "301.25",
                snapshot.summary().totalSales().toPlainString());
    }

    @Test
    public void rejectsUnexpectedSensitiveFields() {
        server.enqueue(json(teamJson().replace(
                "\"company\":\"Pinturas Piloto\"",
                "\"company\":\"Pinturas Piloto\","
                        + "\"email\":\"private@example.com\"")));

        TeamException failure = assertThrows(
                "Unexpected personal fields must fail closed.",
                TeamException.class,
                repository::load);

        assertEquals(
                "A schema leak must be treated as a protocol failure.",
                TeamFailureKind.PROTOCOL,
                failure.kind());
    }

    @Test
    public void mapsForbiddenProblemAndPreservesRequestTrace() {
        String requestId = UUID.randomUUID().toString();
        server.enqueue(new MockResponse()
                .setResponseCode(403)
                .setHeader(CONTENT_TYPE, "application/problem+json")
                .setBody(problemJson(requestId)));

        TeamException failure = assertThrows(
                "A rejected team request must return a typed failure.",
                TeamException.class,
                repository::load);

        assertEquals(
                "Authorization rejection must map to forbidden.",
                TeamFailureKind.FORBIDDEN,
                failure.kind());
        assertEquals(
                "Support correlation must be retained.",
                requestId,
                failure.requestId().orElseThrow());
    }

    @Test
    public void revokedScopeCannotIssueAnotherRequest() {
        repository.close();

        TeamException failure = assertThrows(
                "A closed account scope must reject reads.",
                TeamException.class,
                repository::load);

        assertEquals(
                "A closed scope must fail as access revoked.",
                TeamFailureKind.ACCESS_REVOKED,
                failure.kind());
        assertEquals(
                "A revoked scope must not issue a network request.",
                0,
                server.getRequestCount());
    }

    private RemoteTeamRepository repository(OptionalLong organizationId) {
        return new RemoteTeamRepository(
                new TeamAccountScope(
                        21,
                        "b".repeat(64),
                        organizationId),
                MobileApiFactory.create(
                        new NetworkConfiguration(
                                "http://localhost:" + server.getPort()
                                        + "/api/mobile/v1/",
                                "0.43.0-team",
                                50,
                                true),
                        () -> Optional.of("a".repeat(80))));
    }

    private static MockResponse json(String body) {
        return new MockResponse()
                .setResponseCode(200)
                .setHeader(CONTENT_TYPE, "application/json")
                .setBody(body);
    }

    private static String teamJson() {
        return """
                {
                  "generatedAt":"2026-07-30T12:00:00.000Z",
                  "context":{
                    "visibility":"team",
                    "organization":{"id":9,"name":"Jundiaí"}
                  },
                  "summary":{
                    "teamPainters":1,
                    "totalOrders":2,
                    "totalQuotes":4,
                    "totalSales":"301.25",
                    "topRegion":{"label":"Jundiaí / SP","count":2}
                  },
                  "members":[{
                    "painterId":91,
                    "name":"Pintor Piloto",
                    "company":"Pinturas Piloto",
                    "commissionRate":"10.00",
                    "status":"active",
                    "totalQuotes":4,
                    "totalOrders":2,
                    "conversionBasisPoints":5000,
                    "totalSales":"301.25",
                    "topRegion":{"label":"Jundiaí / SP","count":2}
                  }],
                  "regions":[{"label":"Jundiaí / SP","count":2}]
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
