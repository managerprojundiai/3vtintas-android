package br.com.tresvtintas.mobile.core.auth;

import java.util.Objects;

record GoogleSignInPrompt(
        String serverClientId,
        String nonce,
        GoogleSignInMode mode) {
    GoogleSignInPrompt {
        if (serverClientId == null || serverClientId.isBlank()) {
            throw new IllegalArgumentException("Google server client ID is required.");
        }
        if (nonce == null || nonce.isBlank()) {
            throw new IllegalArgumentException("Google nonce is required.");
        }
        Objects.requireNonNull(mode, "Google sign-in mode is required.");
    }
}
