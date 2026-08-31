package br.com.tresvtintas.mobile.core.network;

import java.net.URI;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Immutable transport configuration with production-safe URL validation.
 */
public final class NetworkConfiguration {
    private static final Pattern VERSION = Pattern.compile(
            "^\\d{1,6}\\.\\d{1,6}\\.\\d{1,6}(?:-[0-9A-Za-z.-]{1,40})?$");
    private static final String API_PATH = "/api/mobile/v1/";
    private static final int MINIMUM_VERSION_CODE = 1;
    private final String baseUrl;
    private final String appVersion;
    private final int appVersionCode;

    public NetworkConfiguration(String baseUrl, String appVersion, int appVersionCode) {
        this(baseUrl, appVersion, appVersionCode, false);
    }

    public NetworkConfiguration(
            String baseUrl,
            String appVersion,
            int appVersionCode,
            boolean allowInsecureLoopback) {
        this.baseUrl = validateBaseUrl(baseUrl, allowInsecureLoopback);
        if (appVersion == null || !VERSION.matcher(appVersion).matches()) {
            throw new IllegalArgumentException("App version does not match the API contract.");
        }
        if (appVersionCode < MINIMUM_VERSION_CODE) {
            throw new IllegalArgumentException("App version code must be positive.");
        }
        this.appVersion = appVersion;
        this.appVersionCode = appVersionCode;
    }

    public String baseUrl() {
        return baseUrl;
    }

    public String appVersion() {
        return appVersion;
    }

    public int appVersionCode() {
        return appVersionCode;
    }

    private static String validateBaseUrl(String value, boolean allowInsecureLoopback) {
        if (value == null) {
            throw new IllegalArgumentException("API base URL is required.");
        }
        URI uri;
        try {
            uri = URI.create(value);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("API base URL is invalid.", exception);
        }
        String host = uri.getHost();
        boolean https = "https".equalsIgnoreCase(uri.getScheme());
        boolean allowedLoopback = allowInsecureLoopback
                && "http".equalsIgnoreCase(uri.getScheme())
                && isLoopback(host);
        if (host == null || uri.getUserInfo() != null || (!https && !allowedLoopback)) {
            throw new IllegalArgumentException("API base URL must use trusted HTTPS.");
        }
        if (!API_PATH.equals(uri.getPath()) || uri.getQuery() != null || uri.getFragment() != null) {
            throw new IllegalArgumentException(
                    "API base URL must end exactly with " + API_PATH);
        }
        return uri.toString();
    }

    private static boolean isLoopback(String host) {
        if (host == null) {
            return false;
        }
        String normalized = host.toLowerCase(Locale.ROOT);
        return "localhost".equals(normalized);
    }
}
