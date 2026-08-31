package br.com.tresvtintas.mobile.core.corporatefinance;

public record CorporateFinanceOrganization(long id, String name) {
    public CorporateFinanceOrganization {
        if (id < 1
                || name == null
                || name.isBlank()
                || name.length() > 200) {
            throw new IllegalArgumentException(
                    "Corporate finance organization is invalid.");
        }
    }
}
