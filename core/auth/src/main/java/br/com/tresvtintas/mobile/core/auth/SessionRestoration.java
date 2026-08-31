package br.com.tresvtintas.mobile.core.auth;

import java.util.Optional;

public record SessionRestoration(Optional<AuthenticatedSession> session) {
    public SessionRestoration {
        session = session == null ? Optional.empty() : session;
    }

    public static SessionRestoration signedOut() {
        return new SessionRestoration(Optional.empty());
    }

    public static SessionRestoration authenticated(AuthenticatedSession session) {
        return new SessionRestoration(Optional.of(session));
    }
}
