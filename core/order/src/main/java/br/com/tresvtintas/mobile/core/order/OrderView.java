package br.com.tresvtintas.mobile.core.order;

public enum OrderView {
    ACTIVE("active"),
    HISTORY("history"),
    ALL("all");

    private final String queryValue;

    OrderView(String queryValue) {
        this.queryValue = queryValue;
    }

    public String queryValue() {
        return queryValue;
    }
}
