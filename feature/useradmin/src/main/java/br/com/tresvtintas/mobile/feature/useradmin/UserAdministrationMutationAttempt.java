package br.com.tresvtintas.mobile.feature.useradmin;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;
import java.util.regex.Pattern;

final class UserAdministrationMutationAttempt {
    private static final Pattern KEY_PATTERN = Pattern.compile("^[\\x21-\\x7e]{16,255}$");
    private static final Pattern FINGERPRINT_PATTERN = Pattern.compile("^[0-9a-f]{64}$");
    private String key = "";
    private String fingerprint = "";

    static UserAdministrationMutationAttempt restored(String key, String fingerprint) {
        UserAdministrationMutationAttempt result = new UserAdministrationMutationAttempt();
        if (key != null
                && fingerprint != null
                && KEY_PATTERN.matcher(key).matches()
                && FINGERPRINT_PATTERN.matcher(fingerprint).matches()) {
            result.key = key;
            result.fingerprint = fingerprint;
        }
        return result;
    }

    String keyFor(String canonicalPayload) {
        if (canonicalPayload == null || canonicalPayload.isEmpty()) {
            throw new IllegalArgumentException("User administration attempt is invalid.");
        }
        String current = fingerprint(canonicalPayload);
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

    private static String fingerprint(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(digest.length * 2);
            for (byte item : digest) {
                result.append(Character.forDigit((item >>> 4) & 0x0f, 16))
                        .append(Character.forDigit(item & 0x0f, 16));
            }
            return result.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new AssertionError("SHA-256 is required by the Java runtime.", exception);
        }
    }
}
