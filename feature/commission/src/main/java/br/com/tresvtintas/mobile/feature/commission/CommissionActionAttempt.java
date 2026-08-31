package br.com.tresvtintas.mobile.feature.commission;

import br.com.tresvtintas.mobile.core.commission.CommissionMutationCommand;
import java.util.UUID;

final class CommissionActionAttempt {
    private String key = "";
    private String fingerprint = "";

    static CommissionActionAttempt restored(
            String key,
            String fingerprint) {
        CommissionActionAttempt result = new CommissionActionAttempt();
        if (key != null
                && fingerprint != null
                && key.length() >= 16
                && key.length() <= 255
                && fingerprint.matches("^[a-f0-9]{64}$")) {
            result.key = key;
            result.fingerprint = fingerprint;
        }
        return result;
    }

    String keyFor(CommissionMutationCommand command) {
        String next = command.fingerprint();
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
