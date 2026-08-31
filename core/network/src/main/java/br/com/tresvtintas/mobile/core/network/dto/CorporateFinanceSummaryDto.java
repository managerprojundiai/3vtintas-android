package br.com.tresvtintas.mobile.core.network.dto;

import java.util.List;
import java.util.Set;

public record CorporateFinanceSummaryDto(
        long id,
        String type,
        String status,
        String source,
        String title,
        String amount,
        String currency,
        String dueAt,
        String settledAt,
        CorporateFinanceOrganizationDto organization,
        Customer customer,
        List<String> allowedActions,
        String createdAt,
        String updatedAt) {
    private static final Set<String> TYPES =
            Set.of("expense", "payable", "receivable");
    private static final Set<String> STATUSES =
            Set.of("pending", "settled", "cancelled");
    private static final Set<String> SOURCES =
            Set.of("manual", "material_order", "commission", "system");
    private static final Set<String> ACTIONS = Set.of("settle", "cancel");

    public CorporateFinanceSummaryDto {
        id = DtoValidation.requirePositive(id, "Corporate finance ID");
        type = DtoValidation.requireText(type, "Corporate finance type", 20);
        status = DtoValidation.requireText(status, "Corporate finance status", 20);
        source = DtoValidation.requireText(source, "Corporate finance source", 30);
        title = DtoValidation.requireText(title, "Corporate finance title", 200);
        amount = money(amount, 8);
        if (!TYPES.contains(type)
                || !STATUSES.contains(status)
                || !SOURCES.contains(source)
                || !"BRL".equals(currency)
                || organization == null) {
            throw new IllegalArgumentException(
                    "Corporate finance summary is invalid.");
        }
        if (dueAt != null) {
            DtoValidation.requireInstant(dueAt, "Corporate finance due date");
        }
        if (settledAt != null) {
            DtoValidation.requireInstant(
                    settledAt,
                    "Corporate finance settlement");
        }
        if (allowedActions == null
                || allowedActions.size() > 2
                || allowedActions.stream().anyMatch(
                        value -> value == null || !ACTIONS.contains(value))
                || Set.copyOf(allowedActions).size() != allowedActions.size()) {
            throw new IllegalArgumentException(
                    "Corporate finance allowed actions are invalid.");
        }
        allowedActions = List.copyOf(allowedActions);
        createdAt = DtoValidation.requireInstant(
                createdAt,
                "Corporate finance creation");
        updatedAt = DtoValidation.requireInstant(
                updatedAt,
                "Corporate finance update");
    }

    static String money(String value, int integerDigits) {
        if (value == null
                || !value.matches("^\\d{1," + integerDigits + "}\\.\\d{2}$")) {
            throw new IllegalArgumentException(
                    "Corporate finance monetary value is invalid.");
        }
        return value;
    }

    public record Customer(long id, String name) {
        public Customer {
            id = DtoValidation.requirePositive(
                    id,
                    "Corporate finance customer ID");
            name = DtoValidation.requireText(
                    name,
                    "Corporate finance customer name",
                    500);
        }
    }
}
