package br.com.tresvtintas.mobile.core.network;

import java.io.IOException;
import java.util.Optional;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

final class BearerAuthenticationInterceptor implements Interceptor {
    private final BearerTokenProvider tokenProvider;
    private final BearerTokenRefresher tokenRefresher;

    BearerAuthenticationInterceptor(BearerTokenProvider tokenProvider) {
        this(tokenProvider, null);
    }

    BearerAuthenticationInterceptor(
            BearerTokenProvider tokenProvider,
            BearerTokenRefresher tokenRefresher) {
        if (tokenProvider == null) {
            throw new IllegalArgumentException("Bearer token provider is required.");
        }
        this.tokenProvider = tokenProvider;
        this.tokenRefresher = tokenRefresher;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        if (!BearerCredentialPolicy.requiresAuthentication(request)) {
            return chain.proceed(request);
        }
        Optional<String> token = tokenProvider.accessToken();
        if (token.isEmpty() && tokenRefresher != null) {
            token = tokenRefresher.refresh(null);
        }
        if (token.isEmpty() || !BearerCredentialPolicy.isSafeToken(token.get())) {
            throw new AuthenticationRequiredException();
        }
        Request authenticated = request.newBuilder()
                .header("Authorization", "Bearer " + token.get())
                .build();
        return chain.proceed(authenticated);
    }

}
