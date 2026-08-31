package br.com.tresvtintas.mobile.core.attendance;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;

public final class AttendanceManagementControllerTest {
    private static final String KEY =
            "00000000-0000-4000-8000-000000000001";
    private static final String ANA = "Ana";
    private static final String BRUNO = "Bruno";

    @Test
    public void loadsTheEntireEligibleDirectoryWithOpaquePagination() {
        FakeRepository repository = new FakeRepository();
        repository.assigneePages = List.of(
                new AttendanceAssigneePage(
                        List.of(new AttendanceAssignee(21, ANA)),
                        Optional.of("next_1")),
                new AttendanceAssigneePage(
                        List.of(new AttendanceAssignee(22, BRUNO)),
                        Optional.empty()));
        AttendanceManagementController controller = controller(repository);
        List<AttendanceManagementState> states = new ArrayList<>();
        controller.subscribe(states::add);

        controller.open(conversation());

        AttendanceManagementState result =
                states.get(states.size() - 1);
        assertEquals(
                "The directory must become ready.",
                AttendanceManagementState.Phase.READY,
                result.phase());
        assertEquals(
                "All authorized pages must be joined in memory.",
                List.of(
                        new AttendanceAssignee(21, ANA),
                        new AttendanceAssignee(22, BRUNO)),
                result.snapshot().orElseThrow().assignees());
        assertEquals(
                "The client must request the fixed safe page size.",
                List.of(100, 100),
                repository.assigneeLimits);
        assertEquals(
                "Only opaque continuation values may be replayed.",
                List.of(Optional.empty(), Optional.of("next_1")),
                repository.assigneeCursors);
    }

    @Test
    public void savesWithOptimisticRevisionAndIdempotency() {
        FakeRepository repository = new FakeRepository();
        repository.assigneePages = List.of(new AttendanceAssigneePage(
                List.of(new AttendanceAssignee(22, BRUNO)),
                Optional.empty()));
        repository.managementResult = new AttendanceManagementResult(
                "whatsapp:1",
                4,
                AttendanceFolder.RESOLVED,
                AttendancePriority.URGENT,
                Optional.of(new AttendanceConversation.AssignedUser(
                        22,
                        Optional.of(BRUNO),
                        false)),
                Instant.parse("2026-07-27T13:00:00Z"),
                false);
        AttendanceManagementController controller = controller(repository);
        AtomicReference<AttendanceManagementState> state =
                new AtomicReference<>();
        controller.subscribe(state::set);
        controller.open(conversation());
        AttendanceManagementSelection selection =
                new AttendanceManagementSelection(
                        AttendanceFolder.RESOLVED,
                        AttendancePriority.URGENT,
                        OptionalLong.of(22));

        controller.save(selection, KEY);

        assertEquals(
                "A confirmed mutation must reach success.",
                AttendanceManagementState.Phase.SUCCESS,
                state.get().phase());
        assertEquals(
                "The server revision must replace the stale revision.",
                4,
                state.get().snapshot().orElseThrow().revision());
        assertEquals(
                "The repository must receive the revision that was displayed.",
                3,
                repository.expectedRevision.get());
        assertEquals(
                "The exact caller key must be retained.",
                KEY,
                repository.idempotencyKey.get());
        assertEquals(
                "The selected values must be sent without role or scope.",
                selection,
                repository.selection.get());
    }

    @Test
    public void mutationConflictPreservesTheAuthorizedDirectory() {
        FakeRepository repository = new FakeRepository();
        repository.assigneePages = List.of(new AttendanceAssigneePage(
                List.of(new AttendanceAssignee(21, ANA)),
                Optional.empty()));
        repository.managementFailure = new AttendanceException(
                AttendanceFailureKind.CONFLICT,
                "Conflict",
                Optional.of("request-1"),
                null);
        AttendanceManagementController controller = controller(repository);
        AtomicReference<AttendanceManagementState> state =
                new AtomicReference<>();
        controller.subscribe(state::set);
        controller.open(conversation());

        controller.save(
                AttendanceManagementSelection.from(conversation()),
                KEY);

        assertEquals(
                "A stale revision must remain a conflict.",
                AttendanceManagementState.Phase.ERROR,
                state.get().phase());
        assertEquals(
                "The safe request ID must remain available.",
                Optional.of("request-1"),
                state.get().requestId());
        assertEquals(
                "Already authorized names may remain available for retry.",
                1,
                state.get().snapshot().orElseThrow().assignees().size());
    }

    @Test
    public void duplicateAssigneesFailClosed() {
        FakeRepository repository = new FakeRepository();
        repository.assigneePages = List.of(
                new AttendanceAssigneePage(
                        List.of(new AttendanceAssignee(21, ANA)),
                        Optional.of("next_1")),
                new AttendanceAssigneePage(
                        List.of(new AttendanceAssignee(21, ANA)),
                        Optional.empty()));
        AttendanceManagementController controller = controller(repository);
        AtomicReference<AttendanceManagementState> state =
                new AtomicReference<>();
        controller.subscribe(state::set);

        controller.open(conversation());

        assertEquals(
                "Duplicate directory identities must fail closed.",
                AttendanceManagementState.Phase.ERROR,
                state.get().phase());
        assertEquals(
                "Duplicate identities are a protocol violation.",
                Optional.of(AttendanceFailureKind.PROTOCOL),
                state.get().failure());
        assertFalse(
                "A rejected directory must not expose partial names.",
                state.get().snapshot().isPresent());
    }

    @Test
    public void selectionCopiesOnlyManageableConversationFields() {
        AttendanceManagementSelection selection =
                AttendanceManagementSelection.from(conversation());

        assertEquals(
                "Folder must be copied.",
                AttendanceFolder.INBOX,
                selection.folder());
        assertEquals(
                "Priority must be copied.",
                AttendancePriority.HIGH,
                selection.priority());
        assertTrue(
                "Current assignment must be copied.",
                selection.assignedToUserId().isPresent());
        assertEquals(
                "Only the current assignee ID may be copied.",
                21,
                selection.assignedToUserId().getAsLong());
    }

    private static AttendanceManagementController controller(
            FakeRepository repository) {
        return new AttendanceManagementController(
                repository,
                Runnable::run,
                Runnable::run);
    }

    private static AttendanceConversation conversation() {
        return new AttendanceConversation(
                "whatsapp:1",
                AttendanceChannel.WHATSAPP,
                "1",
                3,
                Optional.of(new AttendanceConversation.Organization(
                        9,
                        "Loja Centro")),
                new AttendanceConversation.Customer(
                        OptionalLong.of(31),
                        Optional.of("Maria")),
                Optional.of(new AttendanceConversation.AssignedUser(
                        21,
                        Optional.of(ANA),
                        true)),
                AttendanceFolder.INBOX,
                AttendancePriority.HIGH,
                AttendanceHandlingMode.HUMAN,
                "active",
                new AttendanceConversation.Stats(4, 3, 1),
                Optional.empty(),
                Instant.parse("2026-07-27T12:00:00Z"),
                Instant.parse("2026-07-27T12:00:00Z"));
    }

    private static final class FakeRepository
            implements AttendanceRepository {
        private List<AttendanceAssigneePage> assigneePages = List.of();
        private final AtomicInteger assigneeIndex = new AtomicInteger();
        private final List<Integer> assigneeLimits = new ArrayList<>();
        private final List<Optional<String>> assigneeCursors =
                new ArrayList<>();
        private AttendanceManagementResult managementResult;
        private AttendanceException managementFailure;
        private final AtomicInteger expectedRevision = new AtomicInteger();
        private final AtomicReference<String> idempotencyKey =
                new AtomicReference<>();
        private final AtomicReference<AttendanceManagementSelection>
                selection = new AtomicReference<>();

        @Override
        public AttendancePage page(
                AttendanceQuery query,
                Optional<String> cursor) {
            return new AttendancePage(List.of(), Optional.empty());
        }

        @Override
        public AttendanceAssigneePage assignees(
                String conversationId,
                Optional<String> search,
                Optional<String> cursor,
                int limit) {
            assigneeLimits.add(limit);
            assigneeCursors.add(cursor);
            return assigneePages.get(assigneeIndex.getAndIncrement());
        }

        @Override
        public AttendanceManagementResult manage(
                String conversationId,
                int revision,
                AttendanceManagementSelection requested,
                String key) throws AttendanceException {
            expectedRevision.set(revision);
            selection.set(requested);
            idempotencyKey.set(key);
            if (managementFailure != null) {
                throw managementFailure;
            }
            return managementResult;
        }
    }
}
