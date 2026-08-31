package br.com.tresvtintas.mobile.feature.delivery;

import br.com.tresvtintas.mobile.core.delivery.DeliveryManagementAction;
import java.util.Objects;
import java.util.UUID;

final class DeliveryManagementActionAttempt {
    private String key;
    private String fingerprint;

    DeliveryManagementActionAttempt() {
        this("", "");
    }

    private DeliveryManagementActionAttempt(
            String key,
            String fingerprint) {
        this.key = key;
        this.fingerprint = fingerprint;
    }

    static DeliveryManagementActionAttempt restored(
            String key,
            String fingerprint) {
        if (key == null || fingerprint == null
                || (!key.isEmpty() && key.length() < 16)) {
            return new DeliveryManagementActionAttempt();
        }
        return new DeliveryManagementActionAttempt(key, fingerprint);
    }

    String keyFor(
            DeliveryManagementAction action,
            long organizationId,
            long orderId,
            int revision,
            String qualifier) {
        String requested = Objects.requireNonNull(action).name()
                + "|" + organizationId
                + "|" + orderId
                + "|" + revision
                + "|" + Objects.requireNonNullElse(qualifier, "");
        if (!requested.equals(fingerprint) || key.isBlank()) {
            fingerprint = requested;
            key = UUID.randomUUID().toString();
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
}
