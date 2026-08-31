package br.com.tresvtintas.mobile.data.useradmin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import br.com.tresvtintas.mobile.core.model.AppRole;
import br.com.tresvtintas.mobile.core.network.MobileApiFactory;
import br.com.tresvtintas.mobile.core.network.NetworkConfiguration;
import br.com.tresvtintas.mobile.core.useradmin.UserAdministrationException;
import br.com.tresvtintas.mobile.core.useradmin.UserAdministrationFailureKind;
import br.com.tresvtintas.mobile.core.useradmin.UserAdministrationModels.Mutation;
import br.com.tresvtintas.mobile.core.useradmin.UserAdministrationModels.Options;
import br.com.tresvtintas.mobile.core.useradmin.UserAdministrationModels.Page;
import br.com.tresvtintas.mobile.core.useradmin.UserAdministrationQuery;
import br.com.tresvtintas.mobile.core.useradmin.UserAdministrationStatus;
import java.io.IOException;
import java.util.Optional;
import java.util.OptionalLong;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public final class RemoteUserAdministrationRepositoryTest {
    private static final String COLLECTION = "/api/mobile/v1/user-administration";
    private static final String IDEMPOTENCY_HEADER = "Idempotency-Key";
    private static final String IDEMPOTENCY_KEY =
            "00000000-0000-4000-8000-000000000081";
    private static final String REPLAY_HEADER = "X-Idempotency-Replayed";
    private MockWebServer server;
    private RemoteUserAdministrationRepository repository;

    @Before
    public void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        repository = new RemoteUserAdministrationRepository(
                new UserAdministrationAccountScope(31, "b".repeat(64)),
                MobileApiFactory.create(
                        new NetworkConfiguration(
                                "http://localhost:"
                                        + server.getPort()
                                        + "/api/mobile/v1/",
                                "0.45.0",
                                52,
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
        server.enqueue(json(userPageJson()));

        Options options = repository.options();
        Page page = repository.users(
                new UserAdministrationQuery(
                        OptionalLong.of(7),
                        Optional.of(AppRole.SALESPERSON),
                        Optional.of(UserAdministrationStatus.ACTIVE),
                        Optional.of("Maria"),
                        30),
                Optional.of("opaque_cursor"));
        RecordedRequest optionsRequest = server.takeRequest();
        RecordedRequest usersRequest = server.takeRequest();

        assertEquals(
                "The authorized organization must be mapped.",
                "Jundiaí",
                options.organizations().get(0).name());
        assertEquals(
                "The server role must map to the domain role.",
                AppRole.SALESPERSON,
                page.items().get(0).role());
        assertEquals(
                "Options must not receive client authority.",
                COLLECTION + "/options",
                optionsRequest.getPath());
        assertEquals(
                "Only canonical filters may reach the user collection.",
                COLLECTION
                        + "/users?organizationId=7&role=salesperson"
                        + "&status=active&search=Maria"
                        + "&cursor=opaque_cursor&limit=30",
                usersRequest.getPath());
        assertFalse(
                "The signed-in user ID must never be sent as authority.",
                usersRequest.getPath().contains("userId"));
        assertFalse(
                "Authorization revisions belong only to local scoping.",
                usersRequest.getPath().contains("authorizationRevision"));
    }

    @Test
    public void standardRoleCarriesRevisionConfirmationAndIdempotency()
            throws Exception {
        server.enqueue(mutation(false));

        Mutation mutation = repository.assignStandardRole(
                44,
                6,
                AppRole.MANAGER,
                IDEMPOTENCY_KEY);
        RecordedRequest request = server.takeRequest();
        String body = request.getBody().readUtf8();

        assertEquals(
                "The mutation must address only the target resource.",
                COLLECTION + "/users/44/actions",
                request.getPath());
        assertEquals(
                "Every mutation must carry a stable idempotency key.",
                IDEMPOTENCY_KEY,
                request.getHeader(IDEMPOTENCY_HEADER));
        assertTrue(
                "The optimistic revision must be explicit.",
                body.contains("\"expectedRevision\":6"));
        assertTrue(
                "The action discriminator must be explicit.",
                body.contains("\"action\":\"standard_role\""));
        assertTrue(
                "Sensitive role changes must require confirmation.",
                body.contains("\"confirmed\":true"));
        assertEquals(
                "The canonical resource ID must be mapped.",
                44,
                mutation.resourceId());
    }

    @Test
    public void operationalBindingAndBlockingUseAuditableActions()
            throws Exception {
        server.enqueue(mutation(false));
        server.enqueue(mutation(true));

        repository.assignOperationalRole(
                44,
                6,
                AppRole.DELIVERY_DRIVER,
                7,
                IDEMPOTENCY_KEY);
        Mutation replayed = repository.setBlocked(
                44,
                7,
                true,
                IDEMPOTENCY_KEY);
        RecordedRequest binding = server.takeRequest();
        RecordedRequest blocking = server.takeRequest();
        String bindingBody = binding.getBody().readUtf8();
        String blockingBody = blocking.getBody().readUtf8();

        assertTrue(
                "Operational roles must carry their organization binding.",
                bindingBody.contains("\"organizationId\":7"));
        assertTrue(
                "Operational role type must be explicit.",
                bindingBody.contains("\"role\":\"delivery_driver\""));
        assertEquals(
                "Account state changes share the auditable action route.",
                COLLECTION + "/users/44/actions",
                blocking.getPath());
        assertTrue(
                "Blocking intent must be explicit.",
                blockingBody.contains("\"isBlocked\":true"));
        assertTrue(
                "Replay evidence must reach the domain.",
                replayed.replayed());
    }

    @Test
    public void missingReplayEvidenceAndConflictsFailClosed() {
        server.enqueue(json("""
                {"resourceId":44,"revision":7,"changed":true}
                """));
        UserAdministrationException protocol = assertThrows(
                UserAdministrationException.class,
                () -> repository.setBlocked(
                        44,
                        6,
                        true,
                        IDEMPOTENCY_KEY));

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
        UserAdministrationException conflict = assertThrows(
                UserAdministrationException.class,
                () -> repository.assignStandardRole(
                        44,
                        6,
                        AppRole.MANAGER,
                        IDEMPOTENCY_KEY));

        assertEquals(
                "Missing replay evidence must fail as a protocol error.",
                UserAdministrationFailureKind.PROTOCOL,
                protocol.kind());
        assertEquals(
                "Optimistic conflicts must remain distinguishable.",
                UserAdministrationFailureKind.CONFLICT,
                conflict.kind());
        assertEquals(
                "Only the safe support ID may cross the transport layer.",
                Optional.of("00000000-0000-4000-8000-000000000099"),
                conflict.requestId());
        assertFalse(
                "Server internals must not become a UI message.",
                conflict.getMessage().contains("Internal revision"));
    }

    @Test
    public void revokedScopeCannotIssueAnotherRequest() {
        repository.close();

        UserAdministrationException failure = assertThrows(
                UserAdministrationException.class,
                repository::options);

        assertEquals(
                "Closed account scopes must fail as revoked.",
                UserAdministrationFailureKind.ACCESS_REVOKED,
                failure.kind());
        assertEquals(
                "Revoked scopes must not reach the network.",
                0,
                server.getRequestCount());
    }

    private static MockResponse json(String body) {
        return new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(body);
    }

    private static MockResponse mutation(boolean replayed) {
        return json("""
                {"resourceId":44,"revision":7,"changed":true}
                """).setHeader(REPLAY_HEADER, Boolean.toString(replayed));
    }

    private static String optionsJson() {
        return """
                {"organizations":[{"id":7,"name":"Jundiaí",
                "slug":"jundiai"}],
                "standardRoles":["manager","painter","customer","user"],
                "operationalRoles":["salesperson","delivery_driver"]}
                """;
    }

    private static String userPageJson() {
        return """
                {"items":[{"id":44,"name":"Maria",
                "email":"maria@example.com","role":"salesperson",
                "isBlocked":false,"revision":6,
                "assignments":[{"organizationId":7,
                "organizationName":"Jundiaí","role":"salesperson",
                "approvedAt":"2026-07-30T11:30:00Z"}],
                "createdAt":"2026-07-30T11:00:00Z",
                "updatedAt":"2026-07-30T12:00:00Z"}],
                "nextCursor":"opaque_next"}
                """;
    }
}
