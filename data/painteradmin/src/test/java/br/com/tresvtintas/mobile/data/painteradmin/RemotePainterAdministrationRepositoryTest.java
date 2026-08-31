package br.com.tresvtintas.mobile.data.painteradmin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import br.com.tresvtintas.mobile.core.network.MobileApiFactory;
import br.com.tresvtintas.mobile.core.network.NetworkConfiguration;
import br.com.tresvtintas.mobile.core.painteradmin.PainterAdministrationException;
import br.com.tresvtintas.mobile.core.painteradmin.PainterAdministrationFailureKind;
import br.com.tresvtintas.mobile.core.painteradmin.PainterAdministrationModels.Mutation;
import br.com.tresvtintas.mobile.core.painteradmin.PainterAdministrationModels.Options;
import br.com.tresvtintas.mobile.core.painteradmin.PainterAdministrationModels.Page;
import br.com.tresvtintas.mobile.core.painteradmin.PainterAdministrationModels.Painter;
import br.com.tresvtintas.mobile.core.painteradmin.PainterAdministrationQuery;
import br.com.tresvtintas.mobile.core.painteradmin.PainterAdministrationStatus;
import br.com.tresvtintas.mobile.core.painteradmin.PainterDraft;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.OptionalLong;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public final class RemotePainterAdministrationRepositoryTest {
    private static final String COLLECTION =
            "/api/mobile/v1/painter-administration";
    private static final String IDEMPOTENCY_HEADER = "Idempotency-Key";
    private static final String IDEMPOTENCY_KEY =
            "00000000-0000-4000-8000-000000000071";
    private static final String REPLAY_HEADER = "X-Idempotency-Replayed";
    private MockWebServer server;
    private RemotePainterAdministrationRepository repository;

    @Before
    public void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        repository = new RemotePainterAdministrationRepository(
                new PainterAdministrationAccountScope(
                        31,
                        "b".repeat(64)),
                MobileApiFactory.create(
                        new NetworkConfiguration(
                                "http://localhost:"
                                        + server.getPort()
                                        + "/api/mobile/v1/",
                                "0.40.0",
                                51,
                                true),
                        () -> Optional.of("a".repeat(80))));
    }

    @After
    public void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    public void readsStrictOptionsAndFiltersWithoutSendingAuthority()
            throws Exception {
        server.enqueue(json(optionsJson()));
        server.enqueue(json(painterPageJson()));

        Options options = repository.options();
        Page<Painter> page = repository.painters(
                new PainterAdministrationQuery(
                        OptionalLong.of(7),
                        Optional.of(PainterAdministrationStatus.ACTIVE),
                        Optional.of("Maria"),
                        30),
                Optional.of("opaque_cursor"));
        RecordedRequest optionsRequest = server.takeRequest();
        RecordedRequest painterRequest = server.takeRequest();

        assertEquals(
                "The authorized options must be mapped.",
                "Jundiaí",
                options.organizations().get(0).name());
        assertEquals(
                "The painter organization must be mapped.",
                7,
                page.items().get(0).organization().id());
        assertEquals(
                "The options endpoint must not receive client authority.",
                COLLECTION + "/options",
                optionsRequest.getPath());
        assertEquals(
                "Only canonical filters may reach the collection.",
                COLLECTION
                        + "/painters?organizationId=7&status=active"
                        + "&search=Maria&cursor=opaque_cursor&limit=30",
                painterRequest.getPath());
        assertFalse(
                "The client must never submit an authoritative role.",
                painterRequest.getPath().contains("role"));
        assertFalse(
                "The client must never submit an authoritative user ID.",
                painterRequest.getPath().contains("userId"));
    }

    @Test
    public void mutationCarriesRevisionConfirmationAndIdempotency()
            throws Exception {
        server.enqueue(mutation(false));

        Mutation mutation = repository.updateCommission(
                12,
                4,
                new BigDecimal("3.50"),
                IDEMPOTENCY_KEY);
        RecordedRequest request = server.takeRequest();
        String body = request.getBody().readUtf8();

        assertEquals(
                "The mutation must address only the resource ID.",
                COLLECTION + "/painters/12/actions",
                request.getPath());
        assertEquals(
                "Every mutation must carry the stable idempotency key.",
                IDEMPOTENCY_KEY,
                request.getHeader(IDEMPOTENCY_HEADER));
        assertTrue(
                "The optimistic revision must be explicit.",
                body.contains("\"expectedRevision\":4"));
        assertTrue(
                "The action discriminator must be explicit.",
                body.contains("\"action\":\"commission\""));
        assertTrue(
                "The sensitive mutation must require confirmation.",
                body.contains("\"confirmed\":true"));
        assertEquals(
                "The canonical resource ID must be mapped.",
                12,
                mutation.resourceId());
    }

    @Test
    public void approvalAndRejectionUseOneAuditableDecisionRoute()
            throws Exception {
        server.enqueue(mutation(false));
        server.enqueue(mutation(true));

        repository.approvePainter(
                44,
                2,
                7,
                OptionalLong.of(19),
                new BigDecimal("4.25"),
                IDEMPOTENCY_KEY);
        repository.reject(
                45,
                3,
                "Cadastro duplicado",
                IDEMPOTENCY_KEY);
        RecordedRequest approval = server.takeRequest();
        RecordedRequest rejection = server.takeRequest();
        String approvalBody = approval.getBody().readUtf8();
        String rejectionBody = rejection.getBody().readUtf8();

        assertEquals(
                "Painter approval must use the canonical decision endpoint.",
                COLLECTION + "/access-requests/44/decision",
                approval.getPath());
        assertTrue(
                "The store link must be carried as data, never authority.",
                approvalBody.contains("\"organizationId\":7"));
        assertTrue(
                "The manager link must be explicit.",
                approvalBody.contains("\"managerUserId\":19"));
        assertEquals(
                "Rejection must use the same auditable decision endpoint.",
                COLLECTION + "/access-requests/45/decision",
                rejection.getPath());
        assertTrue(
                "The rejection reason must be preserved.",
                rejectionBody.contains("\"reason\":\"Cadastro duplicado\""));
    }

    @Test
    public void replayHeaderIsMandatoryAndMapped() throws Exception {
        server.enqueue(mutation(true));
        Mutation replayed = repository.create(draft(), IDEMPOTENCY_KEY);

        server.enqueue(json("""
                {"resourceId":12,"revision":5,"changed":false}
                """));
        PainterAdministrationException failure = assertThrows(
                PainterAdministrationException.class,
                () -> repository.updateStatus(
                        12,
                        4,
                        PainterAdministrationStatus.BLOCKED,
                        IDEMPOTENCY_KEY));

        assertTrue(
                "A replay must be exposed to avoid false duplicate feedback.",
                replayed.replayed());
        assertEquals(
                "Missing replay evidence must fail closed.",
                PainterAdministrationFailureKind.PROTOCOL,
                failure.kind());
    }

    @Test
    public void mapsConflictWithoutLeakingProblemDetail() {
        server.enqueue(new MockResponse()
                .setResponseCode(409)
                .setHeader("Content-Type", "application/problem+json")
                .setBody("""
                        {"type":"https://3vtintas.com.br/problems/conflict",
                        "title":"Conflito","status":409,
                        "detail":"Internal revision details.",
                        "instance":"urn:3v:request:test",
                        "code":"RESOURCE_CONFLICT",
                        "requestId":"00000000-0000-4000-8000-000000000099"}
                        """));

        PainterAdministrationException failure = assertThrows(
                PainterAdministrationException.class,
                () -> repository.updateManager(
                        12,
                        4,
                        OptionalLong.empty(),
                        IDEMPOTENCY_KEY));

        assertEquals(
                "Optimistic conflicts must remain distinguishable.",
                PainterAdministrationFailureKind.CONFLICT,
                failure.kind());
        assertEquals(
                "Only the safe support ID may leave the transport layer.",
                Optional.of(
                        "00000000-0000-4000-8000-000000000099"),
                failure.requestId());
        assertFalse(
                "Server internals must not become a user-facing exception.",
                failure.getMessage().contains("Internal revision"));
    }

    @Test
    public void revokedScopeCannotIssueAnotherRequest() {
        repository.close();

        PainterAdministrationException failure = assertThrows(
                PainterAdministrationException.class,
                repository::options);

        assertEquals(
                "Closed account scopes must fail as revoked.",
                PainterAdministrationFailureKind.ACCESS_REVOKED,
                failure.kind());
        assertEquals(
                "Revoked scopes must not reach the network.",
                0,
                server.getRequestCount());
    }

    private static PainterDraft draft() {
        return new PainterDraft(
                7,
                "Maria Silva",
                "maria@example.com",
                Optional.empty(),
                Optional.empty(),
                Optional.of("11999999999"),
                Optional.of("Pinturas Maria"),
                Optional.of("Residencial"),
                Optional.empty(),
                Optional.empty(),
                Optional.of("Jundiaí"),
                Optional.of("SP"),
                new BigDecimal("4.25"),
                OptionalLong.of(19),
                Optional.empty());
    }

    private static MockResponse json(String body) {
        return new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(body);
    }

    private static MockResponse mutation(boolean replayed) {
        return json("""
                {"resourceId":12,"revision":5,"changed":true}
                """).setHeader(REPLAY_HEADER, Boolean.toString(replayed));
    }

    private static String optionsJson() {
        return """
                {"organizations":[{"id":7,"name":"Jundiaí"}],
                "managers":[{"id":19,"name":"Gerente",
                "organizationId":7}]}
                """;
    }

    private static String painterPageJson() {
        return """
                {"items":[{"id":12,"userId":31,"name":"Maria",
                "email":"maria@example.com","company":"Pinturas Maria",
                "specialty":"Residencial","status":"active",
                "commissionRate":"4.25",
                "organization":{"id":7,"name":"Jundiaí"},
                "manager":{"id":19,"name":"Gerente",
                "organizationId":7},"revision":4,
                "updatedAt":"2026-07-30T12:00:00Z"}],
                "nextCursor":"opaque_next"}
                """;
    }
}
