package br.com.tresvtintas.mobile.feature.order;

import br.com.tresvtintas.mobile.core.order.OrderAction;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

final class OrderActionAttempt {
    private String key = "";
    private String fingerprint = "";

    static OrderActionAttempt restored(String key, String fingerprint) {
        OrderActionAttempt result = new OrderActionAttempt();
        result.key = validKey(key) ? key : "";
        result.fingerprint = fingerprint == null ? "" : fingerprint;
        if (result.key.isEmpty()) {
            result.fingerprint = "";
        }
        return result;
    }

    String keyFor(
            OrderAction action,
            long orderId,
            int revision,
            String payload) {
        String current = fingerprint(
                action,
                orderId,
                revision,
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
            OrderAction action,
            long orderId,
            int revision,
            String payload) {
        if (action == null || orderId < 1 || revision < 1) {
            throw new IllegalArgumentException(
                    "Order action fingerprint is invalid.");
        }
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(
                    (action.name()
                            + "\n"
                            + orderId
                            + "\n"
                            + revision
                            + "\n"
                            + payload)
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
