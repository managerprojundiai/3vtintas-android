package br.com.tresvtintas.mobile.core.auth;

import br.com.tresvtintas.mobile.core.network.dto.AuthenticatedUser;
import br.com.tresvtintas.mobile.core.network.dto.SessionIdentity;
import java.util.Objects;

public record AuthenticatedSession(AuthenticatedUser user, SessionIdentity session) {
    public AuthenticatedSession {
        Objects.requireNonNull(user, "Authenticated user is required.");
        Objects.requireNonNull(session, "Session identity is required.");
    }
}
