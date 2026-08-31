package br.com.tresvtintas.mobile.core.auth;

import android.app.Activity;
import br.com.tresvtintas.mobile.core.network.dto.AuthChallengeResponse;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Lifecycle-independent state controller; UI listeners may detach safely during configuration change.
 */
public final class AuthController {
    private final AuthSessionEngine engine;
    private final GoogleCredentialGateway credentialGateway;
    private final Executor workerExecutor;
    private final Executor mainExecutor;
    private final Set<AuthStateListener> listeners = new CopyOnWriteArraySet<>();
    private final AtomicBoolean operationInProgress = new AtomicBoolean();
    private volatile AuthState current = AuthState.phase(AuthState.Phase.SIGNED_OUT);

    AuthController(
            AuthSessionEngine engine,
            GoogleCredentialGateway credentialGateway,
            Executor workerExecutor,
            Executor mainExecutor) {
        this.engine = Objects.requireNonNull(engine, "Authentication engine is required.");
        this.credentialGateway = Objects.requireNonNull(
                credentialGateway, "Credential gateway is required.");
        this.workerExecutor = Objects.requireNonNull(workerExecutor, "Worker executor is required.");
        this.mainExecutor = Objects.requireNonNull(mainExecutor, "Main executor is required.");
    }

    public void subscribe(AuthStateListener listener) {
        AuthStateListener required = Objects.requireNonNull(
                listener, "Authentication listener is required.");
        listeners.add(required);
        AuthState snapshot = current;
        mainExecutor.execute(() -> required.onAuthStateChanged(snapshot));
    }

    public void unsubscribe(AuthStateListener listener) {
        listeners.remove(listener);
    }

    public AuthState currentState() {
        return current;
    }

    public void restore() {
        if (!begin(AuthState.Phase.RESTORING)) {
            return;
        }
        workerExecutor.execute(() -> {
            try {
                SessionRestoration result = engine.restore();
                AuthState next = result.session()
                        .map(AuthState::authenticated)
                        .orElseGet(() -> AuthState.signedOut(Optional.empty()));
                complete(next);
            } catch (AuthException exception) {
                complete(AuthState.error(exception));
            }
        });
    }

    public void signInAuthorized(Activity activity) {
        signIn(activity, GoogleSignInMode.AUTHORIZED_WITH_FALLBACK);
    }

    public void signInExplicit(Activity activity) {
        signIn(activity, GoogleSignInMode.EXPLICIT_BUTTON);
    }

    public void logout() {
        if (!begin(AuthState.Phase.SIGNING_OUT)) {
            return;
        }
        workerExecutor.execute(() -> {
            LogoutResult result = engine.logout();
            mainExecutor.execute(() -> clearCredentialProvider(result));
        });
    }

    /**
     * Clears a session rejected by another protected endpoint without attempting another remote
     * request with that identity.
     */
    public void rejectSession() {
        if (!begin(AuthState.Phase.SIGNING_OUT)) {
            return;
        }
        workerExecutor.execute(() -> {
            engine.clearLocalSession();
            LogoutResult rejection = new LogoutResult(
                    false, Optional.of(AuthFailureKind.AUTH_REJECTED));
            mainExecutor.execute(() -> clearCredentialProvider(rejection));
        });
    }

    private void signIn(Activity activity, GoogleSignInMode mode) {
        Objects.requireNonNull(activity, "Activity is required.");
        if (!begin(AuthState.Phase.SIGNING_IN)) {
            return;
        }
        workerExecutor.execute(() -> {
            try {
                AuthChallengeResponse challenge = engine.beginLogin();
                mainExecutor.execute(() -> requestGoogleCredential(
                        activity, challenge, mode));
            } catch (AuthException exception) {
                completeFailure(exception);
            }
        });
    }

    private void requestGoogleCredential(
            Activity activity,
            AuthChallengeResponse challenge,
            GoogleSignInMode mode) {
        GoogleSignInPrompt prompt = new GoogleSignInPrompt(
                challenge.googleServerClientId(),
                challenge.nonce(),
                mode);
        credentialGateway.requestIdToken(activity, prompt, new AuthCallback<>() {
            @Override
            public void onSuccess(String idToken) {
                workerExecutor.execute(() -> completeGoogleLogin(challenge, idToken));
            }

            @Override
            public void onFailure(AuthException exception) {
                completeFailure(exception);
            }
        });
    }

    private void completeGoogleLogin(AuthChallengeResponse challenge, String idToken) {
        try {
            complete(AuthState.authenticated(engine.completeLogin(challenge, idToken)));
        } catch (AuthException exception) {
            completeFailure(exception);
        }
    }

    private void clearCredentialProvider(LogoutResult logout) {
        credentialGateway.clearState(new AuthCallback<>() {
            @Override
            public void onSuccess(Void ignored) {
                complete(AuthState.signedOut(logout.remoteFailure()));
            }

            @Override
            public void onFailure(AuthException exception) {
                Optional<AuthFailureKind> warning = logout.remoteFailure().isPresent()
                        ? logout.remoteFailure()
                        : Optional.of(exception.kind());
                complete(AuthState.signedOut(warning));
            }
        });
    }

    private void completeFailure(AuthException exception) {
        if (exception.kind() == AuthFailureKind.CANCELED
                || exception.kind() == AuthFailureKind.NO_CREDENTIAL) {
            complete(AuthState.signedOut(Optional.of(exception.kind())));
        } else {
            complete(AuthState.error(exception));
        }
    }

    private boolean begin(AuthState.Phase phase) {
        if (!operationInProgress.compareAndSet(false, true)) {
            return false;
        }
        publish(AuthState.phase(phase));
        return true;
    }

    private void complete(AuthState next) {
        operationInProgress.set(false);
        publish(next);
    }

    private void publish(AuthState next) {
        current = next;
        mainExecutor.execute(() -> {
            for (AuthStateListener listener : listeners) {
                listener.onAuthStateChanged(next);
            }
        });
    }
}
