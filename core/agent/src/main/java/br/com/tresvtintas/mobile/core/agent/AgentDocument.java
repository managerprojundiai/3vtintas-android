package br.com.tresvtintas.mobile.core.agent;

import java.util.Locale;
import java.util.Objects;

public record AgentDocument(
        AgentDocumentType type,
        long quoteId,
        String filename) {
    private static final long MAXIMUM_QUOTE_ID =
            999_999_999_999_999L;

    public AgentDocument {
        type = Objects.requireNonNull(
                type,
                "Agent document type is required.");
        if (quoteId < 1 || quoteId > MAXIMUM_QUOTE_ID) {
            throw new IllegalArgumentException(
                    "Agent document quote ID is invalid.");
        }
        String expected = expectedFilename(type, quoteId);
        if (!expected.equals(filename)) {
            throw new IllegalArgumentException(
                    "Agent document filename is invalid.");
        }
    }

    public static String expectedFilename(
            AgentDocumentType type,
            long quoteId) {
        AgentDocumentType requiredType = Objects.requireNonNull(
                type,
                "Agent document type is required.");
        if (quoteId < 1 || quoteId > MAXIMUM_QUOTE_ID) {
            throw new IllegalArgumentException(
                    "Agent document quote ID is invalid.");
        }
        return String.format(
                Locale.ROOT,
                "orcamento-%06d-%s.pdf",
                quoteId,
                requiredType.filenameSuffix());
    }
}
