package br.com.tresvtintas.mobile.core.quote;

public record MaterialQuoteCustomer(long id, String name) {
    public MaterialQuoteCustomer {
        if (id < 1 || name == null || name.isBlank() || name.length() > 200) {
            throw new IllegalArgumentException("Quote customer is invalid.");
        }
    }
}
