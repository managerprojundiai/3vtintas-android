package br.com.tresvtintas.mobile.data.delivery;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import br.com.tresvtintas.mobile.core.delivery.DeliveryException;
import br.com.tresvtintas.mobile.core.delivery.DeliveryFailureKind;
import br.com.tresvtintas.mobile.core.delivery.DeliveryManagementAction;
import br.com.tresvtintas.mobile.core.delivery.DeliveryManagementMutationResult;
import br.com.tresvtintas.mobile.core.delivery.DeliveryManagementPage;
import br.com.tresvtintas.mobile.core.delivery.DeliveryManagementQuery;
import br.com.tresvtintas.mobile.core.delivery.DeliveryManagementView;
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

public final class RemoteDeliveryManagementRepositoryTest {
    private static final String KEY =
            "00000000-0000-4000-8000-000000000904";
    private MockWebServer server;
    private RemoteDeliveryManagementRepository repository;

    @Before
    public void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        repository = new RemoteDeliveryManagementRepository(
                new DeliveryAccountScope(21, "b".repeat(64)),
                MobileApiFactory.create(
                        new NetworkConfiguration(
                                "http://localhost:"
                                        + server.getPort()
                                        + "/api/mobile/v1/",
                                "0.19.0-delivery-management",
                                20,
                                true),
                        () -> Optional.of("a".repeat(80))));
    }

    @After
    public void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    public void readsOnlyTheRequestedOrganizationAndOpaquePage()
            throws Exception {
        server.enqueue(json(validPage()));

        DeliveryManagementPage page = repository.page(
                new DeliveryManagementQuery(
                        7,
                        Optional.of("Maria"),
                        DeliveryManagementView.SCHEDULED,
                        30),
                Optional.of("opaque_1"));
        RecordedRequest request = server.takeRequest();

        assertEquals(
                "The authorized page must contain one order.",
                1,
                page.items().size());
        assertEquals(
                "Only server-authorized actions may reach the client.",
                java.util.Set.of(DeliveryManagementAction.ASSIGN),
                page.items().get(0).allowedActions());
        assertEquals(
                "The opaque cursor must be preserved.",
                Optional.of("opaque_2"),
                page.nextCursor());
        assertEquals(
                "Only supported filters may reach the endpoint.",
                "/api/mobile/v1/delivery-management/orders"
                        + "?organizationId=7&search=Maria"
                        + "&view=scheduled&cursor=opaque_1&limit=30",
                request.getPath());
        assertFalse(
                "The client must never send an authoritative role.",
                request.getPath().contains("role"));
    }

    @Test
    public void sendsStrictIdempotentManagementCommands()
            throws Exception {
        server.enqueue(actionJson(
                mutationJson("schedule", 4, 801, null),
                false));
        server.enqueue(actionJson(
                mutationJson("assign", 5, 801, 21),
                false));
        server.enqueue(actionJson(
                mutationJson("unassign", 6, 801, null),
                true));
        server.enqueue(actionJson(
                mutationJson("complete", 7, 801, null),
                false));

        DeliveryManagementMutationResult scheduled = repository.schedule(
                7,
                501,
                3,
                Instant.parse("2026-07-27T12:00:00Z"),
                90,
                KEY);
        repository.assign(7, 501, 4, OptionalLong.of(21), KEY);
        DeliveryManagementMutationResult unassigned = repository.assign(
                7,
                501,
                5,
                OptionalLong.empty(),
                KEY);
        repository.complete(7, 501, 6, KEY);

        RecordedRequest schedule = server.takeRequest();
        RecordedRequest assign = server.takeRequest();
        RecordedRequest unassign = server.takeRequest();
        RecordedRequest complete = server.takeRequest();

        assertEquals(
                "Schedule must target its dedicated endpoint.",
                "/api/mobile/v1/delivery-management/orders/501/schedule",
                schedule.getPath());
        assertEquals(
                "Assignment must target its dedicated endpoint.",
                "/api/mobile/v1/delivery-management/orders/501"
                        + "/driver-assignment",
                assign.getPath());
        assertEquals(
                "The stable key must be sent unchanged.",
                KEY,
                schedule.getHeader("Idempotency-Key"));
        assertEquals(
                "Schedule must contain the exact scoped contract.",
                "{\"organizationId\":7,\"expectedOrderRevision\":3,"
                        + "\"scheduledAt\":\"2026-07-27T12:00:00Z\","
                        + "\"duration\":90,"
                        + "\"location\":null,\"notes\":null,\"title\":null,"
                        + "\"confirmation\":\"SCHEDULE_DELIVERY\"}",
                schedule.getBody().readUtf8());
        assertEquals(
                "Assignment must contain the exact scoped contract.",
                "{\"organizationId\":7,\"expectedOrderRevision\":4,"
                        + "\"driverUserId\":21,"
                        + "\"confirmation\":\"ASSIGN_DELIVERY_DRIVER\"}",
                assign.getBody().readUtf8());
        assertEquals(
                "Unassignment must preserve an explicit null driver.",
                "{\"organizationId\":7,\"expectedOrderRevision\":5,"
                        + "\"driverUserId\":null,"
                        + "\"confirmation\":\"UNASSIGN_DELIVERY_DRIVER\"}",
                unassign.getBody().readUtf8());
        assertEquals(
                "Completion must contain the exact scoped contract.",
                "{\"organizationId\":7,\"expectedOrderRevision\":6,"
                        + "\"confirmation\":"
                        + "\"COMPLETE_DELIVERY_MANAGEMENT\"}",
                complete.getBody().readUtf8());
        assertEquals(
                "The authoritative revision must reach the UI.",
                4,
                scheduled.orderRevision());
        assertTrue(
                "A replay must be exposed to the UI.",
                unassigned.replayed());
    }

    @Test
    public void rejectsManagementMutationWithoutReplayEvidence() {
        server.enqueue(json(mutationJson("schedule", 4, 801, null)));

        DeliveryException failure = assertThrows(
                "Missing idempotency evidence must fail closed.",
                DeliveryException.class,
                () -> repository.schedule(
                        7,
                        501,
                        3,
                        Instant.parse("2026-07-27T12:00:00Z"),
                        90,
                        KEY));

        assertEquals(
                "Missing replay evidence is a protocol failure.",
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

    private static String mutationJson(
            String action,
            int revision,
            long deliveryId,
            Integer driverUserId) {
        String driver = driverUserId == null
                ? "null"
                : driverUserId.toString();
        return "{\"action\":\"" + action
                + "\",\"orderId\":501,\"deliveryId\":" + deliveryId
                + ",\"orderStatus\":\"confirmed\",\"orderRevision\":"
                + revision
                + ",\"scheduledAt\":\"2026-07-27T12:00:00Z\","
                + "\"assignedDriverUserId\":" + driver
                + ",\"changed\":true}";
    }

    private static String validPage() {
        return """
                {"items":[{"order":{"id":501,"status":"confirmed",
                "revision":3,"itemCount":2,
                "createdAt":"2026-07-25T12:00:00Z",
                "updatedAt":"2026-07-26T12:00:00Z"},
                "organization":{"id":7,"name":"Loja Centro"},
                "customer":{"id":31,"name":"Maria",
                "city":"Goiânia","state":"GO"},
                "delivery":{"id":801,"status":"pending",
                "scheduledAt":"2026-07-27T12:00:00Z",
                "assignedDriver":null},
                "allowedActions":["assign"]}],
                "nextCursor":"opaque_2"}
                """;
    }
}
