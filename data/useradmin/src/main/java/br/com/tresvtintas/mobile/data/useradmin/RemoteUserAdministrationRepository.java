package br.com.tresvtintas.mobile.data.useradmin;

import br.com.tresvtintas.mobile.core.model.AppRole;
import br.com.tresvtintas.mobile.core.network.AuthenticationRequiredException;
import br.com.tresvtintas.mobile.core.network.MobileApiFactory;
import br.com.tresvtintas.mobile.core.network.api.MobileApi;
import br.com.tresvtintas.mobile.core.network.dto.UserAdministrationDtos;
import br.com.tresvtintas.mobile.core.network.problem.MobileProblemCode;
import br.com.tresvtintas.mobile.core.network.problem.ProblemDetails;
import br.com.tresvtintas.mobile.core.network.problem.ProblemDetailsParser;
import br.com.tresvtintas.mobile.core.useradmin.UserAdministrationException;
import br.com.tresvtintas.mobile.core.useradmin.UserAdministrationFailureKind;
import br.com.tresvtintas.mobile.core.useradmin.UserAdministrationModels.Mutation;
import br.com.tresvtintas.mobile.core.useradmin.UserAdministrationModels.Options;
import br.com.tresvtintas.mobile.core.useradmin.UserAdministrationModels.Page;
import br.com.tresvtintas.mobile.core.useradmin.UserAdministrationModels.User;
import br.com.tresvtintas.mobile.core.useradmin.UserAdministrationQuery;
import br.com.tresvtintas.mobile.core.useradmin.UserAdministrationRepository;
import com.fasterxml.jackson.core.JacksonException;
import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import retrofit2.Call;
import retrofit2.Response;

public final class RemoteUserAdministrationRepository
        implements UserAdministrationRepository {
    private static final int HTTP_BAD_REQUEST = 400;
    private static final int HTTP_UNAUTHORIZED = 401;
    private static final int HTTP_FORBIDDEN = 403;
    private static final int HTTP_NOT_FOUND = 404;
    private static final int HTTP_CONFLICT = 409;
    private static final int HTTP_UNPROCESSABLE = 422;
    private static final int HTTP_UPDATE_REQUIRED = 426;
    private static final int HTTP_TOO_MANY_REQUESTS = 429;
    private static final int HTTP_SERVER_ERROR_START = 500;

    private final UserAdministrationAccountScope scope;
    private final MobileApi api;
    private final ProblemDetailsParser problemParser;
    private final AtomicBoolean active = new AtomicBoolean(true);

    public RemoteUserAdministrationRepository(
            UserAdministrationAccountScope scope,
            MobileApi api) {
        this.scope = Objects.requireNonNull(scope, "User scope is required.");
        this.api = Objects.requireNonNull(api, "Mobile API is required.");
        problemParser = MobileApiFactory.problemDetailsParser();
    }

    public UserAdministrationAccountScope scope() {
        return scope;
    }

    @Override
    public Options options() throws UserAdministrationException {
        requireActive();
        try {
            return UserAdministrationDtoMapper.options(
                    body(execute(api.userAdministrationOptions())));
        } catch (IllegalArgumentException exception) {
            throw protocol(exception);
        }
    }

    @Override
    public Page users(UserAdministrationQuery query, Optional<String> cursor)
            throws UserAdministrationException {
        requireActive();
        Objects.requireNonNull(query, "User query is required.");
        try {
            Long organizationId = query.organizationId().isPresent()
                    ? query.organizationId().orElseThrow()
                    : null;
            return UserAdministrationDtoMapper.page(body(execute(
                    api.userAdministrationUsers(
                            organizationId,
                            query.role().map(AppRole::wireValue).orElse(null),
                            query.status().map(value -> value.wireValue()).orElse(null),
                            query.search().orElse(null),
                            cursor.orElse(null),
                            query.pageSize()))));
        } catch (IllegalArgumentException exception) {
            throw protocol(exception);
        }
    }

    @Override
    public User user(long userId) throws UserAdministrationException {
        requireActive();
        try {
            return UserAdministrationDtoMapper.user(
                    body(execute(api.userAdministrationUser(userId))));
        } catch (IllegalArgumentException exception) {
            throw protocol(exception);
        }
    }

    @Override
    public Mutation assignStandardRole(
            long userId,
            int revision,
            AppRole role,
            String key) throws UserAdministrationException {
        requireActive();
        try {
            return mutation(execute(api.assignUserStandardRole(
                    userId,
                    key,
                    new UserAdministrationDtos.StandardRoleRequest(
                            "standard_role",
                            revision,
                            role.wireValue(),
                            true))));
        } catch (IllegalArgumentException exception) {
            throw protocol(exception);
        }
    }

    @Override
    public Mutation assignOperationalRole(
            long userId,
            int revision,
            AppRole role,
            long organizationId,
            String key) throws UserAdministrationException {
        requireActive();
        try {
            return mutation(execute(api.assignUserOperationalRole(
                    userId,
                    key,
                    new UserAdministrationDtos.OperationalRoleRequest(
                            "operational_role",
                            revision,
                            role.wireValue(),
                            organizationId,
                            true))));
        } catch (IllegalArgumentException exception) {
            throw protocol(exception);
        }
    }

    @Override
    public Mutation setBlocked(
            long userId,
            int revision,
            boolean blocked,
            String key) throws UserAdministrationException {
        requireActive();
        try {
            return mutation(execute(api.updateUserAccountStatus(
                    userId,
                    key,
                    new UserAdministrationDtos.AccountStatusRequest(
                            "account_status",
                            revision,
                            blocked,
                            true))));
        } catch (IllegalArgumentException exception) {
            throw protocol(exception);
        }
    }

    public void close() {
        active.set(false);
    }

    private Mutation mutation(Response<UserAdministrationDtos.Mutation> response)
            throws UserAdministrationException {
        return UserAdministrationDtoMapper.mutation(body(response), replayed(response));
    }

    private void requireActive() throws UserAdministrationException {
        if (!active.get()) {
            throw new UserAdministrationException(
                    UserAdministrationFailureKind.ACCESS_REVOKED,
                    "User administration scope is inactive.");
        }
    }

    private <T> Response<T> execute(Call<T> call) throws UserAdministrationException {
        try {
            Response<T> response = call.execute();
            if (!response.isSuccessful()) {
                throw failure(response);
            }
            return response;
        } catch (AuthenticationRequiredException exception) {
            throw new UserAdministrationException(
                    UserAdministrationFailureKind.AUTH_REJECTED,
                    "The protected user session is unavailable.",
                    exception);
        } catch (IOException exception) {
            throw new UserAdministrationException(
                    isProtocolFailure(exception)
                            ? UserAdministrationFailureKind.PROTOCOL
                            : UserAdministrationFailureKind.NETWORK,
                    "The user administration response could not be read.",
                    exception);
        }
    }

    private static <T> T body(Response<T> response) throws UserAdministrationException {
        if (response.body() == null) {
            throw new UserAdministrationException(
                    UserAdministrationFailureKind.PROTOCOL,
                    "The user administration response has no body.");
        }
        return response.body();
    }

    private UserAdministrationException failure(Response<?> response) {
        Optional<ProblemDetails> problem = problemParser.parse(response);
        if (problem.isPresent()) {
            ProblemDetails details = problem.orElseThrow();
            return new UserAdministrationException(
                    map(details.code()),
                    "The user administration request was rejected.",
                    Optional.ofNullable(details.requestId())
                            .filter(value -> !value.isBlank()),
                    null);
        }
        return new UserAdministrationException(
                status(response.code()),
                "The user administration error response is invalid.");
    }

    private static boolean replayed(Response<?> response)
            throws UserAdministrationException {
        String value = response.headers().get("X-Idempotency-Replayed");
        if (!"true".equals(value) && !"false".equals(value)) {
            throw new UserAdministrationException(
                    UserAdministrationFailureKind.PROTOCOL,
                    "The user idempotency response is invalid.");
        }
        return Boolean.parseBoolean(value);
    }

    private static UserAdministrationFailureKind map(MobileProblemCode code) {
        return switch (code) {
            case AUTH_REQUIRED, AUTH_FAILED -> UserAdministrationFailureKind.AUTH_REJECTED;
            case APP_UPDATE_REQUIRED -> UserAdministrationFailureKind.UPDATE_REQUIRED;
            case FORBIDDEN -> UserAdministrationFailureKind.FORBIDDEN;
            case IDEMPOTENCY_IN_PROGRESS ->
                    UserAdministrationFailureKind.IDEMPOTENCY_IN_PROGRESS;
            case IDEMPOTENCY_KEY_REUSED ->
                    UserAdministrationFailureKind.IDEMPOTENCY_KEY_REUSED;
            case INVALID_JSON, INVALID_REQUEST ->
                    UserAdministrationFailureKind.INVALID_REQUEST;
            case NOT_FOUND -> UserAdministrationFailureKind.NOT_FOUND;
            case RATE_LIMITED -> UserAdministrationFailureKind.RATE_LIMITED;
            case RESOURCE_CONFLICT -> UserAdministrationFailureKind.CONFLICT;
            case SERVICE_UNAVAILABLE -> UserAdministrationFailureKind.SERVICE_UNAVAILABLE;
            default -> UserAdministrationFailureKind.PROTOCOL;
        };
    }

    private static UserAdministrationFailureKind status(int code) {
        if (code == HTTP_BAD_REQUEST || code == HTTP_UNPROCESSABLE) {
            return UserAdministrationFailureKind.INVALID_REQUEST;
        }
        if (code == HTTP_UNAUTHORIZED) {
            return UserAdministrationFailureKind.AUTH_REJECTED;
        }
        if (code == HTTP_FORBIDDEN) {
            return UserAdministrationFailureKind.FORBIDDEN;
        }
        if (code == HTTP_NOT_FOUND) {
            return UserAdministrationFailureKind.NOT_FOUND;
        }
        if (code == HTTP_CONFLICT) {
            return UserAdministrationFailureKind.CONFLICT;
        }
        if (code == HTTP_UPDATE_REQUIRED) {
            return UserAdministrationFailureKind.UPDATE_REQUIRED;
        }
        if (code == HTTP_TOO_MANY_REQUESTS) {
            return UserAdministrationFailureKind.RATE_LIMITED;
        }
        if (code >= HTTP_SERVER_ERROR_START) {
            return UserAdministrationFailureKind.SERVICE_UNAVAILABLE;
        }
        return UserAdministrationFailureKind.PROTOCOL;
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

    private UserAdministrationException protocol(IllegalArgumentException cause) {
        return new UserAdministrationException(
                UserAdministrationFailureKind.PROTOCOL,
                "The user administration response was invalid.",
                cause);
    }
}
