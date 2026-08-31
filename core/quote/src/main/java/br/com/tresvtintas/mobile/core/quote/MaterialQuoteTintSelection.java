package br.com.tresvtintas.mobile.core.quote;

public record MaterialQuoteTintSelection(
        long colorId,
        String colorName,
        long tintContextId,
        String lineName,
        String finishName,
        String packageName) {
    public MaterialQuoteTintSelection {
        if (colorId < 1 || tintContextId < 1
                || colorName == null || colorName.isBlank()
                || colorName.length() > 255
                || invalid(lineName, 160)
                || invalid(finishName, 160)
                || invalid(packageName, 160)) {
            throw new IllegalArgumentException("Quote tint selection is invalid.");
        }
        colorName = colorName.trim();
        lineName = lineName.trim();
        finishName = finishName.trim();
        packageName = packageName.trim();
    }

    public String identityKey(long productId) {
        return productId + ":" + colorId + ":" + tintContextId;
    }

    private static boolean invalid(String value, int maximumLength) {
        return value == null || value.isBlank() || value.length() > maximumLength;
    }
}
