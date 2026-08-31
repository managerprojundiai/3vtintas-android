package br.com.tresvtintas.mobile.core.network.dto;

import java.math.BigDecimal;
import java.util.Set;

public record AgentFinanceSummaryDto(
        String scope,
        String operation,
        Long entryId,
        Long organizationId,
        String organizationName,
        Long customerId,
        String customerName,
        String type,
        String title,
        String amount,
        String currency,
        String dueAt,
        String notes,
        String beforeStatus,
        String afterStatus,
        String paymentMethod,
        String paymentReference,
        boolean accessAndStateWillBeRevalidated) {
    private static final String CREATE_OPERATION = "create";
    private static final String SETTLE_OPERATION = "settle";
    private static final Set<String> TYPES =
            Set.of("payable", "receivable", "expense");
    private static final Set<String> METHODS = Set.of(
            "pix",
            "transfer",
            "cash",
            "bank_slip",
            "other");

    public AgentFinanceSummaryDto {
        entryId = DtoValidation.optionalPositive(
                entryId,
                "Agent finance entry ID");
        organizationId = DtoValidation.optionalPositive(
                organizationId,
                "Agent finance organization ID");
        organizationName = DtoValidation.optionalText(
                organizationName,
                "Agent finance organization",
                200);
        customerId = DtoValidation.optionalPositive(
                customerId,
                "Agent finance customer ID");
        customerName = DtoValidation.optionalText(
                customerName,
                "Agent finance customer",
                500);
        title = DtoValidation.requireText(
                title,
                "Agent finance title",
                200);
        if (dueAt != null) {
            dueAt = DtoValidation.requireInstant(
                    dueAt,
                    "Agent finance due date");
        }
        notes = DtoValidation.optionalText(
                notes,
                "Agent finance notes",
                20_000);
        paymentReference = DtoValidation.optionalText(
                paymentReference,
                "Agent finance payment reference",
                2_000);
        if (!Set.of("personal", "corporate").contains(scope)
                || !TYPES.contains(type)
                || !"BRL".equals(currency)
                || !validMoney(amount)
                || !validScope(scope, organizationId, organizationName)
                || !validOperation(
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
            String scope,
            Long organizationId,
            String organizationName) {
        return "corporate".equals(scope)
                ? organizationId != null && organizationName != null
                : organizationId == null && organizationName == null;
    }

    private static boolean validOperation(
            String operation,
            Long entryId,
            String beforeStatus,
            String afterStatus,
            String paymentMethod,
            String paymentReference) {
        if (CREATE_OPERATION.equals(operation)) {
            return entryId == null
                    && beforeStatus == null
                    && "pending".equals(afterStatus)
                    && paymentMethod == null
                    && paymentReference == null;
        }
        if (SETTLE_OPERATION.equals(operation)) {
            return entryId != null
                    && "pending".equals(beforeStatus)
                    && "settled".equals(afterStatus)
                    && METHODS.contains(paymentMethod);
        }
        return "cancel".equals(operation)
                && entryId != null
                && "pending".equals(beforeStatus)
                && "cancelled".equals(afterStatus)
                && paymentMethod == null
                && paymentReference == null;
    }

    private static boolean validMoney(String value) {
        if (value == null
                || !value.matches("^\\d{1,8}\\.\\d{2}$")) {
            return false;
        }
        return new BigDecimal(value).signum() > 0;
    }
}
