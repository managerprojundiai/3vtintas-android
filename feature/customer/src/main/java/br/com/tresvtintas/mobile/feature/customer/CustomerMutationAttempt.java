package br.com.tresvtintas.mobile.feature.customer;

import br.com.tresvtintas.mobile.core.customer.CustomerDraft;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;
import java.util.UUID;

final class CustomerMutationAttempt {
    private String key = "";
    private String fingerprint = "";

    static CustomerMutationAttempt restored(
            String key,
            String fingerprint) {
        CustomerMutationAttempt result = new CustomerMutationAttempt();
        result.key = key;
        result.fingerprint = fingerprint;
        return result;
    }

    String keyFor(CustomerDraft draft) {
        String current = fingerprint(Objects.requireNonNull(
                draft,
                "Customer draft is required."));
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

    private static String fingerprint(CustomerDraft draft) {
        String canonical = String.join(
                "\n",
                draft.name(),
                draft.email().orElse(""),
                draft.phone().orElse(""),
                draft.cpf().orElse(""),
                draft.address().orElse(""),
                draft.city().orElse(""),
                draft.state().orElse(""),
                draft.notes().orElse(""));
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(canonical.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(digest.length * 2);
            for (byte item : digest) {
                result.append(Character.forDigit((item >>> 4) & 0x0f, 16))
                        .append(Character.forDigit(item & 0x0f, 16));
            }
            return result.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new AssertionError(
                    "SHA-256 is required by the Java runtime.",
                    exception);
        }
    }
}
