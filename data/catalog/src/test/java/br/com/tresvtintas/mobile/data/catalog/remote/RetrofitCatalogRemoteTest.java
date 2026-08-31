package br.com.tresvtintas.mobile.data.catalog.remote;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertThrows;

import br.com.tresvtintas.mobile.core.catalog.CatalogException;
import br.com.tresvtintas.mobile.core.catalog.CatalogFailureKind;
import br.com.tresvtintas.mobile.core.catalog.CatalogPage;
import br.com.tresvtintas.mobile.core.catalog.CatalogQuery;
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

public final class RetrofitCatalogRemoteTest {
    private static final long ORGANIZATION_ID = 9L;
    private static final String LIST_PUBLIC_ID = "11111111-1111-4111-8111-111111111111";
    private static final String VERSION_PUBLIC_ID = "22222222-2222-4222-8222-222222222222";
    private MockWebServer server;
    private RetrofitCatalogRemote remote;

    @Before
    public void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        NetworkConfiguration configuration = new NetworkConfiguration(
                "http://localhost:" + server.getPort() + "/api/mobile/v1/",
                "0.5.0-catalog",
                6,
                true);
        MobileApi api = MobileApiFactory.create(
                configuration,
                () -> Optional.of("a".repeat(80)));
        remote = new RetrofitCatalogRemote(api, true, ORGANIZATION_ID);
    }

    @After
    public void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    public void mapsAuthorizedPageAndForwardsKeysetFilters() throws Exception {
        primePricingContext();
        server.enqueue(jsonResponse(validPage()));
        CatalogQuery query = new CatalogQuery(
                Optional.of("premium"),
                OptionalLong.of(2),
                30);

        CatalogPage page = remote.fetch(query, Optional.of("cursor_1"));
        RecordedRequest request = server.takeRequest();

        assertEquals("One product must be mapped.", 1, page.items().size());
        assertEquals("Exact decimal price must be preserved.", "249.90",
                page.items().get(0).price().orElseThrow().toPlainString());
        assertEquals(
                "Search and category filter must be encoded.",
                "/api/mobile/v1/catalog/priced-products"
                        + "?organizationId=9"
                        + "&priceListVersionPublicId=" + VERSION_PUBLIC_ID
                        + "&expectedPolicyRevision=3"
                        + "&search=premium&categoryId=2&cursor=cursor_1&limit=30",
                request.getPath());
        assertEquals(
                "Protected catalog call requires bearer.",
                "Bearer " + "a".repeat(80),
                request.getHeader("Authorization"));
    }

    @Test
    public void mapsRestrictedPageWithoutInventingPrice() throws Exception {
        remote = new RetrofitCatalogRemote(MobileApiFactory.create(
                new NetworkConfiguration(
                        "http://localhost:" + server.getPort() + "/api/mobile/v1/",
                        "0.5.0-catalog",
                        6,
                        true),
                () -> Optional.of("a".repeat(80))), false, ORGANIZATION_ID);
        server.enqueue(jsonResponse(validInformationPage()));

        CatalogPage page = remote.fetch(CatalogQuery.initial(), Optional.empty());
        RecordedRequest request = server.takeRequest();

        assertTrue(
                "A restricted response must remain without a price in the domain.",
                page.items().get(0).price().isEmpty());
        assertEquals(
                "Restricted catalog must use the additive endpoint without prices.",
                "/api/mobile/v1/catalog/product-information?limit=30",
                request.getPath());
    }

    @Test
    public void mapsProblemDetailsAndPreservesRequestId() {
        String requestId = UUID.randomUUID().toString();
        primePricingContext();
        server.enqueue(new MockResponse()
                .setResponseCode(403)
                .setHeader("Content-Type", "application/problem+json")
                .setBody(problemJson(requestId)));

        CatalogException exception = assertThrows(
                "Forbidden response must be typed.",
                CatalogException.class,
                () -> remote.fetch(CatalogQuery.initial(), Optional.empty()));

        assertEquals("Capability failure must remain forbidden.",
                CatalogFailureKind.FORBIDDEN, exception.kind());
        assertEquals("Correlation must reach support UI.",
                requestId, exception.requestId().orElseThrow());
    }

    @Test
    public void rejectsUnknownResponseFieldsAsProtocolFailure() {
        primePricingContext();
        server.enqueue(jsonResponse(validPage().replace(
                "\"nextCursor\":null",
                "\"nextCursor\":null,\"unexpected\":true")));

        CatalogException exception = assertThrows(
                "Strict DTO parsing must reject undocumented fields.",
                CatalogException.class,
                () -> remote.fetch(CatalogQuery.initial(), Optional.empty()));

        assertEquals(
                "Schema violation is not a network outage.",
                CatalogFailureKind.PROTOCOL,
                exception.kind());
    }

    @Test
    public void requiresAnExplicitSelectionWhenTheStoreHasMultipleTables() throws Exception {
        server.enqueue(jsonResponse(pricingContext(true)));
        remote.pricingContext();

        CatalogException exception = assertThrows(
                "A seller must choose a table before prices can be requested.",
                CatalogException.class,
                () -> remote.fetch(CatalogQuery.initial(), Optional.empty()));

        assertEquals(
                "The local guard must remain a stable selection failure.",
                CatalogFailureKind.PRICE_SELECTION_REQUIRED,
                exception.kind());
    }

    private void primePricingContext() {
        server.enqueue(jsonResponse(pricingContext(false)));
        try {
            remote.pricingContext();
            RecordedRequest request = server.takeRequest();
            assertEquals(
                    "Pricing must be scoped to the active store.",
                    "/api/mobile/v1/pricing/context?organizationId=9",
                    request.getPath());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Pricing context setup failed.", exception);
        } catch (CatalogException exception) {
            throw new AssertionError("Pricing context setup failed.", exception);
        }
    }

    private static MockResponse jsonResponse(String body) {
        return new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(body);
    }

    private static String validPage() {
        return """
                {
                  "items":[{
                    "id":1,
                    "category":{"id":2,"name":"Premium"},
                    "name":"Tinta Premium",
                    "description":"Acabamento",
                    "imageUrl":null,
                    "sku":"SKU-1",
                    "unit":"UN",
                    "volume":"18 L",
                    "price":"249.90",
                    "stock":8,
                    "brand":"3V",
                    "updatedAt":"2026-07-25T12:00:00.000Z"
                  }],
                  "nextCursor":null,
                  "pricing":{
                    "organizationId":9,
                    "currency":"BRL",
                    "priceListPublicId":"%s",
                    "priceListCode":"AUTCOM-2",
                    "priceListName":"Tabela 2",
                    "priceListVersionPublicId":"%s",
                    "priceListVersionNumber":1,
                    "policyRevision":3,
                    "selectionMode":"PRIMARY"
                  }
                }
                """.formatted(LIST_PUBLIC_ID, VERSION_PUBLIC_ID);
    }

    private static String validInformationPage() {
        return """
                {
                  "items":[{
                    "id":1,
                    "category":{"id":2,"name":"Premium"},
                    "name":"Tinta Premium",
                    "description":"Acabamento",
                    "imageUrl":null,
                    "sku":"SKU-1",
                    "unit":"UN",
                    "volume":"18 L",
                    "stock":8,
                    "brand":"3V",
                    "updatedAt":"2026-07-25T12:00:00.000Z"
                  }],
                  "nextCursor":null
                }
                """;
    }

    private static String pricingContext(boolean selectionRequired) {
        return """
                {
                  "organizationId":9,
                  "enabled":true,
                  "engineMode":"ACTIVE",
                  "policyRevision":3,
                  "selectionRequired":%s,
                  "selections":[
                    {
                      "priceListPublicId":"%s",
                      "priceListCode":"AUTCOM-2",
                      "priceListName":"Tabela 2",
                      "priceListVersionPublicId":"%s",
                      "priceListVersionNumber":1,
                      "isPrimary":true
                    }%s
                  ]
                }
                """.formatted(
                        selectionRequired,
                        LIST_PUBLIC_ID,
                        VERSION_PUBLIC_ID,
                        selectionRequired
                                ? ",{\"priceListPublicId\":\"33333333-3333-4333-8333-333333333333\","
                                + "\"priceListCode\":\"AUTCOM-3\","
                                + "\"priceListName\":\"Tabela 3\","
                                + "\"priceListVersionPublicId\":\"44444444-4444-4444-8444-444444444444\","
                                + "\"priceListVersionNumber\":1,\"isPrimary\":false}"
                                : "");
    }

    private static String problemJson(String requestId) {
        return """
                {
                  "type":"https://3vtintas.com.br/problems/forbidden",
                  "title":"Forbidden",
                  "status":403,
                  "detail":"Capability required.",
                  "instance":"urn:3v:request:%s",
                  "code":"FORBIDDEN",
                  "requestId":"%s"
                }
                """.formatted(requestId, requestId);
    }
}
