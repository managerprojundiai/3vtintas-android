package br.com.tresvtintas.mobile.core.auth;

import br.com.tresvtintas.mobile.core.network.AuthenticationRequiredException;
import br.com.tresvtintas.mobile.core.network.MobileApiFactory;
import br.com.tresvtintas.mobile.core.network.api.MobileApi;
import br.com.tresvtintas.mobile.core.network.dto.AuthChallengeRequest;
import br.com.tresvtintas.mobile.core.network.dto.AuthChallengeResponse;
import br.com.tresvtintas.mobile.core.network.dto.GoogleLoginRequest;
import br.com.tresvtintas.mobile.core.network.dto.GoogleLoginResponse;
import br.com.tresvtintas.mobile.core.network.dto.MeResponse;
import br.com.tresvtintas.mobile.core.network.dto.RefreshRequest;
import br.com.tresvtintas.mobile.core.network.dto.RefreshResponse;
import br.com.tresvtintas.mobile.core.network.problem.MobileProblemCode;
import br.com.tresvtintas.mobile.core.network.problem.ProblemDetails;
import br.com.tresvtintas.mobile.core.network.problem.ProblemDetailsParser;
import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import retrofit2.Call;
import retrofit2.Response;

final class RetrofitMobileAuthRemote implements MobileAuthRemote {
    private static final int HTTP_TOO_MANY_REQUESTS = 429;
    private static final int HTTP_SERVER_ERROR_MINIMUM = 500;
    private final MobileApi api;
    private final ProblemDetailsParser problemParser;

    RetrofitMobileAuthRemote(MobileApi api) {
        this.api = Objects.requireNonNull(api, "Mobile API is required.");
        this.problemParser = MobileApiFactory.problemDetailsParser();
    }

    @Override
    public AuthChallengeResponse createChallenge(String installationId) throws AuthException {
        return executeRequired(api.createChallenge(new AuthChallengeRequest(installationId)));
    }

    @Override
    public GoogleLoginResponse login(GoogleLoginRequest request) throws AuthException {
        return executeRequired(api.loginWithGoogle(request));
    }

    @Override
    public RefreshResponse refresh(String refreshToken) throws AuthException {
        return executeRequired(api.refresh(new RefreshRequest(refreshToken)));
    }

    @Override
    public MeResponse me() throws AuthException {
        return executeRequired(api.me());
    }

    @Override
    public void logout() throws AuthException {
        execute(api.logout());
    }

    private <T> T executeRequired(Call<T> call) throws AuthException {
        Response<T> response = execute(call);
        T body = response.body();
        if (body == null) {
            throw new AuthException(
                    AuthFailureKind.PROTOCOL,
                    "The authentication response did not contain a body.");
        }
        return body;
    }

    private <T> Response<T> execute(Call<T> call) throws AuthException {
        Response<T> response;
        try {
            response = call.execute();
        } catch (AuthenticationRequiredException exception) {
            throw new AuthException(
                    AuthFailureKind.AUTH_REJECTED,
                    "The local access credential is unavailable.",
                    exception);
        } catch (IOException exception) {
            throw new AuthException(
                    AuthFailureKind.NETWORK,
                    "The authentication service could not be reached.",
                    exception);
        }
        if (!response.isSuccessful()) {
            throw mapFailure(response);
        }
        return response;
    }

    private AuthException mapFailure(Response<?> response) {
        Optional<ProblemDetails> problem = problemParser.parse(response);
        if (problem.isPresent()) {
            ProblemDetails details = problem.orElseThrow();
            return new AuthException(
                    mapProblemCode(details.code()),
                    "The authentication request was rejected.",
                    details.requestId(),
                    null);
        }
        return new AuthException(
                mapStatus(response.code()),
                "The authentication service returned an invalid error response.");
    }

    private static AuthFailureKind mapProblemCode(MobileProblemCode code) {
        return switch (code) {
            case AUTH_REQUIRED, AUTH_FAILED, FORBIDDEN -> AuthFailureKind.AUTH_REJECTED;
            case APP_UPDATE_REQUIRED -> AuthFailureKind.UPDATE_REQUIRED;
            case RATE_LIMITED -> AuthFailureKind.RATE_LIMITED;
            case SERVICE_UNAVAILABLE -> AuthFailureKind.SERVICE_UNAVAILABLE;
            default -> AuthFailureKind.PROTOCOL;
        };
    }

    private static AuthFailureKind mapStatus(int status) {
        if (status == 401 || status == 403) {
            return AuthFailureKind.AUTH_REJECTED;
        }
        if (status == HTTP_TOO_MANY_REQUESTS) {
            return AuthFailureKind.RATE_LIMITED;
        }
        if (status >= HTTP_SERVER_ERROR_MINIMUM) {
            return AuthFailureKind.SERVICE_UNAVAILABLE;
        }
        return AuthFailureKind.PROTOCOL;
    }
}
