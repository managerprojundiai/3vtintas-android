package br.com.tresvtintas.mobile.data.customer.remote;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import br.com.tresvtintas.mobile.core.customer.CustomerDraft;
import br.com.tresvtintas.mobile.core.customer.CustomerException;
import br.com.tresvtintas.mobile.core.customer.CustomerFailureKind;
import br.com.tresvtintas.mobile.core.customer.CustomerMutationResult;
import br.com.tresvtintas.mobile.core.customer.CustomerPage;
import br.com.tresvtintas.mobile.core.customer.CustomerQuery;
import br.com.tresvtintas.mobile.core.network.MobileApiFactory;
import br.com.tresvtintas.mobile.core.network.NetworkConfiguration;
import br.com.tresvtintas.mobile.core.network.api.MobileApi;
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

public final class RetrofitCustomerRemoteTest {
    private static final String KEY =
            "00000000-0000-4000-8000-000000000071";
    private static final String CONTENT_TYPE = "Content-Type";
    private static final String APPLICATION_JSON = "application/json";
    private MockWebServer server;
    private RetrofitCustomerRemote remote;

    @Before
    public void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        NetworkConfiguration configuration = new NetworkConfiguration(
                "http://localhost:" + server.getPort() + "/api/mobile/v1/",
                "0.6.0-customers",
                7,
                true);
        MobileApi api = MobileApiFactory.create(
                configuration,
                () -> Optional.of("a".repeat(80)));
        remote = new RetrofitCustomerRemote(api);
    }

    @After
    public void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    public void mapsAuthorizedPageAndForwardsOnlyPublicFilters()
            throws Exception {
        server.enqueue(jsonResponse(validPage()));

        CustomerPage page = remote.page(
                CustomerQuery.initial().withSearch("cliente"),
                Optional.of("cursor_1"));
        RecordedRequest request = server.takeRequest();

        assertEquals("One customer must be mapped.", 1, page.items().size());
        assertEquals(
                "Cursor must remain opaque.",
                Optional.of("cursor_2"),
                page.nextCursor());
        assertEquals(
                "Only search, cursor and limit are client-controlled.",
                "/api/mobile/v1/customers"
                        + "?search=cliente&cursor=cursor_1&limit=30",
                request.getPath());
        assertEquals(
                "Protected customer call requires bearer.",
                "Bearer " + "a".repeat(80),
                request.getHeader("Authorization"));
        assertFalse(
                "The application must not send a role or wallet scope.",
                request.getPath().contains("role"));
    }

    @Test
    public void createsWithIdempotencyAndReadsReplayHeader()
            throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(201)
                .setHeader(CONTENT_TYPE, APPLICATION_JSON)
                .setHeader("X-Idempotency-Replayed", "false")
                .setBody("{\"customerId\":91,\"changed\":true}"));
        CustomerDraft draft = CustomerDraft.fromRaw(
                "Cliente",
                "cliente@example.test",
                "11999999999",
                "",
                "",
                "São Paulo",
                "SP",
                "");

        CustomerMutationResult result = remote.create(
                OptionalLong.of(20),
                draft,
                KEY);
        RecordedRequest request = server.takeRequest();

        assertEquals("Created customer ID must be retained.", 91, result.customerId());
        assertFalse("First execution is not a replay.", result.replayed());
        assertEquals(
                "Logical mutation key must be forwarded exactly.",
                KEY,
                request.getHeader("Idempotency-Key"));
        assertTrue(
                "Effective organization must be represented.",
                request.getBody().readUtf8().contains("\"organizationId\":20"));
    }

    @Test
    public void updatesClearedFieldsAndReportsReplay() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader(CONTENT_TYPE, APPLICATION_JSON)
                .setHeader("X-Idempotency-Replayed", "true")
                .setBody("{\"customerId\":91,\"changed\":true}"));
        CustomerDraft draft = CustomerDraft.fromRaw(
                "Cliente",
                "",
                "",
                "",
                "",
                "",
                "",
                "");

        CustomerMutationResult result = remote.update(91, draft, KEY);
        RecordedRequest request = server.takeRequest();
        String body = request.getBody().readUtf8();

        assertTrue("Replayed mutation must reach UI.", result.replayed());
        assertEquals(
                "Update uses resource path.",
                "/api/mobile/v1/customers/91",
                request.getPath());
        assertEquals("Update uses PATCH.", "PATCH", request.getMethod());
        assertTrue(
                "Null remains explicit so an existing phone can be cleared.",
                body.contains("\"phone\":null"));
    }

    @Test
    public void mapsNonEnumeratingNotFoundAndPreservesRequestId() {
        String requestId = UUID.randomUUID().toString();
        server.enqueue(new MockResponse()
                .setResponseCode(404)
                .setHeader(CONTENT_TYPE, "application/problem+json")
                .setBody(problemJson(requestId, "NOT_FOUND", 404)));

        CustomerException exception = assertThrows(
                "Hidden or absent customer must be typed identically.",
                CustomerException.class,
                () -> remote.detail(999));

        assertEquals(
                "Resource stays non-enumerating.",
                CustomerFailureKind.NOT_FOUND,
                exception.kind());
        assertEquals(
                "Correlation reaches support UI.",
                requestId,
                exception.requestId().orElseThrow());
    }

    @Test
    public void rejectsMutationWithoutReplayHeaderAsProtocolFailure() {
        server.enqueue(new MockResponse()
                .setResponseCode(201)
                .setHeader(CONTENT_TYPE, APPLICATION_JSON)
                .setBody("{\"customerId\":91,\"changed\":true}"));

        CustomerException exception = assertThrows(
                "Idempotent write response must prove replay status.",
                CustomerException.class,
                () -> remote.create(
                        OptionalLong.empty(),
                        CustomerDraft.fromRaw(
                                "Cliente",
                                "",
                                "",
                                "",
                                "",
                                "",
                                "",
                                ""),
                        KEY));

        assertEquals(
                "Missing contract header is a protocol failure.",
                CustomerFailureKind.PROTOCOL,
                exception.kind());
    }

    private static MockResponse jsonResponse(String body) {
        return new MockResponse()
                .setResponseCode(200)
                .setHeader(CONTENT_TYPE, APPLICATION_JSON)
                .setBody(body);
    }

    private static String validPage() {
        return """
                {
                  "items":[{
                    "id":91,
                    "organizationId":20,
                    "name":"Cliente",
                    "email":"cliente@example.test",
                    "phone":"11999999999",
                    "city":"São Paulo",
                    "state":"SP",
                    "assignedSalespersonUserId":30,
                    "createdAt":"2026-07-25T10:00:00.000Z",
                    "updatedAt":"2026-07-25T11:00:00.000Z"
                  }],
                  "nextCursor":"cursor_2"
                }
                """;
    }

    private static String problemJson(
            String requestId,
            String code,
            int status) {
        return ("{\"type\":\"https://www.3vtintas.com.br/problems/not-found\","
                + "\"title\":\"Not found\",\"status\":%d,"
                + "\"detail\":\"Resource not found.\","
                + "\"instance\":\"urn:3v:request:%s\","
                + "\"code\":\"%s\",\"requestId\":\"%s\"}")
                .formatted(status, requestId, code, requestId);
    }
}
