package br.com.tresvtintas.mobile.core.network;

/**
 * Internal marker for protected OkHttp requests that are not created by Retrofit.
 */
final class AuthenticatedRequestTag {
    static final AuthenticatedRequestTag INSTANCE =
            new AuthenticatedRequestTag();

    private AuthenticatedRequestTag() {
    }
}
