package br.com.tresvtintas.mobile.data.delivery;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import br.com.tresvtintas.mobile.core.delivery.DeliveryAction;
import br.com.tresvtintas.mobile.core.delivery.DeliveryException;
import br.com.tresvtintas.mobile.core.delivery.DeliveryFailureKind;
import br.com.tresvtintas.mobile.core.delivery.DeliveryMutationResult;
import br.com.tresvtintas.mobile.core.delivery.DeliveryPage;
import br.com.tresvtintas.mobile.core.delivery.DeliveryQuery;
import br.com.tresvtintas.mobile.core.delivery.DeliveryRoutePage;
import br.com.tresvtintas.mobile.core.delivery.DeliveryRouteStatus;
import br.com.tresvtintas.mobile.core.delivery.DeliveryStatus;
import br.com.tresvtintas.mobile.core.delivery.DeliveryView;
import br.com.tresvtintas.mobile.core.network.MobileApiFactory;
import br.com.tresvtintas.mobile.core.network.NetworkConfiguration;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.OptionalLong;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public final class RemoteDeliveryRepositoryTest {
    private static final String KEY =
            "00000000-0000-4000-8000-000000000801";
    private MockWebServer server;
    private RemoteDeliveryRepository repository;

    @Before
    public void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        repository = new RemoteDeliveryRepository(
                new DeliveryAccountScope(21, "b".repeat(64)),
                MobileApiFactory.create(
                        new NetworkConfiguration(
                                "http://localhost:"
                                        + server.getPort()
                                        + "/api/mobile/v1/",
                                "0.18.0-deliveries",
                                19,
                                true),
                        () -> Optional.of("a".repeat(80))));
    }

    @After
    public void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    public void readsAuthorizedPageWithSupportedFilters()
            throws Exception {
        server.enqueue(json(validPage()));

        DeliveryPage page = repository.page(
                new DeliveryQuery(
                        Optional.of("Maria"),
                        OptionalLong.of(9),
                        Optional.empty(),
                        DeliveryView.HISTORY,
                        Optional.empty(),
                        Optional.empty(),
                        30),
                Optional.of("opaque_1"));
        RecordedRequest request = server.takeRequest();

        assertEquals(
                "The authorized page must contain one delivery.",
                1,
                page.items().size());
        assertEquals(
                "Only server-authorized actions may reach the client.",
                java.util.Set.of(DeliveryAction.START),
                page.items().get(0).allowedActions());
        assertEquals(
                "The opaque cursor must be preserved.",
                Optional.of("opaque_2"),
                page.nextCursor());
        assertEquals(
                "Only supported filters may reach the endpoint.",
                "/api/mobile/v1/deliveries?search=Maria"
                        + "&organizationId=9&view=history"
                        + "&cursor=opaque_1&limit=30",
                request.getPath());
        assertFalse(
                "The app must never submit an authoritative role.",
                request.getPath().contains("role"));
    }

    @Test
    public void sendsExactCalendarWindowWithoutClientSideScopeFields()
            throws Exception {
        server.enqueue(json(validPage()));
        Instant from = Instant.parse("2026-07-26T03:00:00Z");
        Instant to = Instant.parse("2026-09-06T03:00:00Z");

        repository.page(
                DeliveryQuery.initial()
                        .forOrganization(OptionalLong.of(9))
                        .forAgendaWindow(from, to),
                Optional.empty());
        String path = server.takeRequest().getPath();

        assertTrue(
                "Calendar requests must use the bounded server view.",
                path.contains("view=calendar"));
        assertTrue(
                "Calendar requests must carry the exact lower bound.",
                path.contains("from=2026-07-26T03%3A00%3A00Z"));
        assertTrue(
                "Calendar requests must carry the exact exclusive upper bound.",
                path.contains("to=2026-09-06T03%3A00%3A00Z"));
        assertTrue(
                "Calendar requests may only narrow the granted organization.",
                path.contains("organizationId=9"));
        assertFalse(
                "The app must never submit an authoritative role.",
                path.contains("role"));
    }

    @Test
    public void sendsStrictIdempotentActionContracts()
            throws Exception {
        server.enqueue(actionJson(
                """
                {"action":"start","deliveryId":801,
                "deliveryStatus":"in_transit","orderId":701,
                "orderStatus":"in_progress","orderRevision":5,
                "changed":true}
                """,
                false));
        server.enqueue(actionJson(
                """
                {"action":"complete","deliveryId":801,
                "deliveryStatus":"delivered","orderId":701,
                "orderStatus":"delivered","orderRevision":6,
                "changed":true}
                """,
                true));

        DeliveryMutationResult started =
                repository.start(801, 4, KEY);
        DeliveryMutationResult completed =
                repository.complete(801, 5, KEY);
        RecordedRequest start = server.takeRequest();
        RecordedRequest complete = server.takeRequest();

        assertEquals(
                "Start must target its dedicated endpoint.",
                "/api/mobile/v1/deliveries/801/start",
                start.getPath());
        assertEquals(
                "Completion must target its dedicated endpoint.",
                "/api/mobile/v1/deliveries/801/completion",
                complete.getPath());
        assertEquals(
                "The stable key must be sent unchanged.",
                KEY,
                start.getHeader("Idempotency-Key"));
        assertEquals(
                "Start must contain only the expected revision.",
                "{\"expectedOrderRevision\":4,"
                        + "\"confirmation\":\"START_DELIVERY\"}",
                start.getBody().readUtf8());
        assertEquals(
                "Completion must contain only the expected revision.",
                "{\"expectedOrderRevision\":5,"
                        + "\"confirmation\":\"COMPLETE_DELIVERY\"}",
                complete.getBody().readUtf8());
        assertEquals(
                "Start status must remain authoritative.",
                DeliveryStatus.IN_TRANSIT,
                started.deliveryStatus());
        assertTrue(
                "A replay must be exposed to the UI.",
                completed.replayed());
    }

    @Test
    public void rejectsMutationWithoutReplayHeader() {
        server.enqueue(json("""
                {"action":"start","deliveryId":801,
                "deliveryStatus":"in_transit","orderId":701,
                "orderStatus":"in_progress","orderRevision":5,
                "changed":true}
                """));

        DeliveryException failure = assertThrows(
                DeliveryException.class,
                () -> repository.start(801, 4, KEY));

        assertEquals(
                "Missing idempotency evidence must fail closed.",
                DeliveryFailureKind.PROTOCOL,
                failure.kind());
    }

    @Test
    public void revokedScopeCannotIssueAnotherRequest() {
        repository.close();

        DeliveryException failure = assertThrows(
                DeliveryException.class,
                () -> repository.page(
                        DeliveryQuery.initial(),
                        Optional.empty()));

        assertEquals(
                "A closed scope must fail as revoked.",
                DeliveryFailureKind.ACCESS_REVOKED,
                failure.kind());
        assertEquals(
                "A revoked scope must not issue a request.",
                0,
                server.getRequestCount());
    }

    @Test
    public void readsOnlyTheOwnServerProjectedItinerary() throws Exception {
        server.enqueue(json(validRoutePage()));

        DeliveryRoutePage page = repository.routes(LocalDate.of(2026, 8, 3));
        RecordedRequest request = server.takeRequest();

        assertEquals(
                "The confirmed itinerary must be mapped.",
                DeliveryRouteStatus.CONFIRMED,
                page.current().orElseThrow().status());
        assertEquals(
                "The explicit service date is the only client scope input.",
                "/api/mobile/v1/delivery-routes?serviceDate=2026-08-03",
                request.getPath());
        assertFalse(
                "The app must not submit an authoritative driver or role.",
                request.getPath().contains("driver"));
        assertEquals(
                "The next unfinished stop must remain in provider order.",
                802L,
                page.current().orElseThrow().nextStop().orElseThrow().deliveryId());
    }

    @Test
    public void rejectsProviderInternalsInThePublicRouteEnvelope() {
        server.enqueue(json(validRoutePage().replace(
                "\"status\":\"confirmed\"",
                "\"status\":\"confirmed\",\"provider\":\"internal\"")));

        DeliveryException failure = assertThrows(
                DeliveryException.class,
                () -> repository.routes(LocalDate.of(2026, 8, 3)));

        assertEquals(
                "Unexpected provider internals must fail as a protocol violation.",
                DeliveryFailureKind.PROTOCOL,
                failure.kind());
    }

    private static MockResponse json(String body) {
        return new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(body);
    }

    private static MockResponse actionJson(
            String body,
            boolean replayed) {
        return json(body).setHeader(
                "X-Idempotency-Replayed",
                Boolean.toString(replayed));
    }

    private static String validPage() {
        return """
                {"items":[{"id":801,"status":"pending",
                "allowedActions":["start"],
                "order":{"id":701,"status":"confirmed",
                "revision":4,"itemCount":2},
                "organization":{"id":9,"name":"Loja Centro"},
                "customer":{"id":31,"name":"Maria",
                "city":"Goiânia","state":"GO"},
                "assignedDriver":{"userId":21,"name":"Carlos",
                "assignedToCurrentActor":true},
                "scheduledAt":"2026-07-26T12:00:00.000Z",
                "deliveredAt":null,
                "updatedAt":"2026-07-25T13:00:00.000Z"}],
                "nextCursor":"opaque_2"}
                """;
    }

    private static String validRoutePage() {
        return """
                {"items":[{"routeKey":"00000000-0000-4000-8000-000000000901",
                "organization":{"id":2,"name":"3V Tintas Jundiaí"},
                "driver":{"userId":21,"name":"Yasmin"},
                "serviceDate":"2026-08-03","startsAt":"2026-08-03T11:00:00.000Z",
                "status":"confirmed","returnToOrigin":true,
                "origin":{"address":"Loja Jundiaí","coordinate":{
                "latitudeE7":-231000000,"longitudeE7":-469000000}},
                "destination":null,"totalDistanceMeters":12300,
                "totalTravelDurationSeconds":2100,"revision":2,
                "expiresAt":"2026-08-03T12:00:00.000Z",
                "confirmedAt":"2026-08-03T10:00:00.000Z","cancelledAt":null,
                "stops":[{"deliveryId":801,"orderId":701,
                "orderRevisionSnapshot":3,"position":1,"status":"completed",
                "recipientName":"Cliente A","normalizedAddress":"Rua A, 1",
                "coordinate":{"latitudeE7":-231000001,"longitudeE7":-469000001},
                "estimatedArrivalAt":"2026-08-03T11:30:00.000Z",
                "travelDistanceMeters":5000,"travelDurationSeconds":900,
                "serviceDurationSeconds":600,"revision":1},
                {"deliveryId":802,"orderId":702,"orderRevisionSnapshot":4,
                "position":2,"status":"planned","recipientName":"Cliente B",
                "normalizedAddress":"Rua B, 2","coordinate":{
                "latitudeE7":-231000002,"longitudeE7":-469000002},
                "estimatedArrivalAt":"2026-08-03T12:00:00.000Z",
                "travelDistanceMeters":7300,"travelDurationSeconds":1200,
                "serviceDurationSeconds":600,"revision":1}]}]}
                """;
    }
}
