package br.com.tresvtintas.mobile.core.auth;

import java.util.Optional;

public record LogoutResult(boolean remotelyRevoked, Optional<AuthFailureKind> remoteFailure) {
    public LogoutResult {
        remoteFailure = remoteFailure == null ? Optional.empty() : remoteFailure;
    }
}
