package br.com.tresvtintas.mobile.feature.laborquote;

import br.com.tresvtintas.mobile.core.laborquote.LaborQuoteStatus;
import java.util.UUID;

final class LaborQuoteStatusAttempt {
    private String key = "";
    private String fingerprint = "";

    static LaborQuoteStatusAttempt restored(String key, String fingerprint) {
        LaborQuoteStatusAttempt value = new LaborQuoteStatusAttempt();
        value.key = validKey(key) ? key : "";
        value.fingerprint = fingerprint == null ? "" : fingerprint;
        return value;
    }

    String keyFor(LaborQuoteStatus status, int revision) {
        String next = revision + ":" + status.name();
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
