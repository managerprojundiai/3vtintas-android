package br.com.tresvtintas.mobile.feature.quote;

import java.util.UUID;

final class MaterialQuoteDuplicateAttempt {
    private String key = "";
    private long quoteId;
    private int revision;

    static MaterialQuoteDuplicateAttempt restored(
            String key,
            long quoteId,
            int revision) {
        MaterialQuoteDuplicateAttempt value = new MaterialQuoteDuplicateAttempt();
        value.key = validKey(key) && quoteId > 0 && revision > 0 ? key : "";
        value.quoteId = value.key.isEmpty() ? 0 : quoteId;
        value.revision = value.key.isEmpty() ? 0 : revision;
        return value;
    }

    String keyFor(long sourceQuoteId, int sourceRevision) {
        if (key.isEmpty()
                || quoteId != sourceQuoteId
                || revision != sourceRevision) {
            key = UUID.randomUUID().toString();
            quoteId = sourceQuoteId;
            revision = sourceRevision;
        }
        return key;
    }

    String key() {
        return key;
    }

    long quoteId() {
        return quoteId;
    }

    int revision() {
        return revision;
    }

    void reset() {
        key = "";
        quoteId = 0;
        revision = 0;
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
