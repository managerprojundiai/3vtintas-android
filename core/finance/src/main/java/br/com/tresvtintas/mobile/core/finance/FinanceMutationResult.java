package br.com.tresvtintas.mobile.core.finance;

public record FinanceMutationResult(
        FinanceAction action,
        long entryId,
        FinanceEntryStatus status,
        boolean changed,
        boolean replayed) {
    public FinanceMutationResult {
        if (action == null || entryId < 1 || status == null) {
            throw new IllegalArgumentException("Finance mutation result is invalid.");
        }
    }
}
