package br.com.tresvtintas.mobile.core.dashboard;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public record DashboardSnapshot(
        Instant generatedAt,
        DashboardContext context,
        DashboardWorkload workload,
        Optional<Commissions> commissions,
        Optional<Appointments> appointments,
        Optional<Finance> finance) {

    public DashboardSnapshot {
        Objects.requireNonNull(generatedAt, "Dashboard generation time is required.");
        Objects.requireNonNull(context, "Dashboard context is required.");
        Objects.requireNonNull(workload, "Dashboard workload is required.");
        commissions = optional(commissions);
        appointments = optional(appointments);
        finance = optional(finance);
    }

    public enum CommissionScope {
        SELF,
        TEAM
    }

    public enum AppointmentScope {
        SELF,
        TEAM,
        ALL
    }

    public enum FinanceScope {
        PERSONAL,
        CORPORATE
    }

    public record Commissions(
            CommissionScope scope,
            DashboardCountAmount pending,
            DashboardCountAmount approved) {
        public Commissions {
            Objects.requireNonNull(scope, "Commission scope is required.");
            Objects.requireNonNull(pending, "Pending commissions are required.");
            Objects.requireNonNull(approved, "Approved commissions are required.");
        }
    }

    public record Appointments(
            AppointmentScope scope,
            int today,
            int upcoming) {
        public Appointments {
            Objects.requireNonNull(scope, "Appointment scope is required.");
            if (today < 0 || upcoming < 0) {
                throw new IllegalArgumentException(
                        "Dashboard appointment count is invalid.");
            }
        }
    }

    public record Finance(
            FinanceScope scope,
            DashboardFinanceTotals pending,
            DashboardFinanceTotals overdue) {
        public Finance {
            Objects.requireNonNull(scope, "Finance scope is required.");
            Objects.requireNonNull(pending, "Pending finance totals are required.");
            Objects.requireNonNull(overdue, "Overdue finance totals are required.");
        }
    }

    private static <T> Optional<T> optional(Optional<T> value) {
        return value == null ? Optional.empty() : value;
    }
}
