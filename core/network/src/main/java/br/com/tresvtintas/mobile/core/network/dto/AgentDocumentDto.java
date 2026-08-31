package br.com.tresvtintas.mobile.core.network.dto;

import java.util.Locale;
import java.util.Map;

public record AgentDocumentDto(
        String type,
        long quoteId,
        String filename) {
    private static final long MAXIMUM_QUOTE_ID =
            999_999_999_999_999L;
    private static final Map<String, String> SUFFIXES = Map.of(
            "material_quote_pdf",
            "material",
            "labor_quote_pdf",
            "labor");

    public AgentDocumentDto {
        String suffix = SUFFIXES.get(type);
        if (suffix == null) {
            throw new IllegalArgumentException(
                    "Agent document type is invalid.");
        }
        if (quoteId < 1 || quoteId > MAXIMUM_QUOTE_ID) {
            throw new IllegalArgumentException(
                    "Agent document quote ID is invalid.");
        }
        String expected = String.format(
                Locale.ROOT,
                "orcamento-%06d-%s.pdf",
                quoteId,
                suffix);
        if (!expected.equals(filename)) {
            throw new IllegalArgumentException(
                    "Agent document filename is invalid.");
        }
    }
}
