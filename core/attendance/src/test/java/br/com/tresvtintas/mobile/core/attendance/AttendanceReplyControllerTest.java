package br.com.tresvtintas.mobile.core.attendance;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Optional;
import java.util.Queue;
import org.junit.Test;

public final class AttendanceReplyControllerTest {
    private static final String CONVERSATION_ID = "whatsapp:1";
    private static final String IDEMPOTENCY_KEY =
            "00000000-0000-4000-8000-000000000001";
    private static final String UNUSED_LIST = "List is not used.";

    @Test
    public void sendsNormalizedReplyAndPublishesSuccess() {
        String[] observed = {"", "", ""};
        AttendanceRepository repository = new AttendanceRepository() {
            @Override
            public AttendancePage page(
                    AttendanceQuery query,
                    Optional<String> cursor) {
                throw new AssertionError(UNUSED_LIST);
            }

            @Override
            public AttendanceReplyResult reply(
                    String conversationId,
                    String content,
                    String idempotencyKey) {
                observed[0] = conversationId;
                observed[1] = content;
                observed[2] = idempotencyKey;
                return result(false);
            }
        };
        AttendanceReplyController controller =
                new AttendanceReplyController(
                        repository,
                        Runnable::run,
                        Runnable::run);
        Queue<AttendanceReplyState> states = new ArrayDeque<>();
        controller.subscribe(states::add);

        controller.send(
                CONVERSATION_ID,
                "  Resposta\r\nsegura  ",
                IDEMPOTENCY_KEY);

        assertEquals(
                "The opaque conversation ID must be preserved.",
                CONVERSATION_ID,
                observed[0]);
        assertEquals(
                "The repository must receive normalized content.",
                "Resposta\nsegura",
                observed[1]);
        assertEquals(
                "The same logical attempt must use the provided key.",
                IDEMPOTENCY_KEY,
                observed[2]);
        assertEquals(
                "A successful repository result must finish the mutation.",
                AttendanceReplyState.Phase.SUCCESS,
                latest(states).phase());
        assertFalse(
                "A fresh response must not be reported as replayed.",
                latest(states).result().orElseThrow().replayed());
    }

    @Test
    public void invalidRequestFailsBeforeRepositoryCall() {
        boolean[] called = {false};
        AttendanceRepository repository = new AttendanceRepository() {
            @Override
            public AttendancePage page(
                    AttendanceQuery query,
                    Optional<String> cursor) {
                throw new AssertionError(UNUSED_LIST);
            }

            @Override
            public AttendanceReplyResult reply(
                    String conversationId,
                    String content,
                    String idempotencyKey) {
                called[0] = true;
                return result(false);
            }
        };
        AttendanceReplyController controller =
                new AttendanceReplyController(
                        repository,
                        Runnable::run,
                        Runnable::run);
        Queue<AttendanceReplyState> states = new ArrayDeque<>();
        controller.subscribe(states::add);

        controller.send(CONVERSATION_ID, " ", "short");

        assertFalse("Invalid input must not reach the repository.", called[0]);
        assertEquals(
                "Invalid input must be categorized before I/O.",
                AttendanceFailureKind.INVALID_REQUEST,
                latest(states).failure().orElseThrow());
    }

    @Test
    public void ignoresConcurrentSendUntilTheFirstCompletes() {
        Queue<Runnable> work = new ArrayDeque<>();
        int[] calls = {0};
        AttendanceRepository repository = new AttendanceRepository() {
            @Override
            public AttendancePage page(
                    AttendanceQuery query,
                    Optional<String> cursor) {
                throw new AssertionError(UNUSED_LIST);
            }

            @Override
            public AttendanceReplyResult reply(
                    String conversationId,
                    String content,
                    String idempotencyKey) {
                calls[0] += 1;
                return result(false);
            }
        };
        AttendanceReplyController controller =
                new AttendanceReplyController(
                        repository,
                        work::add,
                        Runnable::run);

        controller.send(
                CONVERSATION_ID,
                "Primeira",
                IDEMPOTENCY_KEY);
        controller.send(
                CONVERSATION_ID,
                "Segunda",
                "00000000-0000-4000-8000-000000000002");

        assertEquals("Only one operation may be queued.", 1, work.size());
        work.remove().run();
        assertEquals("Only the first operation may execute.", 1, calls[0]);
    }

    @Test
    public void closeDiscardsLateCompletion() {
        Queue<Runnable> work = new ArrayDeque<>();
        AttendanceReplyController controller =
                new AttendanceReplyController(
                        repository(result(false)),
                        work::add,
                        Runnable::run);
        Queue<AttendanceReplyState> states = new ArrayDeque<>();
        controller.subscribe(states::add);
        controller.send(
                CONVERSATION_ID,
                "Mensagem",
                IDEMPOTENCY_KEY);

        controller.close();
        work.remove().run();

        assertEquals(
                "Close must be the terminal visible state.",
                AttendanceReplyState.Phase.CLOSED,
                latest(states).phase());
        assertTrue(
                "A closed controller must not publish the late result.",
                states.stream().noneMatch(state ->
                        state.phase()
                                == AttendanceReplyState.Phase.SUCCESS));
    }

    private static AttendanceRepository repository(
            AttendanceReplyResult result) {
        return new AttendanceRepository() {
            @Override
            public AttendancePage page(
                    AttendanceQuery query,
                    Optional<String> cursor) {
                throw new AssertionError(UNUSED_LIST);
            }

            @Override
            public AttendanceReplyResult reply(
                    String conversationId,
                    String content,
                    String idempotencyKey) {
                return result;
            }
        };
    }

    private static AttendanceReplyResult result(boolean replayed) {
        return new AttendanceReplyResult(
                CONVERSATION_ID,
                new AttendanceMessage(
                        "whatsapp:1:12",
                        "12",
                        AttendanceMessageDirection.OUTBOUND,
                        "text",
                        "Mensagem",
                        false,
                        Optional.of("queued"),
                        Instant.parse("2026-07-27T12:00:00Z")),
                AttendanceReplyDeliveryState.QUEUED,
                replayed);
    }

    private static AttendanceReplyState latest(
            Queue<AttendanceReplyState> states) {
        return states.stream()
                .reduce((first, second) -> second)
                .orElseThrow();
    }
}
