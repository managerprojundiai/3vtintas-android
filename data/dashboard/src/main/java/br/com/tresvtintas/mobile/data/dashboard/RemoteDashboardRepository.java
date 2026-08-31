package br.com.tresvtintas.mobile.data.dashboard;

import br.com.tresvtintas.mobile.core.dashboard.DashboardException;
import br.com.tresvtintas.mobile.core.dashboard.DashboardFailureKind;
import br.com.tresvtintas.mobile.core.dashboard.DashboardRepository;
import br.com.tresvtintas.mobile.core.dashboard.DashboardSnapshot;
import br.com.tresvtintas.mobile.core.network.AuthenticationRequiredException;
import br.com.tresvtintas.mobile.core.network.MobileApiFactory;
import br.com.tresvtintas.mobile.core.network.api.MobileApi;
import br.com.tresvtintas.mobile.core.network.dto.DashboardResponseDto;
import br.com.tresvtintas.mobile.core.network.problem.MobileProblemCode;
import br.com.tresvtintas.mobile.core.network.problem.ProblemDetails;
import br.com.tresvtintas.mobile.core.network.problem.ProblemDetailsParser;
import com.fasterxml.jackson.core.JacksonException;
import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import retrofit2.Response;

public final class RemoteDashboardRepository
        implements DashboardRepository {
    private static final int HTTP_BAD_REQUEST = 400;
    private static final int HTTP_UNAUTHORIZED = 401;
    private static final int HTTP_FORBIDDEN = 403;
    private static final int HTTP_UPDATE_REQUIRED = 426;
    private static final int HTTP_TOO_MANY_REQUESTS = 429;
    private static final int HTTP_SERVER_ERROR_START = 500;

    private final DashboardAccountScope accountScope;
    private final MobileApi api;
    private final ProblemDetailsParser problemParser;
    private final AtomicBoolean active = new AtomicBoolean(true);

    public RemoteDashboardRepository(
            DashboardAccountScope accountScope,
            MobileApi api) {
        this.accountScope = Objects.requireNonNull(
                accountScope,
                "Dashboard account scope is required.");
        this.api = Objects.requireNonNull(api, "Mobile API is required.");
        this.problemParser = MobileApiFactory.problemDetailsParser();
    }

    public DashboardAccountScope accountScope() {
        return accountScope;
    }

    @Override
    public DashboardSnapshot load() throws DashboardException {
        requireActive();
        try {
            Long organizationId = accountScope.organizationId().isPresent()
                    ? accountScope.organizationId().orElseThrow()
                    : null;
            Response<DashboardResponseDto> response =
                    api.dashboard(organizationId).execute();
            if (!response.isSuccessful()) {
                throw failure(response);
            }
            if (response.body() == null) {
                throw new DashboardException(
                        DashboardFailureKind.PROTOCOL,
                        "The dashboard response did not contain a body.");
            }
            return DashboardDtoMapper.snapshot(response.body());
        } catch (AuthenticationRequiredException exception) {
            throw new DashboardException(
                    DashboardFailureKind.AUTH_REJECTED,
                    "The protected dashboard session is unavailable.",
                    exception);
        } catch (IOException exception) {
            throw new DashboardException(
                    isProtocolFailure(exception)
                            ? DashboardFailureKind.PROTOCOL
                            : DashboardFailureKind.NETWORK,
                    "The dashboard response could not be read.",
                    exception);
        } catch (IllegalArgumentException exception) {
            throw new DashboardException(
                    DashboardFailureKind.PROTOCOL,
                    "The dashboard response was invalid.",
                    exception);
        }
    }

    public void close() {
        active.set(false);
    }

    private void requireActive() throws DashboardException {
        if (!active.get()) {
            throw new DashboardException(
                    DashboardFailureKind.ACCESS_REVOKED,
                    "Dashboard account scope is no longer active.");
        }
    }

    private DashboardException failure(Response<?> response) {
        Optional<ProblemDetails> problem = problemParser.parse(response);
        if (problem.isPresent()) {
            ProblemDetails details = problem.orElseThrow();
            return new DashboardException(
                    map(details.code()),
                    "The dashboard request was rejected.",
                    details.requestId(),
                    null);
        }
        return new DashboardException(
                status(response.code()),
                "The dashboard service returned an invalid error response.");
    }

    private static DashboardFailureKind map(MobileProblemCode code) {
        return switch (code) {
            case AUTH_REQUIRED, AUTH_FAILED ->
                DashboardFailureKind.AUTH_REJECTED;
            case APP_UPDATE_REQUIRED ->
                DashboardFailureKind.UPDATE_REQUIRED;
            case FORBIDDEN -> DashboardFailureKind.FORBIDDEN;
            case INVALID_JSON, INVALID_REQUEST ->
                DashboardFailureKind.INVALID_REQUEST;
            case RATE_LIMITED -> DashboardFailureKind.RATE_LIMITED;
            case SERVICE_UNAVAILABLE ->
                DashboardFailureKind.SERVICE_UNAVAILABLE;
            default -> DashboardFailureKind.PROTOCOL;
        };
    }

    private static DashboardFailureKind status(int code) {
        if (code == HTTP_BAD_REQUEST) {
            return DashboardFailureKind.INVALID_REQUEST;
        }
        if (code == HTTP_UNAUTHORIZED) {
            return DashboardFailureKind.AUTH_REJECTED;
        }
        if (code == HTTP_FORBIDDEN) {
            return DashboardFailureKind.FORBIDDEN;
        }
        if (code == HTTP_UPDATE_REQUIRED) {
            return DashboardFailureKind.UPDATE_REQUIRED;
        }
        if (code == HTTP_TOO_MANY_REQUESTS) {
            return DashboardFailureKind.RATE_LIMITED;
        }
        if (code >= HTTP_SERVER_ERROR_START) {
            return DashboardFailureKind.SERVICE_UNAVAILABLE;
        }
        return DashboardFailureKind.PROTOCOL;
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
