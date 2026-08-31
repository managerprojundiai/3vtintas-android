package br.com.tresvtintas.mobile.core.quote;

import java.util.Locale;
import java.util.Optional;

public record MaterialQuoteQuery(
        Optional<String> search,
        Optional<MaterialQuoteStatus> status,
        MaterialQuoteView view,
        int pageSize) {
    public MaterialQuoteQuery {
        search = search == null ? Optional.empty() : search;
        search = search.map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(value -> value.toLowerCase(Locale.ROOT));
        if (search.map(String::length).orElse(0) > 120
                || pageSize < 1
                || pageSize > 100) {
            throw new IllegalArgumentException("Quote query is invalid.");
        }
        status = status == null ? Optional.empty() : status;
        view = view == null ? MaterialQuoteView.ACTIVE : view;
        if (status.isPresent() && view != MaterialQuoteView.ALL) {
            throw new IllegalArgumentException(
                    "Status and lifecycle view cannot be combined.");
        }
    }

    public MaterialQuoteQuery(
            Optional<String> search,
            Optional<MaterialQuoteStatus> status,
            int pageSize) {
        this(search, status, MaterialQuoteView.ALL, pageSize);
    }

    public static MaterialQuoteQuery initial() {
        return new MaterialQuoteQuery(
                Optional.empty(),
                Optional.empty(),
                MaterialQuoteView.ACTIVE,
                30);
    }
}
