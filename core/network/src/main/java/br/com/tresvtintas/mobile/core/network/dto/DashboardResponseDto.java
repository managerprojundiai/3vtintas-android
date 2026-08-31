package br.com.tresvtintas.mobile.core.network.dto;

import java.util.Set;
import java.util.regex.Pattern;

public record DashboardResponseDto(
        String generatedAt,
        Context context,
        Workload workload,
        Commissions commissions,
        Appointments appointments,
        Finance finance) {
    private static final Set<String> VISIBILITIES =
            Set.of("self", "team", "all");
    private static final Set<String> COMMISSION_SCOPES =
            Set.of("self", "team");
    private static final Set<String> APPOINTMENT_SCOPES =
            Set.of("self", "team", "all");
    private static final Set<String> FINANCE_SCOPES =
            Set.of("personal", "corporate");
    private static final Pattern MONEY =
            Pattern.compile("^\\d{1,18}\\.\\d{2}$");

    public DashboardResponseDto {
        generatedAt = DtoValidation.requireInstant(
                generatedAt,
                "Dashboard generation time");
        if (context == null || workload == null) {
            throw new IllegalArgumentException(
                    "Dashboard response is invalid.");
        }
    }

    public record Context(
            String visibility,
            Organization organization) {
        public Context {
            visibility = requireEnum(
                    visibility,
                    VISIBILITIES,
                    "Dashboard visibility");
        }
    }

    public record Organization(long id, String name) {
        public Organization {
            id = DtoValidation.requirePositive(id, "Dashboard organization ID");
            name = DtoValidation.requireText(
                    name,
                    "Dashboard organization name",
                    200);
        }
    }

    public record Workload(
            Orders orders,
            Quotes quotes,
            Deliveries deliveries) {
    }

    public record Orders(
            int total,
            int active,
            int awaitingPayment) {
        public Orders {
            nonNegative(total, active, awaitingPayment);
        }
    }

    public record Quotes(
            int activeMaterial,
            int activeLabor,
            int expiringSoon) {
        public Quotes {
            nonNegative(activeMaterial, activeLabor, expiringSoon);
        }
    }

    public record Deliveries(
            int active,
            int today,
            int inTransit,
            Integer awaitingSchedule) {
        public Deliveries {
            nonNegative(active, today, inTransit);
            if (awaitingSchedule != null) {
                nonNegative(awaitingSchedule);
            }
        }
    }

    public record CountAmount(int count, String amount) {
        public CountAmount {
            nonNegative(count);
            if (amount == null || !MONEY.matcher(amount).matches()) {
                throw new IllegalArgumentException(
                        "Dashboard aggregate amount is invalid.");
            }
        }
    }

    public record Commissions(
            String scope,
            CountAmount pending,
            CountAmount approved) {
        public Commissions {
            scope = requireEnum(
                    scope,
                    COMMISSION_SCOPES,
                    "Dashboard commission scope");
            requireAggregates(pending, approved);
        }
    }

    public record Appointments(
            String scope,
            int today,
            int upcoming) {
        public Appointments {
            scope = requireEnum(
                    scope,
                    APPOINTMENT_SCOPES,
                    "Dashboard appointment scope");
            nonNegative(today, upcoming);
        }
    }

    public record Finance(
            String scope,
            FinanceTotals pending,
            FinanceTotals overdue) {
        public Finance {
            scope = requireEnum(
                    scope,
                    FINANCE_SCOPES,
                    "Dashboard finance scope");
            if (pending == null || overdue == null) {
                throw new IllegalArgumentException(
                        "Dashboard finance aggregates are invalid.");
            }
        }
    }

    public record FinanceTotals(
            CountAmount expense,
            CountAmount payable,
            CountAmount receivable) {
        public FinanceTotals {
            requireAggregates(expense, payable, receivable);
        }
    }

    private static String requireEnum(
            String value,
            Set<String> accepted,
            String fieldName) {
        if (value == null || !accepted.contains(value)) {
            throw new IllegalArgumentException(fieldName + " is invalid.");
        }
        return value;
    }

    private static void requireAggregates(CountAmount... values) {
        for (CountAmount value : values) {
            if (value == null) {
                throw new IllegalArgumentException(
                        "Dashboard aggregate is missing.");
            }
        }
    }

    private static void nonNegative(int... values) {
        for (int value : values) {
            if (value < 0) {
                throw new IllegalArgumentException(
                        "Dashboard count is invalid.");
            }
        }
    }
}
