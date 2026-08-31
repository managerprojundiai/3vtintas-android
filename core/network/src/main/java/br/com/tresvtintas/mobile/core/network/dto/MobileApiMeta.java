package br.com.tresvtintas.mobile.core.network.dto;

/**
 * Compatibility and maintenance metadata returned by the mobile API.
 */
public record MobileApiMeta(
        String apiVersion,
        String contractVersion,
        String minimumSupportedAppVersion,
        int minimumSupportedAppVersionCode,
        int minimumSupportedAndroidApiLevel,
        boolean maintenance,
        String serverTime) {

    public MobileApiMeta {
        apiVersion = DtoValidation.requireText(apiVersion, "API version", 20);
        contractVersion = DtoValidation.requireText(contractVersion, "Contract version", 32);
        minimumSupportedAppVersion = DtoValidation.requireText(
                minimumSupportedAppVersion, "Minimum app version", 80);
        if (minimumSupportedAppVersionCode < 1 || minimumSupportedAndroidApiLevel < 1) {
            throw new IllegalArgumentException("Minimum supported versions must be positive.");
        }
        serverTime = DtoValidation.requireInstant(serverTime, "Server time");
    }
}
