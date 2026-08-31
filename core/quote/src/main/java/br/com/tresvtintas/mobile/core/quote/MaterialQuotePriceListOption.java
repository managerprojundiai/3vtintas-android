package br.com.tresvtintas.mobile.core.quote;

import java.util.UUID;

public record MaterialQuotePriceListOption(
        String versionPublicId,
        String code,
        String name,
        int versionNumber,
        boolean primary) {
    public MaterialQuotePriceListOption {
        versionPublicId = UUID.fromString(versionPublicId).toString();
        if (code == null || code.isBlank() || name == null || name.isBlank()
                || versionNumber < 1) {
            throw new IllegalArgumentException("Price-list option is invalid.");
        }
    }
}
