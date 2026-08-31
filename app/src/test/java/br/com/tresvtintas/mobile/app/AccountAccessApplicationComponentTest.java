package br.com.tresvtintas.mobile.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import br.com.tresvtintas.mobile.core.accountaccess.AccountAccessException;
import br.com.tresvtintas.mobile.core.accountaccess.AccountAccessFailureKind;
import br.com.tresvtintas.mobile.core.accountaccess.AccountAccessView;
import br.com.tresvtintas.mobile.core.auth.AuthenticatedSession;
import br.com.tresvtintas.mobile.core.bootstrap.BootstrapSnapshot;
import br.com.tresvtintas.mobile.core.model.AppRole;
import br.com.tresvtintas.mobile.core.model.AuthorizationSnapshot;
import br.com.tresvtintas.mobile.core.model.OrganizationAccessMode;
import br.com.tresvtintas.mobile.core.network.api.MobileApi;
import br.com.tresvtintas.mobile.core.network.dto.AuthenticatedUser;
import br.com.tresvtintas.mobile.core.network.dto.SessionIdentity;
import br.com.tresvtintas.mobile.data.accountaccess.RemoteAccountAccessRepository;
import br.com.tresvtintas.mobile.feature.accountaccess.AccountAccessFeatureRuntime;
import br.com.tresvtintas.mobile.feature.shell.ShellAccessState;
import br.com.tresvtintas.mobile.feature.shell.ShellScopeKind;
import java.lang.reflect.Proxy;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Test;

public final class AccountAccessApplicationComponentTest {
    private static final String SESSION_ID =
            "20000000-0000-4000-8000-000000000001";
    private static final String DEVICE_ID =
            "10000000-0000-4000-8000-000000000001";
    private static final String REVISION = "b".repeat(64);

    @Test
    public void activationBindsRuntimeToExactAuthenticatedIdentity() {
        AtomicBoolean rejected = new AtomicBoolean();
        AccountAccessApplicationComponent component =
                new AccountAccessApplicationComponent(
                        api(),
                        () -> rejected.set(true));
        AuthenticatedSession authenticated = authenticated(
                SESSION_ID,
                DEVICE_ID);

        component.activate(access(SESSION_ID, DEVICE_ID), authenticated);
        AccountAccessFeatureRuntime runtime =
                component.runtime().orElseThrow();
        RemoteAccountAccessRepository repository =
                (RemoteAccountAccessRepository) runtime.repository();

        assertEquals(
                "The server user must become the immutable account scope.",
                21,
                repository.scope().userId());
        assertEquals(
                "The authorization revision must bind the runtime lifetime.",
                REVISION,
                repository.scope().authorizationRevision());
        runtime.sessionRejectionHandler().run();
        assertTrue(
                "The component must preserve the auth rejection boundary.",
                rejected.get());

        component.deactivate();
        AccountAccessException failure = assertThrows(
                AccountAccessException.class,
                () -> repository.page(
                        AccountAccessView.DEVICES,
                        Optional.empty(),
                        30));
        assertEquals(
                "Deactivation must invalidate the previous repository.",
                AccountAccessFailureKind.ACCESS_REVOKED,
                failure.kind());
        component.close();
    }

    @Test
    public void mismatchedBootstrapSessionFailsClosed() {
        AccountAccessApplicationComponent component =
                new AccountAccessApplicationComponent(
                        api(),
                        () -> {
                        });

        component.activate(
                access(
                        "20000000-0000-4000-8000-000000000009",
                        DEVICE_ID),
                authenticated(SESSION_ID, DEVICE_ID));

        assertFalse(
                "A bootstrap for another session must never create a runtime.",
                component.runtime().isPresent());
        component.close();
    }

    private static ShellAccessState access(
            String sessionId,
            String deviceId) {
        AuthenticatedUser user = user();
        return new ShellAccessState(
                new BootstrapSnapshot(
                        user,
                        new SessionIdentity(sessionId, deviceId),
                        AppRole.USER,
                        new AuthorizationSnapshot(
                                Set.of(),
                                OrganizationAccessMode.NONE,
                                List.of(),
                                false,
                                OptionalLong.empty(),
                                REVISION,
                                0),
                        "v1",
                        "0.29.0",
                        Instant.parse("2026-07-27T09:00:00Z")),
                ShellScopeKind.PERSONAL,
                Optional.empty());
    }

    private static AuthenticatedSession authenticated(
            String sessionId,
            String deviceId) {
        return new AuthenticatedSession(
                user(),
                new SessionIdentity(sessionId, deviceId));
    }

    private static AuthenticatedUser user() {
        return new AuthenticatedUser(
                21,
                "Pessoa",
                "pessoa@example.test",
                "user");
    }

    private static MobileApi api() {
        return (MobileApi) Proxy.newProxyInstance(
                Thread.currentThread().getContextClassLoader(),
                new Class<?>[] {MobileApi.class},
                (proxy, method, arguments) -> {
                    throw new AssertionError(
                            "No network call is expected.");
                });
    }
}
