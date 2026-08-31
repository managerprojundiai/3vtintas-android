package br.com.tresvtintas.mobile.core.finance;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;

public record FinanceSummary(
        long id,
        FinanceEntryType type,
        FinanceEntryStatus status,
        FinanceEntrySource source,
        String title,
        BigDecimal amount,
        Optional<Instant> dueAt,
        Optional<Instant> settledAt,
        Optional<Organization> organization,
        Optional<Customer> customer,
        Set<FinanceAction> allowedActions,
        Instant createdAt,
        Instant updatedAt) {
    public FinanceSummary {
        dueAt = dueAt == null ? Optional.empty() : dueAt;
        settledAt = settledAt == null ? Optional.empty() : settledAt;
        organization = organization == null ? Optional.empty() : organization;
        customer = customer == null ? Optional.empty() : customer;
        if (id < 1
                || type == null
                || status == null
                || source == null
                || title == null
                || title.isBlank()
                || title.length() > 200
                || amount == null
                || amount.signum() < 0
                || amount.scale() != 2
                || allowedActions == null
                || allowedActions.stream().anyMatch(java.util.Objects::isNull)
                || allowedActions.contains(FinanceAction.CREATE)
                || createdAt == null
                || updatedAt == null) {
            throw new IllegalArgumentException("Finance summary is invalid.");
        }
        allowedActions = Set.copyOf(allowedActions);
        if (source != FinanceEntrySource.MANUAL
                && !allowedActions.isEmpty()) {
            throw new IllegalArgumentException(
                    "Only manual finance entries may expose actions.");
        }
    }

    public record Organization(long id, String name) {
        public Organization {
            if (id < 1
                    || name == null
                    || name.isBlank()
                    || name.length() > 200) {
                throw new IllegalArgumentException(
                        "Finance organization is invalid.");
            }
        }
    }

    public record Customer(long id, String name) {
        public Customer {
            if (id < 1 || name == null || name.isBlank()) {
                throw new IllegalArgumentException("Finance customer is invalid.");
            }
        }
    }
}
