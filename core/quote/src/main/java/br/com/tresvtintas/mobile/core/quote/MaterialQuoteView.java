package br.com.tresvtintas.mobile.core.quote;

public enum MaterialQuoteView {
    ACTIVE("active"),
    HISTORY("history"),
    ALL("all");

    private final String queryValue;

    MaterialQuoteView(String queryValue) {
        this.queryValue = queryValue;
    }

    public String queryValue() {
        return queryValue;
    }
}
