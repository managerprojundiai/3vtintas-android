package br.com.tresvtintas.mobile.core.appointment;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public record AgendaDay(
        LocalDate date,
        List<AgendaEntry> entries,
        BigDecimal receivableTotal,
        BigDecimal payableTotal,
        BigDecimal expenseTotal) {
    public AgendaDay {
        date = Objects.requireNonNull(date, "Agenda date is required.");
        if (entries == null || entries.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("Agenda entries are invalid.");
        }
        entries = List.copyOf(entries);
        receivableTotal = money(receivableTotal);
        payableTotal = money(payableTotal);
        expenseTotal = money(expenseTotal);
    }

    public static AgendaDay empty(LocalDate date) {
        return new AgendaDay(
                date,
                List.of(),
                BigDecimal.ZERO.setScale(2),
                BigDecimal.ZERO.setScale(2),
                BigDecimal.ZERO.setScale(2));
    }

    public Set<AgendaEntryType> markers() {
        return entries.isEmpty()
                ? Set.of()
                : Set.copyOf(entries.stream()
                        .map(AgendaEntry::type)
                        .collect(java.util.stream.Collectors.toCollection(
                                () -> EnumSet.noneOf(AgendaEntryType.class))));
    }

    private static BigDecimal money(BigDecimal value) {
        BigDecimal required = Objects.requireNonNull(
                value,
                "Agenda total is required.");
        if (required.signum() < 0) {
            throw new IllegalArgumentException("Agenda total is invalid.");
        }
        return required.setScale(2);
    }
}
