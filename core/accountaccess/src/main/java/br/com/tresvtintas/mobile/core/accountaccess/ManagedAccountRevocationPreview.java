package br.com.tresvtintas.mobile.core.accountaccess;

import java.time.Instant;
import java.util.Objects;

public record ManagedAccountRevocationPreview(
        String actionId,
        AccountAccessView view,
        long targetUserId,
        String targetName,
        String resourceId,
        String resourceLabel,
        int revision,
        String consequence,
        Instant expiresAt) {
    private static final int MINIMUM_IDENTIFIER = 1;

    public ManagedAccountRevocationPreview {
        actionId = AccountAccessValidation.uuid(
                actionId,
                "Managed revocation action ID");
        view = Objects.requireNonNull(
                view,
                "Managed revocation view is required.");
        if (targetUserId < MINIMUM_IDENTIFIER
                || revision < MINIMUM_IDENTIFIER) {
            throw new IllegalArgumentException(
                    "Managed revocation target is invalid.");
        }
        targetName = AccountAccessValidation.text(
                targetName,
                "Managed revocation target name",
                200);
        resourceId = AccountAccessValidation.uuid(
                resourceId,
                "Managed revocation resource ID");
        resourceLabel = AccountAccessValidation.text(
                resourceLabel,
                "Managed revocation resource label",
                240);
        consequence = AccountAccessValidation.text(
                consequence,
                "Managed revocation consequence",
                240);
        expiresAt = Objects.requireNonNull(
                expiresAt,
                "Managed revocation expiry is required.");
    }

    public boolean isExpired(Instant now) {
        return !expiresAt.isAfter(Objects.requireNonNull(
                now,
                "Managed revocation clock is required."));
    }
}
