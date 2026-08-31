package br.com.tresvtintas.mobile.data.audit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;

import br.com.tresvtintas.mobile.core.audit.AgentReplayModels.Channel;
import br.com.tresvtintas.mobile.core.audit.AgentReplayModels.ChannelFilter;
import br.com.tresvtintas.mobile.core.audit.AgentReplayModels.Page;
import br.com.tresvtintas.mobile.core.audit.AgentReplayQuery;
import br.com.tresvtintas.mobile.core.audit.AuditException;
import br.com.tresvtintas.mobile.core.audit.AuditFailureKind;
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

public final class RemoteAgentReplayRepositoryTest {
    private MockWebServer server;
    private RemoteAgentReplayRepository repository;

    @Before
    public void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        repository = new RemoteAgentReplayRepository(
                new AuditAccountScope(31, "b".repeat(64)),
                MobileApiFactory.create(
                        new NetworkConfiguration(
                                "http://localhost:" + server.getPort()
                                        + "/api/mobile/v1/",
                                "0.50.0-agent-replay",
                                58,
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
                {"items":[{"channel":"whatsapp",
                  "startedAt":"2026-07-31T12:00:00.000Z",
                  "endedAt":"2026-07-31T12:00:02.000Z",
                  "outcome":"completed","steps":[
                    {"phase":"received","outcome":"ok",
                     "occurredAt":"2026-07-31T12:00:00.000Z"},
                    {"phase":"completed","outcome":"ok",
                     "occurredAt":"2026-07-31T12:00:02.000Z"}],
                  "stepsTruncated":false}],"nextCursor":"opaque_cursor"}
                """));

        Page page = repository.turns(
                new AgentReplayQuery(ChannelFilter.WHATSAPP, 15),
                Optional.of("previous_cursor"));
        RecordedRequest request = server.takeRequest();

        assertEquals("Only canonical replay filters may reach the server.",
                "/api/mobile/v1/audit/agent-replays?channel=whatsapp"
                        + "&cursor=previous_cursor&limit=15",
                request.getPath());
        assertFalse("The local user scope must never be sent as authority.",
                request.getPath().contains("userId"));
        assertFalse("Authorization revisions are lifecycle guards only.",
                request.getPath().contains("authorizationRevision"));
        assertEquals("The safe public channel must map strictly.",
                Channel.WHATSAPP, page.items().get(0).channel());
        assertEquals("Only the safe lifecycle phases may be returned.",
                2, page.items().get(0).steps().size());
    }

    @Test
    public void unknownFieldsAndInternalErrorDetailsFailClosed() {
        server.enqueue(json("""
                {"items":[{"channel":"whatsapp",
                  "startedAt":"2026-07-31T12:00:00.000Z",
                  "endedAt":"2026-07-31T12:00:01.000Z",
                  "outcome":"completed","steps":[],"stepsTruncated":false,
                  "rawPrompt":"must-not-cross"}],"nextCursor":null}
                """));
        AuditException protocol = assertThrows(
                "Unexpected internal fields must fail strict decoding.",
                AuditException.class,
                () -> repository.turns(AgentReplayQuery.initial(), Optional.empty()));

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
                () -> repository.turns(AgentReplayQuery.initial(), Optional.empty()));

        assertEquals("Unexpected fields must fail as protocol errors.",
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
                () -> repository.turns(AgentReplayQuery.initial(), Optional.empty()));

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
