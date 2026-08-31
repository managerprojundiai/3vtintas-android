package br.com.tresvtintas.mobile.core.network.dto;

import java.math.BigDecimal;
import java.util.Set;

public record AgentCommissionSummaryDto(
        long commissionId,
        String operation,
        String kind,
        String recipientRole,
        String recipientName,
        String organizationName,
        Long orderId,
        String orderPaymentStatus,
        String amount,
        String currency,
        int expectedRevision,
        String beforeStatus,
        String afterStatus,
        String cancellationReason,
        String paymentMethod,
        String paymentReference,
        boolean accessRevisionAndEligibilityWillBeRevalidated) {
    private static final String APPROVE_OPERATION = "approve";
    private static final String CANCEL_OPERATION = "cancel";
    private static final Set<String> METHODS = Set.of(
            "pix",
            "transfer",
            "cash",
            "bank_slip",
            "other");

    public AgentCommissionSummaryDto {
        commissionId = DtoValidation.requirePositive(
                commissionId,
                "Agent commission ID");
        recipientName = DtoValidation.requireText(
                recipientName,
                "Agent commission recipient",
                200);
        organizationName = DtoValidation.optionalText(
                organizationName,
                "Agent commission organization",
                200);
        orderId = DtoValidation.optionalPositive(
                orderId,
                "Agent commission order ID");
        cancellationReason = DtoValidation.optionalText(
                cancellationReason,
                "Agent commission cancellation reason",
                500);
        paymentReference = DtoValidation.optionalText(
                paymentReference,
                "Agent commission payment reference",
                255);
        if (!Set.of("seller", "global_admin").contains(kind)
                || !Set.of("painter", "salesperson", "master_admin")
                        .contains(recipientRole)
                || (orderPaymentStatus != null
                        && !Set.of("pending", "received")
                                .contains(orderPaymentStatus))
                || !"BRL".equals(currency)
                || !validMoney(amount)
                || expectedRevision < 1
                || !validOperation(
                        operation,
                        beforeStatus,
                        afterStatus,
                        cancellationReason,
                        paymentMethod,
                        paymentReference)
                || !accessRevisionAndEligibilityWillBeRevalidated) {
            throw new IllegalArgumentException(
                    "Agent commission summary is invalid.");
        }
    }

    private static boolean validOperation(
            String operation,
            String beforeStatus,
            String afterStatus,
            String cancellationReason,
            String paymentMethod,
            String paymentReference) {
        if (APPROVE_OPERATION.equals(operation)) {
            return "pending".equals(beforeStatus)
                    && "approved".equals(afterStatus)
                    && cancellationReason == null
                    && paymentMethod == null
                    && paymentReference == null;
        }
        if (CANCEL_OPERATION.equals(operation)) {
            return Set.of("pending", "approved").contains(beforeStatus)
                    && "cancelled".equals(afterStatus)
                    && cancellationReason != null
                    && cancellationReason.trim().length() >= 10
                    && paymentMethod == null
                    && paymentReference == null;
        }
        return "pay".equals(operation)
                && "approved".equals(beforeStatus)
                && "paid".equals(afterStatus)
                && cancellationReason == null
                && METHODS.contains(paymentMethod);
    }

    private static boolean validMoney(String value) {
        if (value == null
                || !value.matches("^\\d{1,8}\\.\\d{2}$")) {
            return false;
        }
        return new BigDecimal(value).signum() > 0;
    }
}
