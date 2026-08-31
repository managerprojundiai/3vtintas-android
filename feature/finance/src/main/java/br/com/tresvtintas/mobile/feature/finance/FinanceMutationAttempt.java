package br.com.tresvtintas.mobile.feature.finance;

import br.com.tresvtintas.mobile.core.finance.FinanceAction;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

final class FinanceMutationAttempt {
    private String key = "";
    private String fingerprint = "";

    static FinanceMutationAttempt restored(String key, String fingerprint) {
        FinanceMutationAttempt result = new FinanceMutationAttempt();
        result.key = validKey(key) ? key : "";
        result.fingerprint = fingerprint == null ? "" : fingerprint;
        if (result.key.isEmpty()) {
            result.fingerprint = "";
        }
        return result;
    }

    String keyFor(FinanceAction action, long entryId, String payload) {
        String current = fingerprint(
                action,
                entryId,
                payload == null ? "" : payload);
        if (!validKey(key) || !current.equals(fingerprint)) {
            key = UUID.randomUUID().toString();
            fingerprint = current;
        }
        return key;
    }

    void reset() {
        key = "";
        fingerprint = "";
    }

    String key() {
        return key;
    }

    String fingerprint() {
        return fingerprint;
    }

    private static boolean validKey(String value) {
        if (value == null) {
            return false;
        }
        try {
            UUID parsed = UUID.fromString(value);
            return parsed.version() == 4 && parsed.variant() == 2;
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    private static String fingerprint(
            FinanceAction action,
            long entryId,
            String payload) {
        if (action == null || entryId < 0) {
            throw new IllegalArgumentException(
                    "Finance mutation fingerprint is invalid.");
        }
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(
                    (action.name() + "\n" + entryId + "\n" + payload)
                            .getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(digest.length * 2);
            for (byte item : digest) {
                result.append(Character.forDigit((item >>> 4) & 0x0f, 16))
                        .append(Character.forDigit(item & 0x0f, 16));
            }
            return result.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new AssertionError("SHA-256 is required.", exception);
        }
    }
}
