package br.com.tresvtintas.mobile.core.network;

import java.lang.reflect.Method;
import java.util.Optional;
import okhttp3.Request;
import retrofit2.Invocation;

final class BearerCredentialPolicy {
    private static final String PREFIX = "Bearer ";
    private static final int MINIMUM_TOKEN_LENGTH = 80;
    private static final int MAXIMUM_TOKEN_LENGTH = 4096;

    private BearerCredentialPolicy() {
        throw new AssertionError("No instances.");
    }

    static boolean requiresAuthentication(Request request) {
        if (request.tag(AuthenticatedRequestTag.class) != null) {
            return true;
        }
        Invocation invocation = request.tag(Invocation.class);
        if (invocation == null) {
            return false;
        }
        Method method = invocation.method();
        return method.isAnnotationPresent(RequiresAuthentication.class);
    }

    static boolean isSafeToken(String token) {
        return token != null
                && token.length() >= MINIMUM_TOKEN_LENGTH
                && token.length() <= MAXIMUM_TOKEN_LENGTH
                && token.indexOf('\r') < 0
                && token.indexOf('\n') < 0;
    }

    static Optional<String> bearerFrom(Request request) {
        String header = request.header("Authorization");
        if (header == null || !header.startsWith(PREFIX)) {
            return Optional.empty();
        }
        String token = header.substring(PREFIX.length());
        return isSafeToken(token) ? Optional.of(token) : Optional.empty();
    }
}
