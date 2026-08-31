package br.com.tresvtintas.mobile.feature.quote;

import br.com.tresvtintas.mobile.core.quote.MaterialQuoteDraft;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

final class MaterialQuoteMutationAttempt {
    private String key = "";
    private String fingerprint = "";

    static MaterialQuoteMutationAttempt restored(String key, String fingerprint) {
        MaterialQuoteMutationAttempt result = new MaterialQuoteMutationAttempt();
        result.key = key;
        result.fingerprint = fingerprint;
        return result;
    }

    String keyFor(
            MaterialQuoteDraft draft,
            int revision,
            String previewFingerprint) {
        String current = fingerprint(draft, revision, previewFingerprint);
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

    private static String fingerprint(
            MaterialQuoteDraft draft,
            int revision,
            String previewFingerprint) {
        if (previewFingerprint == null
                || !previewFingerprint.matches("^[0-9a-f]{64}$")) {
            throw new IllegalArgumentException("Preview fingerprint is invalid.");
        }
        StringBuilder canonical = new StringBuilder()
                .append(revision).append('\n')
                .append(previewFingerprint).append('\n')
                .append(draft.customerId()).append('\n')
                .append(draft.title().orElse("")).append('\n')
                .append(draft.notes().orElse("")).append('\n')
                .append(draft.validUntil().map(Object::toString).orElse(""))
                .append('\n')
                .append(draft.pricing()
                        .map(value -> value.expectedPolicyRevision() + ":"
                                + value.selectedPriceListVersionPublicId().orElse(""))
                        .orElse("legacy"));
        draft.items().forEach(item -> canonical
                .append('\n')
                .append(item.identityKey())
                .append(':')
                .append(item.quantity().toPlainString()));
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(
                    canonical.toString().getBytes(StandardCharsets.UTF_8));
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
