package br.com.tresvtintas.mobile.data.audit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;

import br.com.tresvtintas.mobile.core.audit.AuditException;
import br.com.tresvtintas.mobile.core.audit.AuditFailureKind;
import br.com.tresvtintas.mobile.core.audit.AuditModels.Page;
import br.com.tresvtintas.mobile.core.audit.AuditQuery;
import br.com.tresvtintas.mobile.core.model.AppRole;
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

public final class RemoteAuditRepositoryTest {
    private MockWebServer server;
    private RemoteAuditRepository repository;

    @Before
    public void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        repository = new RemoteAuditRepository(
                new AuditAccountScope(31, "b".repeat(64)),
                MobileApiFactory.create(
                        new NetworkConfiguration(
                                "http://localhost:" + server.getPort()
                                        + "/api/mobile/v1/",
                                "0.49.0-audit",
                                57,
                                true),
                        () -> Optional.of("a".repeat(80))));
    }

    @After
    public void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    public void mapsOnlyTheSanitizedProjectionAndCanonicalFilters() throws Exception {
        server.enqueue(json("""
                {"items":[
                  {"action":"order.status_changed","entity":"order",
                   "actor":{"name":"Cesar","role":"master_admin"},
                   "occurredAt":"2026-07-31T12:00:00.000Z"},
                  {"action":"system.started","entity":null,"actor":null,
                   "occurredAt":"2026-07-31T11:00:00.000Z"}
                ],"nextCursor":"opaque_cursor"}
                """));

        Page page = repository.events(
                new AuditQuery(
                        Optional.of("order.status_changed"),
                        Optional.of("order"),
                        30),
                Optional.of("previous_cursor"));
        RecordedRequest request = server.takeRequest();

        assertEquals("Only canonical audit filters may reach the server.",
                "/api/mobile/v1/audit/events?action=order.status_changed"
                        + "&entity=order&cursor=previous_cursor&limit=30",
                request.getPath());
        assertFalse("The local user scope must never be sent as authority.",
                request.getPath().contains("userId"));
        assertFalse("Authorization revisions are local lifecycle guards only.",
                request.getPath().contains("authorizationRevision"));
        assertEquals("The public actor role must map strictly.",
                AppRole.MASTER_ADMIN,
                page.items().get(0).actor().orElseThrow().role());
        assertEquals("System events must keep an empty actor.",
                Optional.empty(), page.items().get(1).actor());
    }

    @Test
    public void unknownRolesAndInternalErrorDetailsFailClosed() {
        server.enqueue(json("""
                {"items":[{"action":"test","entity":null,
                "actor":{"name":"Unknown","role":"owner"},
                "occurredAt":"2026-07-31T12:00:00.000Z"}],"nextCursor":null}
                """));
        AuditException protocol = assertThrows(
                "Unknown roles must fail as a protocol error.",
                AuditException.class,
                () -> repository.events(AuditQuery.initial(), Optional.empty()));

        server.enqueue(new MockResponse()
                .setResponseCode(403)
                .setHeader("Content-Type", "application/problem+json")
                .setBody("""
                        {"type":"https://3vtintas.com.br/problems/forbidden",
                        "title":"Proibido","status":403,
                        "detail":"Internal authorization details.",
                        "instance":"urn:3v:request:test","code":"FORBIDDEN",
                        "requestId":"00000000-0000-4000-8000-000000000099"}
                        """));
        AuditException forbidden = assertThrows(
                "Forbidden access must remain explicit.",
                AuditException.class,
                () -> repository.events(AuditQuery.initial(), Optional.empty()));

        assertEquals("Unknown actors must fail closed.",
                AuditFailureKind.PROTOCOL, protocol.kind());
        assertEquals("Server authorization must map to forbidden.",
                AuditFailureKind.FORBIDDEN, forbidden.kind());
        assertFalse("Internal details must not become a UI message.",
                forbidden.getMessage().contains("Internal authorization"));
        assertEquals("Only the safe support ID may cross transport.",
                Optional.of("00000000-0000-4000-8000-000000000099"),
                forbidden.requestId());
    }

    @Test
    public void closedScopeCannotIssueAnotherRequest() {
        repository.close();

        AuditException failure = assertThrows(
                "Closed scopes must fail before transport.",
                AuditException.class,
                () -> repository.events(AuditQuery.initial(), Optional.empty()));

        assertEquals("Closed scopes must be reported as revoked.",
                AuditFailureKind.ACCESS_REVOKED, failure.kind());
        assertEquals("No request may leave a revoked scope.",
                0, server.getRequestCount());
    }

    private static MockResponse json(String body) {
        return new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(body);
    }
}
