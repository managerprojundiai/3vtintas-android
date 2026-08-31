package br.com.tresvtintas.mobile.core.appointment;

import static org.junit.Assert.assertEquals;

import br.com.tresvtintas.mobile.core.delivery.DeliveryAction;
import br.com.tresvtintas.mobile.core.delivery.DeliveryDetail;
import br.com.tresvtintas.mobile.core.delivery.DeliveryException;
import br.com.tresvtintas.mobile.core.delivery.DeliveryMutationResult;
import br.com.tresvtintas.mobile.core.delivery.DeliveryPage;
import br.com.tresvtintas.mobile.core.delivery.DeliveryQuery;
import br.com.tresvtintas.mobile.core.delivery.DeliveryRepository;
import br.com.tresvtintas.mobile.core.delivery.DeliveryStatus;
import br.com.tresvtintas.mobile.core.delivery.DeliverySummary;
import br.com.tresvtintas.mobile.core.delivery.DeliveryView;
import br.com.tresvtintas.mobile.core.finance.FinanceDateBasis;
import br.com.tresvtintas.mobile.core.finance.FinanceDetail;
import br.com.tresvtintas.mobile.core.finance.FinanceDraft;
import br.com.tresvtintas.mobile.core.finance.FinanceException;
import br.com.tresvtintas.mobile.core.finance.FinanceMutationResult;
import br.com.tresvtintas.mobile.core.finance.FinanceOverview;
import br.com.tresvtintas.mobile.core.finance.FinancePage;
import br.com.tresvtintas.mobile.core.finance.FinancePaymentMethod;
import br.com.tresvtintas.mobile.core.finance.FinanceQuery;
import br.com.tresvtintas.mobile.core.finance.FinanceRepository;
import br.com.tresvtintas.mobile.core.model.AppRole;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import java.util.concurrent.Executor;
import org.junit.Test;

public final class AgendaControllerTest {
    private static final String UNUSED_OPERATION = "Operation is not used.";
    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");
    private static final YearMonth MONTH = YearMonth.of(2026, 7);
    private static final LocalDate SELECTED = LocalDate.of(2026, 7, 20);

    @Test
    public void aggregatesAllPagesWithoutLettingTheClientWidenScope() {
        FakeAppointmentRepository appointments =
                new FakeAppointmentRepository();
        FakeFinanceRepository finances = new FakeFinanceRepository();
        AgendaController controller = controller(appointments, finances);

        controller.open();

        AgendaState state = state(controller);
        assertEquals(
                "Integrated agenda must become ready.",
                AgendaState.Phase.READY,
                state.phase());
        assertEquals(
                "All appointment keyset pages must be aggregated.",
                List.of(1L, 2L),
                state.snapshot().orElseThrow().visibleDays().stream()
                        .flatMap(day -> day.entries().stream())
                        .map(AgendaEntry::sourceId)
                        .toList());
        assertEquals(
                "The server-authorized team scope must be preserved.",
                AppointmentScope.TEAM,
                appointments.query.scope());
        assertEquals(
                "Organization scope must remain explicit.",
                OptionalLong.of(7),
                appointments.query.organizationId());
        assertEquals(
                "Opaque appointment cursor must be forwarded unchanged.",
                Optional.of("appointment-next"),
                appointments.cursors.get(1));
        assertEquals(
                "Finance projection must use effective agenda dates.",
                FinanceDateBasis.AGENDA,
                finances.query.dateBasis());
        assertEquals(
                "Finance window must match the exact visible grid.",
                AgendaMonthSnapshot.window(MONTH, ZONE).from(),
                finances.query.dueFrom().orElseThrow());
    }

    @Test
    public void keepsUsableCalendarOnTransientRefreshFailure() {
        FakeAppointmentRepository appointments =
                new FakeAppointmentRepository();
        AgendaController controller = controller(
                appointments,
                new FakeFinanceRepository());
        controller.open();
        appointments.transientFailure = true;

        controller.refresh();

        AgendaState state = state(controller);
        assertEquals(
                "Transient refresh failure must retain a ready calendar.",
                AgendaState.Phase.READY,
                state.phase());
        assertEquals(
                "UI must receive a typed stale-data warning.",
                Optional.of(AgendaFailureKind.NETWORK),
                state.failure());
        assertEquals(
                "Existing entries must remain visible.",
                2,
                state.snapshot().orElseThrow().visibleDays().stream()
                        .mapToInt(day -> day.entries().size())
                        .sum());
    }

    @Test
    public void selectingAVisibleDayDoesNotReloadTheRepositories() {
        FakeAppointmentRepository appointments =
                new FakeAppointmentRepository();
        FakeFinanceRepository finances = new FakeFinanceRepository();
        AgendaController controller = controller(appointments, finances);
        controller.open();
        int appointmentCalls = appointments.cursors.size();
        int financeCalls = finances.calls;

        controller.select(LocalDate.of(2026, 8, 1));

        AgendaState state = state(controller);
        assertEquals(
                "Visible adjacent month day must be selectable.",
                LocalDate.of(2026, 8, 1),
                state.snapshot().orElseThrow().selectedDate());
        assertEquals(
                "Day selection must remain a local projection.",
                appointmentCalls,
                appointments.cursors.size());
        assertEquals(
                "Day selection must not repeat finance I/O.",
                financeCalls,
                finances.calls);
    }

    @Test
    public void publishesTargetMonthImmediatelyAndLatestNavigationWins() {
        FakeAppointmentRepository appointments =
                new FakeAppointmentRepository();
        FakeFinanceRepository finances = new FakeFinanceRepository();
        QueuedExecutor worker = new QueuedExecutor();
        AgendaController controller = controller(
                appointments,
                finances,
                worker);
        AgendaState[] observed = new AgendaState[1];
        controller.subscribe(state -> observed[0] = state);

        controller.open();

        assertEquals(
                "Initial month must render before remote I/O completes.",
                AgendaState.Phase.LOADING,
                observed[0].phase());
        assertEquals(
                "Initial preview must preserve the requested month.",
                MONTH,
                observed[0].snapshot().orElseThrow().month());

        controller.previousMonth();

        assertEquals(
                "A newer navigation must replace a request in flight.",
                YearMonth.of(2026, 6),
                observed[0].snapshot().orElseThrow().month());
        worker.runNext();
        assertEquals(
                "A superseded response must never rewind the visible month.",
                YearMonth.of(2026, 6),
                observed[0].snapshot().orElseThrow().month());
        worker.runNext();
        assertEquals(
                "The latest month must become authoritative.",
                AgendaState.Phase.READY,
                observed[0].phase());
        assertEquals(
                "The latest navigation must win after both requests finish.",
                YearMonth.of(2026, 6),
                observed[0].snapshot().orElseThrow().month());
    }

    @Test
    public void preservesASelectionMadeWhileTheMonthIsLoading() {
        FakeAppointmentRepository appointments =
                new FakeAppointmentRepository();
        FakeFinanceRepository finances = new FakeFinanceRepository();
        QueuedExecutor worker = new QueuedExecutor();
        AgendaController controller = controller(
                appointments,
                finances,
                worker);
        AgendaState[] observed = new AgendaState[1];
        controller.subscribe(state -> observed[0] = state);
        controller.open();

        LocalDate latestSelection = LocalDate.of(2026, 7, 30);
        controller.select(latestSelection);
        worker.runNext();

        assertEquals(
                "Remote completion must preserve the user's latest day.",
                latestSelection,
                observed[0].snapshot().orElseThrow().selectedDate());
        assertEquals(
                "The reconciled month must become ready.",
                AgendaState.Phase.READY,
                observed[0].phase());
    }

    @Test
    public void preservesASelectionWhenACachedRefreshFails() {
        FakeAppointmentRepository appointments =
                new FakeAppointmentRepository();
        FakeFinanceRepository finances = new FakeFinanceRepository();
        QueuedExecutor worker = new QueuedExecutor();
        AgendaController controller = controller(
                appointments,
                finances,
                worker);
        AgendaState[] observed = new AgendaState[1];
        controller.subscribe(state -> observed[0] = state);
        controller.open();
        worker.runNext();

        controller.refresh();
        LocalDate latestSelection = LocalDate.of(2026, 7, 30);
        controller.select(latestSelection);
        appointments.transientFailure = true;
        worker.runNext();

        assertEquals(
                "A stale fallback must preserve the user's latest day.",
                latestSelection,
                observed[0].snapshot().orElseThrow().selectedDate());
        assertEquals(
                "A recoverable failure must retain a ready cached month.",
                AgendaState.Phase.READY,
                observed[0].phase());
        assertEquals(
                "The recoverable failure must remain visible.",
                Optional.of(AgendaFailureKind.NETWORK),
                observed[0].failure());
    }

    @Test
    public void loadsAllDeliveryPagesInsideTheVisibleCalendarWindow() {
        FakeAppointmentRepository appointments =
                new FakeAppointmentRepository();
        FakeDeliveryRepository deliveries = new FakeDeliveryRepository();
        AgendaController controller = new AgendaController(
                appointments,
                Optional.empty(),
                Optional.of(deliveries),
                AppointmentScope.TEAM,
                OptionalLong.of(7),
                ZONE,
                MONTH,
                SELECTED,
                Runnable::run,
                Runnable::run);

        controller.open();

        AgendaState current = state(controller);
        assertEquals(
                "Both independent logistics pages must reach the calendar.",
                List.of(1L, 601L, 2L, 602L),
                current.snapshot().orElseThrow().visibleDays().stream()
                        .flatMap(day -> day.entries().stream())
                        .map(AgendaEntry::sourceId)
                        .toList());
        assertEquals(
                "The delivery source must use its dedicated bounded view.",
                DeliveryView.CALENDAR,
                deliveries.query.view());
        assertEquals(
                "The selected organization may only narrow server ABAC.",
                OptionalLong.of(7),
                deliveries.query.organizationId());
        assertEquals(
                "The logistics range must match the exact 42-day grid.",
                AgendaMonthSnapshot.window(MONTH, ZONE).from(),
                deliveries.query.scheduledFrom().orElseThrow());
        assertEquals(
                "Opaque logistics cursors must be forwarded unchanged.",
                Optional.of("delivery-next"),
                deliveries.cursors.get(1));
    }

    @Test
    public void personalAgendaReliesOnServerIdentityWithoutConflictingOrganizationFilter() {
        FakeAppointmentRepository appointments =
                new FakeAppointmentRepository();
        FakeDeliveryRepository deliveries = new FakeDeliveryRepository();
        AgendaController controller = new AgendaController(
                appointments,
                Optional.empty(),
                Optional.of(deliveries),
                AppointmentScope.SELF,
                OptionalLong.of(7),
                ZONE,
                MONTH,
                SELECTED,
                Runnable::run,
                Runnable::run);

        controller.open();

        assertEquals(
                "Personal appointment reads must remain bound to the authenticated user.",
                AppointmentScope.SELF,
                appointments.query.scope());
        assertEquals(
                "Personal appointment reads must not combine SELF with a forbidden organization filter.",
                OptionalLong.empty(),
                appointments.query.organizationId());
        assertEquals(
                "Personal delivery reads must rely on the server-owned seller scope.",
                OptionalLong.empty(),
                deliveries.query.organizationId());
    }

    private static AgendaController controller(
            FakeAppointmentRepository appointments,
            FakeFinanceRepository finances) {
        return controller(appointments, finances, Runnable::run);
    }

    private static AgendaController controller(
            FakeAppointmentRepository appointments,
            FakeFinanceRepository finances,
            Executor worker) {
        return new AgendaController(
                appointments,
                Optional.of(finances),
                Optional.empty(),
                AppointmentScope.TEAM,
                OptionalLong.of(7),
                ZONE,
                MONTH,
                SELECTED,
                worker,
                Runnable::run);
    }

    private static AgendaState state(AgendaController controller) {
        AgendaState[] value = new AgendaState[1];
        controller.subscribe(state -> value[0] = state);
        return value[0];
    }

    private static AppointmentSummary appointment(
            long id,
            Instant scheduledAt) {
        return new AppointmentSummary(
                id,
                AppointmentKind.GENERAL,
                AppointmentStatus.SCHEDULED,
                "Compromisso " + id,
                scheduledAt,
                60,
                Optional.empty(),
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
                Set.of(AppointmentAction.UPDATE),
                Instant.parse("2026-07-01T12:00:00Z"),
                Instant.parse("2026-07-01T12:00:00Z"));
    }

    private static FinanceOverview emptyOverview() {
        FinanceOverview.MoneyTotal zero =
                new FinanceOverview.MoneyTotal(
                        0,
                        BigDecimal.ZERO.setScale(2));
        FinanceOverview.TypeTotals totals =
                new FinanceOverview.TypeTotals(zero, zero, zero);
        return new FinanceOverview(totals, totals, totals, totals);
    }

    private static DeliverySummary delivery(
            long id,
            long orderId,
            String scheduledAt) {
        return new DeliverySummary(
                id,
                DeliveryStatus.PENDING,
                Set.of(DeliveryAction.START),
                new DeliverySummary.Order(orderId, "confirmed", 2, 1),
                Optional.of(new DeliverySummary.Organization(7, "Centro")),
                Optional.of(new DeliverySummary.Customer(
                        81,
                        "Cliente",
                        Optional.of("Jundiaí"),
                        Optional.of("SP"))),
                Optional.empty(),
                Optional.of(Instant.parse(scheduledAt)),
                Optional.empty(),
                Instant.parse("2026-07-01T12:00:00Z"));
    }

    private static final class FakeAppointmentRepository
            implements AppointmentRepository {
        private final List<Optional<String>> cursors = new ArrayList<>();
        private AppointmentQuery query;
        private boolean transientFailure;

        @Override
        public AppointmentPage page(
                AppointmentQuery received,
                Optional<String> cursor) throws AppointmentException {
            query = received;
            cursors.add(cursor);
            if (transientFailure) {
                throw new AppointmentException(
                        AppointmentFailureKind.NETWORK,
                        "Network unavailable.");
            }
            return cursor.isEmpty()
                    ? new AppointmentPage(
                            List.of(appointment(
                                    1,
                                    Instant.parse(
                                            "2026-07-20T13:00:00Z"))),
                            new AppointmentOverview(2, 0, 0, 0),
                            Optional.of("appointment-next"))
                    : new AppointmentPage(
                            List.of(appointment(
                                    2,
                                    Instant.parse(
                                            "2026-08-01T13:00:00Z"))),
                            new AppointmentOverview(2, 0, 0, 0),
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
            throw new AssertionError(UNUSED_OPERATION);
        }
    }

    private static final class FakeFinanceRepository
            implements FinanceRepository {
        private FinanceQuery query;
        private int calls;

        @Override
        public FinancePage page(
                FinanceQuery received,
                Optional<String> cursor) {
            query = received;
            calls += 1;
            return new FinancePage(
                    List.of(),
                    emptyOverview(),
                    Optional.empty());
        }

        @Override
        public FinanceDetail detail(long entryId) {
            throw new AssertionError(UNUSED_OPERATION);
        }

        @Override
        public FinanceMutationResult create(
                FinanceDraft draft,
                String idempotencyKey) {
            throw new AssertionError(UNUSED_OPERATION);
        }

        @Override
        public FinanceMutationResult settle(
                long entryId,
                FinancePaymentMethod paymentMethod,
                Optional<String> paymentReference,
                String idempotencyKey) throws FinanceException {
            throw new AssertionError(UNUSED_OPERATION);
        }

        @Override
        public FinanceMutationResult cancel(
                long entryId,
                String idempotencyKey) {
            throw new AssertionError(UNUSED_OPERATION);
        }
    }

    private static final class FakeDeliveryRepository
            implements DeliveryRepository {
        private static final String UNUSED_DELIVERY_OPERATION =
                "Delivery operation is not used.";
        private final List<Optional<String>> cursors = new ArrayList<>();
        private DeliveryQuery query;

        @Override
        public DeliveryPage page(
                DeliveryQuery received,
                Optional<String> cursor) {
            query = received;
            cursors.add(cursor);
            return cursor.isEmpty()
                    ? new DeliveryPage(
                            List.of(delivery(
                                    601,
                                    501,
                                    "2026-07-20T14:00:00Z")),
                            Optional.of("delivery-next"))
                    : new DeliveryPage(
                            List.of(delivery(
                                    602,
                                    502,
                                    "2026-08-01T14:00:00Z")),
                            Optional.empty());
        }

        @Override
        public DeliveryDetail detail(long deliveryId) {
            throw new AssertionError(UNUSED_DELIVERY_OPERATION);
        }

        @Override
        public DeliveryMutationResult start(
                long deliveryId,
                int expectedOrderRevision,
                String idempotencyKey) throws DeliveryException {
            throw new AssertionError(UNUSED_DELIVERY_OPERATION);
        }

        @Override
        public DeliveryMutationResult complete(
                long deliveryId,
                int expectedOrderRevision,
                String idempotencyKey) throws DeliveryException {
            throw new AssertionError(UNUSED_DELIVERY_OPERATION);
        }
    }

    private static final class QueuedExecutor implements Executor {
        private final Deque<Runnable> work = new ArrayDeque<>();

        @Override
        public void execute(Runnable command) {
            work.addLast(command);
        }

        private void runNext() {
            work.removeFirst().run();
        }
    }
}
