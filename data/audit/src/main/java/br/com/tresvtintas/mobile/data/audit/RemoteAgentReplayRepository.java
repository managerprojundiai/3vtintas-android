package br.com.tresvtintas.mobile.data.audit;

import br.com.tresvtintas.mobile.core.audit.AgentReplayModels.Page;
import br.com.tresvtintas.mobile.core.audit.AgentReplayQuery;
import br.com.tresvtintas.mobile.core.audit.AgentReplayRepository;
import br.com.tresvtintas.mobile.core.audit.AuditException;
import br.com.tresvtintas.mobile.core.audit.AuditFailureKind;
import br.com.tresvtintas.mobile.core.network.AuthenticationRequiredException;
import br.com.tresvtintas.mobile.core.network.MobileApiFactory;
import br.com.tresvtintas.mobile.core.network.api.MobileApi;
import br.com.tresvtintas.mobile.core.network.dto.AgentReplayPageDto;
import br.com.tresvtintas.mobile.core.network.problem.MobileProblemCode;
import br.com.tresvtintas.mobile.core.network.problem.ProblemDetails;
import br.com.tresvtintas.mobile.core.network.problem.ProblemDetailsParser;
import com.fasterxml.jackson.core.JacksonException;
import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import retrofit2.Response;

public final class RemoteAgentReplayRepository implements AgentReplayRepository {
    private static final int HTTP_BAD_REQUEST = 400;
    private static final int HTTP_UNAUTHORIZED = 401;
    private static final int HTTP_FORBIDDEN = 403;
    private static final int HTTP_UPDATE_REQUIRED = 426;
    private static final int HTTP_TOO_MANY_REQUESTS = 429;
    private static final int HTTP_SERVER_ERROR_START = 500;

    private final AuditAccountScope scope;
    private final MobileApi api;
    private final ProblemDetailsParser problemParser;
    private final AtomicBoolean active = new AtomicBoolean(true);

    public RemoteAgentReplayRepository(AuditAccountScope scope, MobileApi api) {
        this.scope = Objects.requireNonNull(scope, "Agent replay scope is required.");
        this.api = Objects.requireNonNull(api, "Mobile API is required.");
        problemParser = MobileApiFactory.problemDetailsParser();
    }

    public AuditAccountScope scope() {
        return scope;
    }

    @Override
    public Page turns(AgentReplayQuery query, Optional<String> cursor)
            throws AuditException {
        requireActive();
        Objects.requireNonNull(query, "Agent replay query is required.");
        try {
            Response<AgentReplayPageDto> response = api.agentReplays(
                    query.channel().wireValue().orElse(null),
                    cursor.orElse(null),
                    query.pageSize()).execute();
            if (!response.isSuccessful()) {
                throw failure(response);
            }
            if (response.body() == null) {
                throw new AuditException(
                        AuditFailureKind.PROTOCOL,
                        "The agent replay response has no body.");
            }
            return AgentReplayDtoMapper.page(response.body());
        } catch (AuthenticationRequiredException exception) {
            throw new AuditException(
                    AuditFailureKind.AUTH_REJECTED,
                    "The protected agent replay session is unavailable.",
                    exception);
        } catch (IOException exception) {
            throw new AuditException(
                    isProtocolFailure(exception)
                            ? AuditFailureKind.PROTOCOL
                            : AuditFailureKind.NETWORK,
                    "The agent replay response could not be read.",
                    exception);
        } catch (IllegalArgumentException exception) {
            throw new AuditException(
                    AuditFailureKind.PROTOCOL,
                    "The agent replay response was invalid.",
                    exception);
        }
    }

    public void close() {
        active.set(false);
    }

    private void requireActive() throws AuditException {
        if (!active.get()) {
            throw new AuditException(
                    AuditFailureKind.ACCESS_REVOKED,
                    "Agent replay account scope is inactive.");
        }
    }

    private AuditException failure(Response<?> response) {
        Optional<ProblemDetails> problem = problemParser.parse(response);
        if (problem.isPresent()) {
            ProblemDetails details = problem.orElseThrow();
            return new AuditException(
                    map(details.code()),
                    "The agent replay request was rejected.",
                    Optional.ofNullable(details.requestId())
                            .filter(value -> !value.isBlank()),
                    null);
        }
        return new AuditException(
                status(response.code()),
                "The agent replay error response is invalid.");
    }

    private static AuditFailureKind map(MobileProblemCode code) {
        return switch (code) {
            case AUTH_REQUIRED, AUTH_FAILED -> AuditFailureKind.AUTH_REJECTED;
            case APP_UPDATE_REQUIRED -> AuditFailureKind.UPDATE_REQUIRED;
            case FORBIDDEN -> AuditFailureKind.FORBIDDEN;
            case INVALID_JSON, INVALID_REQUEST -> AuditFailureKind.INVALID_REQUEST;
            case RATE_LIMITED -> AuditFailureKind.RATE_LIMITED;
            case SERVICE_UNAVAILABLE -> AuditFailureKind.SERVICE_UNAVAILABLE;
            default -> AuditFailureKind.PROTOCOL;
        };
    }

    private static AuditFailureKind status(int code) {
        if (code == HTTP_BAD_REQUEST) {
            return AuditFailureKind.INVALID_REQUEST;
        }
        if (code == HTTP_UNAUTHORIZED) {
            return AuditFailureKind.AUTH_REJECTED;
        }
        if (code == HTTP_FORBIDDEN) {
            return AuditFailureKind.FORBIDDEN;
        }
        if (code == HTTP_UPDATE_REQUIRED) {
            return AuditFailureKind.UPDATE_REQUIRED;
        }
        if (code == HTTP_TOO_MANY_REQUESTS) {
            return AuditFailureKind.RATE_LIMITED;
        }
        if (code >= HTTP_SERVER_ERROR_START) {
            return AuditFailureKind.SERVICE_UNAVAILABLE;
        }
        return AuditFailureKind.PROTOCOL;
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
