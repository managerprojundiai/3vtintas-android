package br.com.tresvtintas.mobile.core.network.dto;

public record DeviceInfo(
        String installationId,
        String displayName,
        String manufacturer,
        String model,
        int androidApi,
        String appVersion) {
    private static final int MINIMUM_ANDROID_API = 26;

    public DeviceInfo {
        installationId = DtoValidation.requireUuid(installationId, "Installation ID");
        displayName = DtoValidation.requireText(displayName, "Device display name", 120);
        if (manufacturer != null) {
            manufacturer = DtoValidation.requireText(manufacturer, "Manufacturer", 80);
        }
        if (model != null) {
            model = DtoValidation.requireText(model, "Model", 120);
        }
        if (androidApi < MINIMUM_ANDROID_API) {
            throw new IllegalArgumentException("Android API is below the supported minimum.");
        }
        appVersion = DtoValidation.requireText(appVersion, "App version", 32);
    }
}
