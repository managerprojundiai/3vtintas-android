package br.com.tresvtintas.mobile.core.auth;

import android.app.Activity;

interface GoogleCredentialGateway {
    void requestIdToken(
            Activity activity,
            GoogleSignInPrompt prompt,
            AuthCallback<String> callback);

    void clearState(AuthCallback<Void> callback);
}
