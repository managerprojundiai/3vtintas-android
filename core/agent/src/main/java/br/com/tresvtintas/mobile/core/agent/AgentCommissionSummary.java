package br.com.tresvtintas.mobile.core.agent;

import br.com.tresvtintas.mobile.core.commission.CommissionKind;
import br.com.tresvtintas.mobile.core.commission.CommissionPaymentMethod;
import br.com.tresvtintas.mobile.core.commission.CommissionRecipientRole;
import br.com.tresvtintas.mobile.core.commission.CommissionStatus;
import br.com.tresvtintas.mobile.core.order.OrderPaymentStatus;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;

public record AgentCommissionSummary(
        long commissionId,
        AgentCommissionOperation operation,
        CommissionKind kind,
        CommissionRecipientRole recipientRole,
        String recipientName,
        Optional<String> organizationName,
        Optional<Long> orderId,
        Optional<OrderPaymentStatus> orderPaymentStatus,
        BigDecimal amount,
        int expectedRevision,
        CommissionStatus beforeStatus,
        CommissionStatus afterStatus,
        Optional<String> cancellationReason,
        Optional<CommissionPaymentMethod> paymentMethod,
        Optional<String> paymentReference,
        boolean accessRevisionAndEligibilityWillBeRevalidated)
        implements AgentActionSummary {
    public AgentCommissionSummary {
        operation = Objects.requireNonNull(
                operation,
                "Agent commission operation is required.");
        kind = Objects.requireNonNull(
                kind,
                "Agent commission kind is required.");
        recipientRole = Objects.requireNonNull(
                recipientRole,
                "Agent commission recipient role is required.");
        recipientName = requireText(
                recipientName,
                "Agent commission recipient",
                200);
        organizationName = optionalText(
                organizationName,
                "Agent commission organization",
                200);
        orderId = optionalPositive(
                orderId,
                "Agent commission order ID");
        orderPaymentStatus = Objects.requireNonNull(
                orderPaymentStatus,
                "Agent commission order payment status is required.");
        amount = Objects.requireNonNull(
                amount,
                "Agent commission amount is required.");
        beforeStatus = Objects.requireNonNull(
                beforeStatus,
                "Agent commission previous status is required.");
        afterStatus = Objects.requireNonNull(
                afterStatus,
                "Agent commission next status is required.");
        cancellationReason = optionalText(
                cancellationReason,
                "Agent commission cancellation reason",
                500);
        paymentMethod = Objects.requireNonNull(
                paymentMethod,
                "Agent commission payment method is required.");
        paymentReference = optionalText(
                paymentReference,
                "Agent commission payment reference",
                255);
        if (commissionId <= 0
                || expectedRevision <= 0
                || amount.signum() <= 0
                || amount.scale() > 2
                || amount.precision() > 10
                || !validTransition(
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

    private static boolean validTransition(
            AgentCommissionOperation operation,
            CommissionStatus beforeStatus,
            CommissionStatus afterStatus,
            Optional<String> cancellationReason,
            Optional<CommissionPaymentMethod> paymentMethod,
            Optional<String> paymentReference) {
        if (operation == AgentCommissionOperation.APPROVE) {
            return beforeStatus == CommissionStatus.PENDING
                    && afterStatus == CommissionStatus.APPROVED
                    && cancellationReason.isEmpty()
                    && paymentMethod.isEmpty()
                    && paymentReference.isEmpty();
        }
        if (operation == AgentCommissionOperation.CANCEL) {
            return (beforeStatus == CommissionStatus.PENDING
                            || beforeStatus == CommissionStatus.APPROVED)
                    && afterStatus == CommissionStatus.CANCELLED
                    && cancellationReason.filter(
                            value -> value.length() >= 10).isPresent()
                    && paymentMethod.isEmpty()
                    && paymentReference.isEmpty();
        }
        return beforeStatus == CommissionStatus.APPROVED
                && afterStatus == CommissionStatus.PAID
                && cancellationReason.isEmpty()
                && paymentMethod.isPresent();
    }

    private static Optional<Long> optionalPositive(
            Optional<Long> value,
            String label) {
        Optional<Long> required = Objects.requireNonNull(
                value,
                label + " is required.");
        if (required.filter(id -> id <= 0).isPresent()) {
            throw new IllegalArgumentException(label + " is invalid.");
        }
        return required;
    }

    private static Optional<String> optionalText(
            Optional<String> value,
            String label,
            int maxLength) {
        Optional<String> required = Objects.requireNonNull(
                value,
                label + " is required.");
        return required.map(text -> requireText(text, label, maxLength));
    }

    private static String requireText(
            String value,
            String label,
            int maxLength) {
        if (value == null
                || value.isBlank()
                || !value.equals(value.trim())
                || value.length() > maxLength) {
            throw new IllegalArgumentException(label + " is invalid.");
        }
        return value;
    }
}
