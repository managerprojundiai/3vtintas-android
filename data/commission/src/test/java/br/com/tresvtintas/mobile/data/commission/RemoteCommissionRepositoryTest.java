package br.com.tresvtintas.mobile.data.commission;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import br.com.tresvtintas.mobile.core.commission.CommissionDetail;
import br.com.tresvtintas.mobile.core.commission.CommissionException;
import br.com.tresvtintas.mobile.core.commission.CommissionFailureKind;
import br.com.tresvtintas.mobile.core.commission.CommissionKind;
import br.com.tresvtintas.mobile.core.commission.CommissionMutationCommand;
import br.com.tresvtintas.mobile.core.commission.CommissionMutationResult;
import br.com.tresvtintas.mobile.core.commission.CommissionPage;
import br.com.tresvtintas.mobile.core.commission.CommissionPaymentMethod;
import br.com.tresvtintas.mobile.core.commission.CommissionQuery;
import br.com.tresvtintas.mobile.core.commission.CommissionScope;
import br.com.tresvtintas.mobile.core.commission.CommissionStatus;
import br.com.tresvtintas.mobile.core.network.MobileApiFactory;
import br.com.tresvtintas.mobile.core.network.NetworkConfiguration;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public final class RemoteCommissionRepositoryTest {
    private static final String CONTENT_TYPE = "Content-Type";
    private MockWebServer server;
    private RemoteCommissionRepository repository;

    @Before
    public void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        repository = new RemoteCommissionRepository(
                new CommissionAccountScope(21, "b".repeat(64)),
                MobileApiFactory.create(new NetworkConfiguration(
                        "http://localhost:" + server.getPort()
                                + "/api/mobile/v1/",
                        "0.14.0-commissions",
                        15,
                        true),
                        () -> Optional.of("a".repeat(80))));
    }

    @After
    public void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    public void readsAuthorizedPageWithEverySupportedFilter() throws Exception {
        server.enqueue(json(validPage()));
        CommissionQuery query = new CommissionQuery(
                CommissionScope.TEAM,
                Optional.of("  MARIA  "),
                Optional.of(CommissionStatus.APPROVED),
                Optional.of(CommissionKind.SELLER),
                OptionalLong.of(9),
                Optional.of(LocalDate.parse("2026-07-01")),
                Optional.of(LocalDate.parse("2026-07-26")),
                25);

        CommissionPage page = repository.page(query, Optional.of("opaque_1"));
        RecordedRequest request = server.takeRequest();

        assertEquals(
                "The authorized response must contain one commission.",
                1,
                page.items().size());
        assertEquals(
                "Money must preserve exact decimal values.",
                "37.50",
                page.items().get(0).calculation().amount().toPlainString());
        assertEquals(
                "The full filtered overview must be mapped independently of pagination.",
                "123456789.01",
                page.overview().approved().amount().toPlainString());
        assertEquals(
                "The opaque next cursor must be preserved.",
                Optional.of("opaque_2"),
                page.nextCursor());
        assertEquals(
                "Only supported non-authoritative filters may reach the API.",
                "/api/mobile/v1/commissions?scope=team&search=maria"
                        + "&status=approved&kind=seller&organizationId=9"
                        + "&from=2026-07-01&to=2026-07-26"
                        + "&cursor=opaque_1&limit=25",
                request.getPath());
        assertFalse(
                "The client must never submit its role as authorization evidence.",
                request.getPath().contains("role="));
    }

    @Test
    public void readsDetailAndPreservesAuditTrace() throws Exception {
        server.enqueue(json(validDetail()));

        CommissionDetail detail = repository.detail(701, CommissionScope.TEAM);
        RecordedRequest request = server.takeRequest();

        assertEquals(
                "Detail must be looked up under the explicit originating scope.",
                "/api/mobile/v1/commissions/701?scope=team",
                request.getPath());
        assertEquals(
                "The workflow identifier must remain available for audit.",
                Optional.of("commission-701"),
                detail.workflowId());
        assertEquals(
                "The payment batch must be mapped as a server identity.",
                OptionalLong.of(91),
                detail.batchId());
        assertEquals(
                "The approving actor must remain visible.",
                3,
                detail.approvedBy().orElseThrow().userId());
        assertEquals(
                "Cancellation trace is absent on an approved commission.",
                Optional.empty(),
                detail.cancellation());
    }

    @Test
    public void rejectsUnknownEnumsAndUnexpectedSensitiveFields() {
        server.enqueue(json(validPage().replace(
                "\"kind\":\"seller\"",
                "\"kind\":\"invented\"")));
        server.enqueue(json(validPage().replace(
                "\"updatedAt\":\"2026-07-25T13:00:00.000Z\"",
                "\"updatedAt\":\"2026-07-25T13:00:00.000Z\","
                        + "\"calculationSnapshot\":{\"internal\":true}")));

        CommissionException enumFailure = assertThrows(
                CommissionException.class,
                () -> repository.page(
                        CommissionQuery.initial(CommissionScope.SELF),
                        Optional.empty()));
        CommissionException fieldFailure = assertThrows(
                CommissionException.class,
                () -> repository.page(
                        CommissionQuery.initial(CommissionScope.SELF),
                        Optional.empty()));

        assertEquals(
                "Unknown enums must fail closed as protocol errors.",
                CommissionFailureKind.PROTOCOL,
                enumFailure.kind());
        assertEquals(
                "Unexpected raw calculation data must never be silently accepted.",
                CommissionFailureKind.PROTOCOL,
                fieldFailure.kind());
    }

    @Test
    public void mapsHiddenCommissionWithoutResourceEnumeration() {
        String requestId = UUID.randomUUID().toString();
        server.enqueue(new MockResponse()
                .setResponseCode(404)
                .setHeader(CONTENT_TYPE, "application/problem+json")
                .setBody(problemJson(requestId)));

        CommissionException failure = assertThrows(
                CommissionException.class,
                () -> repository.detail(999, CommissionScope.SELF));

        assertEquals(
                "Absent and unauthorized commissions must look identical.",
                CommissionFailureKind.NOT_FOUND,
                failure.kind());
        assertEquals(
                "Request correlation must reach support.",
                requestId,
                failure.requestId().orElseThrow());
    }

    @Test
    public void revokedScopeCannotIssueAnotherRequest() {
        repository.close();

        CommissionException failure = assertThrows(
                CommissionException.class,
                () -> repository.page(
                        CommissionQuery.initial(CommissionScope.SELF),
                        Optional.empty()));

        assertEquals(
                "A closed account scope must fail as revoked.",
                CommissionFailureKind.ACCESS_REVOKED,
                failure.kind());
        assertEquals(
                "A revoked scope must not issue a network request.",
                0,
                server.getRequestCount());
    }

    @Test
    public void sendsTypedMutationsWithRevisionAndIdempotency()
            throws Exception {
        String key = "commission-mutation-key-0001";
        server.enqueue(mutation("approved", 2, false));
        server.enqueue(mutation("cancelled", 3, true));
        server.enqueue(mutation("paid", 4, false));

        CommissionMutationResult approval = repository.transition(
                CommissionMutationCommand.approve(701, 1),
                key);
        CommissionMutationResult cancellation = repository.transition(
                CommissionMutationCommand.cancel(
                        701,
                        2,
                        "Venda cancelada após conferência"),
                key);
        CommissionMutationResult payment = repository.transition(
                CommissionMutationCommand.pay(
                        701,
                        3,
                        CommissionPaymentMethod.PIX,
                        "PIX-701"),
                key);

        RecordedRequest approve = server.takeRequest();
        RecordedRequest cancel = server.takeRequest();
        RecordedRequest pay = server.takeRequest();
        assertEquals(
                "Approval must use its dedicated endpoint.",
                "/api/mobile/v1/commissions/701/approve",
                approve.getPath());
        assertEquals(
                "Cancellation must use its dedicated endpoint.",
                "/api/mobile/v1/commissions/701/cancel",
                cancel.getPath());
        assertEquals(
                "Payment must use its dedicated endpoint.",
                "/api/mobile/v1/commissions/701/payment",
                pay.getPath());
        assertEquals(
                "Every mutation must preserve the logical retry key.",
                key,
                pay.getHeader("Idempotency-Key"));
        assertEquals(
                "Approval must carry confirmation and optimistic revision.",
                "{\"expectedRevision\":1,\"confirmed\":true}",
                approve.getBody().readUtf8());
        assertTrue(
                "Cancellation must carry the reviewed reason.",
                cancel.getBody().readUtf8().contains(
                        "\"reason\":\"Venda cancelada após conferência\""));
        String paymentBody = pay.getBody().readUtf8();
        assertTrue(
                "Payment must carry the selected method.",
                paymentBody.contains("\"paymentMethod\":\"pix\""));
        assertTrue(
                "Payment must carry only the optional reference supplied.",
                paymentBody.contains("\"paymentReference\":\"PIX-701\""));
        assertFalse(
                "The client must never send a role in mutation payloads.",
                paymentBody.contains("\"role\""));
        assertEquals(
                "Mutation response status must be mapped strictly.",
                CommissionStatus.APPROVED,
                approval.status());
        assertTrue(
                "Idempotency replay evidence must be preserved.",
                cancellation.replayed());
        assertEquals(
                "The terminal revision must be preserved.",
                4,
                payment.revision());
    }

    private static MockResponse json(String body) {
        return new MockResponse()
                .setResponseCode(200)
                .setHeader(CONTENT_TYPE, "application/json")
                .setBody(body);
    }

    private static MockResponse mutation(
            String status,
            int revision,
            boolean replayed) {
        return json(
                "{\"commissionId\":701,\"status\":\""
                        + status
                        + "\",\"revision\":"
                        + revision
                        + ",\"changed\":true}")
                .setHeader(
                        "X-Idempotency-Replayed",
                        Boolean.toString(replayed));
    }

    private static String validPage() {
        return """
                {
                  "items":[{
                    "id":701,
                    "kind":"seller",
                    "status":"approved",
                    "recipient":{"userId":21,"role":"salesperson","name":"Ana"},
                    "organization":{"id":9,"name":"Loja Centro"},
                    "order":{"id":501,"type":"material","paymentStatus":"received"},
                    "calculation":{
                      "currency":"BRL",
                      "baseAmount":"750.00",
                      "ratePercent":"5.00",
                      "amount":"37.50",
                      "ruleVersion":"seller-v1"
                    },
                    "revision":2,
                    "eligibleForApproval":false,
                    "allowedActions":["pay"],
                    "approvedAt":"2026-07-25T12:30:00.000Z",
                    "paidAt":null,
                    "cancelledAt":null,
                    "createdAt":"2026-07-25T12:00:00.000Z",
                    "updatedAt":"2026-07-25T13:00:00.000Z"
                  }],
                  "overview":{
                    "pending":{"count":0,"amount":"0.00"},
                    "approved":{"count":1,"amount":"123456789.01"},
                    "paid":{"count":0,"amount":"0.00"},
                    "cancelled":{"count":0,"amount":"0.00"}
                  },
                  "nextCursor":"opaque_2"
                }
                """;
    }

    private static String validDetail() {
        return """
                {
                  "id":701,
                  "kind":"seller",
                  "status":"approved",
                  "recipient":{"userId":21,"role":"salesperson","name":"Ana"},
                  "organization":{"id":9,"name":"Loja Centro"},
                  "order":{"id":501,"type":"material","paymentStatus":"received"},
                  "calculation":{
                    "currency":"BRL",
                    "baseAmount":"750.00",
                    "ratePercent":"5.00",
                    "amount":"37.50",
                    "ruleVersion":"seller-v1"
                  },
                  "revision":2,
                  "eligibleForApproval":false,
                  "allowedActions":["pay"],
                  "approvedAt":"2026-07-25T12:30:00.000Z",
                  "paidAt":null,
                  "cancelledAt":null,
                  "createdAt":"2026-07-25T12:00:00.000Z",
                  "updatedAt":"2026-07-25T13:00:00.000Z",
                  "workflowId":"commission-701",
                  "batchId":91,
                  "approvedBy":{"userId":3,"name":"Gestor"},
                  "payment":null,
                  "cancellation":null
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
