package br.com.tresvtintas.mobile.feature.delivery;

import br.com.tresvtintas.mobile.core.delivery.DeliveryAction;
import java.util.UUID;

final class DeliveryActionAttempt {
    private String key = "";
    private String fingerprint = "";

    static DeliveryActionAttempt restored(String key, String fingerprint) {
        DeliveryActionAttempt result = new DeliveryActionAttempt();
        if (key != null && fingerprint != null
                && key.length() >= 16 && key.length() <= 255) {
            result.key = key;
            result.fingerprint = fingerprint;
        }
        return result;
    }

    String keyFor(
            DeliveryAction action,
            long deliveryId,
            int revision) {
        String next = action.name() + ":" + deliveryId + ":" + revision;
        if (!next.equals(fingerprint)) {
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
}
