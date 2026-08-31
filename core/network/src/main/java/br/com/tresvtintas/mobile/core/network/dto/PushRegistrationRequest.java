package br.com.tresvtintas.mobile.core.network.dto;

import java.util.regex.Pattern;

public record PushRegistrationRequest(String firebaseInstallationId) {
    private static final Pattern INSTALLATION_ID =
            Pattern.compile("^[A-Za-z0-9_-]{10,256}$");

    public PushRegistrationRequest {
        if (firebaseInstallationId == null
                || !INSTALLATION_ID.matcher(firebaseInstallationId).matches()) {
            throw new IllegalArgumentException(
                    "Firebase installation ID is invalid.");
        }
    }
}
