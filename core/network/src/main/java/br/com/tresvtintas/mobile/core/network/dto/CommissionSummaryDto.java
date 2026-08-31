package br.com.tresvtintas.mobile.core.network.dto;

import java.util.List;
import java.util.Set;

public record CommissionSummaryDto(
        long id,
        String kind,
        String status,
        Recipient recipient,
        Organization organization,
        Order order,
        Calculation calculation,
        int revision,
        boolean eligibleForApproval,
        List<String> allowedActions,
        String approvedAt,
        String paidAt,
        String cancelledAt,
        String createdAt,
        String updatedAt) {
    private static final Set<String> KINDS = Set.of("seller", "global_admin");
    private static final Set<String> STATUSES =
            Set.of("pending", "approved", "paid", "cancelled");
    private static final Set<String> ACTIONS =
            Set.of("approve", "cancel", "pay");

    public CommissionSummaryDto {
        id = DtoValidation.requirePositive(id, "Commission ID");
        kind = DtoValidation.requireText(kind, "Commission kind", 30);
        status = DtoValidation.requireText(status, "Commission status", 30);
        if (!KINDS.contains(kind)
                || !STATUSES.contains(status)
                || recipient == null
                || calculation == null
                || revision < 1
                || allowedActions == null
                || allowedActions.size() > ACTIONS.size()
                || !ACTIONS.containsAll(allowedActions)
                || Set.copyOf(allowedActions).size() != allowedActions.size()) {
            throw new IllegalArgumentException("Commission summary is invalid.");
        }
        allowedActions = List.copyOf(allowedActions);
        approvedAt = optionalInstant(approvedAt, "Commission approval");
        paidAt = optionalInstant(paidAt, "Commission payment");
        cancelledAt = optionalInstant(cancelledAt, "Commission cancellation");
        createdAt = DtoValidation.requireInstant(createdAt, "Commission creation");
        updatedAt = DtoValidation.requireInstant(updatedAt, "Commission update");
    }

    static String money(String value) {
        if (value == null || !value.matches("^\\d{1,8}\\.\\d{2}$")) {
            throw new IllegalArgumentException(
                    "Commission monetary value is invalid.");
        }
        return value;
    }

    private static String optionalInstant(String value, String name) {
        if (value != null) {
            DtoValidation.requireInstant(value, name);
        }
        return value;
    }

    public record Recipient(Long userId, String role, String name) {
        private static final Set<String> ROLES =
                Set.of("painter", "salesperson", "master_admin");

        public Recipient {
            userId = DtoValidation.optionalPositive(
                    userId,
                    "Commission recipient ID");
            role = DtoValidation.requireText(
                    role,
                    "Commission recipient role",
                    30);
            name = DtoValidation.optionalText(
                    name,
                    "Commission recipient name",
                    500);
            if (!ROLES.contains(role)) {
                throw new IllegalArgumentException(
                        "Commission recipient role is invalid.");
            }
        }
    }

    public record Organization(long id, String name) {
        public Organization {
            id = DtoValidation.requirePositive(id, "Commission organization ID");
            name = DtoValidation.requireText(
                    name,
                    "Commission organization name",
                    200);
        }
    }

    public record Order(long id, String type, String paymentStatus) {
        public Order {
            id = DtoValidation.requirePositive(id, "Commission order ID");
            type = DtoValidation.requireText(type, "Commission order type", 20);
            paymentStatus = DtoValidation.requireText(
                    paymentStatus,
                    "Commission order payment",
                    20);
            if ((!"material".equals(type) && !"labor".equals(type))
                    || (!"pending".equals(paymentStatus)
                            && !"received".equals(paymentStatus))) {
                throw new IllegalArgumentException(
                        "Commission order is invalid.");
            }
        }
    }

    public record Calculation(
            String currency,
            String baseAmount,
            String ratePercent,
            String amount,
            String ruleVersion) {
        public Calculation {
            if (!"BRL".equals(currency)
                    || ratePercent == null
                    || !ratePercent.matches("^\\d{1,3}\\.\\d{2}$")) {
                throw new IllegalArgumentException(
                        "Commission calculation is invalid.");
            }
            baseAmount = money(baseAmount);
            amount = money(amount);
            ruleVersion = DtoValidation.requireText(
                    ruleVersion,
                    "Commission rule version",
                    64);
        }
    }
}
