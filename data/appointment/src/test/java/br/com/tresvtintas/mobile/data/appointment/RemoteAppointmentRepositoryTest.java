package br.com.tresvtintas.mobile.data.appointment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import br.com.tresvtintas.mobile.core.appointment.AppointmentDraft;
import br.com.tresvtintas.mobile.core.appointment.AppointmentException;
import br.com.tresvtintas.mobile.core.appointment.AppointmentFailureKind;
import br.com.tresvtintas.mobile.core.appointment.AppointmentKind;
import br.com.tresvtintas.mobile.core.appointment.AppointmentMutationResult;
import br.com.tresvtintas.mobile.core.appointment.AppointmentPage;
import br.com.tresvtintas.mobile.core.appointment.AppointmentQuery;
import br.com.tresvtintas.mobile.core.appointment.AppointmentScope;
import br.com.tresvtintas.mobile.core.network.MobileApiFactory;
import br.com.tresvtintas.mobile.core.network.NetworkConfiguration;
import java.io.IOException;
import java.time.Instant;
import java.util.Optional;
import java.util.OptionalLong;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public final class RemoteAppointmentRepositoryTest {
    private static final String CONTENT_TYPE = "Content-Type";
    private MockWebServer server;
    private RemoteAppointmentRepository repository;

    @Before
    public void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        repository = new RemoteAppointmentRepository(
                new AppointmentAccountScope(
                        41,
                        "d".repeat(64),
                        AppointmentScope.SELF),
                MobileApiFactory.create(
                        new NetworkConfiguration(
                                "http://localhost:" + server.getPort()
                                        + "/api/mobile/v1/",
                                "0.16.0-agenda",
                                17,
                                true),
                        () -> Optional.of("a".repeat(80))));
    }

    @After
    public void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    public void readsBoundedOwnedPageAndMapsServerActions()
            throws Exception {
        server.enqueue(json(200, validPage()));
        AppointmentQuery query = AppointmentQuery.around(
                        AppointmentScope.SELF,
                        Instant.parse("2026-07-26T12:00:00Z"))
                .withSearch("  Cliente A ")
                .withOrganization(OptionalLong.of(7));

        AppointmentPage page = repository.page(
                query,
                Optional.of("opaque-2"));
        RecordedRequest request = server.takeRequest();

        assertEquals(
                "Authorized page must map its single row.",
                1,
                page.items().size());
        assertEquals(
                "Allowed actions must remain server authored.",
                4,
                page.items().get(0).allowedActions().size());
        assertEquals(
                "Search normalization must be preserved.",
                "cliente a",
                request.getRequestUrl().queryParameter("search"));
        assertEquals(
                "Organization filter must be explicit.",
                "7",
                request.getRequestUrl().queryParameter("organizationId"));
        assertEquals(
                "Opaque cursor must remain opaque.",
                "opaque-2",
                request.getRequestUrl().queryParameter("cursor"));
        assertFalse(
                "Client must never supply an actor user identifier.",
                request.getPath().contains("actorUserId"));
    }

    @Test
    public void createsConfirmedIdempotentAppointment()
            throws Exception {
        server.enqueue(json(201, """
                {
                  "appointmentId":501,
                  "status":"scheduled",
                  "revision":1,
                  "changed":true
                }
                """).setHeader("X-Idempotency-Replayed", "false"));
        AppointmentDraft draft = new AppointmentDraft(
                AppointmentScope.SELF,
                OptionalLong.empty(),
                OptionalLong.of(7),
                AppointmentKind.GENERAL,
                "Visita técnica",
                Optional.of("Levantamento"),
                Instant.parse("2026-08-01T13:00:00Z"),
                60,
                Optional.of("Loja Centro"),
                OptionalLong.of(91));

        AppointmentMutationResult result = repository.create(
                draft,
                "00000000-0000-4000-8000-000000000701");
        RecordedRequest request = server.takeRequest();
        String body = request.getBody().readUtf8();

        assertTrue(
                "Creation must expose the authoritative change.",
                result.changed());
        assertFalse(
                "Fresh creation cannot be a replay.",
                result.replayed());
        assertEquals(
                "Idempotency key must be sent unchanged.",
                "00000000-0000-4000-8000-000000000701",
                request.getHeader("Idempotency-Key"));
        assertTrue(
                "Creation requires literal user-intent confirmation.",
                body.contains("\"confirmation\":\"CREATE_APPOINTMENT\""));
        assertTrue(
                "Self scope must explicitly serialize no foreign responsible.",
                body.contains("\"responsibleUserId\":null"));
    }

    @Test
    public void mapsConflictAndPreservesSupportCorrelation() {
        String requestId = "00000000-0000-4000-8000-000000000799";
        server.enqueue(new MockResponse()
                .setResponseCode(409)
                .setHeader(CONTENT_TYPE, "application/problem+json")
                .setBody(problemJson(requestId)));

        AppointmentException failure = assertThrows(
                AppointmentException.class,
                () -> repository.detail(501, AppointmentScope.SELF));

        assertEquals(
                "Schedule or revision conflict must remain typed.",
                AppointmentFailureKind.CONFLICT,
                failure.kind());
        assertEquals(
                "Support correlation must reach the caller.",
                requestId,
                failure.requestId().orElseThrow());
    }

    @Test
    public void rejectsMismatchedOrClosedAccountWithoutNetwork() {
        AppointmentException mismatch = assertThrows(
                AppointmentException.class,
                () -> repository.detail(501, AppointmentScope.ALL));
        repository.close();
        AppointmentException closed = assertThrows(
                AppointmentException.class,
                () -> repository.page(
                        AppointmentQuery.around(
                                AppointmentScope.SELF,
                                Instant.parse("2026-07-26T12:00:00Z")),
                        Optional.empty()));

        assertEquals(
                "Mismatched scope must fail closed.",
                AppointmentFailureKind.ACCESS_REVOKED,
                mismatch.kind());
        assertEquals(
                "Closed repository must remain revoked.",
                AppointmentFailureKind.ACCESS_REVOKED,
                closed.kind());
        assertEquals(
                "Fail-closed checks cannot issue network requests.",
                0,
                server.getRequestCount());
    }

    private static MockResponse json(int status, String body) {
        return new MockResponse()
                .setResponseCode(status)
                .setHeader(CONTENT_TYPE, "application/json")
                .setBody(body);
    }

    private static String validPage() {
        return """
                {
                  "items":[{
                    "id":501,
                    "kind":"general",
                    "status":"scheduled",
                    "title":"Visita técnica",
                    "scheduledAt":"2026-08-01T13:00:00.000Z",
                    "durationMinutes":60,
                    "location":"Loja Centro",
                    "responsible":{
                      "id":41,
                      "name":"Vendedor",
                      "role":"salesperson"
                    },
                    "organization":{"id":7,"name":"Centro"},
                    "customer":{"id":91,"name":"Cliente A"},
                    "order":null,
                    "revision":1,
                    "allowedActions":[
                      "update","confirm","complete","cancel"
                    ],
                    "createdAt":"2026-07-26T12:00:00.000Z",
                    "updatedAt":"2026-07-26T12:00:00.000Z"
                  }],
                  "overview":{
                    "scheduled":1,
                    "confirmed":0,
                    "completed":0,
                    "cancelled":0
                  },
                  "nextCursor":"opaque-3"
                }
                """;
    }

    private static String problemJson(String requestId) {
        return ("{\"type\":\"https://www.3vtintas.com.br/problems/conflict\","
                + "\"title\":\"Conflict\",\"status\":409,"
                + "\"detail\":\"Resource conflict.\","
                + "\"instance\":\"urn:3v:request:%s\","
                + "\"code\":\"RESOURCE_CONFLICT\",\"requestId\":\"%s\"}")
                .formatted(requestId, requestId);
    }
}
