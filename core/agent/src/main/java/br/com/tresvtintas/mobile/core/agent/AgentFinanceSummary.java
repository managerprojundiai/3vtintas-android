package br.com.tresvtintas.mobile.core.agent;

import br.com.tresvtintas.mobile.core.finance.FinanceEntryStatus;
import br.com.tresvtintas.mobile.core.finance.FinanceEntryType;
import br.com.tresvtintas.mobile.core.finance.FinancePaymentMethod;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public record AgentFinanceSummary(
        AgentFinanceScope scope,
        AgentFinanceOperation operation,
        Optional<Long> entryId,
        Optional<Long> organizationId,
        Optional<String> organizationName,
        Optional<Long> customerId,
        Optional<String> customerName,
        FinanceEntryType type,
        String title,
        BigDecimal amount,
        Optional<Instant> dueAt,
        Optional<String> notes,
        Optional<FinanceEntryStatus> beforeStatus,
        FinanceEntryStatus afterStatus,
        Optional<FinancePaymentMethod> paymentMethod,
        Optional<String> paymentReference,
        boolean accessAndStateWillBeRevalidated)
        implements AgentActionSummary {
    public AgentFinanceSummary {
        scope = Objects.requireNonNull(
                scope,
                "Agent finance scope is required.");
        operation = Objects.requireNonNull(
                operation,
                "Agent finance operation is required.");
        entryId = optionalPositive(entryId, "Agent finance entry ID");
        organizationId = optionalPositive(
                organizationId,
                "Agent finance organization ID");
        organizationName = optionalText(
                organizationName,
                "Agent finance organization",
                200);
        customerId = optionalPositive(
                customerId,
                "Agent finance customer ID");
        customerName = optionalText(
                customerName,
                "Agent finance customer",
                500);
        type = Objects.requireNonNull(
                type,
                "Agent finance type is required.");
        title = requireText(title, "Agent finance title", 200);
        amount = Objects.requireNonNull(
                amount,
                "Agent finance amount is required.");
        dueAt = Objects.requireNonNull(
                dueAt,
                "Agent finance due date is required.");
        notes = optionalText(notes, "Agent finance notes", 20_000);
        beforeStatus = Objects.requireNonNull(
                beforeStatus,
                "Agent finance previous status is required.");
        afterStatus = Objects.requireNonNull(
                afterStatus,
                "Agent finance next status is required.");
        paymentMethod = Objects.requireNonNull(
                paymentMethod,
                "Agent finance payment method is required.");
        paymentReference = optionalText(
                paymentReference,
                "Agent finance payment reference",
                2_000);
        if (amount.signum() <= 0
                || amount.scale() > 2
                || amount.precision() > 10
                || !validScope(scope, organizationId, organizationName)
                || !validTransition(
                        operation,
                        entryId,
                        beforeStatus,
                        afterStatus,
                        paymentMethod,
                        paymentReference)
                || !accessAndStateWillBeRevalidated) {
            throw new IllegalArgumentException(
                    "Agent finance summary is invalid.");
        }
    }

    private static boolean validScope(
            AgentFinanceScope scope,
            Optional<Long> organizationId,
            Optional<String> organizationName) {
        return scope == AgentFinanceScope.CORPORATE
                ? organizationId.isPresent()
                        && organizationName.isPresent()
                : organizationId.isEmpty()
                        && organizationName.isEmpty();
    }

    private static boolean validTransition(
            AgentFinanceOperation operation,
            Optional<Long> entryId,
            Optional<FinanceEntryStatus> beforeStatus,
            FinanceEntryStatus afterStatus,
            Optional<FinancePaymentMethod> paymentMethod,
            Optional<String> paymentReference) {
        if (operation == AgentFinanceOperation.CREATE) {
            return entryId.isEmpty()
                    && beforeStatus.isEmpty()
                    && afterStatus == FinanceEntryStatus.PENDING
                    && paymentMethod.isEmpty()
                    && paymentReference.isEmpty();
        }
        if (operation == AgentFinanceOperation.SETTLE) {
            return entryId.isPresent()
                    && beforeStatus.filter(
                            value -> value == FinanceEntryStatus.PENDING)
                            .isPresent()
                    && afterStatus == FinanceEntryStatus.SETTLED
                    && paymentMethod.isPresent();
        }
        return entryId.isPresent()
                && beforeStatus.filter(
                        value -> value == FinanceEntryStatus.PENDING)
                        .isPresent()
                && afterStatus == FinanceEntryStatus.CANCELLED
                && paymentMethod.isEmpty()
                && paymentReference.isEmpty();
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
