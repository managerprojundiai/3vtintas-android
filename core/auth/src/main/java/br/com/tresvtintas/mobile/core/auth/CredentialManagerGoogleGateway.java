package br.com.tresvtintas.mobile.core.auth;

import android.app.Activity;
import android.content.Context;
import android.os.CancellationSignal;
import androidx.credentials.ClearCredentialStateRequest;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.ClearCredentialException;
import androidx.credentials.exceptions.GetCredentialCancellationException;
import androidx.credentials.exceptions.GetCredentialException;
import androidx.credentials.exceptions.GetCredentialProviderConfigurationException;
import androidx.credentials.exceptions.GetCredentialUnsupportedException;
import androidx.credentials.exceptions.NoCredentialException;
import java.util.Objects;
import java.util.concurrent.Executor;

/**
 * Credential Manager adapter implementing authorized-account fallback and explicit button flows.
 */
final class CredentialManagerGoogleGateway implements GoogleCredentialGateway {
    private final CredentialManager credentialManager;
    private final Executor callbackExecutor;

    CredentialManagerGoogleGateway(Context context, Executor callbackExecutor) {
        Context applicationContext = Objects.requireNonNull(
                context, "Application context is required.").getApplicationContext();
        this.credentialManager = CredentialManager.create(applicationContext);
        this.callbackExecutor = Objects.requireNonNull(
                callbackExecutor, "Callback executor is required.");
    }

    @Override
    public void requestIdToken(
            Activity activity,
            GoogleSignInPrompt prompt,
            AuthCallback<String> callback) {
        Objects.requireNonNull(activity, "Activity is required.");
        Objects.requireNonNull(prompt, "Google sign-in prompt is required.");
        Objects.requireNonNull(callback, "Authentication callback is required.");
        if (prompt.mode() == GoogleSignInMode.EXPLICIT_BUTTON) {
            request(
                    activity,
                    GoogleCredentialRequestFactory.explicit(prompt),
                    false,
                    prompt,
                    callback);
        } else {
            request(
                    activity,
                    GoogleCredentialRequestFactory.account(prompt, true),
                    true,
                    prompt,
                    callback);
        }
    }

    @Override
    public void clearState(AuthCallback<Void> callback) {
        Objects.requireNonNull(callback, "Authentication callback is required.");
        credentialManager.clearCredentialStateAsync(
                new ClearCredentialStateRequest(),
                new CancellationSignal(),
                callbackExecutor,
                new CredentialManagerCallback<>() {
                    @Override
                    public void onResult(Void result) {
                        callback.onSuccess(null);
                    }

                    @Override
                    public void onError(ClearCredentialException exception) {
                        callback.onFailure(new AuthException(
                                AuthFailureKind.SERVICE_UNAVAILABLE,
                                "Credential provider state could not be cleared.",
                                exception));
                    }
                });
    }

    private void request(
            Activity activity,
            GetCredentialRequest request,
            boolean fallbackAllowed,
            GoogleSignInPrompt prompt,
            AuthCallback<String> callback) {
        credentialManager.getCredentialAsync(
                activity,
                request,
                new CancellationSignal(),
                callbackExecutor,
                new CredentialManagerCallback<>() {
                    @Override
                    public void onResult(GetCredentialResponse response) {
                        try {
                            callback.onSuccess(GoogleCredentialResponseParser.parse(response));
                        } catch (AuthException exception) {
                            callback.onFailure(exception);
                        }
                    }

                    @Override
                    public void onError(GetCredentialException exception) {
                        if (fallbackAllowed && exception instanceof NoCredentialException) {
                            request(
                                    activity,
                                    GoogleCredentialRequestFactory.account(prompt, false),
                                    false,
                                    prompt,
                                    callback);
                            return;
                        }
                        callback.onFailure(mapCredentialFailure(exception));
                    }
                });
    }

    private static AuthException mapCredentialFailure(GetCredentialException exception) {
        if (exception instanceof GetCredentialCancellationException) {
            return new AuthException(
                    AuthFailureKind.CANCELED,
                    "The Google sign-in flow was canceled.",
                    exception);
        }
        if (exception instanceof NoCredentialException) {
            return new AuthException(
                    AuthFailureKind.NO_CREDENTIAL,
                    "No eligible Google Account was available.",
                    exception);
        }
        if (exception instanceof GetCredentialProviderConfigurationException
                || exception instanceof GetCredentialUnsupportedException) {
            return new AuthException(
                    AuthFailureKind.CONFIGURATION,
                    "Credential Manager is unavailable on this device.",
                    exception);
        }
        return new AuthException(
                AuthFailureKind.SERVICE_UNAVAILABLE,
                "The Google credential provider is temporarily unavailable.",
                exception);
    }
}
