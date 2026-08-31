package br.com.tresvtintas.mobile.core.quote;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

public record MaterialQuotePreview(
        String fingerprint,
        String customerName,
        BigDecimal subtotal,
        BigDecimal discount,
        BigDecimal total,
        MaterialQuotePreviewPricing pricing,
        List<MaterialQuotePreviewLine> items) {
    private static final Pattern FINGERPRINT = Pattern.compile("^[0-9a-f]{64}$");

    public MaterialQuotePreview {
        if (fingerprint == null || !FINGERPRINT.matcher(fingerprint).matches()
                || customerName == null || customerName.isBlank()
                || customerName.length() > 200 || pricing == null
                || items == null || items.isEmpty() || items.size() > 100
                || items.contains(null)) {
            throw new IllegalArgumentException("Quote preview is invalid.");
        }
        requireMoney(subtotal, "Preview subtotal");
        requireMoney(discount, "Preview discount");
        requireMoney(total, "Preview total");
        items = List.copyOf(items);
    }

    private static void requireMoney(BigDecimal value, String name) {
        Objects.requireNonNull(value, name + " is required.");
        if (value.signum() < 0 || value.scale() != 2 || value.precision() > 10) {
            throw new IllegalArgumentException(name + " is invalid.");
        }
    }
}
