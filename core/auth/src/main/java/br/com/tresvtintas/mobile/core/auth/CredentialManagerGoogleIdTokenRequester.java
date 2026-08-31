package br.com.tresvtintas.mobile.core.auth;

import android.app.Activity;
import android.content.Context;
import java.util.Objects;
import java.util.concurrent.Executor;

/**
 * Public, narrow Credential Manager facade for deliberate reauthentication
 * flows. It never stores the challenge nonce or returned Google credential.
 */
public final class CredentialManagerGoogleIdTokenRequester
        implements GoogleIdTokenRequester {
    private final GoogleCredentialGateway gateway;

    public CredentialManagerGoogleIdTokenRequester(
            Context context,
            Executor callbackExecutor) {
        gateway = new CredentialManagerGoogleGateway(
                Objects.requireNonNull(
                        context,
                        "Credential context is required."),
                Objects.requireNonNull(
                        callbackExecutor,
                        "Credential callback executor is required."));
    }

    @Override
    public void requestExplicit(
            Activity activity,
            String serverClientId,
            String nonce,
            AuthCallback<String> callback) {
        gateway.requestIdToken(
                Objects.requireNonNull(
                        activity,
                        "Credential activity is required."),
                new GoogleSignInPrompt(
                        serverClientId,
                        nonce,
                        GoogleSignInMode.EXPLICIT_BUTTON),
                Objects.requireNonNull(
                        callback,
                        "Credential callback is required."));
    }
}
