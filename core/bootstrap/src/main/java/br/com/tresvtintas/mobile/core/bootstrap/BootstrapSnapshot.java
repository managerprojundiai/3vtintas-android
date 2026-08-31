package br.com.tresvtintas.mobile.core.bootstrap;

import br.com.tresvtintas.mobile.core.model.AppRole;
import br.com.tresvtintas.mobile.core.model.AuthorizationSnapshot;
import br.com.tresvtintas.mobile.core.network.dto.AuthenticatedUser;
import br.com.tresvtintas.mobile.core.network.dto.SessionIdentity;
import java.time.Instant;
import java.util.Objects;

public record BootstrapSnapshot(
        AuthenticatedUser user,
        SessionIdentity session,
        AppRole role,
        AuthorizationSnapshot authorization,
        String apiVersion,
        String contractVersion,
        Instant serverTime) {

    public BootstrapSnapshot {
        Objects.requireNonNull(user, "Bootstrap user is required.");
        Objects.requireNonNull(session, "Bootstrap session is required.");
        Objects.requireNonNull(role, "Bootstrap role is required.");
        Objects.requireNonNull(authorization, "Bootstrap authorization is required.");
        if (apiVersion == null || apiVersion.isBlank()
                || contractVersion == null || contractVersion.isBlank()) {
            throw new IllegalArgumentException("Bootstrap API versions are required.");
        }
        Objects.requireNonNull(serverTime, "Bootstrap server time is required.");
    }

    public String stableSessionKey() {
        return user.id() + ":" + session.id() + ":" + session.deviceId();
    }
}
