package br.com.tresvtintas.mobile.core.quote;

public record MaterialQuoteTintSnapshot(
        long colorId,
        String colorPublicId,
        String colorName,
        long tintContextId,
        String tintContextPublicId,
        String lineName,
        String finishName,
        String packageName,
        String packageCode,
        String baseCode,
        String unitCode) {
    public MaterialQuoteTintSnapshot {
        if (colorId < 1 || tintContextId < 1
                || blank(colorPublicId) || blank(colorName) || blank(tintContextPublicId)
                || blank(lineName) || blank(finishName) || blank(packageName)
                || blank(packageCode) || blank(baseCode) || blank(unitCode)) {
            throw new IllegalArgumentException("Quote tint snapshot is incomplete.");
        }
    }

    public MaterialQuoteTintSelection selection() {
        return new MaterialQuoteTintSelection(
                colorId,
                colorName,
                tintContextId,
                lineName,
                finishName,
                packageName);
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
