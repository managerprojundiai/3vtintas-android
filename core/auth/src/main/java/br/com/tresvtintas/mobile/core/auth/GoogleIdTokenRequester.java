package br.com.tresvtintas.mobile.core.auth;

import android.app.Activity;

@FunctionalInterface
public interface GoogleIdTokenRequester {
    void requestExplicit(
            Activity activity,
            String serverClientId,
            String nonce,
            AuthCallback<String> callback);
}
