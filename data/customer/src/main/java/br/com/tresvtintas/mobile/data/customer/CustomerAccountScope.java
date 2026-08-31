package br.com.tresvtintas.mobile.data.customer;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;
import java.util.OptionalLong;

public record CustomerAccountScope(
        long userId,
        String authorizationRevision,
        OptionalLong organizationId) {
    private static final int MINIMUM_ID = 1;

    public CustomerAccountScope {
        if (userId < MINIMUM_ID) {
            throw new IllegalArgumentException("Customer scope user ID is invalid.");
        }
        authorizationRevision = Objects.requireNonNull(
                authorizationRevision,
                "Authorization revision is required.");
        if (!authorizationRevision.matches("^[0-9a-f]{64}$")) {
            throw new IllegalArgumentException(
                    "Authorization revision is invalid.");
        }
        organizationId = organizationId == null
                ? OptionalLong.empty()
                : organizationId;
        if (organizationId.isPresent()
                && organizationId.getAsLong() < MINIMUM_ID) {
            throw new IllegalArgumentException(
                    "Customer scope organization ID is invalid.");
        }
    }

    public String accountKey() {
        String canonical = userId
                + "\n"
                + authorizationRevision
                + "\n"
                + (organizationId.isPresent()
                        ? organizationId.getAsLong()
                        : "");
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(canonical.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(digest.length * 2);
            for (byte item : digest) {
                result.append(Character.forDigit((item >>> 4) & 0x0f, 16))
                        .append(Character.forDigit(item & 0x0f, 16));
            }
            return result.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new AssertionError(
                    "SHA-256 is required by the Java runtime.",
                    exception);
        }
    }
}
