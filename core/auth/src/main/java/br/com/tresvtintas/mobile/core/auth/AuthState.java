package br.com.tresvtintas.mobile.core.auth;

import java.util.Optional;

public record AuthState(
        Phase phase,
        Optional<AuthenticatedSession> session,
        Optional<AuthFailureKind> failure,
        Optional<String> requestId) {

    public enum Phase {
        RESTORING,
        SIGNED_OUT,
        SIGNING_IN,
        AUTHENTICATED,
        SIGNING_OUT,
        ERROR
    }

    public AuthState {
        if (phase == null) {
            throw new IllegalArgumentException("Authentication phase is required.");
        }
        session = session == null ? Optional.empty() : session;
        failure = failure == null ? Optional.empty() : failure;
        requestId = requestId == null ? Optional.empty() : requestId;
        if (phase == Phase.AUTHENTICATED && session.isEmpty()) {
            throw new IllegalArgumentException("Authenticated state requires a session.");
        }
    }

    public static AuthState phase(Phase phase) {
        return new AuthState(phase, Optional.empty(), Optional.empty(), Optional.empty());
    }

    public static AuthState authenticated(AuthenticatedSession session) {
        return new AuthState(
                Phase.AUTHENTICATED,
                Optional.of(session),
                Optional.empty(),
                Optional.empty());
    }

    public static AuthState signedOut(Optional<AuthFailureKind> warning) {
        return new AuthState(
                Phase.SIGNED_OUT,
                Optional.empty(),
                warning,
                Optional.empty());
    }

    public static AuthState error(AuthException exception) {
        return new AuthState(
                Phase.ERROR,
                Optional.empty(),
                Optional.of(exception.kind()),
                exception.requestId());
    }
}
