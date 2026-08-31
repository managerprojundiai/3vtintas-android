package br.com.tresvtintas.mobile.core.auth;

import br.com.tresvtintas.mobile.core.network.MobileNetworkClient;
import br.com.tresvtintas.mobile.core.network.api.MobileApi;
import java.util.Objects;

/**
 * Authentication composition graph plus the protected API that shares its in-memory access token
 * and single-flight refresh lifecycle.
 */
public record AuthRuntime(
        AuthController controller,
        MobileNetworkClient protectedClient) {
    public AuthRuntime {
        Objects.requireNonNull(controller, "Authentication controller is required.");
        Objects.requireNonNull(
                protectedClient,
                "Protected mobile client is required.");
    }

    public MobileApi protectedApi() {
        return protectedClient.api();
    }
}
