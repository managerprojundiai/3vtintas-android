package br.com.tresvtintas.mobile.core.accountaccess;

import java.util.Objects;

public record ManagedAccountRevocationResult(
        String actionId,
        AccountAccessView view,
        long targetUserId,
        String resourceId,
        boolean changed,
        int revision) {
    private static final int MINIMUM_IDENTIFIER = 1;

    public ManagedAccountRevocationResult {
        actionId = AccountAccessValidation.uuid(
                actionId,
                "Managed revocation result action ID");
        view = Objects.requireNonNull(
                view,
                "Managed revocation result view is required.");
        resourceId = AccountAccessValidation.uuid(
                resourceId,
                "Managed revocation result resource ID");
        if (targetUserId < MINIMUM_IDENTIFIER
                || !changed
                || revision < MINIMUM_IDENTIFIER) {
            throw new IllegalArgumentException(
                    "Managed revocation result is invalid.");
        }
    }
}
