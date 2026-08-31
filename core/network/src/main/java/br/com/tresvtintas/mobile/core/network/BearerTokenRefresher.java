package br.com.tresvtintas.mobile.core.network;

import java.io.IOException;
import java.util.Optional;

/**
 * Single-flight refresh boundary invoked after an authenticated 401 response.
 */
@FunctionalInterface
public interface BearerTokenRefresher {
    Optional<String> refresh(String rejectedAccessToken) throws IOException;
}
