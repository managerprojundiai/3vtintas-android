package br.com.tresvtintas.mobile.feature.laborquote;

import br.com.tresvtintas.mobile.core.laborquote.LaborQuoteDraft;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

final class LaborQuoteMutationAttempt {
    private String key = "";
    private String fingerprint = "";

    static LaborQuoteMutationAttempt restored(String key, String fingerprint) {
        LaborQuoteMutationAttempt value = new LaborQuoteMutationAttempt();
        value.key = validKey(key) ? key : "";
        value.fingerprint = fingerprint == null ? "" : fingerprint;
        return value;
    }

    String keyFor(LaborQuoteDraft draft, int revision) {
        String next = fingerprint(draft, revision);
        if (key.isEmpty() || !next.equals(fingerprint)) {
            key = UUID.randomUUID().toString();
            fingerprint = next;
        }
        return key;
    }

    String key() {
        return key;
    }

    String fingerprint() {
        return fingerprint;
    }

    void reset() {
        key = "";
        fingerprint = "";
    }

    private static String fingerprint(LaborQuoteDraft draft, int revision) {
        StringBuilder canonical = new StringBuilder(512)
                .append(revision).append('|')
                .append(draft.customerId()).append('|')
                .append(draft.title().orElse("")).append('|')
                .append(draft.notes().orElse("")).append('|')
                .append(draft.validUntil().map(Object::toString).orElse(""))
                .append('|').append(draft.discount().toPlainString());
        draft.items().forEach(item -> canonical
                .append('|').append(item.description())
                .append('|').append(item.quantity().toPlainString())
                .append('|').append(item.unit())
                .append('|').append(item.unitPrice().toPlainString()));
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(canonical.toString().getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte value : digest) {
                hex.append(String.format(java.util.Locale.ROOT, "%02x", value & 0xff));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable.", exception);
        }
    }

    private static boolean validKey(String value) {
        if (value == null) {
            return false;
        }
        try {
            return value.equals(UUID.fromString(value).toString());
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }
}
