package br.com.tresvtintas.mobile.core.finance;

import java.math.BigDecimal;

public record FinanceOverview(
        TypeTotals pending,
        TypeTotals settled,
        TypeTotals cancelled,
        TypeTotals overdue) {
    public FinanceOverview {
        if (pending == null
                || settled == null
                || cancelled == null
                || overdue == null) {
            throw new IllegalArgumentException("Finance overview is invalid.");
        }
    }

    public record TypeTotals(
            MoneyTotal expense,
            MoneyTotal payable,
            MoneyTotal receivable) {
        public TypeTotals {
            if (expense == null || payable == null || receivable == null) {
                throw new IllegalArgumentException(
                        "Finance type totals are invalid.");
            }
        }
    }

    public record MoneyTotal(int count, BigDecimal amount) {
        public MoneyTotal {
            if (count < 0
                    || amount == null
                    || amount.signum() < 0
                    || amount.scale() != 2) {
                throw new IllegalArgumentException("Finance totals are invalid.");
            }
        }
    }
}
