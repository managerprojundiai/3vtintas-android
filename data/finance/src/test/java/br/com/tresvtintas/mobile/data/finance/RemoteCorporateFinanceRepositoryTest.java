package br.com.tresvtintas.mobile.data.finance;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import br.com.tresvtintas.mobile.core.corporatefinance.CorporateFinanceOrganizationPage;
import br.com.tresvtintas.mobile.core.finance.FinanceAction;
import br.com.tresvtintas.mobile.core.finance.FinanceDraft;
import br.com.tresvtintas.mobile.core.finance.FinanceEntrySource;
import br.com.tresvtintas.mobile.core.finance.FinanceEntryType;
import br.com.tresvtintas.mobile.core.finance.FinanceException;
import br.com.tresvtintas.mobile.core.finance.FinanceFailureKind;
import br.com.tresvtintas.mobile.core.finance.FinanceMutationResult;
import br.com.tresvtintas.mobile.core.finance.FinancePage;
import br.com.tresvtintas.mobile.core.finance.FinanceQuery;
import br.com.tresvtintas.mobile.core.network.MobileApiFactory;
import br.com.tresvtintas.mobile.core.network.NetworkConfiguration;
import br.com.tresvtintas.mobile.core.network.api.MobileApi;
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

public final class RemoteCorporateFinanceRepositoryTest {
    private static final String CONTENT_TYPE = "Content-Type";
    private MockWebServer server;
    private MobileApi api;

    @Before
    public void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        api = MobileApiFactory.create(
                new NetworkConfiguration(
                        "http://localhost:" + server.getPort()
                                + "/api/mobile/v1/",
                        "0.17.0-corporate-finance",
                        18,
                        true),
                () -> Optional.of("a".repeat(80)));
    }

    @After
    public void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    public void readsOrganizationScopedPageWithSourceFilter()
            throws Exception {
        server.enqueue(json(200, validPage()));
        RemoteCorporateFinanceRepository repository =
                new RemoteCorporateFinanceRepository(
                        scope(OptionalLong.of(7), false),
                        api);
        FinanceQuery query = FinanceQuery.initial()
                .withSource(FinanceEntrySource.MATERIAL_ORDER);

        FinancePage page = repository.page(query, Optional.empty());
        RecordedRequest request = server.takeRequest();

        assertEquals(
                "The scoped page must expose its authorized row.",
                1,
                page.items().size());
        assertEquals(
                "Organization context must survive strict mapping.",
                "Loja Centro",
                page.items()
                        .get(0)
                        .organization()
                        .orElseThrow()
                        .name());
        assertEquals(
                "Protected source must survive strict mapping.",
                FinanceEntrySource.MATERIAL_ORDER,
                page.items().get(0).source());
        assertEquals(
                "Organization filter must be sent explicitly.",
                "7",
                request.getRequestUrl()
                        .queryParameter("organizationId"));
        assertEquals(
                "Source filter must be sent explicitly.",
                "material_order",
                request.getRequestUrl().queryParameter("source"));
    }

    @Test
    public void sendsAgendaDateBasisWithinTheCorporateScope()
            throws Exception {
        server.enqueue(json(200, validPage()));
        RemoteCorporateFinanceRepository repository =
                new RemoteCorporateFinanceRepository(
                        scope(OptionalLong.of(7), false),
                        api);
        FinanceQuery query = FinanceQuery.initial().forAgendaWindow(
                java.time.Instant.parse("2026-07-01T03:00:00Z"),
                java.time.Instant.parse("2026-08-01T03:00:00Z"));

        repository.page(query, Optional.empty());
        RecordedRequest request = server.takeRequest();

        assertEquals(
                "Integrated agenda must keep its organization scope.",
                "7",
                request.getRequestUrl().queryParameter("organizationId"));
        assertEquals(
                "Integrated agenda must request effective finance dates.",
                "agenda",
                request.getRequestUrl().queryParameter("dateBasis"));
    }

    @Test
    public void listsOnlyServerAuthorizedOrganizations() throws Exception {
        server.enqueue(json(200, """
                {
                  "items":[{"id":7,"name":"Loja Centro"}],
                  "nextCursor":null
                }
                """));
        RemoteCorporateFinanceOrganizationRepository repository =
                new RemoteCorporateFinanceOrganizationRepository(
                        scope(OptionalLong.empty(), true),
                        api);

        CorporateFinanceOrganizationPage page = repository.page(
                Optional.of("centro"),
                Optional.empty(),
                30);
        RecordedRequest request = server.takeRequest();

        assertEquals(
                "Only server-authorized organizations may be listed.",
                1,
                page.items().size());
        assertEquals(
                "Organization name must remain intact.",
                "Loja Centro",
                page.items().get(0).name());
        assertEquals(
                "Directory search must be sent explicitly.",
                "centro",
                request.getRequestUrl().queryParameter("search"));
    }

    @Test
    public void createsWithExplicitOrganizationAndIdempotency()
            throws Exception {
        server.enqueue(mutation(201, false, """
                {"entryId":991,"status":"pending"}
                """));
        RemoteCorporateFinanceRepository repository =
                new RemoteCorporateFinanceRepository(
                        scope(OptionalLong.of(7), false),
                        api);
        String key = "corporate-finance-create-991";

        FinanceMutationResult result = repository.create(
                new FinanceDraft(
                        FinanceEntryType.EXPENSE,
                        "Combustível",
                        new BigDecimal("50.00"),
                        Optional.empty(),
                        OptionalLong.empty(),
                        Optional.empty()),
                key);
        RecordedRequest request = server.takeRequest();
        String body = request.getBody().readUtf8();

        assertEquals(
                "Mutation action must remain explicit.",
                FinanceAction.CREATE,
                result.action());
        assertFalse(
                "First successful mutation must not be a replay.",
                result.replayed());
        assertEquals(
                "Idempotency key must reach the server unchanged.",
                key,
                request.getHeader("Idempotency-Key"));
        assertEquals(
                "Creation must target only the corporate route.",
                "/api/mobile/v1/corporate-finance/entries",
                request.getPath());
        assertTrue(
                "Creation must bind the selected organization.",
                body.contains("\"organizationId\":7"));
        assertTrue(
                "Creation must carry the literal confirmation.",
                body.contains(
                        "\"confirmation\":"
                                + "\"CREATE_CORPORATE_FINANCIAL_ENTRY\""));
    }

    @Test
    public void globalReadScopeCannotCreateWithoutOrganization() {
        RemoteCorporateFinanceRepository repository =
                new RemoteCorporateFinanceRepository(
                        scope(OptionalLong.empty(), true),
                        api);

        FinanceException failure = assertThrows(
                FinanceException.class,
                () -> repository.create(
                        new FinanceDraft(
                                FinanceEntryType.EXPENSE,
                                "Combustível",
                                new BigDecimal("50.00"),
                                Optional.empty(),
                                OptionalLong.empty(),
                                Optional.empty()),
                        "corporate-finance-global-create"));

        assertEquals(
                "Global read scope cannot imply write scope.",
                FinanceFailureKind.INVALID_REQUEST,
                failure.kind());
        assertEquals(
                "Rejected global creation must not issue a request.",
                0,
                server.getRequestCount());
    }

    private static CorporateFinanceAccountScope scope(
            OptionalLong organizationId,
            boolean globalAccess) {
        return new CorporateFinanceAccountScope(
                21,
                "c".repeat(64),
                organizationId,
                globalAccess);
    }

    private static MockResponse json(int status, String body) {
        return new MockResponse()
                .setResponseCode(status)
                .setHeader(CONTENT_TYPE, "application/json")
                .setBody(body);
    }

    private static MockResponse mutation(
            int status,
            boolean replayed,
            String body) {
        return json(status, body).setHeader(
                "X-Idempotency-Replayed",
                Boolean.toString(replayed));
    }

    private static String validPage() {
        return """
                {
                  "items":[{
                    "id":701,
                    "type":"payable",
                    "status":"pending",
                    "source":"material_order",
                    "title":"Pedido 3001",
                    "amount":"123.45",
                    "currency":"BRL",
                    "dueAt":"2026-07-30T03:00:00.000Z",
                    "settledAt":null,
                    "organization":{"id":7,"name":"Loja Centro"},
                    "customer":null,
                    "allowedActions":[],
                    "createdAt":"2026-07-26T12:00:00.000Z",
                    "updatedAt":"2026-07-26T12:05:00.000Z"
                  }],
                  "overview":{
                    "pending":{
                      "expense":{"count":0,"amount":"0.00"},
                      "payable":{"count":1,"amount":"123.45"},
                      "receivable":{"count":0,"amount":"0.00"}
                    },
                    "settled":{
                      "expense":{"count":0,"amount":"0.00"},
                      "payable":{"count":0,"amount":"0.00"},
                      "receivable":{"count":0,"amount":"0.00"}
                    },
                    "cancelled":{
                      "expense":{"count":0,"amount":"0.00"},
                      "payable":{"count":0,"amount":"0.00"},
                      "receivable":{"count":0,"amount":"0.00"}
                    },
                    "overdue":{
                      "expense":{"count":0,"amount":"0.00"},
                      "payable":{"count":0,"amount":"0.00"},
                      "receivable":{"count":0,"amount":"0.00"}
                    }
                  },
                  "nextCursor":null
                }
                """;
    }
}
