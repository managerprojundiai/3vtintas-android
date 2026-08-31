package br.com.tresvtintas.mobile.core.network;

import java.io.IOException;

/**
 * Prevents an authenticated request from leaving the device without a bearer credential.
 */
public final class AuthenticationRequiredException extends IOException {
    private static final long serialVersionUID = 1L;

    public AuthenticationRequiredException() {
        super("A valid access token is required for this request.");
    }
}
