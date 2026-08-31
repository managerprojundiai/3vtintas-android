package br.com.tresvtintas.mobile.core.laborquote;

public enum LaborQuoteView {
    ACTIVE("active"),
    HISTORY("history"),
    ALL("all");

    private final String queryValue;

    LaborQuoteView(String queryValue) {
        this.queryValue = queryValue;
    }

    public String queryValue() {
        return queryValue;
    }
}
