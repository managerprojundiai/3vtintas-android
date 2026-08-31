package br.com.tresvtintas.mobile.core.quote;

import java.util.List;

public record MaterialQuoteTintColorPage(
        List<MaterialQuoteTintColor> items,
        boolean hasMore) {
    public MaterialQuoteTintColorPage {
        if (items == null || items.size() > 30) {
            throw new IllegalArgumentException("Tint color page is invalid.");
        }
        items = List.copyOf(items);
    }
}
