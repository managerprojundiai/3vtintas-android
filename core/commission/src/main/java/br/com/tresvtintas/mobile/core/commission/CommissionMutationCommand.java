package br.com.tresvtintas.mobile.core.commission;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;
import java.util.Optional;

public record CommissionMutationCommand(
        CommissionAction action,
        long commissionId,
        int expectedRevision,
        Optional<String> cancellationReason,
        Optional<CommissionPaymentMethod> paymentMethod,
        Optional<String> paymentReference) {
    public CommissionMutationCommand {
        Objects.requireNonNull(action, "Commission action is required.");
        cancellationReason = Objects.requireNonNull(
                cancellationReason,
                "Commission cancellation reason is required.");
        paymentMethod = Objects.requireNonNull(
                paymentMethod,
                "Commission payment method is required.");
        paymentReference = Objects.requireNonNull(
                paymentReference,
                "Commission payment reference is required.");
        if (commissionId < 1 || expectedRevision < 1) {
            throw new IllegalArgumentException(
                    "Commission mutation identity is invalid.");
        }
        cancellationReason = cancellationReason
                .map(String::trim)
                .filter(value -> !value.isEmpty());
        paymentReference = paymentReference
                .map(String::trim)
                .filter(value -> !value.isEmpty());
        boolean cancellationValid = action == CommissionAction.CANCEL
                ? cancellationReason
                        .filter(value -> value.length() >= 10
                                && value.length() <= 500)
                        .isPresent()
                : cancellationReason.isEmpty();
        boolean paymentValid = action == CommissionAction.PAY
                ? paymentMethod.isPresent()
                        && paymentReference
                                .map(value -> value.length() <= 255)
                                .orElse(true)
                : paymentMethod.isEmpty() && paymentReference.isEmpty();
        if (!cancellationValid || !paymentValid) {
            throw new IllegalArgumentException(
                    "Commission mutation payload is invalid.");
        }
    }

    public static CommissionMutationCommand approve(
            long commissionId,
            int expectedRevision) {
        return new CommissionMutationCommand(
                CommissionAction.APPROVE,
                commissionId,
                expectedRevision,
                Optional.empty(),
                Optional.empty(),
                Optional.empty());
    }

    public static CommissionMutationCommand cancel(
            long commissionId,
            int expectedRevision,
            String reason) {
        return new CommissionMutationCommand(
                CommissionAction.CANCEL,
                commissionId,
                expectedRevision,
                Optional.ofNullable(reason),
                Optional.empty(),
                Optional.empty());
    }

    public static CommissionMutationCommand pay(
            long commissionId,
            int expectedRevision,
            CommissionPaymentMethod method,
            String reference) {
        return new CommissionMutationCommand(
                CommissionAction.PAY,
                commissionId,
                expectedRevision,
                Optional.empty(),
                Optional.ofNullable(method),
                Optional.ofNullable(reference));
    }

    public String fingerprint() {
        String canonical = String.join(
                ":",
                action.name(),
                Long.toString(commissionId),
                Integer.toString(expectedRevision),
                cancellationReason.orElse(""),
                paymentMethod.map(Enum::name).orElse(""),
                paymentReference.orElse(""));
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(
                            canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "SHA-256 must be available.",
                    exception);
        }
    }
}
