package br.com.tresvtintas.mobile.data.finance;

import br.com.tresvtintas.mobile.core.finance.FinanceAction;
import br.com.tresvtintas.mobile.core.finance.FinanceEntryStatus;
import br.com.tresvtintas.mobile.core.finance.FinanceException;
import br.com.tresvtintas.mobile.core.finance.FinanceFailureKind;
import br.com.tresvtintas.mobile.core.finance.FinanceMutationResult;
import br.com.tresvtintas.mobile.core.network.AuthenticationRequiredException;
import br.com.tresvtintas.mobile.core.network.MobileApiFactory;
import br.com.tresvtintas.mobile.core.network.problem.MobileProblemCode;
import br.com.tresvtintas.mobile.core.network.problem.ProblemDetails;
import br.com.tresvtintas.mobile.core.network.problem.ProblemDetailsParser;
import com.fasterxml.jackson.core.JacksonException;
import java.io.IOException;
import java.util.Locale;
import java.util.Optional;
import retrofit2.Call;
import retrofit2.Response;

final class FinanceHttpSupport {
    private static final int HTTP_BAD_REQUEST = 400;
    private static final int HTTP_UNAUTHORIZED = 401;
    private static final int HTTP_FORBIDDEN = 403;
    private static final int HTTP_NOT_FOUND = 404;
    private static final int HTTP_CONFLICT = 409;
    private static final int HTTP_UNPROCESSABLE = 422;
    private static final int HTTP_UPDATE_REQUIRED = 426;
    private static final int HTTP_TOO_MANY_REQUESTS = 429;
    private static final int HTTP_SERVER_ERROR_START = 500;
    private static final ProblemDetailsParser PROBLEM_PARSER =
            MobileApiFactory.problemDetailsParser();

    private FinanceHttpSupport() {
    }

    static <T> Response<T> execute(Call<T> call) throws FinanceException {
        try {
            Response<T> response = call.execute();
            if (!response.isSuccessful()) {
                throw failure(response);
            }
            return response;
        } catch (AuthenticationRequiredException exception) {
            throw new FinanceException(
                    FinanceFailureKind.AUTH_REJECTED,
                    "The protected finance session is unavailable.",
                    exception);
        } catch (IOException exception) {
            throw new FinanceException(
                    isProtocolFailure(exception)
                            ? FinanceFailureKind.PROTOCOL
                            : FinanceFailureKind.NETWORK,
                    "The finance response could not be read.",
                    exception);
        } catch (IllegalArgumentException exception) {
            throw protocol(exception);
        }
    }

    static <T> T body(Response<T> response) throws FinanceException {
        if (response.body() == null) {
            throw new FinanceException(
                    FinanceFailureKind.PROTOCOL,
                    "The finance response did not contain a body.");
        }
        return response.body();
    }

    static FinanceMutationResult mutation(
            FinanceAction action,
            long entryId,
            String status,
            Boolean changedValue,
            Response<?> response) throws FinanceException {
        boolean replayed = replayed(response);
        boolean changed = changedValue == null
                ? action == FinanceAction.CREATE && !replayed
                : changedValue;
        try {
            return new FinanceMutationResult(
                    action,
                    entryId,
                    enumValue(FinanceEntryStatus.class, status),
                    changed,
                    replayed);
        } catch (IllegalArgumentException exception) {
            throw protocol(exception);
        }
    }

    static String optionalText(Optional<String> value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        String text = value.orElseThrow().trim();
        return text.isEmpty() ? null : text;
    }

    static String wire(String value) {
        return value.toLowerCase(Locale.ROOT);
    }

    static FinanceException protocol(IllegalArgumentException exception) {
        return new FinanceException(
                FinanceFailureKind.PROTOCOL,
                "The finance response was invalid.",
                exception);
    }

    private static FinanceException failure(Response<?> response) {
        Optional<ProblemDetails> problem = PROBLEM_PARSER.parse(response);
        if (problem.isPresent()) {
            ProblemDetails details = problem.orElseThrow();
            return new FinanceException(
                    map(details.code()),
                    "The finance request was rejected.",
                    details.requestId(),
                    null);
        }
        return new FinanceException(
                status(response.code()),
                "The finance service returned an invalid error response.");
    }

    private static FinanceFailureKind map(MobileProblemCode code) {
        return switch (code) {
            case AUTH_REQUIRED, AUTH_FAILED ->
                FinanceFailureKind.AUTH_REJECTED;
            case APP_UPDATE_REQUIRED -> FinanceFailureKind.UPDATE_REQUIRED;
            case FORBIDDEN -> FinanceFailureKind.FORBIDDEN;
            case IDEMPOTENCY_IN_PROGRESS ->
                FinanceFailureKind.IDEMPOTENCY_IN_PROGRESS;
            case IDEMPOTENCY_KEY_REUSED ->
                FinanceFailureKind.IDEMPOTENCY_KEY_REUSED;
            case INVALID_JSON, INVALID_REQUEST ->
                FinanceFailureKind.INVALID_REQUEST;
            case NOT_FOUND -> FinanceFailureKind.NOT_FOUND;
            case RATE_LIMITED -> FinanceFailureKind.RATE_LIMITED;
            case RESOURCE_CONFLICT -> FinanceFailureKind.CONFLICT;
            case SERVICE_UNAVAILABLE ->
                FinanceFailureKind.SERVICE_UNAVAILABLE;
            default -> FinanceFailureKind.PROTOCOL;
        };
    }

    private static FinanceFailureKind status(int code) {
        if (code == HTTP_BAD_REQUEST) {
            return FinanceFailureKind.INVALID_REQUEST;
        }
        if (code == HTTP_UNAUTHORIZED) {
            return FinanceFailureKind.AUTH_REJECTED;
        }
        if (code == HTTP_FORBIDDEN) {
            return FinanceFailureKind.FORBIDDEN;
        }
        if (code == HTTP_NOT_FOUND) {
            return FinanceFailureKind.NOT_FOUND;
        }
        if (code == HTTP_CONFLICT) {
            return FinanceFailureKind.CONFLICT;
        }
        if (code == HTTP_UNPROCESSABLE) {
            return FinanceFailureKind.IDEMPOTENCY_KEY_REUSED;
        }
        if (code == HTTP_UPDATE_REQUIRED) {
            return FinanceFailureKind.UPDATE_REQUIRED;
        }
        if (code == HTTP_TOO_MANY_REQUESTS) {
            return FinanceFailureKind.RATE_LIMITED;
        }
        if (code >= HTTP_SERVER_ERROR_START) {
            return FinanceFailureKind.SERVICE_UNAVAILABLE;
        }
        return FinanceFailureKind.PROTOCOL;
    }

    private static boolean replayed(Response<?> response)
            throws FinanceException {
        String value = response.headers().get("X-Idempotency-Replayed");
        if (!"true".equals(value) && !"false".equals(value)) {
            throw new FinanceException(
                    FinanceFailureKind.PROTOCOL,
                    "The finance idempotency response is invalid.");
        }
        return Boolean.parseBoolean(value);
    }

    private static boolean isProtocolFailure(IOException exception) {
        Throwable cursor = exception;
        while (cursor != null) {
            if (cursor instanceof JacksonException) {
                return true;
            }
            cursor = cursor.getCause();
        }
        return false;
    }

    private static <T extends Enum<T>> T enumValue(
            Class<T> type,
            String value) {
        return Enum.valueOf(type, value.toUpperCase(Locale.ROOT));
    }
}
