package br.com.tresvtintas.mobile.core.attendance;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;

public final class AttendanceDetailControllerTest {
    private static final String CONVERSATION_ID = "whatsapp:1";
    private static final String MESSAGE_ID_THREE = "whatsapp:1:3";
    private static final String LIST_NOT_USED = "List is not used.";

    @Test
    public void opensAuthorizedDetailAndPaginatesOlderMessages()
            throws Exception {
        Queue<AttendanceMessagePage> pages = new ArrayDeque<>();
        pages.add(new AttendanceMessagePage(
                CONVERSATION_ID,
                List.of(message(MESSAGE_ID_THREE)),
                Optional.of("opaque_1")));
        pages.add(new AttendanceMessagePage(
                CONVERSATION_ID,
                List.of(message("whatsapp:1:2")),
                Optional.empty()));
        AttendanceRepository repository = repository(
                conversation(CONVERSATION_ID),
                pages);
        AttendanceDetailController controller =
                new AttendanceDetailController(
                        repository,
                        Runnable::run,
                        Runnable::run);
        Queue<AttendanceDetailState> states = new ArrayDeque<>();
        controller.subscribe(states::add);

        controller.open(CONVERSATION_ID);
        controller.loadMore();

        AttendanceDetailState latest = latest(states);
        assertEquals(
                "Both authorized pages must remain in volatile state.",
                2,
                latest.snapshot().orElseThrow().messages().size());
        assertFalse(
                "The last page must clear continuation.",
                latest.snapshot().orElseThrow().hasMore());
        assertTrue(
                "All planned pages must be consumed.",
                pages.isEmpty());
    }

    @Test
    public void transientRefreshKeepsPreviouslyAuthorizedMessages() {
        int[] calls = {0};
        AttendanceConversation detail = conversation(CONVERSATION_ID);
        AttendanceRepository repository = new AttendanceRepository() {
            @Override
            public AttendancePage page(
                    AttendanceQuery query,
                    Optional<String> cursor) {
                throw new AssertionError(LIST_NOT_USED);
            }

            @Override
            public AttendanceConversation conversation(
                    String conversationId) throws AttendanceException {
                if (calls[0] > 0) {
                    throw new AttendanceException(
                            AttendanceFailureKind.NETWORK,
                            "offline");
                }
                return detail;
            }

            @Override
            public AttendanceMessagePage messagePage(
                    String conversationId,
                    Optional<String> cursor,
                    int limit) {
                calls[0] += 1;
                return new AttendanceMessagePage(
                        conversationId,
                        List.of(message(MESSAGE_ID_THREE)),
                        Optional.empty());
            }

            @Override
            public AttendanceReadCursorResult markRead(
                    String conversationId,
                    String throughMessageId) {
                return readResult(conversationId, throughMessageId);
            }
        };
        AttendanceDetailController controller =
                new AttendanceDetailController(
                        repository,
                        Runnable::run,
                        Runnable::run);
        Queue<AttendanceDetailState> states = new ArrayDeque<>();
        controller.subscribe(states::add);

        controller.open(CONVERSATION_ID);
        controller.refresh();

        AttendanceDetailState latest = latest(states);
        assertEquals(
                "Transient refresh failure must retain ready content.",
                AttendanceDetailState.Phase.READY,
                latest.phase());
        assertEquals(
                "The transient failure must remain visible.",
                AttendanceFailureKind.NETWORK,
                latest.failure().orElseThrow());
        assertEquals(
                "Previously authorized messages must remain in memory.",
                1,
                latest.snapshot().orElseThrow().messages().size());
    }

    @Test
    public void invisibleConversationFailsClosedWithoutSnapshot() {
        AttendanceRepository repository = new AttendanceRepository() {
            @Override
            public AttendancePage page(
                    AttendanceQuery query,
                    Optional<String> cursor) {
                throw new AssertionError(LIST_NOT_USED);
            }

            @Override
            public AttendanceConversation conversation(
                    String conversationId) throws AttendanceException {
                throw new AttendanceException(
                        AttendanceFailureKind.NOT_FOUND,
                        "not found");
            }
        };
        AttendanceDetailController controller =
                new AttendanceDetailController(
                        repository,
                        Runnable::run,
                        Runnable::run);
        Queue<AttendanceDetailState> states = new ArrayDeque<>();
        controller.subscribe(states::add);

        controller.open("site_chat:invisible");

        AttendanceDetailState latest = latest(states);
        assertEquals(
                "Invisible resources must end in the error phase.",
                AttendanceDetailState.Phase.ERROR,
                latest.phase());
        assertEquals(
                "Invisible resources must retain the not-found category.",
                AttendanceFailureKind.NOT_FOUND,
                latest.failure().orElseThrow());
        assertTrue(
                "Invisible resources must not retain a snapshot.",
                latest.snapshot().isEmpty());
    }

    @Test
    public void marksThroughTheNewestDisplayedMessageAndUpdatesUnread()
            throws Exception {
        AtomicReference<String> marked = new AtomicReference<>();
        AttendanceRepository repository = new AttendanceRepository() {
            @Override
            public AttendancePage page(
                    AttendanceQuery query,
                    Optional<String> cursor) {
                throw new AssertionError(LIST_NOT_USED);
            }

            @Override
            public AttendanceConversation conversation(
                    String conversationId) {
                return AttendanceDetailControllerTest.conversation(
                        conversationId);
            }

            @Override
            public AttendanceMessagePage messagePage(
                    String conversationId,
                    Optional<String> cursor,
                    int limit) {
                return new AttendanceMessagePage(
                        conversationId,
                        List.of(
                                message(MESSAGE_ID_THREE),
                                message("whatsapp:1:4")),
                        Optional.empty());
            }

            @Override
            public AttendanceReadCursorResult markRead(
                    String conversationId,
                    String throughMessageId) {
                marked.set(throughMessageId);
                return new AttendanceReadCursorResult(
                        conversationId,
                        throughMessageId,
                        0,
                        Instant.parse("2026-07-27T12:01:00Z"));
            }
        };
        AttendanceDetailController controller =
                new AttendanceDetailController(
                        repository,
                        Runnable::run,
                        Runnable::run);
        Queue<AttendanceDetailState> states = new ArrayDeque<>();
        controller.subscribe(states::add);

        controller.open(CONVERSATION_ID);

        AttendanceDetailState latest = latest(states);
        assertEquals(
                "The greatest displayed message ID must define the cursor.",
                "whatsapp:1:4",
                marked.get());
        assertEquals(
                "The authoritative unread count must update the snapshot.",
                0,
                latest.snapshot().orElseThrow()
                        .conversation().stats().unreadCount());
    }

    @Test
    public void transientReadMarkerFailureKeepsFreshAuthorizedContent() {
        AttendanceRepository repository = new AttendanceRepository() {
            @Override
            public AttendancePage page(
                    AttendanceQuery query,
                    Optional<String> cursor) {
                throw new AssertionError(LIST_NOT_USED);
            }

            @Override
            public AttendanceConversation conversation(
                    String conversationId) {
                return AttendanceDetailControllerTest.conversation(
                        conversationId);
            }

            @Override
            public AttendanceMessagePage messagePage(
                    String conversationId,
                    Optional<String> cursor,
                    int limit) {
                return new AttendanceMessagePage(
                        conversationId,
                        List.of(message(MESSAGE_ID_THREE)),
                        Optional.empty());
            }

            @Override
            public AttendanceReadCursorResult markRead(
                    String conversationId,
                    String throughMessageId)
                    throws AttendanceException {
                throw new AttendanceException(
                        AttendanceFailureKind.NETWORK,
                        "offline");
            }
        };
        AttendanceDetailController controller =
                new AttendanceDetailController(
                        repository,
                        Runnable::run,
                        Runnable::run);
        Queue<AttendanceDetailState> states = new ArrayDeque<>();
        controller.subscribe(states::add);

        controller.open(CONVERSATION_ID);

        AttendanceDetailState latest = latest(states);
        assertEquals(
                "Transient cursor failure must keep the detail ready.",
                AttendanceDetailState.Phase.READY,
                latest.phase());
        assertEquals(
                "The warning category must remain visible.",
                AttendanceFailureKind.NETWORK,
                latest.failure().orElseThrow());
        assertEquals(
                "Freshly authorized content must remain visible.",
                1,
                latest.snapshot().orElseThrow().messages().size());
    }

    @Test
    public void rejectsMalformedConversationIdentifiersBeforeNetwork() {
        AttendanceRepository repository = repository(
                conversation(CONVERSATION_ID),
                new ArrayDeque<>());
        AttendanceDetailController controller =
                new AttendanceDetailController(
                        repository,
                        Runnable::run,
                        Runnable::run);

        assertThrows(
                "WhatsApp identifiers require a positive numeric source.",
                IllegalArgumentException.class,
                () -> controller.open("whatsapp:"));
        assertThrows(
                "Site identifiers must not contain path separators.",
                IllegalArgumentException.class,
                () -> controller.open("site_chat:a/b"));
    }

    @Test
    public void rejectsCrossConversationPaginationWithoutAppending() {
        Queue<AttendanceMessagePage> pages = new ArrayDeque<>();
        pages.add(new AttendanceMessagePage(
                CONVERSATION_ID,
                List.of(message(MESSAGE_ID_THREE)),
                Optional.of("opaque_1")));
        pages.add(new AttendanceMessagePage(
                "whatsapp:2",
                List.of(message("whatsapp:2:2")),
                Optional.empty()));
        AttendanceDetailController controller =
                new AttendanceDetailController(
                        repository(
                                conversation(CONVERSATION_ID),
                                pages),
                        Runnable::run,
                        Runnable::run);
        Queue<AttendanceDetailState> states = new ArrayDeque<>();
        controller.subscribe(states::add);

        controller.open(CONVERSATION_ID);
        controller.loadMore();

        AttendanceDetailState latest = latest(states);
        assertEquals(
                "Cross-conversation pages must fail as protocol errors.",
                AttendanceFailureKind.PROTOCOL,
                latest.failure().orElseThrow());
        assertTrue(
                "Protocol violations must discard the volatile snapshot.",
                latest.snapshot().isEmpty());
    }

    private static AttendanceRepository repository(
            AttendanceConversation detail,
            Queue<AttendanceMessagePage> pages) {
        return new AttendanceRepository() {
            @Override
            public AttendancePage page(
                    AttendanceQuery query,
                    Optional<String> cursor) {
                throw new AssertionError(LIST_NOT_USED);
            }

            @Override
            public AttendanceConversation conversation(
                    String conversationId) {
                return detail;
            }

            @Override
            public AttendanceMessagePage messagePage(
                    String conversationId,
                    Optional<String> cursor,
                    int limit) {
                return pages.remove();
            }

            @Override
            public AttendanceReadCursorResult markRead(
                    String conversationId,
                    String throughMessageId) {
                return readResult(conversationId, throughMessageId);
            }
        };
    }

    private static AttendanceDetailState latest(
            Queue<AttendanceDetailState> states) {
        return states.stream()
                .reduce((first, second) -> second)
                .orElseThrow();
    }

    private static AttendanceConversation conversation(String id) {
        return new AttendanceConversation(
                id,
                AttendanceChannel.WHATSAPP,
                "1",
                3,
                Optional.empty(),
                new AttendanceConversation.Customer(
                        OptionalLong.empty(),
                        Optional.of("Cliente")),
                Optional.empty(),
                AttendanceFolder.INBOX,
                AttendancePriority.NORMAL,
                AttendanceHandlingMode.HUMAN,
                "active",
                new AttendanceConversation.Stats(1, 1, 1),
                Optional.empty(),
                Instant.parse("2026-07-27T12:00:00Z"),
                Instant.parse("2026-07-27T12:00:00Z"));
    }

    private static AttendanceMessage message(String id) {
        return new AttendanceMessage(
                id,
                id.substring(id.lastIndexOf(':') + 1),
                AttendanceMessageDirection.INBOUND,
                "text",
                "Mensagem",
                false,
                Optional.empty(),
                Instant.parse("2026-07-27T12:00:00Z"));
    }

    private static AttendanceReadCursorResult readResult(
            String conversationId,
            String throughMessageId) {
        return new AttendanceReadCursorResult(
                conversationId,
                throughMessageId,
                0,
                Instant.parse("2026-07-27T12:01:00Z"));
    }
}
