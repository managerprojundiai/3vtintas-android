package br.com.tresvtintas.mobile.data.agent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import br.com.tresvtintas.mobile.core.agent.AgentActionDecision;
import br.com.tresvtintas.mobile.core.agent.AgentActionKind;
import br.com.tresvtintas.mobile.core.agent.AgentActionStatus;
import br.com.tresvtintas.mobile.core.agent.AgentMaterialQuoteAmendSummary;
import br.com.tresvtintas.mobile.core.agent.AgentMaterialQuoteCreateSummary;
import br.com.tresvtintas.mobile.core.agent.AgentConversation;
import br.com.tresvtintas.mobile.core.agent.AgentConversationPage;
import br.com.tresvtintas.mobile.core.agent.AgentDocumentType;
import br.com.tresvtintas.mobile.core.agent.AgentException;
import br.com.tresvtintas.mobile.core.agent.AgentFailureKind;
import br.com.tresvtintas.mobile.core.agent.AgentFinanceResult;
import br.com.tresvtintas.mobile.core.agent.AgentFinanceSummary;
import br.com.tresvtintas.mobile.core.agent.AgentMessagePage;
import br.com.tresvtintas.mobile.core.agent.AgentStepUpChallenge;
import br.com.tresvtintas.mobile.core.agent.AgentStepUpGrant;
import br.com.tresvtintas.mobile.core.network.MobileApiFactory;
import br.com.tresvtintas.mobile.core.network.NetworkConfiguration;
import java.io.IOException;
import java.util.Optional;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public final class RemoteAgentRepositoryTest {
    private static final String WIRE_ID = "__ID__";
    private static final String WIRE_ACTION = "__ACTION__";
    private static final String CONVERSATION_ID =
            "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa";
    private static final String IDEMPOTENCY_KEY =
            "bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb";
    private MockWebServer server;
    private RemoteAgentRepository repository;

    @Before
    public void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        repository = new RemoteAgentRepository(
                new AgentAccountScope(41, "b".repeat(64)),
                MobileApiFactory.createClient(
                                new NetworkConfiguration(
                                        "http://localhost:"
                                         + server.getPort()
                                                 + "/api/mobile/v1/",
                                        "0.27.0-agent-pdf",
                                        28,
                                        true),
                                () -> Optional.of("a".repeat(80)),
                                null)
                        .api());
    }

    @After
    public void tearDown() throws IOException {
        repository.close();
        server.shutdown();
    }

    @Test
    public void mapsConversationPageWithoutPersistingContent()
            throws Exception {
        server.enqueue(json(200, """
                {
                  "items": [{
                    "id": "__ID__",
                    "title": "Consulta de preços",
                    "status": "active",
                    "createdOnThisDevice": true,
                    "lastActivityAt": "2026-07-27T12:00:00Z",
                    "createdAt": "2026-07-27T11:00:00Z",
                    "updatedAt": "2026-07-27T12:00:00Z"
                  }],
                  "nextCursor": "cursor_1"
                }
                """.replace(WIRE_ID, CONVERSATION_ID)));

        AgentConversationPage page = repository.conversations(
                Optional.empty(),
                30);

        assertEquals(
                "One owned conversation must be mapped.",
                1,
                page.items().size());
        assertEquals(
                "Opaque conversation ID must be retained.",
                CONVERSATION_ID,
                page.items().get(0).id());
        assertEquals(
                "Server cursor must remain opaque.",
                Optional.of("cursor_1"),
                page.nextCursor());
        RecordedRequest request = server.takeRequest();
        assertEquals(
                "Only active personal conversations are requested.",
                "active",
                request.getRequestUrl().queryParameter("status"));
    }

    @Test
    public void createsConversationWithIdempotencyContract()
            throws Exception {
        server.enqueue(json(201, """
                {
                  "id": "__ID__",
                  "title": "Nova conversa",
                  "status": "active",
                  "createdOnThisDevice": true,
                  "lastActivityAt": "2026-07-27T12:00:00Z",
                  "createdAt": "2026-07-27T12:00:00Z",
                  "updatedAt": "2026-07-27T12:00:00Z"
                }
                """.replace(WIRE_ID, CONVERSATION_ID))
                .addHeader("X-Idempotency-Replayed", "false"));

        AgentConversation conversation =
                repository.createConversation(
                        "Nova conversa",
                        IDEMPOTENCY_KEY);

        assertTrue(
                "Created conversation must retain device provenance.",
                conversation.createdOnThisDevice());
        RecordedRequest request = server.takeRequest();
        assertEquals(
                "Create must use the supplied stable key.",
                IDEMPOTENCY_KEY,
                request.getHeader("Idempotency-Key"));
        assertFalse(
                "Create body must contain no role or user scope.",
                request.getBody().readUtf8().contains("userId"));
    }

    @Test
    public void mapsProtectedDocumentDescriptorWithoutReceivingUrl()
            throws Exception {
        server.enqueue(json(200, """
                {
                  "conversationId": "__ID__",
                  "items": [{
                    "id": 91,
                    "role": "assistant",
                    "content": "Seu orçamento está pronto.",
                    "documents": [{
                      "type": "material_quote_pdf",
                      "quoteId": 42,
                      "filename": "orcamento-000042-material.pdf"
                    }],
                    "actions": [],
                    "turnId": null,
                    "createdAt": "2026-07-27T12:00:00Z"
                  }],
                  "nextCursor": null
                }
                """.replace(WIRE_ID, CONVERSATION_ID)));

        AgentMessagePage page = repository.messages(
                CONVERSATION_ID,
                Optional.empty(),
                30);

        assertEquals(
                "One assistant message must be mapped.",
                1,
                page.items().size());
        assertEquals(
                "Only one protected document descriptor is allowed.",
                1,
                page.items().get(0).documents().size());
        assertEquals(
                "The material quote type must remain explicit.",
                AgentDocumentType.MATERIAL_QUOTE_PDF,
                page.items().get(0).documents().get(0).type());
        assertEquals(
                "The opaque business identifier must be retained.",
                42,
                page.items().get(0).documents().get(0).quoteId());
    }

    @Test
    public void rejectsDocumentDescriptorContainingServerUrl()
            throws Exception {
        server.enqueue(json(200, """
                {
                  "conversationId": "__ID__",
                  "items": [{
                    "id": 91,
                    "role": "assistant",
                    "content": "Seu orçamento está pronto.",
                    "documents": [{
                      "type": "material_quote_pdf",
                      "quoteId": 42,
                      "filename": "orcamento-000042-material.pdf",
                      "url": "https://example.invalid/public-token"
                    }],
                    "actions": [],
                    "turnId": null,
                    "createdAt": "2026-07-27T12:00:00Z"
                  }],
                  "nextCursor": null
                }
                """.replace(WIRE_ID, CONVERSATION_ID)));

        AgentException failure = assertThrows(
                "Unknown URL fields must fail closed.",
                AgentException.class,
                () -> repository.messages(
                        CONVERSATION_ID,
                        Optional.empty(),
                        30));

        assertEquals(
                "An expanded wire contract is a protocol failure.",
                AgentFailureKind.PROTOCOL,
                failure.kind());
    }

    @Test
    public void mapsAndConfirmsDeviceBoundActionWithStableKey()
            throws Exception {
        String actionId =
                "cccccccc-cccc-4ccc-8ccc-cccccccccccc";
        server.enqueue(json(200, """
                {
                  "conversationId": "__ID__",
                  "items": [{
                    "id": 92,
                    "role": "assistant",
                    "content": "Revise antes de enviar.",
                    "documents": [],
                    "actions": [{
                      "id": "__ACTION__",
                      "kind": "material_quote_send",
                      "status": "pending",
                      "title": "Enviar orçamento de material",
                      "summary": {
                        "quoteId": 42,
                        "quoteTitle": "Tinta da fachada",
                        "customerName": "Cliente 3V",
                        "total": "1299.90",
                        "targetStatus": "sent",
                        "pricingWillBeRevalidated": true
                      },
                      "result": null,
                      "expiresAt": "2026-07-27T12:10:00Z"
                    }],
                    "turnId": null,
                    "createdAt": "2026-07-27T12:00:00Z"
                  }],
                  "nextCursor": null
                }
                """.replace(WIRE_ID, CONVERSATION_ID)
                .replace(WIRE_ACTION, actionId)));

        AgentMessagePage page = repository.messages(
                CONVERSATION_ID,
                Optional.empty(),
                30);

        assertEquals(
                "Only one visual action may be attached.",
                1,
                page.items().get(0).actions().size());
        assertEquals(
                "Pending action status must be explicit.",
                AgentActionStatus.PENDING,
                page.items().get(0).actions().get(0).status());

        server.enqueue(json(200, """
                {
                  "id": "__ACTION__",
                  "kind": "material_quote_send",
                  "status": "executed",
                  "title": "Enviar orçamento de material",
                  "summary": {
                    "quoteId": 42,
                    "quoteTitle": "Tinta da fachada",
                    "customerName": "Cliente 3V",
                    "total": "1299.90",
                    "targetStatus": "sent",
                    "pricingWillBeRevalidated": true
                  },
                  "result": {
                    "quoteId": 42,
                    "status": "sent",
                    "revision": 2,
                    "pricingChanged": false
                  },
                  "expiresAt": "2026-07-27T12:10:00Z"
                }
                """.replace(WIRE_ACTION, actionId))
                .addHeader("X-Idempotency-Replayed", "false"));

        var result = repository.decideAction(
                actionId,
                AgentActionDecision.CONFIRM,
                IDEMPOTENCY_KEY);

        assertEquals(
                "The server terminal status must be retained.",
                AgentActionStatus.EXECUTED,
                result.action().status());
        assertFalse(
                "A first execution must not be marked as replayed.",
                result.replayed());
        RecordedRequest request = server.takeRequest();
        assertEquals(
                "The history request must precede the mutation.",
                "GET",
                request.getMethod());
        request = server.takeRequest();
        assertEquals(
                "The stable key must be forwarded unchanged.",
                IDEMPOTENCY_KEY,
                request.getHeader("Idempotency-Key"));
        assertEquals(
                "The decision path must contain only the opaque action ID.",
                "/api/mobile/v1/agent/actions/"
                        + actionId
                        + "/decision",
                request.getPath());
        assertEquals(
                "Confirmation literals must be exact.",
                "{\"decision\":\"confirm\","
                        + "\"confirmation\":\"CONFIRM_AGENT_ACTION\"}",
                request.getBody().readUtf8());
    }

    @Test
    public void mapsEveryReviewedCreationLineWithoutPrivatePayload()
            throws Exception {
        String actionId =
                "dddddddd-dddd-4ddd-8ddd-dddddddddddd";
        server.enqueue(json(200, """
                {
                  "conversationId": "__ID__",
                  "items": [{
                    "id": 93,
                    "role": "assistant",
                    "content": "Revise todos os itens.",
                    "documents": [],
                    "actions": [{
                      "id": "__ACTION__",
                      "kind": "material_quote_create",
                      "status": "pending",
                      "title": "Criar orçamento de material",
                      "summary": {
                        "quoteTitle": "Pintura interna",
                        "customerName": "Cliente 3V",
                        "notes": "Parede interna",
                        "validUntil": "2026-08-03",
                        "itemCount": 1,
                        "items": [{
                          "productId": 7,
                          "description": "Tinta Premium",
                          "quantity": "2.00",
                          "unit": "un",
                          "unitPrice": "100.00",
                          "total": "200.00"
                        }],
                        "subtotal": "200.00",
                        "total": "200.00",
                        "targetStatus": "draft",
                        "pricingWillBeRevalidated": true
                      },
                      "result": null,
                      "expiresAt": "2026-07-27T12:10:00Z"
                    }],
                    "turnId": null,
                    "createdAt": "2026-07-27T12:00:00Z"
                  }],
                  "nextCursor": null
                }
                """.replace(WIRE_ID, CONVERSATION_ID)
                .replace(WIRE_ACTION, actionId)));

        AgentMessagePage page = repository.messages(
                CONVERSATION_ID,
                Optional.empty(),
                30);
        var action = page.items().get(0).actions().get(0);
        var summary =
                (AgentMaterialQuoteCreateSummary) action.summary();

        assertEquals(
                "The creation kind must remain explicit.",
                AgentActionKind.MATERIAL_QUOTE_CREATE,
                action.kind());
        assertEquals(
                "Every reviewed item must survive transport.",
                1,
                summary.items().size());
        assertEquals(
                "The official product ID must remain available.",
                7,
                summary.items().get(0).productId());
    }

    @Test
    public void mapsTheCompleteAmendmentComparisonWithoutPrivatePayload()
            throws Exception {
        String actionId =
                "eeeeeeee-eeee-4eee-8eee-eeeeeeeeeeee";
        server.enqueue(json(200, """
                {
                  "conversationId": "__ID__",
                  "items": [{
                    "id": 94,
                    "role": "assistant",
                    "content": "Revise a alteração.",
                    "documents": [],
                    "actions": [{
                      "id": "__ACTION__",
                      "kind": "material_quote_amend",
                      "status": "pending",
                      "title": "Alterar orçamento de material",
                      "summary": {
                        "quoteId": 91,
                        "quoteTitle": "Pintura interna",
                        "customerName": "Cliente 3V",
                        "expectedRevision": 4,
                        "changes": [{
                          "operation": "update_quantity",
                          "productId": 7,
                          "description": "Tinta Premium",
                          "beforeQuantity": "2.00",
                          "afterQuantity": "3.00"
                        }],
                        "before": {
                          "itemCount": 1,
                          "items": [{
                            "productId": 7,
                            "description": "Tinta Premium",
                            "quantity": "2.00",
                            "unit": "un",
                            "unitPrice": "100.00",
                            "total": "200.00"
                          }],
                          "subtotal": "200.00",
                          "discount": "10.00",
                          "total": "190.00"
                        },
                        "after": {
                          "itemCount": 1,
                          "items": [{
                            "productId": 7,
                            "description": "Tinta Premium",
                            "quantity": "3.00",
                            "unit": "un",
                            "unitPrice": "100.00",
                            "total": "300.00"
                          }],
                          "subtotal": "300.00",
                          "discount": "10.00",
                          "total": "290.00"
                        },
                        "total": "290.00",
                        "pricingWillBeRevalidated": true
                      },
                      "result": null,
                      "expiresAt": "2026-07-27T12:10:00Z"
                    }],
                    "turnId": null,
                    "createdAt": "2026-07-27T12:00:00Z"
                  }],
                  "nextCursor": null
                }
                """.replace(WIRE_ID, CONVERSATION_ID)
                .replace(WIRE_ACTION, actionId)));

        AgentMessagePage page = repository.messages(
                CONVERSATION_ID,
                Optional.empty(),
                30);
        var action = page.items().get(0).actions().get(0);
        var summary =
                (AgentMaterialQuoteAmendSummary) action.summary();

        assertEquals(
                "The amendment kind must remain explicit.",
                AgentActionKind.MATERIAL_QUOTE_AMEND,
                action.kind());
        assertEquals(
                "The server revision must be bound to the review.",
                4,
                summary.expectedRevision());
        assertEquals(
                "The before and after quantities must both survive.",
                "3.00",
                summary.after().items().get(0)
                        .quantity()
                        .toPlainString());
    }

    @Test
    public void performsProtectedFinanceDecisionWithBoundStepUp()
            throws Exception {
        String actionId =
                "ffffffff-ffff-4fff-8fff-ffffffffffff";
        String challengeId =
                "dddddddd-dddd-4ddd-8ddd-dddddddddddd";
        String credential = "jwt-" + "x".repeat(80);
        String token = "3vsu1_" + "a".repeat(43);
        server.enqueue(json(201, """
                {
                  "challengeId": "__ID__",
                  "nonce": "3vn1_nnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnn",
                  "googleServerClientId": "473842962788-example.apps.googleusercontent.com",
                  "expiresAt": "2026-07-29T13:00:00Z"
                }
                """.replace(WIRE_ID, challengeId)));

        AgentStepUpChallenge challenge =
                repository.requestStepUp(actionId);

        assertEquals(
                "The challenge ID must remain bound to the action.",
                challengeId,
                challenge.id());
        RecordedRequest request = server.takeRequest();
        assertEquals(
                "Challenge creation must use the protected action path.",
                "/api/mobile/v1/agent/actions/"
                        + actionId
                        + "/step-up/challenge",
                request.getPath());
        assertEquals(
                "Challenge creation requires an explicit literal.",
                "{\"confirmation\":\"REQUEST_AGENT_ACTION_STEP_UP\"}",
                request.getBody().readUtf8());

        server.enqueue(json(200, """
                {
                  "stepUpToken": "__TOKEN__",
                  "expiresAt": "2026-07-29T13:00:30Z"
                }
                """.replace("__TOKEN__", token)));

        AgentStepUpGrant grant = repository.verifyStepUp(
                actionId,
                challengeId,
                credential);

        assertEquals(
                "The short-lived server grant must remain unchanged.",
                token,
                grant.token());
        request = server.takeRequest();
        assertEquals(
                "Google proof must use the matching action path.",
                "/api/mobile/v1/agent/actions/"
                        + actionId
                        + "/step-up/google",
                request.getPath());
        String verificationBody = request.getBody().readUtf8();
        assertTrue(
                "Verification must bind the challenge identifier.",
                verificationBody.contains(challengeId));
        assertTrue(
                "Verification must forward the explicit Google proof.",
                verificationBody.contains(credential));

        server.enqueue(json(200, """
                {
                  "id": "__ACTION__",
                  "kind": "personal_finance_create",
                  "status": "executed",
                  "title": "Criar lançamento financeiro pessoal",
                  "summary": {
                    "finance": {
                      "scope": "personal",
                      "operation": "create",
                      "entryId": null,
                      "organizationId": null,
                      "organizationName": null,
                      "customerId": 18,
                      "customerName": "Pintor de teste",
                      "type": "expense",
                      "title": "Combustível",
                      "amount": "80.00",
                      "currency": "BRL",
                      "dueAt": null,
                      "notes": "Visita técnica",
                      "beforeStatus": null,
                      "afterStatus": "pending",
                      "paymentMethod": null,
                      "paymentReference": null,
                      "accessAndStateWillBeRevalidated": true
                    }
                  },
                  "result": {
                    "finance": {
                      "scope": "personal",
                      "entryId": 31,
                      "status": "pending",
                      "changed": true
                    }
                  },
                  "requiresStepUp": true,
                  "expiresAt": "2026-07-29T13:00:00Z"
                }
                """.replace(WIRE_ACTION, actionId))
                .addHeader("X-Idempotency-Replayed", "false"));

        var decision = repository.decideAction(
                actionId,
                AgentActionDecision.CONFIRM,
                IDEMPOTENCY_KEY,
                Optional.of(token));

        assertEquals(
                "The protected result must retain its finance summary.",
                AgentFinanceSummary.class,
                decision.action().summary().getClass());
        assertEquals(
                "The protected result must retain its finance outcome.",
                AgentFinanceResult.class,
                decision.action().result().orElseThrow().getClass());
        request = server.takeRequest();
        String decisionBody = request.getBody().readUtf8();
        assertTrue(
                "Only the short-lived grant is attached to confirmation.",
                decisionBody.contains(
                        "\"stepUpToken\":\"" + token + "\""));
        assertFalse(
                "The original Google proof must not reach the mutation.",
                decisionBody.contains(credential));
    }

    private static MockResponse json(int code, String body) {
        return new MockResponse()
                .setResponseCode(code)
                .addHeader("Content-Type", "application/json")
                .setBody(body);
    }
}
