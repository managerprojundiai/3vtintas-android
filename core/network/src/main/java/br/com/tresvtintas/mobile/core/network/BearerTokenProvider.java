package br.com.tresvtintas.mobile.core.network;

import java.util.Optional;

/**
 * Supplies a currently valid process-memory bearer token.
 */
@FunctionalInterface
public interface BearerTokenProvider {
    Optional<String> accessToken();
}
