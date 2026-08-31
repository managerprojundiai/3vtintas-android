package br.com.tresvtintas.mobile.data.accountaccess;

import br.com.tresvtintas.mobile.core.accountaccess.AccountAccessException;
import br.com.tresvtintas.mobile.core.accountaccess.AccountAccessFailureKind;
import br.com.tresvtintas.mobile.core.network.AuthenticationRequiredException;
import br.com.tresvtintas.mobile.core.network.MobileApiFactory;
import br.com.tresvtintas.mobile.core.network.problem.MobileProblemCode;
import br.com.tresvtintas.mobile.core.network.problem.ProblemDetails;
import br.com.tresvtintas.mobile.core.network.problem.ProblemDetailsParser;
import com.fasterxml.jackson.core.JacksonException;
import java.io.IOException;
import java.util.Optional;
import retrofit2.Call;
import retrofit2.Response;

final class AccountAccessHttpClient {
    private static final int HTTP_BAD_REQUEST = 400;
    private static final int HTTP_UNAUTHORIZED = 401;
    private static final int HTTP_FORBIDDEN = 403;
    private static final int HTTP_NOT_FOUND = 404;
    private static final int HTTP_CONFLICT = 409;
    private static final int HTTP_UPDATE_REQUIRED = 426;
    private static final int HTTP_TOO_MANY_REQUESTS = 429;
    private static final int HTTP_SERVER_ERROR_START = 500;
    private final ProblemDetailsParser problemParser =
            MobileApiFactory.problemDetailsParser();

    <T> T body(Call<T> call) throws AccountAccessException {
        try {
            Response<T> response = call.execute();
            if (!response.isSuccessful()) {
                throw failure(response);
            }
            if (response.body() == null) {
                throw new AccountAccessException(
                        AccountAccessFailureKind.PROTOCOL,
                        "The account access response did not contain a body.");
            }
            return response.body();
        } catch (AuthenticationRequiredException exception) {
            throw new AccountAccessException(
                    AccountAccessFailureKind.AUTH_REJECTED,
                    "The protected account session is unavailable.",
                    exception);
        } catch (IOException exception) {
            throw new AccountAccessException(
                    isProtocolFailure(exception)
                            ? AccountAccessFailureKind.PROTOCOL
                            : AccountAccessFailureKind.NETWORK,
                    "The account access response could not be read.",
                    exception);
        } catch (IllegalArgumentException exception) {
            throw protocol(exception);
        }
    }

    AccountAccessException protocol(
            IllegalArgumentException exception) {
        return new AccountAccessException(
                AccountAccessFailureKind.PROTOCOL,
                "The account access response was invalid.",
                exception);
    }

    private AccountAccessException failure(Response<?> response) {
        Optional<ProblemDetails> problem = problemParser.parse(response);
        if (problem.isPresent()) {
            ProblemDetails details = problem.orElseThrow();
            return new AccountAccessException(
                    map(details.code()),
                    "The account access request was rejected.",
                    details.requestId(),
                    null);
        }
        return new AccountAccessException(
                status(response.code()),
                "The account access service returned an invalid error response.");
    }

    private static AccountAccessFailureKind map(MobileProblemCode code) {
        return switch (code) {
            case AUTH_REQUIRED, AUTH_FAILED ->
                AccountAccessFailureKind.AUTH_REJECTED;
            case APP_UPDATE_REQUIRED ->
                AccountAccessFailureKind.UPDATE_REQUIRED;
            case FORBIDDEN -> AccountAccessFailureKind.FORBIDDEN;
            case INVALID_JSON, INVALID_REQUEST ->
                AccountAccessFailureKind.INVALID_REQUEST;
            case NOT_FOUND -> AccountAccessFailureKind.NOT_FOUND;
            case IDEMPOTENCY_IN_PROGRESS, IDEMPOTENCY_KEY_REUSED,
                    RESOURCE_CONFLICT -> AccountAccessFailureKind.CONFLICT;
            case RATE_LIMITED -> AccountAccessFailureKind.RATE_LIMITED;
            case SERVICE_UNAVAILABLE ->
                AccountAccessFailureKind.SERVICE_UNAVAILABLE;
            default -> AccountAccessFailureKind.PROTOCOL;
        };
    }

    private static AccountAccessFailureKind status(int code) {
        if (code == HTTP_BAD_REQUEST) {
            return AccountAccessFailureKind.INVALID_REQUEST;
        }
        if (code == HTTP_UNAUTHORIZED) {
            return AccountAccessFailureKind.AUTH_REJECTED;
        }
        if (code == HTTP_FORBIDDEN) {
            return AccountAccessFailureKind.FORBIDDEN;
        }
        if (code == HTTP_NOT_FOUND) {
            return AccountAccessFailureKind.NOT_FOUND;
        }
        if (code == HTTP_CONFLICT) {
            return AccountAccessFailureKind.CONFLICT;
        }
        if (code == HTTP_UPDATE_REQUIRED) {
            return AccountAccessFailureKind.UPDATE_REQUIRED;
        }
        if (code == HTTP_TOO_MANY_REQUESTS) {
            return AccountAccessFailureKind.RATE_LIMITED;
        }
        if (code >= HTTP_SERVER_ERROR_START) {
            return AccountAccessFailureKind.SERVICE_UNAVAILABLE;
        }
        return AccountAccessFailureKind.PROTOCOL;
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
}
