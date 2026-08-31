package br.com.tresvtintas.mobile.core.auth;

@FunctionalInterface
public interface AuthStateListener {
    void onAuthStateChanged(AuthState state);
}
