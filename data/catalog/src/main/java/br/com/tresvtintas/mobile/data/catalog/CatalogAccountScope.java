package br.com.tresvtintas.mobile.data.catalog;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

public record CatalogAccountScope(
        String accountKey,
        String authorizationRevision,
        long organizationId) {
    private static final int MINIMUM_IDENTIFIER = 1;
    private static final int REVISION_LENGTH = 64;

    public CatalogAccountScope {
        if (accountKey == null || accountKey.length() != REVISION_LENGTH) {
            throw new IllegalArgumentException("Catalog account key is invalid.");
        }
        if (authorizationRevision == null
                || authorizationRevision.length() != REVISION_LENGTH) {
            throw new IllegalArgumentException("Catalog authorization revision is invalid.");
        }
        if (organizationId < MINIMUM_IDENTIFIER) {
            throw new IllegalArgumentException("Catalog organization ID must be positive.");
        }
    }

    public static CatalogAccountScope from(
            long userId,
            String authorizationRevision,
            long organizationId) {
        if (userId < MINIMUM_IDENTIFIER) {
            throw new IllegalArgumentException("Catalog user ID must be positive.");
        }
        Objects.requireNonNull(
                authorizationRevision,
                "Catalog authorization revision is required.");
        return new CatalogAccountScope(
                sha256("3v-catalog-account:" + userId + ":" + organizationId),
                authorizationRevision,
                organizationId);
    }

    private static String sha256(String value) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(bytes.length * 2);
            for (byte item : bytes) {
                result.append(Character.forDigit((item >>> 4) & 0x0f, 16))
                        .append(Character.forDigit(item & 0x0f, 16));
            }
            return result.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new AssertionError("SHA-256 is required by the Java runtime.", exception);
        }
    }
}
