package br.com.tresvtintas.mobile.data.finance;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import br.com.tresvtintas.mobile.core.finance.FinanceAction;
import br.com.tresvtintas.mobile.core.finance.FinanceDetail;
import br.com.tresvtintas.mobile.core.finance.FinanceDraft;
import br.com.tresvtintas.mobile.core.finance.FinanceDueFilter;
import br.com.tresvtintas.mobile.core.finance.FinanceDateBasis;
import br.com.tresvtintas.mobile.core.finance.FinanceEntryStatus;
import br.com.tresvtintas.mobile.core.finance.FinanceEntryType;
import br.com.tresvtintas.mobile.core.finance.FinanceException;
import br.com.tresvtintas.mobile.core.finance.FinanceFailureKind;
import br.com.tresvtintas.mobile.core.finance.FinanceMutationResult;
import br.com.tresvtintas.mobile.core.finance.FinancePage;
import br.com.tresvtintas.mobile.core.finance.FinancePaymentMethod;
import br.com.tresvtintas.mobile.core.finance.FinanceQuery;
import br.com.tresvtintas.mobile.core.network.MobileApiFactory;
import br.com.tresvtintas.mobile.core.network.NetworkConfiguration;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public final class RemoteFinanceRepositoryTest {
    private static final String CONTENT_TYPE = "Content-Type";
    private MockWebServer server;
    private RemoteFinanceRepository repository;

    @Before
    public void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        repository = new RemoteFinanceRepository(
                new FinanceAccountScope(21, "c".repeat(64)),
                MobileApiFactory.create(
                        new NetworkConfiguration(
                                "http://localhost:" + server.getPort()
                                        + "/api/mobile/v1/",
                                "0.15.0-finance",
                                16,
                                true),
                        () -> Optional.of("a".repeat(80))));
    }

    @After
    public void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    public void readsOwnedPageWithSafeFiltersAndExactOverview()
            throws Exception {
        server.enqueue(json(200, validPage()));
        FinanceQuery query = new FinanceQuery(
                Optional.of("  Cliente JOÃO  "),
                Optional.of(FinanceEntryStatus.PENDING),
                Optional.of(FinanceEntryType.RECEIVABLE),
                Optional.empty(),
                FinanceDueFilter.UPCOMING,
                FinanceDateBasis.DUE,
                Optional.of(Instant.parse("2026-07-26T03:00:00Z")),
                Optional.of(Instant.parse("2026-08-01T03:00:00Z")),
                25);

        FinancePage page = repository.page(query, Optional.of("opaque_1"));
        RecordedRequest request = server.takeRequest();

        assertEquals(
                "The owned page must contain its authorized row.",
                1,
                page.items().size());
        assertEquals(
                "Aggregate money must preserve exact decimal precision.",
                "123456789.01",
                page.overview()
                        .pending()
                        .receivable()
                        .amount()
                        .toPlainString());
        assertEquals(
                "Search must be normalized.",
                "cliente joão",
                request.getRequestUrl().queryParameter("search"));
        assertEquals(
                "Due filter must be explicit.",
                "upcoming",
                request.getRequestUrl().queryParameter("due"));
        assertEquals(
                "Opaque cursor must remain unchanged.",
                "opaque_1",
                request.getRequestUrl().queryParameter("cursor"));
        assertFalse(
                "Owner must never be client supplied.",
                request.getPath().contains("ownerUserId"));
        assertFalse(
                "Role must never be client supplied.",
                request.getPath().contains("role="));
        assertFalse(
                "Personal finance must not accept organization scope.",
                request.getPath().contains("organizationId"));
    }

    @Test
    public void sendsAgendaDateBasisOnlyForTheIntegratedCalendar()
            throws Exception {
        server.enqueue(json(200, validPage()));
        FinanceQuery query = FinanceQuery.initial().forAgendaWindow(
                Instant.parse("2026-07-01T03:00:00Z"),
                Instant.parse("2026-08-01T03:00:00Z"));

        repository.page(query, Optional.empty());
        RecordedRequest request = server.takeRequest();

        assertEquals(
                "Integrated agenda must request the effective finance date.",
                "agenda",
                request.getRequestUrl().queryParameter("dateBasis"));
    }

    @Test
    public void mapsDetailWithoutPersistingOrRequestingAnotherOwner()
            throws Exception {
        server.enqueue(json(200, validDetail()));

        FinanceDetail detail = repository.detail(701);
        RecordedRequest request = server.takeRequest();

        assertEquals(
                "Detail must use only the resource identifier.",
                "/api/mobile/v1/personal-finance/entries/701",
                request.getPath());
        assertEquals(
                "Authorized notes must be mapped.",
                Optional.of("Parcela única"),
                detail.notes());
        assertEquals(
                "Payment method must remain typed.",
                FinancePaymentMethod.PIX,
                detail.payment().orElseThrow().method().orElseThrow());
    }

    @Test
    public void sendsConfirmedIdempotentMutationsAndValidatesReplayHeader()
            throws Exception {
        server.enqueue(mutation(201, false, """
                {"entryId":702,"status":"pending"}
                """));
        server.enqueue(mutation(200, true, """
                {"entryId":702,"status":"settled","changed":true}
                """));
        FinanceDraft draft = new FinanceDraft(
                FinanceEntryType.EXPENSE,
                "Combustível",
                new BigDecimal("125.50"),
                Optional.of(Instant.parse("2026-07-30T03:00:00Z")),
                OptionalLong.of(91),
                Optional.of("Visita"));

        FinanceMutationResult created = repository.create(
                draft,
                "00000000-0000-4000-8000-000000000501");
        RecordedRequest creation = server.takeRequest();
        FinanceMutationResult settled = repository.settle(
                702,
                FinancePaymentMethod.PIX,
                Optional.of("e2e-91"),
                "00000000-0000-4000-8000-000000000502");
        RecordedRequest settlement = server.takeRequest();

        assertEquals(
                "Create action must remain explicit.",
                FinanceAction.CREATE,
                created.action());
        assertTrue(
                "A fresh creation must report a change.",
                created.changed());
        assertFalse(
                "A fresh creation cannot be a replay.",
                created.replayed());
        assertEquals(
                "Idempotency key must be sent unchanged.",
                "00000000-0000-4000-8000-000000000501",
                creation.getHeader("Idempotency-Key"));
        assertTrue(
                "Creation requires the literal confirmation.",
                creation.getBody().readUtf8().contains(
                        "\"confirmation\":"
                                + "\"CREATE_PERSONAL_FINANCIAL_ENTRY\""));
        assertEquals(
                "Settlement action must remain explicit.",
                FinanceAction.SETTLE,
                settled.action());
        assertTrue(
                "Replay metadata must reach the caller.",
                settled.replayed());
        assertTrue(
                "Settlement requires the literal confirmation.",
                settlement.getBody().readUtf8().contains(
                        "\"confirmation\":"
                                + "\"SETTLE_PERSONAL_FINANCIAL_ENTRY\""));
    }

    @Test
    public void mapsHiddenResourceWithoutEnumeration() {
        String requestId = UUID.randomUUID().toString();
        server.enqueue(new MockResponse()
                .setResponseCode(404)
                .setHeader(CONTENT_TYPE, "application/problem+json")
                .setBody(problemJson(requestId)));

        FinanceException failure = assertThrows(
                FinanceException.class,
                () -> repository.detail(999));

        assertEquals(
                "Hidden and missing resources must look identical.",
                FinanceFailureKind.NOT_FOUND,
                failure.kind());
        assertEquals(
                "Support correlation must be preserved.",
                requestId,
                failure.requestId().orElseThrow());
    }

    @Test
    public void closedAccountScopeCannotIssueNetworkRequests() {
        repository.close();

        FinanceException failure = assertThrows(
                FinanceException.class,
                () -> repository.page(
                        FinanceQuery.initial(),
                        Optional.empty()));

        assertEquals(
                "Closed scope must fail as revoked.",
                FinanceFailureKind.ACCESS_REVOKED,
                failure.kind());
        assertEquals(
                "Closed scope must not issue a request.",
                0,
                server.getRequestCount());
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
                    "type":"receivable",
                    "status":"pending",
                    "source":"manual",
                    "title":"Cliente João",
                    "amount":"250.00",
                    "currency":"BRL",
                    "dueAt":"2026-07-30T03:00:00.000Z",
                    "settledAt":null,
                    "customer":{"id":91,"name":"João"},
                    "allowedActions":["settle","cancel"],
                    "createdAt":"2026-07-26T12:00:00.000Z",
                    "updatedAt":"2026-07-26T12:05:00.000Z"
                  }],
                  "overview":{
                    "pending":{
                      "expense":{"count":0,"amount":"0.00"},
                      "payable":{"count":0,"amount":"0.00"},
                      "receivable":{"count":1,"amount":"123456789.01"}
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
                  "nextCursor":"opaque_2"
                }
                """;
    }

    private static String validDetail() {
        return """
                {
                  "id":701,
                  "type":"receivable",
                  "status":"settled",
                  "source":"manual",
                  "title":"Cliente João",
                  "amount":"250.00",
                  "currency":"BRL",
                  "dueAt":"2026-07-30T03:00:00.000Z",
                  "settledAt":"2026-07-27T14:00:00.000Z",
                  "customer":{"id":91,"name":"João"},
                  "allowedActions":[],
                  "createdAt":"2026-07-26T12:00:00.000Z",
                  "updatedAt":"2026-07-27T14:00:00.000Z",
                  "notes":"Parcela única",
                  "payment":{"method":"pix","reference":"e2e-91"}
                }
                """;
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
