package br.com.tresvtintas.mobile.data.team;

import br.com.tresvtintas.mobile.core.network.AuthenticationRequiredException;
import br.com.tresvtintas.mobile.core.network.MobileApiFactory;
import br.com.tresvtintas.mobile.core.network.api.MobileApi;
import br.com.tresvtintas.mobile.core.network.dto.TeamResponseDto;
import br.com.tresvtintas.mobile.core.network.problem.MobileProblemCode;
import br.com.tresvtintas.mobile.core.network.problem.ProblemDetails;
import br.com.tresvtintas.mobile.core.network.problem.ProblemDetailsParser;
import br.com.tresvtintas.mobile.core.team.TeamException;
import br.com.tresvtintas.mobile.core.team.TeamFailureKind;
import br.com.tresvtintas.mobile.core.team.TeamRepository;
import br.com.tresvtintas.mobile.core.team.TeamSnapshot;
import com.fasterxml.jackson.core.JacksonException;
import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import retrofit2.Response;

public final class RemoteTeamRepository implements TeamRepository {
    private static final int HTTP_BAD_REQUEST = 400;
    private static final int HTTP_UNAUTHORIZED = 401;
    private static final int HTTP_FORBIDDEN = 403;
    private static final int HTTP_UPDATE_REQUIRED = 426;
    private static final int HTTP_TOO_MANY_REQUESTS = 429;
    private static final int HTTP_SERVER_ERROR_START = 500;

    private final TeamAccountScope accountScope;
    private final MobileApi api;
    private final ProblemDetailsParser problemParser;
    private final AtomicBoolean active = new AtomicBoolean(true);

    public RemoteTeamRepository(
            TeamAccountScope accountScope,
            MobileApi api) {
        this.accountScope = Objects.requireNonNull(
                accountScope,
                "Team account scope is required.");
        this.api = Objects.requireNonNull(api, "Mobile API is required.");
        this.problemParser = MobileApiFactory.problemDetailsParser();
    }

    public TeamAccountScope accountScope() {
        return accountScope;
    }

    @Override
    public TeamSnapshot load() throws TeamException {
        requireActive();
        try {
            Long organizationId = accountScope.organizationId().isPresent()
                    ? accountScope.organizationId().orElseThrow()
                    : null;
            Response<TeamResponseDto> response =
                    api.team(organizationId).execute();
            if (!response.isSuccessful()) {
                throw failure(response);
            }
            if (response.body() == null) {
                throw new TeamException(
                        TeamFailureKind.PROTOCOL,
                        "The team response did not contain a body.");
            }
            return TeamDtoMapper.snapshot(response.body());
        } catch (AuthenticationRequiredException exception) {
            throw new TeamException(
                    TeamFailureKind.AUTH_REJECTED,
                    "The protected team session is unavailable.",
                    exception);
        } catch (IOException exception) {
            throw new TeamException(
                    isProtocolFailure(exception)
                            ? TeamFailureKind.PROTOCOL
                            : TeamFailureKind.NETWORK,
                    "The team response could not be read.",
                    exception);
        } catch (IllegalArgumentException exception) {
            throw new TeamException(
                    TeamFailureKind.PROTOCOL,
                    "The team response was invalid.",
                    exception);
        }
    }

    public void close() {
        active.set(false);
    }

    private void requireActive() throws TeamException {
        if (!active.get()) {
            throw new TeamException(
                    TeamFailureKind.ACCESS_REVOKED,
                    "Team account scope is no longer active.");
        }
    }

    private TeamException failure(Response<?> response) {
        Optional<ProblemDetails> problem = problemParser.parse(response);
        if (problem.isPresent()) {
            ProblemDetails details = problem.orElseThrow();
            return new TeamException(
                    map(details.code()),
                    "The team request was rejected.",
                    details.requestId(),
                    null);
        }
        return new TeamException(
                status(response.code()),
                "The team service returned an invalid error response.");
    }

    private static TeamFailureKind map(MobileProblemCode code) {
        return switch (code) {
            case AUTH_REQUIRED, AUTH_FAILED ->
                TeamFailureKind.AUTH_REJECTED;
            case APP_UPDATE_REQUIRED -> TeamFailureKind.UPDATE_REQUIRED;
            case FORBIDDEN -> TeamFailureKind.FORBIDDEN;
            case INVALID_JSON, INVALID_REQUEST ->
                TeamFailureKind.INVALID_REQUEST;
            case RATE_LIMITED -> TeamFailureKind.RATE_LIMITED;
            case SERVICE_UNAVAILABLE ->
                TeamFailureKind.SERVICE_UNAVAILABLE;
            default -> TeamFailureKind.PROTOCOL;
        };
    }

    private static TeamFailureKind status(int code) {
        if (code == HTTP_BAD_REQUEST) {
            return TeamFailureKind.INVALID_REQUEST;
        }
        if (code == HTTP_UNAUTHORIZED) {
            return TeamFailureKind.AUTH_REJECTED;
        }
        if (code == HTTP_FORBIDDEN) {
            return TeamFailureKind.FORBIDDEN;
        }
        if (code == HTTP_UPDATE_REQUIRED) {
            return TeamFailureKind.UPDATE_REQUIRED;
        }
        if (code == HTTP_TOO_MANY_REQUESTS) {
            return TeamFailureKind.RATE_LIMITED;
        }
        if (code >= HTTP_SERVER_ERROR_START) {
            return TeamFailureKind.SERVICE_UNAVAILABLE;
        }
        return TeamFailureKind.PROTOCOL;
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
