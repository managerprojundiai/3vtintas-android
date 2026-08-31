package br.com.tresvtintas.mobile.data.laborquote;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import br.com.tresvtintas.mobile.core.laborquote.LaborQuoteDraft;
import br.com.tresvtintas.mobile.core.laborquote.LaborQuoteDraftLine;
import br.com.tresvtintas.mobile.core.laborquote.LaborQuoteException;
import br.com.tresvtintas.mobile.core.laborquote.LaborQuoteFailureKind;
import br.com.tresvtintas.mobile.core.laborquote.LaborQuotePage;
import br.com.tresvtintas.mobile.core.laborquote.LaborQuotePdfDownload;
import br.com.tresvtintas.mobile.core.laborquote.LaborQuoteQuery;
import br.com.tresvtintas.mobile.core.laborquote.LaborQuoteStatus;
import br.com.tresvtintas.mobile.core.laborquote.LaborQuoteStatusMutationResult;
import br.com.tresvtintas.mobile.core.laborquote.LaborQuoteView;
import br.com.tresvtintas.mobile.core.network.MobileApiFactory;
import br.com.tresvtintas.mobile.core.network.NetworkConfiguration;
import br.com.tresvtintas.mobile.core.network.api.MobileApi;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public final class RemoteLaborQuoteRepositoryTest {
    private static final String CONTENT_TYPE = "Content-Type";
    private static final String APPLICATION_JSON = "application/json";
    private static final String KEY = "00000000-0000-4000-8000-000000000091";
    private MockWebServer server;
    private RemoteLaborQuoteRepository repository;

    @Before
    public void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        NetworkConfiguration configuration = new NetworkConfiguration(
                "http://localhost:" + server.getPort() + "/api/mobile/v1/",
                "0.10.0-labor-quotes",
                11,
                true);
        MobileApi api = MobileApiFactory.create(
                configuration,
                () -> Optional.of("a".repeat(80)));
        repository = new RemoteLaborQuoteRepository(
                new LaborQuoteAccountScope(10, "b".repeat(64)),
                api);
    }

    @After
    public void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    public void readsAuthorizedPageWithoutClientScopeClaims() throws Exception {
        server.enqueue(json(validPage()));

        LaborQuotePage page = repository.page(
                new LaborQuoteQuery(Optional.of("Cliente"), Optional.empty(), 30),
                Optional.of("cursor_1"));
        RecordedRequest request = server.takeRequest();

        assertEquals("One labor quote must be mapped.", 1, page.items().size());
        assertEquals("Painter identity must come from the server.",
                "Pinturas 3V", page.items().get(0).painter().name());
        assertEquals("Cursor must remain opaque.",
                Optional.of("cursor_2"), page.nextCursor());
        assertEquals(
                "Only public filters belong in the request.",
                "/api/mobile/v1/labor-quotes?search=cliente&cursor=cursor_1&limit=30",
                request.getPath());
        assertFalse("Manager scope cannot be supplied by Android.",
                request.getPath().contains("manager"));
        assertFalse("Painter scope cannot be supplied by Android.",
                request.getPath().contains("painterId"));
    }

    @Test
    public void createsWithManualServiceValuesButNoClientTotals() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(201)
                .setHeader(CONTENT_TYPE, APPLICATION_JSON)
                .setHeader("X-Idempotency-Replayed", "false")
                .setBody(mutation()));

        repository.create(draft(), KEY);
        RecordedRequest request = server.takeRequest();
        String body = request.getBody().readUtf8();

        assertEquals("Idempotency key must be forwarded.",
                KEY, request.getHeader("Idempotency-Key"));
        assertTrue("Service description must be sent.",
                body.contains("\"description\":\"Pintura interna\""));
        assertTrue("Painter-entered unit price must be sent.",
                body.contains("\"unitPrice\":\"25.00\""));
        assertTrue("Explicit discount must be sent.",
                body.contains("\"discount\":\"50.00\""));
        assertFalse("Client total must never be authoritative.",
                body.contains("\"total\""));
        assertFalse("Painter identity is server-derived.",
                body.contains("painterId"));
        assertFalse("Manager scope is server-derived.",
                body.contains("managerUserId"));
    }

    @Test
    public void sendsLifecycleViewAndDuplicatesWithoutAClientBody()
            throws Exception {
        server.enqueue(json(validPage()));
        repository.page(
                new LaborQuoteQuery(
                        Optional.empty(),
                        Optional.empty(),
                        LaborQuoteView.ACTIVE,
                        20),
                Optional.empty());
        assertEquals(
                "Lifecycle view must be explicit.",
                "/api/mobile/v1/labor-quotes?view=active&limit=20",
                server.takeRequest().getPath());

        server.enqueue(new MockResponse()
                .setResponseCode(201)
                .setHeader(CONTENT_TYPE, APPLICATION_JSON)
                .setHeader("X-Idempotency-Replayed", "false")
                .setBody(mutation()));
        repository.duplicate(701, KEY);
        RecordedRequest duplicate = server.takeRequest();

        assertEquals("Duplicate must use POST.", "POST", duplicate.getMethod());
        assertEquals(
                "Duplicate route must remain versioned.",
                "/api/mobile/v1/labor-quotes/701/duplicate",
                duplicate.getPath());
        assertEquals("Idempotency key must be forwarded.",
                KEY, duplicate.getHeader("Idempotency-Key"));
        assertEquals("No client draft or total may be supplied.",
                "", duplicate.getBody().readUtf8());
    }

    @Test
    public void transitionsWithRevisionAndIdempotency() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader(CONTENT_TYPE, APPLICATION_JSON)
                .setHeader("X-Idempotency-Replayed", "true")
                .setBody("""
                        {
                          "quoteId":701,
                          "previousStatus":"draft",
                          "status":"sent",
                          "revision":2,
                          "total":"200.00",
                          "changed":true
                        }
                        """));

        LaborQuoteStatusMutationResult result = repository.transition(
                701,
                1,
                LaborQuoteStatus.SENT,
                KEY);
        RecordedRequest request = server.takeRequest();

        assertEquals("Status route must remain versioned.",
                "/api/mobile/v1/labor-quotes/701/status", request.getPath());
        assertEquals("Status transition must use POST.", "POST", request.getMethod());
        assertTrue("Expected revision must be sent.",
                request.getBody().readUtf8().contains("\"expectedRevision\":1"));
        assertEquals("Server status must be mapped.",
                LaborQuoteStatus.SENT, result.status());
        assertTrue("Replay metadata must reach the caller.", result.replayed());
    }

    @Test
    public void streamsOnlyStrictNonCacheableLaborPdf() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader(CONTENT_TYPE, "application/pdf")
                .setHeader("Cache-Control", "private, no-store, max-age=0")
                .setHeader("X-Content-Type-Options", "nosniff")
                .setHeader(
                        "Content-Disposition",
                        "attachment; filename=\"orcamento-000701-labor.pdf\"")
                .setBody("%PDF-1.7\nlabor"));
        ByteArrayOutputStream destination = new ByteArrayOutputStream();

        LaborQuotePdfDownload download = repository.downloadPdf(701, destination);

        assertEquals("Filename must match the labor contract.",
                "orcamento-000701-labor.pdf", download.filename());
        assertEquals("All PDF bytes must be streamed.",
                "%PDF-1.7\nlabor", destination.toString(
                java.nio.charset.StandardCharsets.UTF_8));
        assertEquals("PDF route must remain versioned.",
                "/api/mobile/v1/labor-quotes/701/pdf", server.takeRequest().getPath());
    }

    @Test
    public void mapsHiddenQuoteAndRevokesClosedScope() {
        String requestId = UUID.randomUUID().toString();
        server.enqueue(new MockResponse()
                .setResponseCode(404)
                .setHeader(CONTENT_TYPE, "application/problem+json")
                .setBody(problem(requestId)));

        LaborQuoteException hidden = assertThrows(
                LaborQuoteException.class,
                () -> repository.detail(999));
        assertEquals("Hidden resources must remain non-enumerating.",
                LaborQuoteFailureKind.NOT_FOUND, hidden.kind());
        assertEquals("Request correlation must reach support.",
                requestId, hidden.requestId().orElseThrow());

        repository.close();
        LaborQuoteException revoked = assertThrows(
                LaborQuoteException.class,
                () -> repository.page(LaborQuoteQuery.initial(), Optional.empty()));
        assertEquals("Closed account scopes must fail locally.",
                LaborQuoteFailureKind.ACCESS_REVOKED, revoked.kind());
    }

    private static LaborQuoteDraft draft() {
        return new LaborQuoteDraft(
                9823,
                "Cliente",
                Optional.of("Pintura"),
                Optional.empty(),
                Optional.empty(),
                new BigDecimal("50"),
                List.of(new LaborQuoteDraftLine(
                        "Pintura interna",
                        BigDecimal.TEN,
                        "m²",
                        new BigDecimal("25"))));
    }

    private static MockResponse json(String body) {
        return new MockResponse()
                .setResponseCode(200)
                .setHeader(CONTENT_TYPE, APPLICATION_JSON)
                .setBody(body);
    }

    private static String mutation() {
        return """
                {"quoteId":701,"revision":1,"total":"200.00","changed":true}
                """;
    }

    private static String validPage() {
        return """
                {
                  "items":[{
                    "id":701,
                    "customer":{"id":9823,"name":"Cliente"},
                    "painter":{"id":9840,"name":"Pinturas 3V"},
                    "organizationId":null,
                    "title":"Pintura",
                    "status":"draft",
                    "subtotal":"250.00",
                    "discount":"50.00",
                    "total":"200.00",
                    "revision":1,
                    "itemCount":1,
                    "validUntil":null,
                    "createdAt":"2026-07-25T10:00:00.000Z",
                    "updatedAt":"2026-07-25T10:00:00.000Z"
                  }],
                  "nextCursor":"cursor_2"
                }
                """;
    }

    private static String problem(String requestId) {
        return ("{\"type\":\"https://www.3vtintas.com.br/problems/not-found\","
                + "\"title\":\"Not found\",\"status\":404,"
                + "\"detail\":\"Resource not found.\","
                + "\"instance\":\"urn:3v:request:%s\","
                + "\"code\":\"NOT_FOUND\",\"requestId\":\"%s\"}")
                .formatted(requestId, requestId);
    }
}
