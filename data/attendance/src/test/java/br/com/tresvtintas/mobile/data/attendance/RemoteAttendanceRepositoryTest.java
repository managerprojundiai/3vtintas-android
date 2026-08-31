package br.com.tresvtintas.mobile.data.attendance;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import br.com.tresvtintas.mobile.core.attendance.AttendanceAssignment;
import br.com.tresvtintas.mobile.core.attendance.AttendanceAssigneePage;
import br.com.tresvtintas.mobile.core.attendance.AttendanceChannel;
import br.com.tresvtintas.mobile.core.attendance.AttendanceException;
import br.com.tresvtintas.mobile.core.attendance.AttendanceFailureKind;
import br.com.tresvtintas.mobile.core.attendance.AttendanceFolder;
import br.com.tresvtintas.mobile.core.attendance.AttendanceConversation;
import br.com.tresvtintas.mobile.core.attendance.AttendanceMessagePage;
import br.com.tresvtintas.mobile.core.attendance.AttendanceManagementResult;
import br.com.tresvtintas.mobile.core.attendance.AttendanceManagementSelection;
import br.com.tresvtintas.mobile.core.attendance.AttendancePage;
import br.com.tresvtintas.mobile.core.attendance.AttendancePriority;
import br.com.tresvtintas.mobile.core.attendance.AttendanceQuery;
import br.com.tresvtintas.mobile.core.attendance.AttendanceReplyDeliveryState;
import br.com.tresvtintas.mobile.core.attendance.AttendanceReplyResult;
import br.com.tresvtintas.mobile.core.attendance.AttendanceReadCursorResult;
import br.com.tresvtintas.mobile.core.network.MobileApiFactory;
import br.com.tresvtintas.mobile.core.network.NetworkConfiguration;
import java.io.IOException;
import java.util.Optional;
import java.util.OptionalLong;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public final class RemoteAttendanceRepositoryTest {
    private static final String CONVERSATION_ID = "whatsapp:1";
    private static final String CONTENT_TYPE_HEADER = "Content-Type";
    private static final String JSON_CONTENT_TYPE = "application/json";
    private static final String IDEMPOTENCY_KEY =
            "00000000-0000-4000-8000-000000000001";
    private static final String MESSAGE = "Mensagem";
    private static final String REPLAY_HEADER =
            "X-Idempotency-Replayed";
    private static final String QUEUED = "queued";
    private static final String SAFE_REPLY = "Resposta segura";
    private static final String ATTENDANCE_COLLECTION =
            "/api/mobile/v1/attendance/conversations";
    private static final String ORGANIZATION_FIELD = "organizationId";
    private static final String ROLE_FIELD = "role";
    private MockWebServer server;
    private RemoteAttendanceRepository repository;

    @Before
    public void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        repository = new RemoteAttendanceRepository(
                new AttendanceAccountScope(
                        21,
                        "b".repeat(64),
                        OptionalLong.of(9)),
                MobileApiFactory.create(
                        new NetworkConfiguration(
                                "http://localhost:"
                                        + server.getPort()
                                        + "/api/mobile/v1/",
                                "0.24.0-attendance-read-cursor",
                                24,
                                true),
                        () -> Optional.of("a".repeat(80))));
    }

    @After
    public void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    public void readsStrictAuthorizedPageAndSendsOnlyFilters()
            throws Exception {
        server.enqueue(json(validPage()));
        AttendanceQuery query = new AttendanceQuery(
                OptionalLong.of(9),
                Optional.of("Maria"),
                Optional.of(AttendanceChannel.WHATSAPP),
                Optional.of(AttendanceFolder.INBOX),
                Optional.of(AttendancePriority.HIGH),
                AttendanceAssignment.MINE,
                30);

        AttendancePage page = repository.page(
                query,
                Optional.of("opaque_1"));
        RecordedRequest request = server.takeRequest();

        assertEquals(
                "The page must contain the server-authorized conversation.",
                1,
                page.items().size());
        assertEquals(
                "The minimum customer display name must be mapped.",
                "Maria",
                page.items().get(0).customer()
                        .displayName()
                        .orElseThrow());
        assertEquals(
                "The opaque continuation must be preserved.",
                Optional.of("opaque_2"),
                page.nextCursor());
        assertEquals(
                "Only canonical filters may reach the endpoint.",
                ATTENDANCE_COLLECTION
                        + "?search=Maria&organizationId=9"
                        + "&channel=whatsapp&folder=inbox"
                        + "&priority=high&assignment=mine"
                        + "&cursor=opaque_1&limit=30",
                request.getPath());
        assertFalse(
                "The client must never send an authoritative role.",
                request.getPath().contains(ROLE_FIELD));
    }

    @Test
    public void revokedScopeCannotReadAgain() {
        repository.close();

        AttendanceException failure = assertThrows(
                AttendanceException.class,
                () -> repository.page(
                        AttendanceQuery.initial(
                                OptionalLong.of(9),
                                true),
                        Optional.empty()));

        assertEquals(
                "Closed scopes must fail as revoked.",
                AttendanceFailureKind.ACCESS_REVOKED,
                failure.kind());
        assertEquals(
                "Closed scopes must not issue a network request.",
                0,
                server.getRequestCount());
    }

    @Test
    public void readsDetailAndExternalMessagesWithoutSendingScope()
            throws Exception {
        server.enqueue(json(validConversation()));
        server.enqueue(json("""
                {"conversationId":"whatsapp:1","items":[
                {"id":"whatsapp:1:9","sourceId":"9",
                "direction":"inbound","type":"text",
                "content":"Preciso de tinta","truncated":false,
                "status":"received",
                "createdAt":"2026-07-27T12:00:00Z"}],
                "nextCursor":"older_1"}
                """));

        AttendanceConversation detail =
                repository.conversation(CONVERSATION_ID);
        RecordedRequest detailRequest = server.takeRequest();
        AttendanceMessagePage messages = repository.messagePage(
                CONVERSATION_ID,
                Optional.empty(),
                50);
        RecordedRequest messagesRequest = server.takeRequest();

        assertEquals(
                "The authorized detail ID must be preserved.",
                CONVERSATION_ID,
                detail.id());
        assertEquals(
                "The optimistic revision must be preserved.",
                5,
                detail.revision());
        assertEquals(
                "External message content must be mapped.",
                "Preciso de tinta",
                messages.items().get(0).content());
        assertEquals(
                "The detail request must address only the opaque ID.",
                ATTENDANCE_COLLECTION + "/whatsapp:1",
                detailRequest.getPath());
        assertEquals(
                "The message request must carry only cursor and limit.",
                ATTENDANCE_COLLECTION
                        + "/whatsapp:1/messages?limit=50",
                messagesRequest.getPath());
        assertFalse(
                "The message request must not send organization scope.",
                messagesRequest.getPath().contains(ORGANIZATION_FIELD));
        assertFalse(
                "The message request must not send an authoritative role.",
                messagesRequest.getPath().contains(ROLE_FIELD));
    }

    @Test
    public void invisibleDetailMapsToNotFoundWithoutLeakingBody()
            throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(404)
                .setHeader(
                        CONTENT_TYPE_HEADER,
                        "application/problem+json")
                .setBody("""
                        {"type":"https://3vtintas.com.br/problems/not-found",
                        "title":"Não encontrado","status":404,
                        "detail":"O atendimento solicitado não existe.",
                        "instance":"urn:3v:request:test",
                        "code":"NOT_FOUND",
                        "requestId":"00000000-0000-4000-8000-000000000099"}
                        """));

        AttendanceException failure = assertThrows(
                AttendanceException.class,
                () -> repository.conversation("whatsapp:999"));

        assertEquals(
                "Invisible resources must map to not found.",
                AttendanceFailureKind.NOT_FOUND,
                failure.kind());
    }

    @Test
    public void rejectsInternalMetadataAddedToAMessage() {
        server.enqueue(json("""
                {"conversationId":"whatsapp:1","items":[
                {"id":"whatsapp:1:9","sourceId":"9",
                "direction":"inbound","type":"text",
                "content":"Mensagem","truncated":false,
                "status":null,
                "createdAt":"2026-07-27T12:00:00Z",
                "metadata":{"private":true}}],
                "nextCursor":null}
                """));

        AttendanceException failure = assertThrows(
                AttendanceException.class,
                () -> repository.messagePage(
                        CONVERSATION_ID,
                        Optional.empty(),
                        50));

        assertEquals(
                "Unexpected internal metadata must be a protocol failure.",
                AttendanceFailureKind.PROTOCOL,
                failure.kind());
    }

    @Test
    public void rejectsBlankOptionalMessageStatus() {
        server.enqueue(json("""
                {"conversationId":"whatsapp:1","items":[
                {"id":"whatsapp:1:9","sourceId":"9",
                "direction":"inbound","type":"text",
                "content":"Mensagem","truncated":false,
                "status":" ",
                "createdAt":"2026-07-27T12:00:00Z"}],
                "nextCursor":null}
                """));

        AttendanceException failure = assertThrows(
                AttendanceException.class,
                () -> repository.messagePage(
                        CONVERSATION_ID,
                        Optional.empty(),
                        50));

        assertEquals(
                "Blank statuses must be rejected as protocol failures.",
                AttendanceFailureKind.PROTOCOL,
                failure.kind());
    }

    @Test
    public void rejectsUnexpectedOrMalformedPayload() {
        server.enqueue(json("""
                {"items":[{"id":"whatsapp:1","channel":"whatsapp",
                "sourceId":"1","revision":5,"organization":null,
                "customer":{"id":null,"displayName":"Cliente"},
                "assignedUser":null,"folder":"invalid",
                "priority":"normal","handlingMode":"ai",
                "state":"active","stats":{"inboundCount":1,
                "outboundCount":0,"unreadCount":1},
                "lastMessage":null,
                "activityAt":"2026-07-27T12:00:00Z",
                "updatedAt":"2026-07-27T12:00:00Z"}],
                "nextCursor":null}
                """));

        AttendanceException failure = assertThrows(
                AttendanceException.class,
                () -> repository.page(
                        AttendanceQuery.initial(
                                OptionalLong.empty(),
                                false),
                        Optional.empty()));

        assertEquals(
                "Malformed enums must fail as a protocol violation.",
                AttendanceFailureKind.PROTOCOL,
                failure.kind());
    }

    @Test
    public void postsIdempotentWhatsappReplyAndMapsQueuedState()
            throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(202)
                .setHeader(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE)
                .setHeader(REPLAY_HEADER, "false")
                .setBody(validReply(QUEUED)));

        AttendanceReplyResult result = repository.reply(
                CONVERSATION_ID,
                SAFE_REPLY,
                IDEMPOTENCY_KEY);
        RecordedRequest request = server.takeRequest();
        String body = request.getBody().readUtf8();

        assertEquals(
                "The opaque conversation ID must be the only path scope.",
                ATTENDANCE_COLLECTION
                        + "/whatsapp:1/messages",
                request.getPath());
        assertEquals(
                "Reply must use the write method.",
                "POST",
                request.getMethod());
        assertEquals(
                "The caller-generated key must reach the server unchanged.",
                IDEMPOTENCY_KEY,
                request.getHeader("Idempotency-Key"));
        assertTrue(
                "The normalized content must be serialized.",
                body.contains("\"content\":\"Resposta segura\""));
        assertTrue(
                "The literal confirmation must prove user intent.",
                body.contains(
                        "\"confirmation\":\"SEND_ATTENDANCE_REPLY\""));
        assertFalse(
                "The app must not claim an authoritative organization.",
                body.contains(ORGANIZATION_FIELD));
        assertFalse(
                "The app must not claim an authoritative role.",
                body.contains(ROLE_FIELD));
        assertEquals(
                "HTTP 202 must map to the durable delivery queue.",
                AttendanceReplyDeliveryState.QUEUED,
                result.deliveryState());
        assertFalse(
                "The first mutation must not be marked as replayed.",
                result.replayed());
    }

    @Test
    public void mapsSiteReplyAndStrictReplayHeader()
            throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(201)
                .setHeader(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE)
                .setHeader(REPLAY_HEADER, "true")
                .setBody(validReply("available")));

        AttendanceReplyResult result = repository.reply(
                CONVERSATION_ID,
                MESSAGE,
                IDEMPOTENCY_KEY);

        assertEquals(
                "HTTP 201 must map to immediately available.",
                AttendanceReplyDeliveryState.AVAILABLE,
                result.deliveryState());
        assertTrue(
                "The strict response header must expose replay status.",
                result.replayed());

        server.enqueue(new MockResponse()
                .setResponseCode(202)
                .setHeader(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE)
                .setBody(validReply(QUEUED)));
        AttendanceException failure = assertThrows(
                AttendanceException.class,
                () -> repository.reply(
                        CONVERSATION_ID,
                        MESSAGE,
                        "00000000-0000-4000-8000-000000000002"));
        assertEquals(
                "A missing replay marker is a protocol violation.",
                AttendanceFailureKind.PROTOCOL,
                failure.kind());
    }

    @Test
    public void mapsUnavailableChannelWithoutProviderDetails() {
        server.enqueue(new MockResponse()
                .setResponseCode(409)
                .setHeader(
                        CONTENT_TYPE_HEADER,
                        "application/problem+json")
                .setBody("""
                        {"type":"https://3vtintas.com.br/problems/conflict",
                        "title":"Conflito","status":409,
                        "detail":"O recurso não está disponível.",
                        "instance":"urn:3v:request:test",
                        "code":"RESOURCE_CONFLICT",
                        "requestId":"00000000-0000-4000-8000-000000000088"}
                        """));

        AttendanceException failure = assertThrows(
                AttendanceException.class,
                () -> repository.reply(
                        CONVERSATION_ID,
                        MESSAGE,
                        IDEMPOTENCY_KEY));

        assertEquals(
                "Unavailable channels must remain a retryable conflict.",
                AttendanceFailureKind.CONFLICT,
                failure.kind());
        assertEquals(
                "The safe support request ID must remain available.",
                Optional.of(
                        "00000000-0000-4000-8000-000000000088"),
                failure.requestId());
    }

    @Test
    public void rejectsDeliveryStateThatConflictsWithHttpStatus() {
        server.enqueue(new MockResponse()
                .setResponseCode(201)
                .setHeader(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE)
                .setHeader(REPLAY_HEADER, "false")
                .setBody(validReply(QUEUED)));

        AttendanceException failure = assertThrows(
                AttendanceException.class,
                () -> repository.reply(
                        CONVERSATION_ID,
                        MESSAGE,
                        IDEMPOTENCY_KEY));

        assertEquals(
                "Inconsistent success metadata must fail closed.",
                AttendanceFailureKind.PROTOCOL,
                failure.kind());
    }

    @Test
    public void readsOnlyEligibleAssigneeProjection() throws Exception {
        server.enqueue(json("""
                {"items":[
                {"id":21,"displayName":"Ana"},
                {"id":22,"displayName":"Bruno"}],
                "nextCursor":"next_1"}
                """));

        AttendanceAssigneePage page = repository.assignees(
                CONVERSATION_ID,
                Optional.of("An"),
                Optional.empty(),
                100);
        RecordedRequest request = server.takeRequest();

        assertEquals(
                "Only safe display identities may be mapped.",
                "Ana",
                page.items().get(0).displayName());
        assertEquals(
                "The opaque directory cursor must be retained.",
                Optional.of("next_1"),
                page.nextCursor());
        assertEquals(
                "Directory scope must come only from the conversation.",
                ATTENDANCE_COLLECTION
                        + "/whatsapp:1/assignees?search=An&limit=100",
                request.getPath());
        assertFalse(
                "The client must not send an organization claim.",
                request.getPath().contains(ORGANIZATION_FIELD));
        assertFalse(
                "The client must not send an authoritative role.",
                request.getPath().contains(ROLE_FIELD));
    }

    @Test
    public void patchesFullManagementStateWithRevisionAndReplay()
            throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE)
                .setHeader(REPLAY_HEADER, "false")
                .setBody("""
                        {"conversationId":"whatsapp:1","revision":6,
                        "folder":"resolved","priority":"urgent",
                        "assignedUser":null,
                        "updatedAt":"2026-07-27T13:00:00Z"}
                        """));
        AttendanceManagementSelection selection =
                new AttendanceManagementSelection(
                        AttendanceFolder.RESOLVED,
                        AttendancePriority.URGENT,
                        OptionalLong.empty());

        AttendanceManagementResult result = repository.manage(
                CONVERSATION_ID,
                5,
                selection,
                IDEMPOTENCY_KEY);
        RecordedRequest request = server.takeRequest();
        String body = request.getBody().readUtf8();

        assertEquals(
                "Management must target only the opaque conversation.",
                ATTENDANCE_COLLECTION + "/whatsapp:1",
                request.getPath());
        assertEquals(
                "Management must use PATCH.",
                "PATCH",
                request.getMethod());
        assertEquals(
                "The caller-generated key must be preserved.",
                IDEMPOTENCY_KEY,
                request.getHeader("Idempotency-Key"));
        assertTrue(
                "The displayed revision must be serialized.",
                body.contains("\"expectedRevision\":5"));
        assertTrue(
                "The selected folder must be serialized.",
                body.contains("\"folder\":\"resolved\""));
        assertTrue(
                "The selected priority must be serialized.",
                body.contains("\"priority\":\"urgent\""));
        assertTrue(
                "An explicit unassignment must be serialized as null.",
                body.contains("\"assignedToUserId\":null"));
        assertTrue(
                "The confirmation literal must prove user intent.",
                body.contains(
                        "\"confirmation\":"
                                + "\"UPDATE_ATTENDANCE_CONVERSATION\""));
        assertFalse(
                "The app must not claim an organization.",
                body.contains(ORGANIZATION_FIELD));
        assertFalse(
                "The app must not claim a role.",
                body.contains(ROLE_FIELD));
        assertEquals(
                "The authoritative revision must be mapped.",
                6,
                result.revision());
        assertEquals(
                "The authoritative folder must be mapped.",
                AttendanceFolder.RESOLVED,
                result.folder());
        assertTrue(
                "The authoritative unassignment must be mapped.",
                result.assignedUser().isEmpty());
        assertFalse(
                "The first result must not be marked replayed.",
                result.replayed());
    }

    @Test
    public void managementConflictKeepsTheSafeRequestId() {
        server.enqueue(new MockResponse()
                .setResponseCode(409)
                .setHeader(
                        CONTENT_TYPE_HEADER,
                        "application/problem+json")
                .setBody("""
                        {"type":"https://3vtintas.com.br/problems/conflict",
                        "title":"Conflito","status":409,
                        "detail":"O recurso foi alterado.",
                        "instance":"urn:3v:request:test",
                        "code":"RESOURCE_CONFLICT",
                        "requestId":"00000000-0000-4000-8000-000000000077"}
                        """));

        AttendanceException failure = assertThrows(
                AttendanceException.class,
                () -> repository.manage(
                        CONVERSATION_ID,
                        5,
                        new AttendanceManagementSelection(
                                AttendanceFolder.INBOX,
                                AttendancePriority.NORMAL,
                                OptionalLong.of(21)),
                        IDEMPOTENCY_KEY));

        assertEquals(
                "Stale revisions must remain conflicts.",
                AttendanceFailureKind.CONFLICT,
                failure.kind());
        assertEquals(
                "The safe support request ID must remain available.",
                Optional.of(
                        "00000000-0000-4000-8000-000000000077"),
                failure.requestId());
    }

    @Test
    public void marksOnlyTheDisplayedMessageAsRead() throws Exception {
        server.enqueue(json("""
                {"conversationId":"whatsapp:1",
                "readThroughMessageId":"whatsapp:1:92",
                "unreadCount":1,
                "updatedAt":"2026-07-27T13:01:00Z"}
                """));

        AttendanceReadCursorResult result = repository.markRead(
                CONVERSATION_ID,
                "whatsapp:1:92");
        RecordedRequest request = server.takeRequest();
        String body = request.getBody().readUtf8();

        assertEquals(
                "The cursor must target the protected conversation.",
                ATTENDANCE_COLLECTION
                        + "/whatsapp:1/read-cursor",
                request.getPath());
        assertEquals(
                "A monotonic read marker must use PUT.",
                "PUT",
                request.getMethod());
        assertTrue(
                "The last displayed public message must be explicit.",
                body.contains(
                        "\"throughMessageId\":\"whatsapp:1:92\""));
        assertTrue(
                "The read confirmation literal must be present.",
                body.contains(
                        "\"confirmation\":\"MARK_ATTENDANCE_READ\""));
        assertFalse(
                "The client must not claim an organization.",
                body.contains(ORGANIZATION_FIELD));
        assertFalse(
                "The client must not claim a role.",
                body.contains(ROLE_FIELD));
        assertEquals(
                "Messages that arrived later must remain unread.",
                1,
                result.unreadCount());
    }

    @Test
    public void rejectsCrossConversationReadMarkerBeforeNetwork() {
        AttendanceException failure = assertThrows(
                AttendanceException.class,
                () -> repository.markRead(
                        CONVERSATION_ID,
                        "whatsapp:2:92"));

        assertEquals(
                "Cross-conversation IDs must fail locally.",
                AttendanceFailureKind.INVALID_REQUEST,
                failure.kind());
        assertEquals(
                "No request may be made for a mismatched message.",
                0,
                server.getRequestCount());
    }

    private static MockResponse json(String body) {
        return new MockResponse()
                .setResponseCode(200)
                .setHeader(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE)
                .setBody(body);
    }

    private static String validPage() {
        return "{\"items\":["
                + validConversation()
                + "],\"nextCursor\":\"opaque_2\"}";
    }

    private static String validConversation() {
        return """
                {"id":"whatsapp:1","channel":"whatsapp",
                "sourceId":"1","revision":5,
                "organization":{"id":9,"name":"Loja Centro"},
                "customer":{"id":31,"displayName":"Maria"},
                "assignedUser":{"id":21,"name":"Carlos",
                "assignedToCurrentActor":true},
                "folder":"inbox","priority":"high",
                "handlingMode":"human","state":"active",
                "stats":{"inboundCount":4,"outboundCount":3,
                "unreadCount":1},
                "lastMessage":{"direction":"inbound","type":"text",
                "preview":"Preciso de ajuda","status":null,
                "createdAt":"2026-07-27T12:00:00Z"},
                "activityAt":"2026-07-27T12:00:00Z",
                "updatedAt":"2026-07-27T12:00:00Z"}
                """;
    }

    private static String validReply(String deliveryState) {
        return "{\"conversationId\":\"whatsapp:1\","
                + "\"message\":{\"id\":\"whatsapp:1:12\","
                + "\"sourceId\":\"12\",\"direction\":\"outbound\","
                + "\"type\":\"text\",\"content\":\"Resposta segura\","
                + "\"truncated\":false,\"status\":\"queued\","
                + "\"createdAt\":\"2026-07-27T12:01:00Z\"},"
                + "\"deliveryState\":\""
                + deliveryState
                + "\"}";
    }
}
