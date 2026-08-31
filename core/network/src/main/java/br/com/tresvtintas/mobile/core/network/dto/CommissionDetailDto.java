package br.com.tresvtintas.mobile.core.network.dto;

public record CommissionDetailDto(
        long id,
        String kind,
        String status,
        CommissionSummaryDto.Recipient recipient,
        CommissionSummaryDto.Organization organization,
        CommissionSummaryDto.Order order,
        CommissionSummaryDto.Calculation calculation,
        int revision,
        boolean eligibleForApproval,
        java.util.List<String> allowedActions,
        String approvedAt,
        String paidAt,
        String cancelledAt,
        String createdAt,
        String updatedAt,
        String workflowId,
        Long batchId,
        Actor approvedBy,
        Payment payment,
        Cancellation cancellation) {
    public CommissionDetailDto {
        if (allowedActions == null) {
            throw new IllegalArgumentException(
                    "Commission actions are required.");
        }
        allowedActions = java.util.List.copyOf(allowedActions);
        new CommissionSummaryDto(
                id,
                kind,
                status,
                recipient,
                organization,
                order,
                calculation,
                revision,
                eligibleForApproval,
                allowedActions,
                approvedAt,
                paidAt,
                cancelledAt,
                createdAt,
                updatedAt);
        workflowId = DtoValidation.optionalText(
                workflowId,
                "Commission workflow",
                64);
        batchId = DtoValidation.optionalPositive(batchId, "Commission batch ID");
    }

    public record Payment(
            String method,
            String reference,
            Actor paidBy) {
        private static final java.util.Set<String> METHODS = java.util.Set.of(
                "pix",
                "transfer",
                "cash",
                "bank_slip",
                "other");

        public Payment {
            method = DtoValidation.optionalText(
                    method,
                    "Commission payment method",
                    20);
            reference = DtoValidation.optionalText(
                    reference,
                    "Commission payment reference",
                    255);
            if (method != null && !METHODS.contains(method)) {
                throw new IllegalArgumentException(
                        "Commission payment method is invalid.");
            }
        }
    }

    public record Actor(long userId, String name) {
        public Actor {
            userId = DtoValidation.requirePositive(userId, "Commission actor ID");
            name = DtoValidation.optionalText(
                    name,
                    "Commission actor name",
                    500);
        }
    }

    public record Cancellation(String reason, Actor cancelledBy) {
        public Cancellation {
            reason = DtoValidation.optionalText(
                    reason,
                    "Commission cancellation reason",
                    10_000);
        }
    }
}
