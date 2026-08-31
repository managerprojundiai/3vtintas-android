package br.com.tresvtintas.mobile.core.laborquote;

import java.util.List;
import java.util.Optional;

public record LaborQuotePage(List<LaborQuoteSummary> items, Optional<String> nextCursor) {
    private static final int MAXIMUM_PAGE_SIZE = 100;

    public LaborQuotePage {
        items = items == null ? List.of() : List.copyOf(items);
        if (items.size() > MAXIMUM_PAGE_SIZE) {
            throw new IllegalArgumentException("Labor quote page is too large.");
        }
        nextCursor = nextCursor == null ? Optional.empty() : nextCursor;
    }
}
