package br.com.tresvtintas.mobile.core.commission;

public record CommissionMutationResult(
        CommissionAction action,
        long commissionId,
        CommissionStatus status,
        int revision,
        boolean changed,
        boolean replayed) {
    public CommissionMutationResult {
        if (action == null
                || commissionId < 1
                || status == null
                || revision < 1) {
            throw new IllegalArgumentException(
                    "Commission mutation result is invalid.");
        }
    }
}
