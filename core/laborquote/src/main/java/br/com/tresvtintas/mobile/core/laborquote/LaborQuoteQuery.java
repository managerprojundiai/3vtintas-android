package br.com.tresvtintas.mobile.core.laborquote;

import java.util.Locale;
import java.util.Optional;

public record LaborQuoteQuery(
        Optional<String> search,
        Optional<LaborQuoteStatus> status,
        LaborQuoteView view,
        int pageSize) {
    public LaborQuoteQuery {
        search = search == null ? Optional.empty() : search;
        search = search.map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(value -> value.toLowerCase(Locale.ROOT));
        status = status == null ? Optional.empty() : status;
        view = view == null ? LaborQuoteView.ACTIVE : view;
        if (status.isPresent() && view != LaborQuoteView.ALL) {
            throw new IllegalArgumentException(
                    "Status and lifecycle view cannot be combined.");
        }
        if (search.map(String::length).orElse(0) > 120 || pageSize < 1 || pageSize > 100) {
            throw new IllegalArgumentException("Labor quote query is invalid.");
        }
    }

    public LaborQuoteQuery(
            Optional<String> search,
            Optional<LaborQuoteStatus> status,
            int pageSize) {
        this(search, status, LaborQuoteView.ALL, pageSize);
    }

    public static LaborQuoteQuery initial() {
        return new LaborQuoteQuery(
                Optional.empty(), Optional.empty(), LaborQuoteView.ACTIVE, 30);
    }
}
