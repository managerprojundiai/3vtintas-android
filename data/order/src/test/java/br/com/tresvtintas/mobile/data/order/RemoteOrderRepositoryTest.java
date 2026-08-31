package br.com.tresvtintas.mobile.data.order;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertThrows;

import br.com.tresvtintas.mobile.core.network.MobileApiFactory;
import br.com.tresvtintas.mobile.core.network.NetworkConfiguration;
import br.com.tresvtintas.mobile.core.order.OrderException;
import br.com.tresvtintas.mobile.core.order.OrderDetail;
import br.com.tresvtintas.mobile.core.order.OrderAction;
import br.com.tresvtintas.mobile.core.order.OrderActionResult;
import br.com.tresvtintas.mobile.core.order.OrderFailureKind;
import br.com.tresvtintas.mobile.core.order.OrderPage;
import br.com.tresvtintas.mobile.core.order.OrderConversionResult;
import br.com.tresvtintas.mobile.core.order.OrderQuery;
import br.com.tresvtintas.mobile.core.order.OrderPaymentMethod;
import br.com.tresvtintas.mobile.core.order.OrderStatus;
import br.com.tresvtintas.mobile.core.order.OrderView;
import java.io.IOException;
import java.util.Optional;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public final class RemoteOrderRepositoryTest {
    private MockWebServer server;
    private RemoteOrderRepository repository;

    @Before
    public void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        repository = new RemoteOrderRepository(
                new OrderAccountScope(21, "b".repeat(64)),
                MobileApiFactory.create(new NetworkConfiguration(
                        "http://localhost:" + server.getPort() + "/api/mobile/v1/",
                        "0.13.0-order-actions", 10, true),
                        () -> Optional.of("a".repeat(80))),
                true);
    }

    @After
    public void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    public void readsAuthorizedPageAndKeepsServerScopeAuthoritative() throws Exception {
        server.enqueue(json(validPage()));

        OrderPage page = repository.page(new OrderQuery(Optional.of("Maria"),
                Optional.empty(), Optional.empty(), OrderView.HISTORY, 30),
                Optional.of("opaque_1"));
        RecordedRequest request = server.takeRequest();

        assertEquals("The authorized page must contain one order.", 1, page.items().size());
        assertEquals(
                "Money must retain the server decimal value.",
                "125.40",
                page.items().get(0).total().orElseThrow().toPlainString());
        assertEquals(
                "The opaque next cursor must be preserved.",
                Optional.of("opaque_2"),
                page.nextCursor());
        assertEquals(
                "Only server-authorized actions may reach the client.",
                java.util.Set.of(OrderAction.CONFIRM, OrderAction.CANCEL),
                page.items().get(0).allowedActions());
        assertEquals(
                "The request must contain only supported query filters.",
                "/api/mobile/v1/orders?search=maria&view=history&cursor=opaque_1&limit=30",
                request.getPath());
        assertFalse(
                "The client must never submit an authoritative role.",
                request.getPath().contains("role"));
        assertFalse(
                "The client must never submit an authoritative organization.",
                request.getPath().contains("organizationId"));
    }

    @Test
    public void restrictedRoleUsesInformationContractsWithoutMonetaryFallback() throws Exception {
        repository = repository(false);
        server.enqueue(json(validPage().replace("\"total\":\"125.40\",", "")));
        server.enqueue(json(validInformationDetail()));

        OrderPage page = repository.page(OrderQuery.initial(), Optional.empty());
        OrderDetail detail = repository.detail(701);
        RecordedRequest pageRequest = server.takeRequest();
        RecordedRequest detailRequest = server.takeRequest();

        assertTrue(
                "A restricted order summary must not invent a monetary value.",
                page.items().get(0).total().isEmpty());
        assertTrue(
                "A restricted order detail must not contain a pricing snapshot.",
                detail.pricing().isEmpty());
        assertTrue(
                "A restricted order item must not contain a unit price.",
                detail.items().get(0).unitPrice().isEmpty());
        assertEquals(
                "Restricted pages must use the allowlisted information endpoint.",
                "/api/mobile/v1/order-information?view=active&limit=30",
                pageRequest.getPath());
        assertEquals(
                "Restricted details must use the allowlisted information endpoint.",
                "/api/mobile/v1/order-information/701",
                detailRequest.getPath());
    }

    @Test
    public void authorizedDetailPreservesImmutablePricingSnapshot() throws Exception {
        server.enqueue(json(validPricedDetail()));

        OrderDetail detail = repository.detail(701);
        RecordedRequest request = server.takeRequest();

        assertEquals(
                "Authorized details must use the priced endpoint.",
                "/api/mobile/v1/priced-orders/701",
                request.getPath());
        assertTrue(
                "The resolved table snapshot must remain available to authorized roles.",
                detail.pricing().orElseThrow().resolved());
        assertEquals(
                "The exact price table version must be preserved.",
                2,
                detail.pricing().orElseThrow().priceListVersionNumber().orElseThrow());
    }

    @Test
    public void rejectsUnknownProtocolEnum() {
        server.enqueue(json(validPage().replace("\"pending\"", "\"invented\"")));

        OrderException failure = assertThrows(OrderException.class,
                () -> repository.page(OrderQuery.initial(), Optional.empty()));

        assertEquals(
                "An unknown enum must fail closed as a protocol error.",
                OrderFailureKind.PROTOCOL,
                failure.kind());
    }

    @Test
    public void revokedScopeCannotIssueAnotherRequest() {
        repository.close();

        OrderException failure = assertThrows(OrderException.class,
                () -> repository.page(OrderQuery.initial(), Optional.empty()));

        assertEquals(
                "A closed account scope must fail as revoked.",
                OrderFailureKind.ACCESS_REVOKED,
                failure.kind());
        assertEquals(
                "A revoked scope must not issue a network request.",
                0,
                server.getRequestCount());
    }

    @Test
    public void preservesSafeProblemGuidanceAndRequestCorrelation() {
        server.enqueue(new MockResponse().setResponseCode(409)
                .setHeader("Content-Type", "application/problem+json")
                .setBody("""
                        {
                          "type":"https://www.3vtintas.com.br/problems/resource-conflict",
                          "title":"Conflito com o estado atual do recurso",
                          "status":409,
                          "detail":"Vincule um vendedor ativo à carteira do cliente antes de criar o pedido.",
                          "instance":"urn:3v:request:a848be6b-b24c-4d3a-8448-9d8ae5147d25",
                          "code":"RESOURCE_CONFLICT",
                          "requestId":"a848be6b-b24c-4d3a-8448-9d8ae5147d25"
                        }
                        """));

        OrderException failure = assertThrows(
                OrderException.class,
                () -> repository.convertMaterialQuote(
                        501,
                        4,
                        "00000000-0000-4000-8000-000000000502"));

        assertEquals(
                "The stable failure kind must remain authoritative.",
                OrderFailureKind.CONFLICT,
                failure.kind());
        assertEquals(
                "Safe server guidance must be available to the feature.",
                "Vincule um vendedor ativo à carteira do cliente antes de criar o pedido.",
                failure.userDetail().orElseThrow());
        assertEquals(
                "Support correlation must survive repository mapping.",
                "a848be6b-b24c-4d3a-8448-9d8ae5147d25",
                failure.requestId().orElseThrow());
    }

    @Test
    public void convertsWithRevisionConfirmationAndIdempotencyHeader() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(201)
                .setHeader("Content-Type", "application/json")
                .setHeader("X-Idempotency-Replayed", "false")
                .setBody("""
                        {"quoteId":501,"quoteRevision":5,"orderId":701,"orderRevision":1,
                        "status":"pending","total":"125.40","deliveryId":801,
                        "seller":{"userId":21,"role":"salesperson"}}
                        """));
        String key = "00000000-0000-4000-8000-000000000501";

        OrderConversionResult result = repository.convertMaterialQuote(501, 4, key);
        RecordedRequest request = server.takeRequest();

        assertEquals("The created order ID must be returned.", 701, result.orderId());
        assertFalse("The first conversion response must not be a replay.", result.replayed());
        assertEquals("Conversion must use POST.", "POST", request.getMethod());
        assertEquals(
                "Conversion must target the selected material quote.",
                "/api/mobile/v1/material-quotes/501/order",
                request.getPath());
        assertEquals(
                "The stable attempt key must be sent unchanged.",
                key,
                request.getHeader("Idempotency-Key"));
        assertEquals(
                "The body must contain revision and explicit confirmation only.",
                "{\"expectedRevision\":4,\"confirmed\":true}",
                request.getBody().readUtf8());
    }

    @Test
    public void sendsStrictIdempotentOrderActionContracts() throws Exception {
        server.enqueue(actionJson("""
                {"action":"confirm","orderId":701,"revision":2,
                "status":"confirmed","paymentStatus":"pending","changed":true}
                """, false));
        server.enqueue(actionJson("""
                {"action":"cancel","orderId":701,"revision":3,
                "status":"cancelled","paymentStatus":"pending","changed":true}
                """, false));
        server.enqueue(actionJson("""
                {"action":"record_payment","orderId":702,"revision":5,
                "status":"confirmed","paymentStatus":"received","changed":true}
                """, true));

        OrderActionResult status = repository.transitionStatus(
                701,
                1,
                OrderStatus.CONFIRMED,
                "00000000-0000-4000-8000-000000000701");
        OrderActionResult cancellation = repository.cancel(
                701,
                2,
                Optional.of("  cliente solicitou  "),
                "00000000-0000-4000-8000-000000000702");
        OrderActionResult payment = repository.recordPayment(
                702,
                4,
                OrderPaymentMethod.PIX,
                Optional.of("  E2E-123  "),
                "00000000-0000-4000-8000-000000000703");

        RecordedRequest statusRequest = server.takeRequest();
        RecordedRequest cancellationRequest = server.takeRequest();
        RecordedRequest paymentRequest = server.takeRequest();
        assertEquals("Status action must be mapped.", OrderAction.CONFIRM, status.action());
        assertEquals(
                "Status payload must contain revision and explicit confirmation.",
                "{\"expectedRevision\":1,\"status\":\"confirmed\",\"confirmed\":true}",
                statusRequest.getBody().readUtf8());
        assertEquals(
                "Cancellation must target its dedicated endpoint.",
                "/api/mobile/v1/orders/701/cancellation",
                cancellationRequest.getPath());
        assertEquals(
                "Cancellation reason must be normalized.",
                "{\"expectedRevision\":2,\"reason\":\"cliente solicitou\",\"confirmed\":true}",
                cancellationRequest.getBody().readUtf8());
        assertEquals(
                "Payment must target its dedicated endpoint.",
                "/api/mobile/v1/orders/702/payment-receipt",
                paymentRequest.getPath());
        assertEquals(
                "Payment payload must remain explicit and bounded.",
                "{\"expectedRevision\":4,\"paymentMethod\":\"pix\",\"paymentReference\":\"E2E-123\",\"confirmed\":true}",
                paymentRequest.getBody().readUtf8());
        assertTrue(
                "The replay response must be exposed to the UI.",
                payment.replayed());
        assertEquals(
                "Cancellation result must remain authoritative.",
                OrderStatus.CANCELLED,
                cancellation.status());
    }

    private static MockResponse json(String body) {
        return new MockResponse().setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(body);
    }

    private static MockResponse actionJson(String body, boolean replayed) {
        return json(body).setHeader(
                "X-Idempotency-Replayed",
                Boolean.toString(replayed));
    }

    private static String validPage() {
        return """
                {"items":[{"id":701,"type":"material","status":"pending","revision":1,
                "paymentStatus":"pending","allowedActions":["confirm","cancel"],
                "total":"125.40","itemCount":2,"quoteId":501,
                "organization":{"id":9,"name":"Loja Centro"},
                "seller":{"userId":21,"role":"salesperson","name":"Ana"},
                "customer":{"id":31,"name":"Maria"},
                "delivery":{"id":41,"status":"in_transit","estimatedAt":"2026-07-26T12:00:00.000Z","assignedToCurrentActor":false},
                "createdAt":"2026-07-25T12:00:00.000Z","updatedAt":"2026-07-25T13:00:00.000Z"}],
                "nextCursor":"opaque_2"}
                """;
    }

    private RemoteOrderRepository repository(boolean canReadPrices) {
        return new RemoteOrderRepository(
                new OrderAccountScope(21, "b".repeat(64)),
                MobileApiFactory.create(new NetworkConfiguration(
                        "http://localhost:" + server.getPort() + "/api/mobile/v1/",
                        "0.13.0-order-actions", 10, true),
                        () -> Optional.of("a".repeat(80))),
                canReadPrices);
    }

    private static String validInformationDetail() {
        return """
                {"id":701,"type":"material","status":"pending","revision":1,
                "paymentStatus":"pending","allowedActions":[],"itemCount":1,"quoteId":501,
                "organization":{"id":9,"name":"Loja Centro"},
                "seller":{"userId":21,"role":"salesperson","name":"Ana"},
                "customer":{"id":31,"name":"Maria"},"delivery":null,
                "createdAt":"2026-07-25T12:00:00.000Z","updatedAt":"2026-07-25T13:00:00.000Z",
                "notes":null,"customerContact":null,"deliveryDetails":null,
                "items":[{"id":1,"productId":11,"description":"Tinta fosca",
                "quantity":"2.00","unit":"lata"}]}
                """;
    }

    private static String validPricedDetail() {
        return """
                {"id":701,"type":"material","status":"pending","revision":1,
                "paymentStatus":"pending","allowedActions":[],"total":"125.40",
                "itemCount":1,"quoteId":501,"organization":{"id":9,"name":"Loja Centro"},
                "seller":{"userId":21,"role":"salesperson","name":"Ana"},
                "customer":{"id":31,"name":"Maria"},"delivery":null,
                "createdAt":"2026-07-25T12:00:00.000Z","updatedAt":"2026-07-25T13:00:00.000Z",
                "notes":null,"pricing":{"state":"RESOLVED","priceListCode":"AUTCOM-2",
                "priceListName":"Tabela 2","priceListVersionPublicId":"11111111-1111-4111-8111-111111111111",
                "priceListVersionNumber":2,"policyRevision":3,"selectionMode":"STORE_DEFAULT",
                "resolvedAt":"2026-07-25T11:59:00.000Z"},
                "customerContact":null,"deliveryDetails":null,
                "items":[{"id":1,"productId":11,"description":"Tinta fosca",
                "quantity":"2.00","unit":"lata","unitPrice":"62.70","total":"125.40"}]}
                """;
    }
}
