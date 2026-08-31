package br.com.tresvtintas.mobile.core.appointment;

import br.com.tresvtintas.mobile.core.finance.FinanceEntryStatus;
import br.com.tresvtintas.mobile.core.finance.FinanceSummary;
import br.com.tresvtintas.mobile.core.delivery.DeliverySummary;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public record AgendaMonthSnapshot(
        YearMonth month,
        LocalDate selectedDate,
        List<AgendaDay> visibleDays) {
    private static final int VISIBLE_DAY_COUNT = 42;

    public AgendaMonthSnapshot {
        month = Objects.requireNonNull(month, "Agenda month is required.");
        selectedDate = Objects.requireNonNull(
                selectedDate,
                "Agenda selected date is required.");
        LocalDate requiredSelectedDate = selectedDate;
        if (visibleDays == null
                || visibleDays.size() != VISIBLE_DAY_COUNT
                || visibleDays.stream().anyMatch(Objects::isNull)
                || visibleDays.stream()
                        .map(AgendaDay::date)
                        .distinct()
                        .count() != VISIBLE_DAY_COUNT
                || visibleDays.stream()
                        .noneMatch(day -> day.date().equals(
                                requiredSelectedDate))) {
            throw new IllegalArgumentException(
                    "Agenda month snapshot is invalid.");
        }
        visibleDays = List.copyOf(visibleDays);
    }

    public AgendaDay selectedDay() {
        return visibleDays.stream()
                .filter(day -> day.date().equals(selectedDate))
                .findFirst()
                .orElseThrow();
    }

    public AgendaMonthSnapshot select(LocalDate date) {
        if (date == null
                || visibleDays.stream().noneMatch(day -> day.date().equals(date))) {
            throw new IllegalArgumentException(
                    "Selected agenda date is outside the visible calendar.");
        }
        return new AgendaMonthSnapshot(month, date, visibleDays);
    }

    public static DateWindow window(YearMonth month, ZoneId zoneId) {
        YearMonth requiredMonth = Objects.requireNonNull(
                month,
                "Agenda month is required.");
        ZoneId requiredZone = Objects.requireNonNull(
                zoneId,
                "Agenda timezone is required.");
        LocalDate first = requiredMonth.atDay(1)
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY));
        LocalDate afterLast = first.plusDays(VISIBLE_DAY_COUNT);
        return new DateWindow(
                first,
                afterLast,
                first.atStartOfDay(requiredZone).toInstant(),
                afterLast.atStartOfDay(requiredZone).toInstant());
    }

    public static AgendaMonthSnapshot from(
            YearMonth month,
            LocalDate selectedDate,
            ZoneId zoneId,
            List<AppointmentSummary> appointments,
            List<DeliverySummary> deliveries,
            List<FinanceSummary> finances) {
        DateWindow window = window(month, zoneId);
        Map<LocalDate, List<AgendaEntry>> grouped = new LinkedHashMap<>();
        java.util.stream.Stream.iterate(
                        window.firstDate(),
                        date -> date.plusDays(1))
                .limit(VISIBLE_DAY_COUNT)
                .forEach(date -> grouped.put(date, new ArrayList<>()));
        List<DeliverySummary> deliveryItems = safe(deliveries);
        Set<Long> deliveryOrderIds = deliveryItems.stream()
                .map(value -> value.order().id())
                .collect(Collectors.toUnmodifiableSet());
        for (AppointmentSummary appointment : safe(appointments)) {
            if (appointment.kind() == AppointmentKind.DELIVERY
                    && appointment.order()
                            .map(value -> deliveryOrderIds.contains(value.id()))
                            .orElse(false)) {
                continue;
            }
            LocalDate date = appointment.scheduledAt()
                    .atZone(zoneId)
                    .toLocalDate();
            Optional.ofNullable(grouped.get(date)).ifPresent(entries ->
                    entries.add(appointment(appointment)));
        }
        for (DeliverySummary delivery : deliveryItems) {
            delivery.scheduledAt().ifPresent(instant -> {
                LocalDate date = instant.atZone(zoneId).toLocalDate();
                Optional.ofNullable(grouped.get(date)).ifPresent(entries ->
                        entries.add(delivery(delivery, instant)));
            });
        }
        for (FinanceSummary finance : safe(finances)) {
            financialInstant(finance).ifPresent(instant -> {
                LocalDate date = instant.atZone(zoneId).toLocalDate();
                Optional.ofNullable(grouped.get(date)).ifPresent(entries ->
                        entries.add(finance(finance, instant)));
            });
        }
        List<AgendaDay> days = grouped.entrySet().stream()
                .map(entry -> day(entry.getKey(), entry.getValue()))
                .toList();
        LocalDate selected = selectedDate != null
                        && !selectedDate.isBefore(window.firstDate())
                        && selectedDate.isBefore(window.afterLastDate())
                ? selectedDate
                : month.atDay(1);
        return new AgendaMonthSnapshot(month, selected, days);
    }

    public static AgendaMonthSnapshot empty(
            YearMonth month,
            LocalDate selectedDate,
            ZoneId zoneId) {
        return from(
                month,
                selectedDate,
                zoneId,
                List.of(),
                List.of(),
                List.of());
    }

    private static <T> List<T> safe(List<T> values) {
        if (values == null || values.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException(
                    "Agenda source entries are invalid.");
        }
        return values;
    }

    private static AgendaEntry appointment(AppointmentSummary value) {
        AgendaEntryType type = switch (value.kind()) {
            case GENERAL -> AgendaEntryType.APPOINTMENT;
            case DELIVERY -> AgendaEntryType.DELIVERY;
            case COLLECTION -> AgendaEntryType.COLLECTION;
        };
        return new AgendaEntry(
                type,
                AgendaEntrySource.APPOINTMENT,
                value.id(),
                value.title(),
                value.scheduledAt(),
                value.status().name(),
                Optional.empty());
    }

    private static AgendaEntry delivery(
            DeliverySummary value,
            java.time.Instant instant) {
        String customer = value.customer()
                .map(DeliverySummary.Customer::name)
                .map(name -> " - " + name)
                .orElse("");
        String title = "Entrega do pedido #" + value.order().id() + customer;
        return new AgendaEntry(
                AgendaEntryType.DELIVERY,
                AgendaEntrySource.DELIVERY,
                value.id(),
                title.substring(0, Math.min(title.length(), 200)),
                instant,
                value.status().name(),
                Optional.empty());
    }

    private static Optional<java.time.Instant> financialInstant(
            FinanceSummary value) {
        if (value.status() == FinanceEntryStatus.SETTLED
                && value.settledAt().isPresent()) {
            return value.settledAt();
        }
        return value.dueAt();
    }

    private static AgendaEntry finance(
            FinanceSummary value,
            java.time.Instant instant) {
        AgendaEntryType type = switch (value.type()) {
            case RECEIVABLE -> AgendaEntryType.RECEIVABLE;
            case PAYABLE -> AgendaEntryType.PAYABLE;
            case EXPENSE -> AgendaEntryType.EXPENSE;
        };
        return new AgendaEntry(
                type,
                AgendaEntrySource.FINANCE,
                value.id(),
                value.title(),
                instant,
                value.status().name(),
                Optional.of(value.amount()));
    }

    private static AgendaDay day(
            LocalDate date,
            List<AgendaEntry> source) {
        List<AgendaEntry> entries = source.stream()
                .sorted(Comparator.comparing(AgendaEntry::occursAt)
                        .thenComparing(AgendaEntry::sourceId))
                .toList();
        BigDecimal receivable = total(entries, AgendaEntryType.RECEIVABLE);
        BigDecimal payable = total(entries, AgendaEntryType.PAYABLE);
        BigDecimal expense = total(entries, AgendaEntryType.EXPENSE);
        return new AgendaDay(
                date,
                entries,
                receivable,
                payable,
                expense);
    }

    private static BigDecimal total(
            List<AgendaEntry> entries,
            AgendaEntryType type) {
        return entries.stream()
                .filter(entry -> entry.type() == type)
                .map(entry -> entry.amount().orElseThrow())
                .reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add);
    }

    public record DateWindow(
            LocalDate firstDate,
            LocalDate afterLastDate,
            java.time.Instant from,
            java.time.Instant toExclusive) {
        public DateWindow {
            Objects.requireNonNull(firstDate, "First date is required.");
            Objects.requireNonNull(afterLastDate, "Last date is required.");
            Objects.requireNonNull(from, "From instant is required.");
            Objects.requireNonNull(toExclusive, "To instant is required.");
            if (!afterLastDate.equals(firstDate.plusDays(VISIBLE_DAY_COUNT))
                    || !toExclusive.isAfter(from)) {
                throw new IllegalArgumentException(
                        "Agenda date window is invalid.");
            }
        }
    }
}
