package br.com.tresvtintas.mobile.core.network.dto;

public record BootstrapApiStatus(
        String apiVersion,
        String contractVersion,
        String minimumSupportedAppVersion,
        int minimumSupportedAppVersionCode,
        int minimumSupportedAndroidApiLevel,
        boolean maintenance,
        String serverTime) {
    private static final int MINIMUM_POSITIVE_VERSION = 1;

    public BootstrapApiStatus {
        apiVersion = DtoValidation.requireText(apiVersion, "API version", 20);
        contractVersion = DtoValidation.requireText(
                contractVersion, "Contract version", 40);
        minimumSupportedAppVersion = DtoValidation.requireText(
                minimumSupportedAppVersion, "Minimum app version", 40);
        if (minimumSupportedAppVersionCode < MINIMUM_POSITIVE_VERSION) {
            throw new IllegalArgumentException("Minimum app version code must be positive.");
        }
        if (minimumSupportedAndroidApiLevel < MINIMUM_POSITIVE_VERSION) {
            throw new IllegalArgumentException("Minimum Android API must be positive.");
        }
        serverTime = DtoValidation.requireText(serverTime, "Server time", 64);
    }
}
