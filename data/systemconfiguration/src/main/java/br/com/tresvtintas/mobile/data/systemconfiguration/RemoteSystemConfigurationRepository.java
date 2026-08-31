package br.com.tresvtintas.mobile.data.systemconfiguration;

import br.com.tresvtintas.mobile.core.network.AuthenticationRequiredException;
import br.com.tresvtintas.mobile.core.network.MobileApiFactory;
import br.com.tresvtintas.mobile.core.network.api.MobileApi;
import br.com.tresvtintas.mobile.core.network.dto.SystemConfigurationDtos;
import br.com.tresvtintas.mobile.core.network.problem.MobileProblemCode;
import br.com.tresvtintas.mobile.core.network.problem.ProblemDetails;
import br.com.tresvtintas.mobile.core.network.problem.ProblemDetailsParser;
import br.com.tresvtintas.mobile.core.systemconfiguration.SystemConfigurationException;
import br.com.tresvtintas.mobile.core.systemconfiguration.SystemConfigurationFailureKind;
import br.com.tresvtintas.mobile.core.systemconfiguration.SystemConfigurationModels.Mutation;
import br.com.tresvtintas.mobile.core.systemconfiguration.SystemConfigurationModels.Snapshot;
import br.com.tresvtintas.mobile.core.systemconfiguration.SystemConfigurationModels.Values;
import br.com.tresvtintas.mobile.core.systemconfiguration.SystemConfigurationRepository;
import com.fasterxml.jackson.core.JacksonException;
import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import retrofit2.Call;
import retrofit2.Response;

public final class RemoteSystemConfigurationRepository
        implements SystemConfigurationRepository {
    private static final int HTTP_UNAUTHORIZED = 401;
    private static final int HTTP_FORBIDDEN = 403;
    private static final int HTTP_CONFLICT = 409;
    private static final int HTTP_UPDATE_REQUIRED = 426;
    private static final int HTTP_TOO_MANY_REQUESTS = 429;
    private static final int HTTP_SERVER_ERROR_START = 500;

    private final SystemConfigurationAccountScope scope;
    private final MobileApi api;
    private final ProblemDetailsParser problemParser;
    private final AtomicBoolean active = new AtomicBoolean(true);

    public RemoteSystemConfigurationRepository(
            SystemConfigurationAccountScope scope,
            MobileApi api) {
        this.scope = Objects.requireNonNull(scope, "Account scope is required.");
        this.api = Objects.requireNonNull(api, "Mobile API is required.");
        problemParser = MobileApiFactory.problemDetailsParser();
    }

    public SystemConfigurationAccountScope scope() {
        return scope;
    }

    @Override
    public Snapshot load() throws SystemConfigurationException {
        requireActive();
        try {
            return SystemConfigurationDtoMapper.snapshot(body(execute(
                    api.systemConfiguration())));
        } catch (IllegalArgumentException exception) {
            throw protocol(exception);
        }
    }

    @Override
    public Mutation update(
            Values values,
            int expectedRevision,
            String idempotencyKey) throws SystemConfigurationException {
        requireActive();
        try {
            Response<SystemConfigurationDtos.Mutation> response = execute(
                    api.updateSystemConfiguration(
                            idempotencyKey,
                            SystemConfigurationDtoMapper.request(
                                    values,
                                    expectedRevision)));
            return SystemConfigurationDtoMapper.mutation(
                    body(response),
                    replayed(response));
        } catch (IllegalArgumentException exception) {
            throw protocol(exception);
        }
    }

    public void close() {
        active.set(false);
    }

    private void requireActive() throws SystemConfigurationException {
        if (!active.get()) {
            throw new SystemConfigurationException(
                    SystemConfigurationFailureKind.ACCESS_REVOKED,
                    "System configuration scope is inactive.");
        }
    }

    private <T> Response<T> execute(Call<T> call)
            throws SystemConfigurationException {
        try {
            Response<T> response = call.execute();
            if (!response.isSuccessful()) {
                throw failure(response);
            }
            return response;
        } catch (AuthenticationRequiredException exception) {
            throw new SystemConfigurationException(
                    SystemConfigurationFailureKind.AUTH_REJECTED,
                    "The protected configuration session is unavailable.",
                    exception);
        } catch (IOException exception) {
            throw new SystemConfigurationException(
                    isProtocolFailure(exception)
                            ? SystemConfigurationFailureKind.PROTOCOL
                            : SystemConfigurationFailureKind.NETWORK,
                    "The configuration response could not be read.",
                    exception);
        }
    }

    private static <T> T body(Response<T> response)
            throws SystemConfigurationException {
        if (response.body() == null) {
            throw new SystemConfigurationException(
                    SystemConfigurationFailureKind.PROTOCOL,
                    "The configuration response has no body.");
        }
        return response.body();
    }

    private SystemConfigurationException failure(Response<?> response) {
        Optional<ProblemDetails> problem = problemParser.parse(response);
        if (problem.isPresent()) {
            ProblemDetails details = problem.orElseThrow();
            return new SystemConfigurationException(
                    map(details.code()),
                    "The configuration request was rejected.",
                    Optional.ofNullable(details.requestId())
                            .filter(value -> !value.isBlank()),
                    null);
        }
        return new SystemConfigurationException(
                status(response.code()),
                "The configuration error response is invalid.");
    }

    private static boolean replayed(Response<?> response)
            throws SystemConfigurationException {
        String value = response.headers().get("X-Idempotency-Replayed");
        if (!"true".equals(value) && !"false".equals(value)) {
            throw new SystemConfigurationException(
                    SystemConfigurationFailureKind.PROTOCOL,
                    "The configuration idempotency response is invalid.");
        }
        return Boolean.parseBoolean(value);
    }

    private static SystemConfigurationFailureKind map(MobileProblemCode code) {
        return switch (code) {
            case AUTH_REQUIRED, AUTH_FAILED ->
                    SystemConfigurationFailureKind.AUTH_REJECTED;
            case FORBIDDEN -> SystemConfigurationFailureKind.FORBIDDEN;
            case INVALID_JSON, INVALID_REQUEST ->
                    SystemConfigurationFailureKind.INVALID_REQUEST;
            case RESOURCE_CONFLICT -> SystemConfigurationFailureKind.CONFLICT;
            case IDEMPOTENCY_IN_PROGRESS ->
                    SystemConfigurationFailureKind.IDEMPOTENCY_IN_PROGRESS;
            case IDEMPOTENCY_KEY_REUSED ->
                    SystemConfigurationFailureKind.IDEMPOTENCY_KEY_REUSED;
            case RATE_LIMITED -> SystemConfigurationFailureKind.RATE_LIMITED;
            case SERVICE_UNAVAILABLE ->
                    SystemConfigurationFailureKind.SERVICE_UNAVAILABLE;
            case APP_UPDATE_REQUIRED ->
                    SystemConfigurationFailureKind.UPDATE_REQUIRED;
            default -> SystemConfigurationFailureKind.PROTOCOL;
        };
    }

    private static SystemConfigurationFailureKind status(int code) {
        if (code == HTTP_UNAUTHORIZED) {
            return SystemConfigurationFailureKind.AUTH_REJECTED;
        }
        if (code == HTTP_FORBIDDEN) {
            return SystemConfigurationFailureKind.FORBIDDEN;
        }
        if (code == HTTP_CONFLICT) {
            return SystemConfigurationFailureKind.CONFLICT;
        }
        if (code == HTTP_UPDATE_REQUIRED) {
            return SystemConfigurationFailureKind.UPDATE_REQUIRED;
        }
        if (code == HTTP_TOO_MANY_REQUESTS) {
            return SystemConfigurationFailureKind.RATE_LIMITED;
        }
        if (code >= HTTP_SERVER_ERROR_START) {
            return SystemConfigurationFailureKind.SERVICE_UNAVAILABLE;
        }
        return SystemConfigurationFailureKind.INVALID_REQUEST;
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

    private static SystemConfigurationException protocol(Throwable cause) {
        return new SystemConfigurationException(
                SystemConfigurationFailureKind.PROTOCOL,
                "The configuration response was invalid.",
                cause);
    }
}
