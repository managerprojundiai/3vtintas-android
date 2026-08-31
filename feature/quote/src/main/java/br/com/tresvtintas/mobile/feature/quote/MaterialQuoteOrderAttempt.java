package br.com.tresvtintas.mobile.feature.quote;

import java.util.UUID;

final class MaterialQuoteOrderAttempt {
    private String key = "";
    private long quoteId;
    private int revision;
    static MaterialQuoteOrderAttempt restored(String key, long quoteId, int revision) {
        MaterialQuoteOrderAttempt value = new MaterialQuoteOrderAttempt();
        try {
            if (key != null && key.equals(UUID.fromString(key).toString()) && quoteId > 0 && revision > 0) {
                value.key = key;
                value.quoteId = quoteId;
                value.revision = revision;
            }
        } catch (IllegalArgumentException ignored) {
            // Invalid restored state is intentionally discarded.
        }
        return value;
    }
    String keyFor(long id, int currentRevision) {
        if (key.isEmpty() || quoteId != id || revision != currentRevision) {
            key = UUID.randomUUID().toString();
            quoteId = id;
            revision = currentRevision;
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
}
