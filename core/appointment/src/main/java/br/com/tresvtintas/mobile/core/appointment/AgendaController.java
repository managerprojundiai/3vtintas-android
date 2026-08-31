package br.com.tresvtintas.mobile.core.appointment;

import br.com.tresvtintas.mobile.core.finance.FinanceException;
import br.com.tresvtintas.mobile.core.finance.FinancePage;
import br.com.tresvtintas.mobile.core.finance.FinanceQuery;
import br.com.tresvtintas.mobile.core.finance.FinanceRepository;
import br.com.tresvtintas.mobile.core.finance.FinanceSummary;
import br.com.tresvtintas.mobile.core.delivery.DeliveryException;
import br.com.tresvtintas.mobile.core.delivery.DeliveryPage;
import br.com.tresvtintas.mobile.core.delivery.DeliveryQuery;
import br.com.tresvtintas.mobile.core.delivery.DeliveryRepository;
import br.com.tresvtintas.mobile.core.delivery.DeliverySummary;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public final class AgendaController {
    private static final int PAGE_SIZE = 100;
    private static final int MAXIMUM_PAGES_PER_SOURCE = 20;
    private final AppointmentRepository appointments;
    private final Optional<FinanceRepository> finances;
    private final Optional<DeliveryRepository> deliveries;
    private final AppointmentScope scope;
    private final OptionalLong organizationId;
    private final ZoneId zoneId;
    private final Executor worker;
    private final Executor main;
    private final Set<AgendaStateListener> listeners =
            new CopyOnWriteArraySet<>();
    private final java.util.Map<YearMonth, AgendaMonthSnapshot> cache =
            new ConcurrentHashMap<>();
    private final AtomicBoolean busy = new AtomicBoolean();
    private final AtomicLong generation = new AtomicLong();
    private volatile AgendaState current = AgendaState.empty();
    private volatile YearMonth month;
    private volatile LocalDate selectedDate;

    public AgendaController(
            AppointmentRepository appointments,
            Optional<FinanceRepository> finances,
            Optional<DeliveryRepository> deliveries,
            AppointmentScope scope,
            OptionalLong organizationId,
            ZoneId zoneId,
            YearMonth initialMonth,
            LocalDate initialSelectedDate,
            Executor worker,
            Executor main) {
        this.appointments = Objects.requireNonNull(
                appointments,
                "Appointment repository is required.");
        this.finances = finances == null ? Optional.empty() : finances;
        this.deliveries = deliveries == null ? Optional.empty() : deliveries;
        this.scope = Objects.requireNonNull(
                scope,
                "Appointment scope is required.");
        this.organizationId = organizationId == null
                ? OptionalLong.empty()
                : organizationId;
        this.zoneId = Objects.requireNonNull(
                zoneId,
                "Agenda timezone is required.");
        this.month = Objects.requireNonNull(
                initialMonth,
                "Initial agenda month is required.");
        this.selectedDate = Objects.requireNonNull(
                initialSelectedDate,
                "Initial agenda date is required.");
        this.worker = Objects.requireNonNull(
                worker,
                "Agenda worker is required.");
        this.main = Objects.requireNonNull(main, "Main executor is required.");
        if (this.organizationId.isPresent()
                && this.organizationId.orElseThrow() < 1) {
            throw new IllegalArgumentException(
                    "Agenda organization is invalid.");
        }
    }

    public void subscribe(AgendaStateListener listener) {
        AgendaStateListener required = Objects.requireNonNull(
                listener,
                "Agenda listener is required.");
        listeners.add(required);
        AgendaState snapshot = current;
        main.execute(() -> required.onAgendaStateChanged(snapshot));
    }

    public void unsubscribe(AgendaStateListener listener) {
        listeners.remove(listener);
    }

    public void open() {
        load(month, selectedDate, false);
    }

    public void previousMonth() {
        YearMonth target = month.minusMonths(1);
        load(target, target.atDay(1), false);
    }

    public void nextMonth() {
        YearMonth target = month.plusMonths(1);
        load(target, target.atDay(1), false);
    }

    public void today(LocalDate today) {
        LocalDate required = Objects.requireNonNull(
                today,
                "Today is required.");
        load(YearMonth.from(required), required, false);
    }

    public void refresh() {
        load(month, selectedDate, true);
    }

    public synchronized void select(LocalDate date) {
        AgendaState snapshot = current;
        if (snapshot.snapshot().isEmpty()) {
            return;
        }
        AgendaMonthSnapshot selected = snapshot.snapshot()
                .orElseThrow()
                .select(date);
        selectedDate = date;
        cache.put(selected.month(), selected);
        if (snapshot.phase() == AgendaState.Phase.LOADING) {
            publish(AgendaState.loading(selected));
        } else if (snapshot.phase() == AgendaState.Phase.REFRESHING) {
            publish(AgendaState.refreshing(selected));
        } else {
            publish(AgendaState.ready(
                    selected,
                    snapshot.failure(),
                    snapshot.requestId()));
        }
    }

    public synchronized void close() {
        generation.incrementAndGet();
        busy.set(false);
        publish(AgendaState.closed());
        listeners.clear();
    }

    private synchronized void load(
            YearMonth targetMonth,
            LocalDate targetDate,
            boolean refresh) {
        if (current.phase() == AgendaState.Phase.CLOSED
                || refresh && !busy.compareAndSet(false, true)) {
            return;
        }
        busy.set(true);
        long operation = generation.incrementAndGet();
        Optional<AgendaMonthSnapshot> cached =
                Optional.ofNullable(cache.get(targetMonth))
                        .map(value -> value.select(targetDate));
        Optional<AgendaMonthSnapshot> fallback = refresh
                ? current.snapshot()
                : cached;
        AgendaMonthSnapshot preview = fallback.orElseGet(() ->
                AgendaMonthSnapshot.empty(
                        targetMonth,
                        targetDate,
                        zoneId));
        month = targetMonth;
        selectedDate = targetDate;
        fallback.ifPresentOrElse(
                value -> publish(AgendaState.refreshing(value)),
                () -> publish(AgendaState.loading(preview)));
        worker.execute(() -> execute(
                operation,
                targetMonth,
                targetDate,
                fallback));
    }

    private void execute(
            long operation,
            YearMonth targetMonth,
            LocalDate targetDate,
            Optional<AgendaMonthSnapshot> fallback) {
        try {
            AgendaMonthSnapshot.DateWindow window =
                    AgendaMonthSnapshot.window(targetMonth, zoneId);
            List<AppointmentSummary> appointmentItems = appointments(window);
            List<DeliverySummary> deliveryItems = deliveries(window);
            List<FinanceSummary> financeItems = finances(window);
            ready(
                    operation,
                    AgendaMonthSnapshot.from(
                            targetMonth,
                            targetDate,
                            zoneId,
                            appointmentItems,
                            deliveryItems,
                            financeItems));
        } catch (AppointmentException failure) {
            failed(
                    operation,
                    fallback,
                    AgendaFailureKind.valueOf(failure.kind().name()),
                    failure.requestId());
        } catch (FinanceException failure) {
            failed(
                    operation,
                    fallback,
                    AgendaFailureKind.valueOf(failure.kind().name()),
                    failure.requestId());
        } catch (DeliveryException failure) {
            failed(
                    operation,
                    fallback,
                    AgendaFailureKind.valueOf(failure.kind().name()),
                    failure.requestId());
        } catch (IllegalStateException failure) {
            failed(
                    operation,
                    fallback,
                    AgendaFailureKind.PROTOCOL,
                    Optional.empty());
        }
    }

    private List<DeliverySummary> deliveries(
            AgendaMonthSnapshot.DateWindow window)
            throws DeliveryException {
        if (deliveries.isEmpty()) {
            return List.of();
        }
        OptionalLong deliveryOrganizationId =
                scope == AppointmentScope.SELF
                        ? OptionalLong.empty()
                        : organizationId;
        DeliveryQuery query = DeliveryQuery.initial()
                .forOrganization(deliveryOrganizationId)
                .forAgendaWindow(window.from(), window.toExclusive());
        List<DeliverySummary> items = new ArrayList<>();
        Optional<String> cursor = Optional.empty();
        Set<String> seen = new HashSet<>();
        for (int pageIndex = 0;
                pageIndex < MAXIMUM_PAGES_PER_SOURCE;
                pageIndex++) {
            DeliveryPage page = deliveries.orElseThrow().page(query, cursor);
            items.addAll(page.items());
            if (page.nextCursor().isEmpty()) {
                return List.copyOf(items);
            }
            String next = page.nextCursor().orElseThrow();
            if (!seen.add(next)) {
                throw new IllegalStateException("Delivery cursor repeated.");
            }
            cursor = Optional.of(next);
        }
        throw new IllegalStateException(
                "Delivery month exceeds the safe page limit.");
    }

    private List<AppointmentSummary> appointments(
            AgendaMonthSnapshot.DateWindow window)
            throws AppointmentException {
        OptionalLong appointmentOrganizationId =
                scope == AppointmentScope.SELF
                        ? OptionalLong.empty()
                        : organizationId;
        AppointmentQuery query = new AppointmentQuery(
                scope,
                appointmentOrganizationId,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                window.from(),
                window.toExclusive(),
                PAGE_SIZE);
        List<AppointmentSummary> items = new ArrayList<>();
        Optional<String> cursor = Optional.empty();
        Set<String> seen = new HashSet<>();
        for (int pageIndex = 0;
                pageIndex < MAXIMUM_PAGES_PER_SOURCE;
                pageIndex++) {
            AppointmentPage page = appointments.page(query, cursor);
            items.addAll(page.items());
            if (page.nextCursor().isEmpty()) {
                return List.copyOf(items);
            }
            String next = page.nextCursor().orElseThrow();
            if (!seen.add(next)) {
                throw new IllegalStateException(
                        "Appointment cursor repeated.");
            }
            cursor = Optional.of(next);
        }
        throw new IllegalStateException(
                "Appointment month exceeds the safe page limit.");
    }

    private List<FinanceSummary> finances(
            AgendaMonthSnapshot.DateWindow window)
            throws FinanceException {
        if (finances.isEmpty()) {
            return List.of();
        }
        FinanceQuery query = FinanceQuery.initial().forAgendaWindow(
                window.from(),
                window.toExclusive());
        List<FinanceSummary> items = new ArrayList<>();
        Optional<String> cursor = Optional.empty();
        Set<String> seen = new HashSet<>();
        for (int pageIndex = 0;
                pageIndex < MAXIMUM_PAGES_PER_SOURCE;
                pageIndex++) {
            FinancePage page = finances.orElseThrow().page(query, cursor);
            items.addAll(page.items());
            if (page.nextCursor().isEmpty()) {
                return List.copyOf(items);
            }
            String next = page.nextCursor().orElseThrow();
            if (!seen.add(next)) {
                throw new IllegalStateException("Finance cursor repeated.");
            }
            cursor = Optional.of(next);
        }
        throw new IllegalStateException(
                "Finance month exceeds the safe page limit.");
    }

    private synchronized void ready(
            long operation,
            AgendaMonthSnapshot snapshot) {
        if (!current(operation)) {
            return;
        }
        AgendaMonthSnapshot resolved = preserveSelection(snapshot);
        busy.set(false);
        month = resolved.month();
        selectedDate = resolved.selectedDate();
        cache.put(resolved.month(), resolved);
        publish(AgendaState.ready(
                resolved,
                Optional.empty(),
                Optional.empty()));
    }

    private synchronized void failed(
            long operation,
            Optional<AgendaMonthSnapshot> fallback,
            AgendaFailureKind failure,
            Optional<String> requestId) {
        if (!current(operation)) {
            return;
        }
        busy.set(false);
        if (fallback.isPresent() && failure.supportsStaleSnapshot()) {
            AgendaMonthSnapshot resolved =
                    preserveSelection(fallback.orElseThrow());
            month = resolved.month();
            selectedDate = resolved.selectedDate();
            cache.put(resolved.month(), resolved);
            publish(AgendaState.ready(
                    resolved,
                    Optional.of(failure),
                    requestId));
            return;
        }
        publish(AgendaState.error(failure, requestId));
    }

    private AgendaMonthSnapshot preserveSelection(
            AgendaMonthSnapshot snapshot) {
        return snapshot.visibleDays().stream()
                .anyMatch(day -> day.date().equals(selectedDate))
                ? snapshot.select(selectedDate)
                : snapshot;
    }

    private boolean current(long operation) {
        return generation.get() == operation
                && current.phase() != AgendaState.Phase.CLOSED;
    }

    private void publish(AgendaState state) {
        current = state;
        main.execute(() -> listeners.forEach(
                listener -> listener.onAgendaStateChanged(state)));
    }
}
