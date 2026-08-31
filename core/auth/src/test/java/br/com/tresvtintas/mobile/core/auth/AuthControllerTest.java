package br.com.tresvtintas.mobile.core.auth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import br.com.tresvtintas.mobile.core.security.AccessTokenMemoryStore;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

public final class AuthControllerTest {
    @Test
    public void publishesRestoreAndLogoutStateTransitions() {
        FakeSessionVault vault = new FakeSessionVault();
        SessionCredentialStore credentials = new SessionCredentialStore(
                new AccessTokenMemoryStore(),
                vault,
                Clock.fixed(AuthTestFixtures.NOW, ZoneOffset.UTC));
        FakeMobileAuthRemote remote = new FakeMobileAuthRemote();
        AuthSessionEngine engine = new AuthSessionEngine(
                remote,
                () -> remote,
                credentials,
                AuthTestFixtures.device(),
                Clock.fixed(AuthTestFixtures.NOW, ZoneOffset.UTC));
        AuthController controller = new AuthController(
                engine,
                new ImmediateCredentialGateway(),
                Runnable::run,
                Runnable::run);
        List<AuthState.Phase> phases = new ArrayList<>();
        controller.subscribe(state -> phases.add(state.phase()));

        controller.restore();
        controller.logout();

        assertEquals(
                List.of(
                        AuthState.Phase.SIGNED_OUT,
                        AuthState.Phase.RESTORING,
                        AuthState.Phase.SIGNED_OUT,
                        AuthState.Phase.SIGNING_OUT,
                        AuthState.Phase.SIGNED_OUT),
                phases);
    }

    @Test
    public void protectedEndpointRejectionClearsLocalSessionWithoutRemoteLogout() throws Exception {
        FakeSessionVault vault = new FakeSessionVault();
        SessionCredentialStore credentials = new SessionCredentialStore(
                new AccessTokenMemoryStore(),
                vault,
                Clock.fixed(AuthTestFixtures.NOW, ZoneOffset.UTC));
        credentials.replace(AuthTestFixtures.credentials(
                AuthTestFixtures.ACCESS_TOKEN_A,
                AuthTestFixtures.REFRESH_TOKEN_A));
        FakeMobileAuthRemote remote = new FakeMobileAuthRemote();
        AuthController controller = new AuthController(
                new AuthSessionEngine(
                        remote,
                        () -> remote,
                        credentials,
                        AuthTestFixtures.device(),
                        Clock.fixed(AuthTestFixtures.NOW, ZoneOffset.UTC)),
                new ImmediateCredentialGateway(),
                Runnable::run,
                Runnable::run);

        controller.rejectSession();

        assertEquals(AuthState.Phase.SIGNED_OUT, controller.currentState().phase());
        assertEquals(
                AuthFailureKind.AUTH_REJECTED,
                controller.currentState().failure().orElseThrow());
        assertTrue(vault.load().isEmpty());
        assertFalse("Rejected bootstrap must not call logout again.", remote.logoutCalled);
    }

    private static final class ImmediateCredentialGateway
            implements GoogleCredentialGateway {
        @Override
        public void requestIdToken(
                Activity activity,
                GoogleSignInPrompt prompt,
                AuthCallback<String> callback) {
            callback.onSuccess("g".repeat(80));
        }

        @Override
        public void clearState(AuthCallback<Void> callback) {
            callback.onSuccess(null);
        }
    }
}
