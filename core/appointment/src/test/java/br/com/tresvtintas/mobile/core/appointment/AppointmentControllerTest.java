package br.com.tresvtintas.mobile.core.appointment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import br.com.tresvtintas.mobile.core.model.AppRole;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.Test;

public final class AppointmentControllerTest {
    private static final String UNUSED_OPERATION = "Operation is not used.";
    private static final String REFERENCE_INSTANT =
            "2026-07-26T12:00:00Z";
    private static final AppointmentOverview OVERVIEW =
            new AppointmentOverview(2, 0, 0, 0);

    @Test
    public void appendsOpaquePagesAndKeepsServerOverview() {
        FakeRepository repository = new FakeRepository();
        AppointmentQuery query = AppointmentQuery.around(
                AppointmentScope.SELF,
                Instant.parse(REFERENCE_INSTANT));
        AppointmentListController controller =
                new AppointmentListController(
                        repository,
                        query,
                        Runnable::run,
                        Runnable::run);

        controller.open(query);
        controller.loadMore();

        AppointmentListState state = listState(controller);
        assertEquals(
                "Keyset pages must append without losing the first row.",
                List.of(1L, 2L),
                state.snapshot().orElseThrow().items().stream()
                        .map(AppointmentSummary::id)
                        .toList());
        assertEquals(
                "Opaque cursor must be forwarded unchanged.",
                Optional.of("opaque-next"),
                repository.receivedCursor);
        assertEquals(
                "The exact server overview must remain authoritative.",
                OVERVIEW,
                state.snapshot().orElseThrow().overview());
    }

    @Test
    public void retainsUsableSnapshotOnTransientRefreshFailure() {
        FakeRepository repository = new FakeRepository();
        AppointmentQuery query = AppointmentQuery.around(
                AppointmentScope.SELF,
                Instant.parse(REFERENCE_INSTANT));
        AppointmentListController controller =
                new AppointmentListController(
                        repository,
                        query,
                        Runnable::run,
                        Runnable::run);
        controller.open(query);
        repository.transientFailure = true;

        controller.refresh();

        AppointmentListState state = listState(controller);
        assertEquals(
                "Transient failure must retain ready data.",
                AppointmentListState.Phase.READY,
                state.phase());
        assertEquals(
                "The UI must receive a typed stale warning.",
                Optional.of(AppointmentFailureKind.NETWORK),
                state.failure());
        assertEquals(
                "Existing rows must stay visible.",
                1,
                state.snapshot().orElseThrow().items().size());
    }

    @Test
    public void executesIdempotentTransitionWithExactRevision() {
        FakeRepository repository = new FakeRepository();
        AppointmentMutationController controller =
                new AppointmentMutationController(
                        repository,
                        Runnable::run,
                        Runnable::run);

        controller.transition(
                1,
                AppointmentScope.SELF,
                7,
                AppointmentStatus.CONFIRMED,
                "00000000-0000-4000-8000-000000000701");

        AppointmentMutationState state = mutationState(controller);
        assertEquals(
                "Transition must finish with a typed success.",
                AppointmentMutationState.Phase.SUCCESS,
                state.phase());
        assertEquals(
                "Optimistic revision must be sent unchanged.",
                7,
                repository.receivedRevision);
        assertTrue(
                "Server change metadata must reach the UI.",
                state.result().orElseThrow().changed());
    }

    private static AppointmentListState listState(
            AppointmentListController controller) {
        AppointmentListState[] value = new AppointmentListState[1];
        controller.subscribe(state -> value[0] = state);
        return value[0];
    }

    private static AppointmentMutationState mutationState(
            AppointmentMutationController controller) {
        AppointmentMutationState[] value = new AppointmentMutationState[1];
        controller.subscribe(state -> value[0] = state);
        return value[0];
    }

    private static AppointmentSummary appointment(long id) {
        return new AppointmentSummary(
                id,
                AppointmentKind.GENERAL,
                AppointmentStatus.SCHEDULED,
                "Visita técnica",
                Instant.parse("2026-08-01T13:00:00Z"),
                60,
                Optional.of("Loja Centro"),
                new AppointmentPerson(
                        41,
                        Optional.of("Vendedor"),
                        AppRole.SALESPERSON),
                Optional.of(new AppointmentSummary.Organization(
                        7,
                        "Centro")),
                Optional.empty(),
                Optional.empty(),
                1,
                Set.of(
                        AppointmentAction.UPDATE,
                        AppointmentAction.CONFIRM,
                        AppointmentAction.COMPLETE,
                        AppointmentAction.CANCEL),
                Instant.parse(REFERENCE_INSTANT),
                Instant.parse(REFERENCE_INSTANT));
    }

    private static final class FakeRepository
            implements AppointmentRepository {
        private Optional<String> receivedCursor = Optional.empty();
        private boolean transientFailure;
        private long receivedRevision;

        @Override
        public AppointmentPage page(
                AppointmentQuery query,
                Optional<String> cursor) throws AppointmentException {
            receivedCursor = cursor;
            if (transientFailure) {
                throw new AppointmentException(
                        AppointmentFailureKind.NETWORK,
                        "Network unavailable.");
            }
            return cursor.isEmpty()
                    ? new AppointmentPage(
                            List.of(appointment(1)),
                            OVERVIEW,
                            Optional.of("opaque-next"))
                    : new AppointmentPage(
                            List.of(appointment(2)),
                            OVERVIEW,
                            Optional.empty());
        }

        @Override
        public AppointmentDetail detail(
                long appointmentId,
                AppointmentScope scope) {
            throw new AssertionError(UNUSED_OPERATION);
        }

        @Override
        public AppointmentResponsiblePage responsibles(
                AppointmentResponsibleQuery query,
                Optional<String> cursor) {
            throw new AssertionError(UNUSED_OPERATION);
        }

        @Override
        public AppointmentMutationResult create(
                AppointmentDraft draft,
                String idempotencyKey) {
            throw new AssertionError(UNUSED_OPERATION);
        }

        @Override
        public AppointmentMutationResult update(
                long appointmentId,
                AppointmentEdit edit,
                String idempotencyKey) {
            throw new AssertionError(UNUSED_OPERATION);
        }

        @Override
        public AppointmentMutationResult transition(
                long appointmentId,
                AppointmentScope scope,
                long expectedRevision,
                AppointmentStatus status,
                String idempotencyKey) {
            receivedRevision = expectedRevision;
            return new AppointmentMutationResult(
                    appointmentId,
                    status,
                    expectedRevision + 1,
                    true,
                    false);
        }
    }
}
