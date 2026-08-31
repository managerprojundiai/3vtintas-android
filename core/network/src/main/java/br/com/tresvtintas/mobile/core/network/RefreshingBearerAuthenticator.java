package br.com.tresvtintas.mobile.core.network;

import java.io.IOException;
import java.util.Optional;
import okhttp3.Authenticator;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Route;

final class RefreshingBearerAuthenticator implements Authenticator {
    private final BearerTokenRefresher tokenRefresher;

    RefreshingBearerAuthenticator(BearerTokenRefresher tokenRefresher) {
        if (tokenRefresher == null) {
            throw new IllegalArgumentException("Bearer token refresher is required.");
        }
        this.tokenRefresher = tokenRefresher;
    }

    @Override
    public Request authenticate(Route route, Response response) throws IOException {
        Request request = response.request();
        if (!BearerCredentialPolicy.requiresAuthentication(request)
                || response.priorResponse() != null) {
            return null;
        }
        Optional<String> rejected = BearerCredentialPolicy.bearerFrom(request);
        if (rejected.isEmpty()) {
            return null;
        }
        Optional<String> refreshed = tokenRefresher.refresh(rejected.get());
        if (refreshed.isEmpty()
                || !BearerCredentialPolicy.isSafeToken(refreshed.get())
                || rejected.get().equals(refreshed.get())) {
            return null;
        }
        return request.newBuilder()
                .header("Authorization", "Bearer " + refreshed.get())
                .build();
    }

}
