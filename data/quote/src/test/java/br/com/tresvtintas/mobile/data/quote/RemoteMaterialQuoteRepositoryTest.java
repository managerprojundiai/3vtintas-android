package br.com.tresvtintas.mobile.data.quote;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import br.com.tresvtintas.mobile.core.network.MobileApiFactory;
import br.com.tresvtintas.mobile.core.network.NetworkConfiguration;
import br.com.tresvtintas.mobile.core.network.api.MobileApi;
import br.com.tresvtintas.mobile.core.quote.MaterialQuoteDraft;
import br.com.tresvtintas.mobile.core.quote.MaterialQuoteDraftLine;
import br.com.tresvtintas.mobile.core.quote.MaterialQuoteDetail;
import br.com.tresvtintas.mobile.core.quote.MaterialQuoteException;
import br.com.tresvtintas.mobile.core.quote.MaterialQuoteFailureKind;
import br.com.tresvtintas.mobile.core.quote.MaterialQuoteMutationResult;
import br.com.tresvtintas.mobile.core.quote.MaterialQuotePage;
import br.com.tresvtintas.mobile.core.quote.MaterialQuotePdfDownload;
import br.com.tresvtintas.mobile.core.quote.MaterialQuotePricingSelection;
import br.com.tresvtintas.mobile.core.quote.MaterialQuotePricingContext;
import br.com.tresvtintas.mobile.core.quote.MaterialQuotePreview;
import br.com.tresvtintas.mobile.core.quote.MaterialQuoteTintColorPage;
import br.com.tresvtintas.mobile.core.quote.MaterialQuoteTintConfiguration;
import br.com.tresvtintas.mobile.core.quote.MaterialQuoteTintSelection;
import br.com.tresvtintas.mobile.core.quote.MaterialQuoteQuery;
import br.com.tresvtintas.mobile.core.quote.MaterialQuoteStatus;
import br.com.tresvtintas.mobile.core.quote.MaterialQuoteStatusMutationResult;
import br.com.tresvtintas.mobile.core.quote.MaterialQuoteView;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
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

public final class RemoteMaterialQuoteRepositoryTest {
    private static final String CONTENT_TYPE = "Content-Type";
    private static final String APPLICATION_JSON = "application/json";
    private static final String IDEMPOTENCY_REPLAYED =
            "X-Idempotency-Replayed";
    private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";
    private static final String FALSE_VALUE = "false";
    private static final String KEY =
            "00000000-0000-4000-8000-000000000081";
    private static final String PREVIEW_FINGERPRINT = "b".repeat(64);
    private MockWebServer server;
    private RemoteMaterialQuoteRepository repository;

    @Before
    public void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        NetworkConfiguration configuration = new NetworkConfiguration(
                "http://localhost:" + server.getPort() + "/api/mobile/v1/",
                "0.9.0-material-quote-pdf",
                10,
                true);
        MobileApi api = MobileApiFactory.create(
                configuration,
                () -> Optional.of("a".repeat(80)));
        repository = new RemoteMaterialQuoteRepository(
                new MaterialQuoteAccountScope(10, "b".repeat(64)),
                api);
    }

    @After
    public void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    public void readsAuthorizedKeysetPageWithoutClientScopeClaims()
            throws Exception {
        server.enqueue(jsonResponse(validPage()));

        MaterialQuotePage page = repository.page(
                new MaterialQuoteQuery(
                        Optional.of("Cliente"),
                        Optional.empty(),
                        30),
                Optional.of("cursor_1"));
        RecordedRequest request = server.takeRequest();

        assertEquals("One quote must be mapped.", 1, page.items().size());
        assertEquals(
                "Cursor must remain opaque.",
                Optional.of("cursor_2"),
                page.nextCursor());
        assertEquals(
                "Only public filters belong in the request.",
                "/api/mobile/v1/material-quotes"
                        + "?search=cliente&cursor=cursor_1&limit=30",
                request.getPath());
        assertFalse(
                "Role cannot be supplied by the Android client.",
                request.getPath().contains("role"));
        assertFalse(
                "Organization scope cannot be supplied by the Android client.",
                request.getPath().contains("organization"));
    }

    @Test
    public void createsWithProductAndQuantityButNeverClientPrice()
            throws Exception {
        server.enqueue(jsonResponse(previewJson()));
        MaterialQuotePreview preview = repository.previewCreate(draft());
        RecordedRequest previewRequest = server.takeRequest();

        assertNull("Preview must never require an idempotency key.",
                previewRequest.getHeader(IDEMPOTENCY_KEY_HEADER));
        assertEquals("Preview must use its dedicated route.",
                "/api/mobile/v1/material-quotes/preview",
                previewRequest.getPath());
        assertEquals("Official server total must reach confirmation.",
                "39.80", preview.total().toPlainString());
        assertEquals("Published table must reach confirmation.",
                "Tabela 2",
                preview.pricing().priceListName().orElseThrow());

        server.enqueue(new MockResponse()
                .setResponseCode(201)
                .setHeader(CONTENT_TYPE, APPLICATION_JSON)
                .setHeader(IDEMPOTENCY_REPLAYED, FALSE_VALUE)
                .setBody(mutationJson()));

        MaterialQuoteMutationResult result = repository.create(
                draft(),
                preview.fingerprint(),
                KEY);
        RecordedRequest request = server.takeRequest();
        String body = request.getBody().readUtf8();

        assertEquals("Server total is authoritative.", "39.80",
                result.total().toPlainString());
        assertFalse("First execution cannot be a replay.", result.replayed());
        assertEquals("Idempotency key must be forwarded.", KEY,
                request.getHeader(IDEMPOTENCY_KEY_HEADER));
        assertTrue("Product identity must be sent.",
                body.contains("\"productId\":9831"));
        assertTrue("Quantity must be sent.",
                body.contains("\"quantity\":\"2.00\""));
        assertFalse("Displayed price must never be trusted as input.",
                body.contains("unitPrice"));
        assertFalse("Client total must never be trusted as input.",
                body.contains("\"total\""));
        assertFalse("Absent pricing must be omitted for the strict API contract.",
                body.contains("\"pricing\""));
        assertFalse("Ready products must omit absent tint identity fields.",
                body.contains("\"colorId\"")
                        || body.contains("\"tintContextId\""));
        assertTrue("Confirmation must be bound to the official preview.",
                body.contains("\"expectedPreviewFingerprint\":\""
                        + PREVIEW_FINGERPRINT + "\""));
    }

    @Test
    public void omitsAutomaticPrimaryVersionInsteadOfSendingNull()
            throws Exception {
        MaterialQuoteDraft base = draft();
        MaterialQuoteDraft automaticPrimary = new MaterialQuoteDraft(
                base.customerId(),
                base.customerName(),
                base.title(),
                base.notes(),
                base.validUntil(),
                Optional.of(new MaterialQuotePricingSelection(
                        1,
                        Optional.empty())),
                base.items());
        server.enqueue(jsonResponse(previewJson()));

        repository.previewCreate(automaticPrimary);
        String body = server.takeRequest().getBody().readUtf8();

        assertTrue("Automatic pricing must preserve the policy revision.",
                body.contains("\"expectedPolicyRevision\":1"));
        assertFalse("The automatic primary version is optional, never null.",
                body.contains("selectedPriceListVersionPublicId"));
        assertFalse("Optional quote fields must be omitted instead of null.",
                body.contains("\"notes\"")
                        || body.contains("\"validUntil\""));
    }

    @Test
    public void updatesOnlyAfterAnOfficialRevisionBoundPreview()
            throws Exception {
        server.enqueue(jsonResponse(updatePreviewJson()));

        MaterialQuotePreview preview = repository.previewUpdate(501, 3, draft());
        RecordedRequest previewRequest = server.takeRequest();
        String previewBody = previewRequest.getBody().readUtf8();

        assertEquals("Update preview must use the resource route.",
                "/api/mobile/v1/material-quotes/501/preview",
                previewRequest.getPath());
        assertNull("Update preview must not consume an idempotency key.",
                previewRequest.getHeader(IDEMPOTENCY_KEY_HEADER));
        assertTrue("Preview must be bound to the displayed revision.",
                previewBody.contains("\"expectedRevision\":3"));
        assertEquals("Server discount must reach the visual confirmation.",
                "5.00", preview.discount().toPlainString());
        assertEquals("Server total after discount must remain authoritative.",
                "34.80", preview.total().toPlainString());

        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader(CONTENT_TYPE, APPLICATION_JSON)
                .setHeader(IDEMPOTENCY_REPLAYED, FALSE_VALUE)
                .setBody(mutationJson()));

        repository.update(501, 3, draft(), preview.fingerprint(), KEY);
        RecordedRequest updateRequest = server.takeRequest();
        String updateBody = updateRequest.getBody().readUtf8();

        assertEquals("Update must address the same opaque resource.",
                "/api/mobile/v1/material-quotes/501",
                updateRequest.getPath());
        assertEquals("Confirmation needs an idempotency key.", KEY,
                updateRequest.getHeader(IDEMPOTENCY_KEY_HEADER));
        assertTrue("Confirmation must preserve the displayed revision.",
                updateBody.contains("\"expectedRevision\":3"));
        assertTrue("Confirmation must preserve the preview fingerprint.",
                updateBody.contains("\"expectedPreviewFingerprint\":\""
                        + PREVIEW_FINGERPRINT + "\""));
        assertFalse("Confirmation cannot submit a client-computed discount.",
                updateBody.contains("discount"));
    }

    @Test
    public void loadsPublishedPricingChoicesWithoutAmounts() throws Exception {
        String list = UUID.randomUUID().toString();
        String version = UUID.randomUUID().toString();
        server.enqueue(jsonResponse("""
                {
                  "organizationId":9,
                  "enabled":true,
                  "engineMode":"ACTIVE",
                  "policyRevision":12,
                  "selectionRequired":true,
                  "selections":[
                    {
                      "priceListPublicId":"%s",
                      "priceListCode":"AUTCOM_2",
                      "priceListName":"Tabela 2",
                      "priceListVersionPublicId":"%s",
                      "priceListVersionNumber":3,
                      "isPrimary":true
                    },
                    {
                      "priceListPublicId":"%s",
                      "priceListCode":"AUTCOM_3",
                      "priceListName":"Tabela 3",
                      "priceListVersionPublicId":"%s",
                      "priceListVersionNumber":1,
                      "isPrimary":false
                    }
                  ]
                }
                """
                .replaceFirst("%s", list)
                .replaceFirst("%s", version)
                .replaceFirst("%s", UUID.randomUUID().toString())
                .replaceFirst("%s", UUID.randomUUID().toString())));

        MaterialQuotePricingContext context = repository.pricingContext(9);
        RecordedRequest request = server.takeRequest();

        assertEquals("Pricing context must be scoped by organization.",
                "/api/mobile/v1/pricing/context?organizationId=9",
                request.getPath());
        assertTrue("Salesperson must select when multiple tables are active.",
                context.selectionRequired());
        assertEquals("Policy revision must be preserved.",
                12, context.policyRevision());
        assertEquals("Only published selection metadata reaches the client.",
                2, context.options().size());
        assertEquals("The opaque selected version must create the draft claim.",
                version,
                context.selection(version)
                        .selectedPriceListVersionPublicId().orElseThrow());
    }

    @Test
    public void mapsFrozenTableVersionAndFullTintIdentity() throws Exception {
        server.enqueue(jsonResponse(resolvedDetail()));

        MaterialQuoteDetail detail = repository.detail(501);
        RecordedRequest request = server.takeRequest();

        assertEquals("Detail route must remain versioned.",
                "/api/mobile/v1/material-quotes/501", request.getPath());
        assertTrue("Resolved table metadata must reach the domain.",
                detail.pricing().resolved());
        assertEquals("Table name must be the frozen snapshot.",
                "Tabela 2", detail.pricing().priceListName().orElseThrow());
        assertEquals("Version must be the frozen snapshot.", 3,
                detail.pricing().priceListVersionNumber().orElseThrow());
        assertEquals("The server line price remains authoritative.",
                "535.90", detail.items().get(0).unitPrice().toPlainString());
        assertEquals("Color context must preserve the package.",
                "18 L", detail.items().get(0).tint().orElseThrow().packageName());
        assertEquals("Editable drafts need the numeric color identity.",
                3001L, detail.items().get(0).tint().orElseThrow().colorId());
        assertEquals("Editable drafts need the numeric tint-context identity.",
                4001L, detail.items().get(0).tint().orElseThrow().tintContextId());
        assertEquals("Color context must preserve the base.",
                "BASE-P", detail.items().get(0).tint().orElseThrow().baseCode());
    }

    @Test
    public void sendsOnlyVersionIdentityAndPolicyRevisionForPricing()
            throws Exception {
        String version = UUID.randomUUID().toString();
        MaterialQuoteDraft base = draft();
        MaterialQuoteDraft selected = new MaterialQuoteDraft(
                base.customerId(),
                base.customerName(),
                base.title(),
                base.notes(),
                base.validUntil(),
                Optional.of(new MaterialQuotePricingSelection(
                        11,
                        Optional.of(version))),
                base.items());
        server.enqueue(new MockResponse()
                .setResponseCode(201)
                .setHeader(CONTENT_TYPE, APPLICATION_JSON)
                .setHeader(IDEMPOTENCY_REPLAYED, FALSE_VALUE)
                .setBody(mutationJson()));

        repository.create(selected, PREVIEW_FINGERPRINT, KEY);
        String body = server.takeRequest().getBody().readUtf8();

        assertTrue(
                "The current policy revision must be sent.",
                body.contains("\"expectedPolicyRevision\":11"));
        assertTrue(
                "The opaque selected version must be sent.",
                body.contains("\"selectedPriceListVersionPublicId\":\""
                        + version + "\""));
        assertFalse(
                "The pricing selection must never carry a client amount.",
                body.contains("amount"));
    }

    @Test
    public void loadsTintConfigurationAndColorUnderTheSamePricingClaim()
            throws Exception {
        String version = "20000000-0000-4000-8000-000000000003";
        MaterialQuotePricingSelection pricing = new MaterialQuotePricingSelection(
                12,
                Optional.of(version));
        server.enqueue(jsonResponse(tintConfigurations(version, 12)));

        List<MaterialQuoteTintConfiguration> configurations =
                repository.tintConfigurations(9, pricing);
        RecordedRequest configurationRequest = server.takeRequest();

        assertEquals("The server configuration must be mapped exactly.",
                "Emborrachada", configurations.get(0).lineName());
        assertEquals("Organization scope must be explicit for pricing.",
                "9", configurationRequest.getRequestUrl()
                        .queryParameter("organizationId"));
        assertEquals("The selected table must remain opaque.",
                version, configurationRequest.getRequestUrl()
                        .queryParameter("priceListVersionPublicId"));
        assertEquals("The policy revision must pin the lookup.",
                "12", configurationRequest.getRequestUrl()
                        .queryParameter("expectedPolicyRevision"));

        server.enqueue(jsonResponse(tintColors(version, 12)));
        MaterialQuoteTintColorPage page = repository.tintColors(
                9,
                pricing,
                configurations.get(0),
                "cinza crômio",
                20);
        RecordedRequest colorRequest = server.takeRequest();

        assertEquals("The exact server amount is display-only.",
                "537.16", page.items().get(0).amount().toPlainString());
        assertEquals("The product/color/context identity must remain paired.",
                "9831:3001:4001", page.items().get(0).selection()
                        .identityKey(page.items().get(0).productId()));
        assertEquals("Line must be sent as a server-provided filter.",
                "Emborrachada", colorRequest.getRequestUrl()
                        .queryParameter("lineName"));
        assertEquals("Search must preserve user accents.",
                "cinza crômio", colorRequest.getRequestUrl()
                        .queryParameter("search"));
    }

    @Test
    public void refusesTintResultsFromAnotherPolicyRevision() {
        String version = "20000000-0000-4000-8000-000000000003";
        server.enqueue(jsonResponse(tintConfigurations(version, 13)));

        MaterialQuoteException exception = assertThrows(
                "A concurrent table change must invalidate the selection.",
                MaterialQuoteException.class,
                () -> repository.tintConfigurations(
                        9,
                        new MaterialQuotePricingSelection(
                                12,
                                Optional.of(version))));

        assertEquals("A stale selection must use the stable failure kind.",
                MaterialQuoteFailureKind.PRICE_POLICY_CHANGED,
                exception.kind());
    }

    @Test
    public void sendsTintIdentityButNeverTheDisplayedAmount() throws Exception {
        MaterialQuoteDraft base = draft();
        MaterialQuoteDraft tinted = new MaterialQuoteDraft(
                base.customerId(),
                base.customerName(),
                base.title(),
                base.notes(),
                base.validUntil(),
                base.pricing(),
                List.of(new MaterialQuoteDraftLine(
                        9831,
                        "LKC Emborrachada Cinza Crômio",
                        BigDecimal.ONE,
                        new BigDecimal("537.16"),
                        Optional.of(new MaterialQuoteTintSelection(
                                3001,
                                "Cinza Crômio",
                                4001,
                                "Emborrachada",
                                "Fosco",
                                "18 L")))));
        server.enqueue(new MockResponse()
                .setResponseCode(201)
                .setHeader(CONTENT_TYPE, APPLICATION_JSON)
                .setHeader(IDEMPOTENCY_REPLAYED, FALSE_VALUE)
                .setBody(mutationJson()));

        repository.create(tinted, PREVIEW_FINGERPRINT, KEY);
        String body = server.takeRequest().getBody().readUtf8();

        assertTrue("Color ID must be submitted for server resolution.",
                body.contains("\"colorId\":3001"));
        assertTrue("Tint context ID must be submitted as the paired identity.",
                body.contains("\"tintContextId\":4001"));
        assertFalse("Displayed tint price must never be accepted as input.",
                body.contains("537.16"));
    }

    @Test
    public void sendsLifecycleViewAndDuplicatesWithoutAClientBody()
            throws Exception {
        server.enqueue(jsonResponse(validPage()));
        repository.page(
                new MaterialQuoteQuery(
                        Optional.empty(),
                        Optional.empty(),
                        MaterialQuoteView.HISTORY,
                        20),
                Optional.empty());
        assertEquals(
                "Lifecycle view must be explicit.",
                "/api/mobile/v1/material-quotes?view=history&limit=20",
                server.takeRequest().getPath());

        server.enqueue(new MockResponse()
                .setResponseCode(201)
                .setHeader(CONTENT_TYPE, APPLICATION_JSON)
                .setHeader(IDEMPOTENCY_REPLAYED, FALSE_VALUE)
                .setBody(mutationJson()));
        MaterialQuoteMutationResult result = repository.duplicate(501, KEY);
        RecordedRequest duplicate = server.takeRequest();

        assertEquals("Duplicate must use POST.", "POST", duplicate.getMethod());
        assertEquals(
                "Duplicate route must remain versioned.",
                "/api/mobile/v1/material-quotes/501/duplicate",
                duplicate.getPath());
        assertEquals("Idempotency key must be forwarded.",
                KEY, duplicate.getHeader(IDEMPOTENCY_KEY_HEADER));
        assertEquals("No client draft or total may be supplied.",
                "", duplicate.getBody().readUtf8());
        assertEquals("The server-created identity must be returned.",
                501, result.quoteId());
    }

    @Test
    public void mapsHiddenQuoteWithoutResourceEnumeration() {
        String requestId = UUID.randomUUID().toString();
        server.enqueue(new MockResponse()
                .setResponseCode(404)
                .setHeader(CONTENT_TYPE, "application/problem+json")
                .setBody(problemJson(requestId)));

        MaterialQuoteException exception = assertThrows(
                "Absent and unauthorized quotes must look identical.",
                MaterialQuoteException.class,
                () -> repository.detail(999));

        assertEquals("Hidden quote remains non-enumerating.",
                MaterialQuoteFailureKind.NOT_FOUND,
                exception.kind());
        assertEquals("Request correlation must reach support.",
                requestId,
                exception.requestId().orElseThrow());
    }

    @Test
    public void requiresReplayMetadataOnEveryMutation() {
        server.enqueue(new MockResponse()
                .setResponseCode(201)
                .setHeader(CONTENT_TYPE, APPLICATION_JSON)
                .setBody(mutationJson()));

        MaterialQuoteException exception = assertThrows(
                "A write without replay metadata violates the contract.",
                MaterialQuoteException.class,
                () -> repository.create(
                        draft(),
                        PREVIEW_FINGERPRINT,
                        KEY));

        assertEquals("Missing metadata is a protocol failure.",
                MaterialQuoteFailureKind.PROTOCOL,
                exception.kind());
    }

    @Test
    public void transitionsWithRevisionAndMapsServerRepricing() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader(CONTENT_TYPE, APPLICATION_JSON)
                .setHeader(IDEMPOTENCY_REPLAYED, FALSE_VALUE)
                .setBody("""
                        {
                          "quoteId":501,
                          "previousStatus":"draft",
                          "status":"sent",
                          "revision":2,
                          "total":"45.80",
                          "pricingChanged":true,
                          "changed":true
                        }
                        """));

        MaterialQuoteStatusMutationResult result = repository.transition(
                501,
                1,
                MaterialQuoteStatus.SENT,
                KEY);
        RecordedRequest request = server.takeRequest();
        String body = request.getBody().readUtf8();

        assertEquals("Status route must remain versioned.",
                "/api/mobile/v1/material-quotes/501/status",
                request.getPath());
        assertEquals("Status mutation must use POST.",
                "POST", request.getMethod());
        assertEquals("Idempotency key must be forwarded.",
                KEY, request.getHeader(IDEMPOTENCY_KEY_HEADER));
        assertTrue("Expected revision must be sent.",
                body.contains("\"expectedRevision\":1"));
        assertTrue("Manual target must be sent.",
                body.contains("\"status\":\"sent\""));
        assertEquals("Server status must be mapped.",
                MaterialQuoteStatus.SENT, result.status());
        assertEquals("Repriced total must be authoritative.",
                "45.80", result.total().toPlainString());
        assertTrue("Price changes must reach the UI.",
                result.pricingChanged());
        assertFalse("First status execution cannot be a replay.",
                result.replayed());
    }

    @Test
    public void streamsAuthorizedPdfWithStrictHeadersAndSafeFilename()
            throws Exception {
        server.enqueue(pdfResponse("%PDF-1.7\nquote"));
        ByteArrayOutputStream destination = new ByteArrayOutputStream();

        MaterialQuotePdfDownload download = repository.downloadPdf(
                501,
                destination);
        RecordedRequest request = server.takeRequest();

        assertEquals("PDF route must remain versioned.",
                "/api/mobile/v1/material-quotes/501/pdf",
                request.getPath());
        assertEquals("PDF download must use GET.", "GET", request.getMethod());
        assertEquals("The safe filename must come from the server.",
                "orcamento-000501-material.pdf",
                download.filename());
        assertEquals("All bytes must be streamed to private cache.",
                "%PDF-1.7\nquote",
                destination.toString(java.nio.charset.StandardCharsets.UTF_8));
        assertEquals("The byte count must be exact.",
                destination.size(), download.bytesWritten());
    }

    @Test
    public void rejectsNonPdfBytesAndUnsafeFilenames() {
        server.enqueue(pdfResponse("NOT-PDF"));
        MaterialQuoteException signatureFailure = assertThrows(
                "A forged content type cannot bypass the PDF signature.",
                MaterialQuoteException.class,
                () -> repository.downloadPdf(501, new ByteArrayOutputStream()));
        assertEquals("Invalid bytes are a protocol failure.",
                MaterialQuoteFailureKind.PROTOCOL,
                signatureFailure.kind());

        server.enqueue(pdfResponse("%PDF-1.7")
                .setHeader("Content-Disposition",
                        "attachment; filename=\"../../cliente.pdf\""));
        MaterialQuoteException filenameFailure = assertThrows(
                "Path-like server filenames must fail closed.",
                MaterialQuoteException.class,
                () -> repository.downloadPdf(501, new ByteArrayOutputStream()));
        assertEquals("Unsafe filename is a protocol failure.",
                MaterialQuoteFailureKind.PROTOCOL,
                filenameFailure.kind());
    }

    @Test
    public void rejectsCacheableResponsesAndClassifiesLocalWriteFailures()
            throws IOException {
        server.enqueue(pdfResponse("%PDF-1.7")
                .removeHeader("Cache-Control"));
        MaterialQuoteException headerFailure = assertThrows(
                "A cacheable PDF response must fail closed.",
                MaterialQuoteException.class,
                () -> repository.downloadPdf(501, new ByteArrayOutputStream()));
        assertEquals("Unsafe headers are a protocol failure.",
                MaterialQuoteFailureKind.PROTOCOL,
                headerFailure.kind());

        server.enqueue(pdfResponse("%PDF-1.7"));
        try (OutputStream failingDestination = new OutputStream() {
                @Override
                public void write(int value) throws IOException {
                    throw new IOException("disk full");
                }
            }) {
            MaterialQuoteException storageFailure = assertThrows(
                    "A local write failure must not look like a network outage.",
                    MaterialQuoteException.class,
                    () -> repository.downloadPdf(501, failingDestination));
            assertEquals("Storage failure must remain actionable.",
                    MaterialQuoteFailureKind.LOCAL_STORAGE,
                    storageFailure.kind());
        }
    }

    @Test
    public void closeRevokesAllSubsequentReadsLocally() {
        repository.close();

        MaterialQuoteException exception = assertThrows(
                "An old account runtime must fail closed.",
                MaterialQuoteException.class,
                () -> repository.page(
                        MaterialQuoteQuery.initial(),
                        Optional.empty()));

        assertEquals("Revocation differs from a network outage.",
                MaterialQuoteFailureKind.ACCESS_REVOKED,
                exception.kind());
    }

    private static MaterialQuoteDraft draft() {
        return new MaterialQuoteDraft(
                9820,
                "Cliente",
                Optional.of("Orçamento"),
                Optional.empty(),
                Optional.empty(),
                List.of(new MaterialQuoteDraftLine(
                        9831,
                        "Tinta",
                        new BigDecimal("2.00"),
                        new BigDecimal("0.01"))));
    }

    private static MockResponse jsonResponse(String body) {
        return new MockResponse()
                .setResponseCode(200)
                .setHeader(CONTENT_TYPE, APPLICATION_JSON)
                .setBody(body);
    }

    private static MockResponse pdfResponse(String body) {
        return new MockResponse()
                .setResponseCode(200)
                .setHeader(CONTENT_TYPE, "application/pdf")
                .setHeader("Cache-Control", "private, no-store, max-age=0")
                .setHeader("X-Content-Type-Options", "nosniff")
                .setHeader("Content-Disposition",
                        "attachment; filename=\"orcamento-000501-material.pdf\"")
                .setBody(body);
    }

    private static String mutationJson() {
        return """
                {
                  "quoteId":501,
                  "revision":1,
                  "total":"39.80",
                  "changed":true
                }
                """;
    }

    private static String validPage() {
        return """
                {
                  "items":[{
                    "id":501,
                    "customer":{"id":9820,"name":"Cliente"},
                    "organizationId":9810,
                    "title":"Orçamento",
                    "status":"draft",
                    "subtotal":"39.80",
                    "discount":"0.00",
                    "total":"39.80",
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

    private static String resolvedDetail() {
        return """
                {
                  "id":501,
                  "customer":{"id":9820,"name":"Cliente"},
                  "organizationId":9810,
                  "title":"Orçamento",
                  "status":"draft",
                  "subtotal":"535.90",
                  "discount":"0.00",
                  "total":"535.90",
                  "revision":1,
                  "itemCount":1,
                  "validUntil":null,
                  "createdAt":"2026-07-25T10:00:00.000Z",
                  "updatedAt":"2026-07-25T10:00:00.000Z",
                  "notes":null,
                  "pricing":{
                    "state":"RESOLVED",
                    "priceListCode":"AUTCOM_2",
                    "priceListName":"Tabela 2",
                    "priceListVersionPublicId":"20000000-0000-4000-8000-000000000003",
                    "priceListVersionNumber":3,
                    "policyRevision":7,
                    "selectionMode":"EXPLICIT_ACTIVE",
                    "resolvedAt":"2026-08-05T12:00:00.000Z"
                  },
                  "items":[{
                    "id":801,
                    "productId":9831,
                    "description":"Tinta Premium",
                    "quantity":"1.00",
                    "unit":"lata",
                    "unitPrice":"535.90",
                    "total":"535.90",
                    "tint":{
                      "colorId":3001,
                      "colorPublicId":"30000000-0000-4000-8000-000000000001",
                      "colorName":"Azul Oceano",
                      "tintContextId":4001,
                      "tintContextPublicId":"40000000-0000-4000-8000-000000000001",
                      "lineName":"Premium",
                      "finishName":"Fosco",
                      "packageName":"18 L",
                      "packageCode":"LA-18000",
                      "baseCode":"BASE-P",
                      "unitCode":"L"
                    },
                    "currentProduct":{"active":true,"stock":12}
                  }]
                }
                """;
    }

    private static String previewJson() {
        return """
                {
                  "fingerprint":"FINGERPRINT_MARKER",
                  "customer":{"id":9820,"name":"Cliente"},
                  "organizationId":9810,
                  "title":"Orçamento",
                  "notes":null,
                  "validUntil":null,
                  "subtotal":"39.80",
                  "total":"39.80",
                  "pricing":{
                    "state":"RESOLVED",
                    "priceListPublicId":"10000000-0000-4000-8000-000000000002",
                    "priceListCode":"AUTCOM_2",
                    "priceListName":"Tabela 2",
                    "priceListVersionPublicId":"20000000-0000-4000-8000-000000000003",
                    "priceListVersionNumber":3,
                    "policyRevision":7,
                    "selectionMode":"EXPLICIT_ACTIVE"
                  },
                  "items":[{
                    "productId":9831,
                    "description":"Tinta",
                    "quantity":"2.00",
                    "unit":"lata",
                    "unitPrice":"19.90",
                    "total":"39.80",
                    "colorId":null,
                    "tintContextId":null
                  }]
                }
                """.replace("FINGERPRINT_MARKER", PREVIEW_FINGERPRINT);
    }

    private static String updatePreviewJson() {
        String createPreview = previewJson()
                .replaceFirst("\"total\":\"39\\.80\"", "\"total\":\"34.80\"");
        int finalObjectDelimiter = createPreview.lastIndexOf('}');
        return createPreview.substring(0, finalObjectDelimiter)
                + ",\n  \"quoteId\":501,"
                + "\n  \"expectedRevision\":3,"
                + "\n  \"discount\":\"5.00\"\n"
                + createPreview.substring(finalObjectDelimiter);
    }

    private static String tintConfigurations(String version, int revision) {
        return """
                {
                  "items":[{
                    "sourceSystem":"LKC",
                    "lineName":"Emborrachada",
                    "finishName":"Fosco",
                    "packageName":"18 L"
                  }],
                  "pricing":null
                }
                """.replace("null", tintPricing(version, revision));
    }

    private static String tintColors(String version, int revision) {
        return """
                {
                  "items":[{
                    "productId":9831,
                    "productName":"LKC Emborrachada Cinza Crômio",
                    "productSku":"04094",
                    "productUnit":"lata",
                    "productBrand":"Lukscolor",
                    "colorId":3001,
                    "colorPublicId":"30000000-0000-4000-8000-000000000001",
                    "colorName":"Cinza Crômio",
                    "hexColor":"#8B8C89",
                    "tintContextId":4001,
                    "tintContextPublicId":"40000000-0000-4000-8000-000000000001",
                    "sourceSystem":"LKC",
                    "lineName":"Emborrachada",
                    "finishName":"Fosco",
                    "packageName":"18 L",
                    "amount":"537.16",
                    "currency":"BRL"
                  }],
                  "hasMore":false,
                  "pricing":null
                }
                """.replace("null", tintPricing(version, revision));
    }

    private static String tintPricing(String version, int revision) {
        return """
                {
                  "organizationId":9,
                  "currency":"BRL",
                  "priceListPublicId":"10000000-0000-4000-8000-000000000002",
                  "priceListCode":"AUTCOM_2",
                  "priceListName":"Tabela 2",
                  "priceListVersionPublicId":"VERSION_MARKER",
                  "priceListVersionNumber":3,
                  "policyRevision":REVISION_MARKER,
                  "selectionMode":"EXPLICIT_ACTIVE"
                }
                """
                .replace("VERSION_MARKER", version)
                .replace("REVISION_MARKER", Integer.toString(revision));
    }

    private static String problemJson(String requestId) {
        return ("{\"type\":\"https://www.3vtintas.com.br/problems/not-found\","
                + "\"title\":\"Not found\",\"status\":404,"
                + "\"detail\":\"Resource not found.\","
                + "\"instance\":\"urn:3v:request:%s\","
                + "\"code\":\"NOT_FOUND\",\"requestId\":\"%s\"}")
                .formatted(requestId, requestId);
    }
}
