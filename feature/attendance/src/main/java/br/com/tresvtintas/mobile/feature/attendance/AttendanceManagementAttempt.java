package br.com.tresvtintas.mobile.feature.attendance;

import br.com.tresvtintas.mobile.core.attendance.AttendanceManagementSelection;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;
import java.util.regex.Pattern;

final class AttendanceManagementAttempt {
    private static final Pattern KEY_PATTERN = Pattern.compile(
            "^[\\x21-\\x7e]{16,255}$");
    private static final Pattern FINGERPRINT_PATTERN = Pattern.compile(
            "^[0-9a-f]{64}$");
    private String key = "";
    private String fingerprint = "";

    static AttendanceManagementAttempt restored(
            String key,
            String fingerprint) {
        AttendanceManagementAttempt result =
                new AttendanceManagementAttempt();
        if (key != null
                && fingerprint != null
                && KEY_PATTERN.matcher(key).matches()
                && FINGERPRINT_PATTERN.matcher(fingerprint).matches()) {
            result.key = key;
            result.fingerprint = fingerprint;
        }
        return result;
    }

    String keyFor(
            String conversationId,
            int revision,
            AttendanceManagementSelection selection) {
        if (conversationId == null
                || conversationId.isBlank()
                || revision < 1
                || selection == null) {
            throw new IllegalArgumentException(
                    "Attendance management attempt is invalid.");
        }
        String assignment = selection.assignedToUserId().isPresent()
                ? Long.toString(
                        selection.assignedToUserId().getAsLong())
                : "unassigned";
        String current = fingerprint(
                conversationId
                        + "\n"
                        + revision
                        + "\n"
                        + selection.folder().wireValue()
                        + "\n"
                        + selection.priority().wireValue()
                        + "\n"
                        + assignment);
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

    private static String fingerprint(String canonical) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(canonical.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(digest.length * 2);
            for (byte item : digest) {
                result.append(
                                Character.forDigit(
                                        (item >>> 4) & 0x0f,
                                        16))
                        .append(
                                Character.forDigit(
                                        item & 0x0f,
                                        16));
            }
            return result.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new AssertionError(
                    "SHA-256 is required by the Java runtime.",
                    exception);
        }
    }
}
