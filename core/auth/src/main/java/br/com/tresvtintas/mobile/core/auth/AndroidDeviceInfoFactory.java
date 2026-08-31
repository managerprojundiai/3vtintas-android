package br.com.tresvtintas.mobile.core.auth;

import android.os.Build;
import br.com.tresvtintas.mobile.core.network.dto.DeviceInfo;

/**
 * Produces the bounded, non-identifying Android metadata accepted by the mobile API.
 */
public final class AndroidDeviceInfoFactory {
    private static final int MANUFACTURER_MAXIMUM = 80;
    private static final int MODEL_MAXIMUM = 120;

    private AndroidDeviceInfoFactory() {
        throw new AssertionError("No instances.");
    }

    public static DeviceInfo create(String installationId, String appVersion) {
        String manufacturer = normalized(Build.MANUFACTURER, MANUFACTURER_MAXIMUM);
        String model = normalized(Build.MODEL, MODEL_MAXIMUM);
        String displayName = displayName(manufacturer, model);
        return new DeviceInfo(
                installationId,
                displayName,
                manufacturer,
                model,
                Build.VERSION.SDK_INT,
                appVersion);
    }

    static String normalized(String value, int maximumLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        StringBuilder sanitized = new StringBuilder();
        boolean previousWhitespace = false;
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            boolean whitespace = Character.isWhitespace(character)
                    || Character.isISOControl(character);
            if (whitespace && !previousWhitespace && sanitized.length() > 0) {
                sanitized.append(' ');
            } else if (!whitespace) {
                sanitized.append(character);
            }
            previousWhitespace = whitespace;
            if (sanitized.length() >= maximumLength) {
                break;
            }
        }
        String result = sanitized.toString().trim();
        return result.isEmpty() ? null : result;
    }

    private static String displayName(String manufacturer, String model) {
        String combined = String.join(
                " ",
                manufacturer == null ? "" : manufacturer,
                model == null ? "" : model).trim();
        return combined.isEmpty() ? "Dispositivo Android" : combined;
    }
}
