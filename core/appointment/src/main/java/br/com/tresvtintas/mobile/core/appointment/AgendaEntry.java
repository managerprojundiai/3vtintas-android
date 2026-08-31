package br.com.tresvtintas.mobile.core.appointment;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public record AgendaEntry(
        AgendaEntryType type,
        AgendaEntrySource source,
        long sourceId,
        String title,
        Instant occursAt,
        String status,
        Optional<BigDecimal> amount) {
    public AgendaEntry {
        type = Objects.requireNonNull(type, "Agenda entry type is required.");
        source = Objects.requireNonNull(
                source,
                "Agenda entry source is required.");
        title = Objects.requireNonNull(title, "Agenda entry title is required.")
                .trim();
        occursAt = Objects.requireNonNull(
                occursAt,
                "Agenda entry instant is required.");
        status = Objects.requireNonNull(
                status,
                "Agenda entry status is required.");
        amount = amount == null ? Optional.empty() : amount;
        if (sourceId < 1
                || title.isEmpty()
                || title.length() > 200
                || status.isBlank()
                || (type.isFinancial() != amount.isPresent())
                || (type.isFinancial()
                        != (source == AgendaEntrySource.FINANCE))
                || (source == AgendaEntrySource.DELIVERY
                        && type != AgendaEntryType.DELIVERY)
                || amount.filter(value -> value.signum() < 0).isPresent()) {
            throw new IllegalArgumentException("Agenda entry is invalid.");
        }
    }
}
