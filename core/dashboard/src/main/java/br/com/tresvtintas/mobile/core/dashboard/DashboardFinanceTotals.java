package br.com.tresvtintas.mobile.core.dashboard;

import java.util.Objects;

public record DashboardFinanceTotals(
        DashboardCountAmount expense,
        DashboardCountAmount payable,
        DashboardCountAmount receivable) {

    public DashboardFinanceTotals {
        Objects.requireNonNull(expense, "Expense aggregate is required.");
        Objects.requireNonNull(payable, "Payable aggregate is required.");
        Objects.requireNonNull(receivable, "Receivable aggregate is required.");
    }
}
