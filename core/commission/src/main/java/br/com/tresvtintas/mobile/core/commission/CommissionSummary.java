package br.com.tresvtintas.mobile.core.commission;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;

public record CommissionSummary(
        long id,
        CommissionKind kind,
        CommissionStatus status,
        Recipient recipient,
        Optional<Organization> organization,
        Optional<Order> order,
        Calculation calculation,
        int revision,
        boolean eligibleForApproval,
        Set<CommissionAction> allowedActions,
        Optional<Instant> approvedAt,
        Optional<Instant> paidAt,
        Optional<Instant> cancelledAt,
        Instant createdAt,
        Instant updatedAt) {
    public CommissionSummary {
        if (id < 1
                || kind == null
                || status == null
                || recipient == null
                || calculation == null
                || revision < 1
                || createdAt == null
                || updatedAt == null) {
            throw new IllegalArgumentException("Commission summary is invalid.");
        }
        organization = organization == null ? Optional.empty() : organization;
        order = order == null ? Optional.empty() : order;
        allowedActions = allowedActions == null
                ? Set.of()
                : Set.copyOf(allowedActions);
        if (allowedActions.size() > CommissionAction.values().length) {
            throw new IllegalArgumentException(
                    "Commission actions are invalid.");
        }
        approvedAt = approvedAt == null ? Optional.empty() : approvedAt;
        paidAt = paidAt == null ? Optional.empty() : paidAt;
        cancelledAt = cancelledAt == null ? Optional.empty() : cancelledAt;
    }

    public record Recipient(
            OptionalLong userId,
            CommissionRecipientRole role,
            Optional<String> name) {
        public Recipient {
            userId = userId == null ? OptionalLong.empty() : userId;
            name = name == null ? Optional.empty() : name;
            if ((userId.isPresent() && userId.orElseThrow() < 1)
                    || role == null
                    || name.filter(String::isBlank).isPresent()) {
                throw new IllegalArgumentException("Commission recipient is invalid.");
            }
        }
    }

    public record Organization(long id, String name) {
        public Organization {
            if (id < 1 || name == null || name.isBlank()) {
                throw new IllegalArgumentException("Commission organization is invalid.");
            }
        }
    }

    public record Order(long id, String type, String paymentStatus) {
        public Order {
            if (id < 1
                    || (!"material".equals(type) && !"labor".equals(type))
                    || (!"pending".equals(paymentStatus)
                            && !"received".equals(paymentStatus))) {
                throw new IllegalArgumentException("Commission order is invalid.");
            }
        }
    }

    public record Calculation(
            String currency,
            BigDecimal baseAmount,
            BigDecimal ratePercent,
            BigDecimal amount,
            String ruleVersion) {
        public Calculation {
            if (!"BRL".equals(currency)
                    || baseAmount == null
                    || baseAmount.signum() < 0
                    || ratePercent == null
                    || ratePercent.signum() < 0
                    || ratePercent.compareTo(new BigDecimal("100.00")) > 0
                    || amount == null
                    || amount.signum() < 0
                    || ruleVersion == null
                    || ruleVersion.isBlank()) {
                throw new IllegalArgumentException("Commission calculation is invalid.");
            }
        }
    }
}
