package br.com.tresvtintas.mobile.core.bootstrap;

import br.com.tresvtintas.mobile.core.network.AuthenticationRequiredException;
import br.com.tresvtintas.mobile.core.network.MobileApiFactory;
import br.com.tresvtintas.mobile.core.network.api.MobileApi;
import br.com.tresvtintas.mobile.core.network.dto.BootstrapResponse;
import br.com.tresvtintas.mobile.core.network.problem.MobileProblemCode;
import br.com.tresvtintas.mobile.core.network.problem.ProblemDetails;
import br.com.tresvtintas.mobile.core.network.problem.ProblemDetailsParser;
import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import retrofit2.Response;

final class RetrofitBootstrapRemote implements BootstrapRemote {
    private static final int HTTP_UNAUTHORIZED = 401;
    private static final int HTTP_UPGRADE_REQUIRED = 426;
    private static final int HTTP_TOO_MANY_REQUESTS = 429;
    private static final int HTTP_SERVER_ERROR_MINIMUM = 500;
    private final MobileApi api;
    private final ProblemDetailsParser problemParser;

    RetrofitBootstrapRemote(MobileApi api) {
        this.api = Objects.requireNonNull(api, "Mobile API is required.");
        this.problemParser = MobileApiFactory.problemDetailsParser();
    }

    @Override
    public BootstrapResponse load() throws BootstrapException {
        Response<BootstrapResponse> response;
        try {
            response = api.bootstrap().execute();
        } catch (AuthenticationRequiredException exception) {
            throw new BootstrapException(
                    BootstrapFailureKind.AUTH_REJECTED,
                    "The protected session is unavailable.",
                    exception);
        } catch (IOException exception) {
            throw new BootstrapException(
                    BootstrapFailureKind.NETWORK,
                    "The bootstrap service could not be reached.",
                    exception);
        }
        if (!response.isSuccessful()) {
            throw mapFailure(response);
        }
        BootstrapResponse body = response.body();
        if (body == null) {
            throw new BootstrapException(
                    BootstrapFailureKind.PROTOCOL,
                    "The bootstrap response did not contain a body.");
        }
        return body;
    }

    private BootstrapException mapFailure(Response<?> response) {
        Optional<ProblemDetails> problem = problemParser.parse(response);
        if (problem.isPresent()) {
            ProblemDetails details = problem.orElseThrow();
            return new BootstrapException(
                    mapProblemCode(details.code()),
                    "The bootstrap request was rejected.",
                    details.requestId(),
                    null);
        }
        return new BootstrapException(
                mapStatus(response.code()),
                "The bootstrap service returned an invalid error response.");
    }

    private static BootstrapFailureKind mapProblemCode(MobileProblemCode code) {
        return switch (code) {
            case AUTH_REQUIRED, AUTH_FAILED -> BootstrapFailureKind.AUTH_REJECTED;
            case APP_UPDATE_REQUIRED -> BootstrapFailureKind.UPDATE_REQUIRED;
            case RATE_LIMITED -> BootstrapFailureKind.RATE_LIMITED;
            case SERVICE_UNAVAILABLE -> BootstrapFailureKind.SERVICE_UNAVAILABLE;
            default -> BootstrapFailureKind.PROTOCOL;
        };
    }

    private static BootstrapFailureKind mapStatus(int status) {
        if (status == HTTP_UNAUTHORIZED) {
            return BootstrapFailureKind.AUTH_REJECTED;
        }
        if (status == HTTP_UPGRADE_REQUIRED) {
            return BootstrapFailureKind.UPDATE_REQUIRED;
        }
        if (status == HTTP_TOO_MANY_REQUESTS) {
            return BootstrapFailureKind.RATE_LIMITED;
        }
        if (status >= HTTP_SERVER_ERROR_MINIMUM) {
            return BootstrapFailureKind.SERVICE_UNAVAILABLE;
        }
        return BootstrapFailureKind.PROTOCOL;
    }
}
