package br.com.tresvtintas.mobile.core.catalog;

import java.util.UUID;

public record CatalogPriceListOption(
        String versionPublicId,
        String code,
        String name,
        int versionNumber,
        boolean primary) {
    public CatalogPriceListOption {
        versionPublicId = UUID.fromString(versionPublicId).toString();
        if (code == null || code.isBlank() || code.length() > 80
                || name == null || name.isBlank() || name.length() > 160
                || versionNumber < 1) {
            throw new IllegalArgumentException("Catalog price-list option is invalid.");
        }
    }
}
