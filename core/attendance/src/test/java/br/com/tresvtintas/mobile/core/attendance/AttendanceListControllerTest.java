package br.com.tresvtintas.mobile.core.attendance;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Queue;
import org.junit.Test;

public final class AttendanceListControllerTest {
    @Test
    public void paginatesWithOpaqueCursorWithoutPersistingMessages()
            throws Exception {
        Queue<AttendancePage> pages = new ArrayDeque<>();
        pages.add(new AttendancePage(
                List.of(conversation("whatsapp:1")),
                Optional.of("opaque_1")));
        pages.add(new AttendancePage(
                List.of(conversation("site_chat:2")),
                Optional.empty()));
        AttendanceRepository repository = (query, cursor) ->
                pages.remove();
        AttendanceListController controller =
                new AttendanceListController(
                        repository,
                        Runnable::run,
                        Runnable::run);
        Queue<AttendanceListState> states = new ArrayDeque<>();
        controller.subscribe(states::add);

        controller.open(AttendanceQuery.initial(
                OptionalLong.empty(),
                false));
        controller.loadMore();

        AttendanceListState latest = states.stream()
                .reduce((first, second) -> second)
                .orElseThrow();
        assertEquals(
                "Both pages must be represented in memory.",
                2,
                latest.snapshot().orElseThrow().items().size());
        assertFalse(
                "The final page must clear continuation.",
                latest.snapshot().orElseThrow().hasMore());
        assertTrue(
                "Both planned repository pages must be consumed.",
                pages.isEmpty());
    }

    @Test
    public void refreshKeepsPreviousRowsOnTransientFailure() {
        int[] calls = {0};
        AttendanceRepository repository = (query, cursor) -> {
            int currentCall = calls[0];
            calls[0] = currentCall + 1;
            if (currentCall == 0) {
                return new AttendancePage(
                        List.of(conversation("whatsapp:1")),
                        Optional.empty());
            }
            throw new AttendanceException(
                    AttendanceFailureKind.NETWORK,
                    "offline");
        };
        AttendanceListController controller =
                new AttendanceListController(
                        repository,
                        Runnable::run,
                        Runnable::run);
        Queue<AttendanceListState> states = new ArrayDeque<>();
        controller.subscribe(states::add);

        controller.open(AttendanceQuery.initial(
                OptionalLong.empty(),
                false));
        controller.refresh();

        AttendanceListState latest = states.stream()
                .reduce((first, second) -> second)
                .orElseThrow();
        assertEquals(
                "A transient refresh failure must retain the ready phase.",
                AttendanceListState.Phase.READY,
                latest.phase());
        assertEquals(
                "The transient failure must remain visible to the UI.",
                AttendanceFailureKind.NETWORK,
                latest.failure().orElseThrow());
        assertEquals(
                "Previously authorized rows must remain available in memory.",
                1,
                latest.snapshot().orElseThrow().items().size());
    }

    @Test
    public void newerFilterSupersedesAnInflightQuery() {
        Queue<Runnable> work = new ArrayDeque<>();
        AttendanceRepository repository = (query, cursor) ->
                new AttendancePage(
                        List.of(conversation(
                                query.search().orElse("initial"))),
                        Optional.empty());
        AttendanceListController controller =
                new AttendanceListController(
                        repository,
                        work::add,
                        Runnable::run);
        Queue<AttendanceListState> states = new ArrayDeque<>();
        controller.subscribe(states::add);
        AttendanceQuery initial = AttendanceQuery.initial(
                OptionalLong.empty(),
                false);

        controller.open(initial);
        controller.open(initial.withSearch("new-filter"));
        work.remove().run();
        work.remove().run();

        AttendanceListState latest = states.stream()
                .reduce((first, second) -> second)
                .orElseThrow();
        assertEquals(
                "Only the most recent query may publish its result.",
                Optional.of("new-filter"),
                latest.snapshot().orElseThrow().query().search());
        assertEquals(
                "The superseding query must become the visible row.",
                "new-filter",
                latest.snapshot().orElseThrow().items().get(0).id());
    }

    @Test
    public void failedFilteredQueryRemainsAvailableForRetry() {
        AttendanceRepository repository = (query, cursor) -> {
            throw new AttendanceException(
                    AttendanceFailureKind.NETWORK,
                    "offline");
        };
        AttendanceListController controller =
                new AttendanceListController(
                        repository,
                        Runnable::run,
                        Runnable::run);
        AttendanceQuery filtered = AttendanceQuery.initial(
                        OptionalLong.of(9),
                        false)
                .withSearch("Maria")
                .withAssignment(AttendanceAssignment.UNASSIGNED);

        controller.open(filtered);

        assertEquals(
                "A failed request must retain its filters for retry.",
                Optional.of(filtered),
                controller.currentQuery());
    }

    private static AttendanceConversation conversation(String id) {
        return new AttendanceConversation(
                id,
                id.startsWith("whatsapp")
                        ? AttendanceChannel.WHATSAPP
                        : AttendanceChannel.SITE_CHAT,
                "1",
                3,
                Optional.empty(),
                new AttendanceConversation.Customer(
                        OptionalLong.empty(),
                        Optional.of("Cliente")),
                Optional.empty(),
                AttendanceFolder.INBOX,
                AttendancePriority.NORMAL,
                AttendanceHandlingMode.AI,
                "active",
                new AttendanceConversation.Stats(1, 1, 1),
                Optional.empty(),
                java.time.Instant.parse("2026-07-27T12:00:00Z"),
                java.time.Instant.parse("2026-07-27T12:00:00Z"));
    }
}
