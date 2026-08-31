package br.com.tresvtintas.mobile.core.quote;

public record MaterialQuoteTintConfiguration(
        String sourceSystem,
        String lineName,
        String finishName,
        String packageName) {
    public MaterialQuoteTintConfiguration {
        sourceSystem = required(sourceSystem, 16);
        if (!"CORIMO".equals(sourceSystem) && !"LKC".equals(sourceSystem)) {
            throw new IllegalArgumentException("Tint source system is invalid.");
        }
        lineName = required(lineName, 160);
        finishName = required(finishName, 160);
        packageName = required(packageName, 160);
    }

    private static String required(String value, int maximumLength) {
        if (value == null || value.isBlank() || value.length() > maximumLength) {
            throw new IllegalArgumentException("Tint configuration is invalid.");
        }
        return value.trim();
    }
}
