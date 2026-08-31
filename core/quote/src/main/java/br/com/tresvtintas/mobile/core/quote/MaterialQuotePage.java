package br.com.tresvtintas.mobile.core.quote;

import java.util.List;
import java.util.Optional;

public record MaterialQuotePage(
        List<MaterialQuoteSummary> items,
        Optional<String> nextCursor) {
    private static final int MAXIMUM_PAGE_SIZE = 100;

    public MaterialQuotePage {
        items = items == null ? List.of() : List.copyOf(items);
        if (items.size() > MAXIMUM_PAGE_SIZE) {
            throw new IllegalArgumentException("Quote page is too large.");
        }
        nextCursor = nextCursor == null ? Optional.empty() : nextCursor;
    }
}
