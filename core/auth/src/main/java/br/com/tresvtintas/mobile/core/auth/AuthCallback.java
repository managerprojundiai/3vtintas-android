package br.com.tresvtintas.mobile.core.auth;

public interface AuthCallback<T> {
    void onSuccess(T value);

    void onFailure(AuthException exception);
}
