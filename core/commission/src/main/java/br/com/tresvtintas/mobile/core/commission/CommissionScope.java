package br.com.tresvtintas.mobile.core.commission;

public enum CommissionScope {
    SELF("self"),
    TEAM("team");

    private final String queryValue;

    CommissionScope(String queryValue) {
        this.queryValue = queryValue;
    }

    public String queryValue() {
        return queryValue;
    }
}
