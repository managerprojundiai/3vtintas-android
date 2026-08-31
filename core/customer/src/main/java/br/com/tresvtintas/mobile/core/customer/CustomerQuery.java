package br.com.tresvtintas.mobile.core.customer;

import java.util.Locale;
import java.util.Optional;

public record CustomerQuery(Optional<String> search, int pageSize) {
    public static final int DEFAULT_PAGE_SIZE = 30;
    private static final int MAXIMUM_PAGE_SIZE = 100;

    public CustomerQuery {
        search = search == null ? Optional.empty() : search;
        search = search
                .flatMap(value -> CustomerValues.optionalText(
                        value,
                        "Customer search",
                        80))
                .map(value -> value.toLowerCase(Locale.ROOT));
        if (pageSize < 1 || pageSize > MAXIMUM_PAGE_SIZE) {
            throw new IllegalArgumentException("Customer page size is invalid.");
        }
    }

    public static CustomerQuery initial() {
        return new CustomerQuery(Optional.empty(), DEFAULT_PAGE_SIZE);
    }

    public CustomerQuery withSearch(String value) {
        return new CustomerQuery(Optional.ofNullable(value), pageSize);
    }
}
