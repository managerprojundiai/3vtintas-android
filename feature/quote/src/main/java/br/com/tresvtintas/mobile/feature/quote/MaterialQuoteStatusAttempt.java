package br.com.tresvtintas.mobile.feature.quote;

import br.com.tresvtintas.mobile.core.quote.MaterialQuoteStatus;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

final class MaterialQuoteStatusAttempt {
    private String key = "";
    private String fingerprint = "";

    static MaterialQuoteStatusAttempt restored(String key, String fingerprint) {
        MaterialQuoteStatusAttempt result = new MaterialQuoteStatusAttempt();
        result.key = key == null ? "" : key;
        result.fingerprint = fingerprint == null ? "" : fingerprint;
        return result;
    }

    String keyFor(MaterialQuoteStatus status, int revision) {
        String current = fingerprint(status, revision);
        if (key.isEmpty() || !current.equals(fingerprint)) {
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

    private static String fingerprint(MaterialQuoteStatus status, int revision) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(
                    (revision + "\n" + status.name())
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
