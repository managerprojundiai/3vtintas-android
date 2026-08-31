package br.com.tresvtintas.mobile.data.organizationadmin;

import br.com.tresvtintas.mobile.core.network.AuthenticationRequiredException;
import br.com.tresvtintas.mobile.core.network.MobileApiFactory;
import br.com.tresvtintas.mobile.core.network.api.MobileApi;
import br.com.tresvtintas.mobile.core.network.dto.OrganizationAdministrationDtos;
import br.com.tresvtintas.mobile.core.network.problem.MobileProblemCode;
import br.com.tresvtintas.mobile.core.network.problem.ProblemDetails;
import br.com.tresvtintas.mobile.core.network.problem.ProblemDetailsParser;
import br.com.tresvtintas.mobile.core.organizationadmin.OrganizationAdministrationException;
import br.com.tresvtintas.mobile.core.organizationadmin.OrganizationAdministrationFailureKind;
import br.com.tresvtintas.mobile.core.organizationadmin.OrganizationAdministrationModels.Mutation;
import br.com.tresvtintas.mobile.core.organizationadmin.OrganizationAdministrationModels.Organization;
import br.com.tresvtintas.mobile.core.organizationadmin.OrganizationAdministrationModels.Page;
import br.com.tresvtintas.mobile.core.organizationadmin.OrganizationAdministrationQuery;
import br.com.tresvtintas.mobile.core.organizationadmin.OrganizationAdministrationRepository;
import br.com.tresvtintas.mobile.core.organizationadmin.OrganizationAdministrationStatus;
import com.fasterxml.jackson.core.JacksonException;
import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import retrofit2.Call;
import retrofit2.Response;

public final class RemoteOrganizationAdministrationRepository
        implements OrganizationAdministrationRepository {
    private static final int HTTP_BAD_REQUEST = 400;
    private static final int HTTP_UNAUTHORIZED = 401;
    private static final int HTTP_FORBIDDEN = 403;
    private static final int HTTP_NOT_FOUND = 404;
    private static final int HTTP_CONFLICT = 409;
    private static final int HTTP_UNPROCESSABLE = 422;
    private static final int HTTP_UPDATE_REQUIRED = 426;
    private static final int HTTP_TOO_MANY_REQUESTS = 429;
    private static final int HTTP_SERVER_ERROR_START = 500;

    private final OrganizationAdministrationAccountScope scope;
    private final MobileApi api;
    private final ProblemDetailsParser problemParser;
    private final AtomicBoolean active = new AtomicBoolean(true);

    public RemoteOrganizationAdministrationRepository(
            OrganizationAdministrationAccountScope scope,
            MobileApi api) {
        this.scope = Objects.requireNonNull(scope, "Organization scope is required.");
        this.api = Objects.requireNonNull(api, "Mobile API is required.");
        problemParser = MobileApiFactory.problemDetailsParser();
    }

    public OrganizationAdministrationAccountScope scope() {
        return scope;
    }

    @Override
    public Page organizations(
            OrganizationAdministrationQuery query,
            Optional<String> cursor) throws OrganizationAdministrationException {
        requireActive();
        Objects.requireNonNull(query, "Organization query is required.");
        try {
            return OrganizationAdministrationDtoMapper.page(body(execute(
                    api.organizationAdministrationOrganizations(
                            query.status()
                                    .map(OrganizationAdministrationStatus::wireValue)
                                    .orElse(null),
                            query.search().orElse(null),
                            cursor.orElse(null),
                            query.pageSize()))));
        } catch (IllegalArgumentException exception) {
            throw protocol(exception);
        }
    }

    @Override
    public Organization organization(long organizationId)
            throws OrganizationAdministrationException {
        requireActive();
        try {
            return OrganizationAdministrationDtoMapper.organization(body(execute(
                    api.organizationAdministrationOrganization(organizationId))));
        } catch (IllegalArgumentException exception) {
            throw protocol(exception);
        }
    }

    @Override
    public Mutation create(String name, String slug, String key)
            throws OrganizationAdministrationException {
        requireActive();
        try {
            return mutation(execute(
                    api.createOrganizationAdministrationOrganization(
                            key,
                            new OrganizationAdministrationDtos.CreateRequest(
                                    name,
                                    slug,
                                    true))));
        } catch (IllegalArgumentException exception) {
            throw protocol(exception);
        }
    }

    @Override
    public Mutation rename(
            long organizationId,
            int revision,
            String name,
            String key) throws OrganizationAdministrationException {
        requireActive();
        try {
            return mutation(execute(
                    api.renameOrganizationAdministrationOrganization(
                            organizationId,
                            key,
                            new OrganizationAdministrationDtos.RenameRequest(
                                    "rename",
                                    revision,
                                    name,
                                    true))));
        } catch (IllegalArgumentException exception) {
            throw protocol(exception);
        }
    }

    @Override
    public Mutation setStatus(
            long organizationId,
            int revision,
            OrganizationAdministrationStatus status,
            String key) throws OrganizationAdministrationException {
        requireActive();
        if (status != OrganizationAdministrationStatus.ACTIVE
                && status != OrganizationAdministrationStatus.BLOCKED) {
            throw new OrganizationAdministrationException(
                    OrganizationAdministrationFailureKind.INVALID_REQUEST,
                    "Organization target status is invalid.");
        }
        try {
            return mutation(execute(api.updateOrganizationAdministrationStatus(
                    organizationId,
                    key,
                    new OrganizationAdministrationDtos.StatusRequest(
                            "status",
                            revision,
                            status.wireValue(),
                            true))));
        } catch (IllegalArgumentException exception) {
            throw protocol(exception);
        }
    }

    public void close() {
        active.set(false);
    }

    private Mutation mutation(
            Response<OrganizationAdministrationDtos.Mutation> response)
            throws OrganizationAdministrationException {
        return OrganizationAdministrationDtoMapper.mutation(
                body(response),
                replayed(response));
    }

    private void requireActive() throws OrganizationAdministrationException {
        if (!active.get()) {
            throw new OrganizationAdministrationException(
                    OrganizationAdministrationFailureKind.ACCESS_REVOKED,
                    "Organization administration scope is inactive.");
        }
    }

    private <T> Response<T> execute(Call<T> call)
            throws OrganizationAdministrationException {
        try {
            Response<T> response = call.execute();
            if (!response.isSuccessful()) {
                throw failure(response);
            }
            return response;
        } catch (AuthenticationRequiredException exception) {
            throw new OrganizationAdministrationException(
                    OrganizationAdministrationFailureKind.AUTH_REJECTED,
                    "The protected organization session is unavailable.",
                    exception);
        } catch (IOException exception) {
            throw new OrganizationAdministrationException(
                    isProtocolFailure(exception)
                            ? OrganizationAdministrationFailureKind.PROTOCOL
                            : OrganizationAdministrationFailureKind.NETWORK,
                    "The organization administration response could not be read.",
                    exception);
        }
    }

    private static <T> T body(Response<T> response)
            throws OrganizationAdministrationException {
        if (response.body() == null) {
            throw new OrganizationAdministrationException(
                    OrganizationAdministrationFailureKind.PROTOCOL,
                    "The organization administration response has no body.");
        }
        return response.body();
    }

    private OrganizationAdministrationException failure(Response<?> response) {
        Optional<ProblemDetails> problem = problemParser.parse(response);
        if (problem.isPresent()) {
            ProblemDetails details = problem.orElseThrow();
            return new OrganizationAdministrationException(
                    map(details.code()),
                    "The organization administration request was rejected.",
                    Optional.ofNullable(details.requestId())
                            .filter(value -> !value.isBlank()),
                    null);
        }
        return new OrganizationAdministrationException(
                status(response.code()),
                "The organization administration error response is invalid.");
    }

    private static boolean replayed(Response<?> response)
            throws OrganizationAdministrationException {
        String value = response.headers().get("X-Idempotency-Replayed");
        if (!"true".equals(value) && !"false".equals(value)) {
            throw new OrganizationAdministrationException(
                    OrganizationAdministrationFailureKind.PROTOCOL,
                    "The organization idempotency response is invalid.");
        }
        return Boolean.parseBoolean(value);
    }

    private static OrganizationAdministrationFailureKind map(
            MobileProblemCode code) {
        return switch (code) {
            case AUTH_REQUIRED, AUTH_FAILED ->
                    OrganizationAdministrationFailureKind.AUTH_REJECTED;
            case APP_UPDATE_REQUIRED ->
                    OrganizationAdministrationFailureKind.UPDATE_REQUIRED;
            case FORBIDDEN -> OrganizationAdministrationFailureKind.FORBIDDEN;
            case IDEMPOTENCY_IN_PROGRESS ->
                    OrganizationAdministrationFailureKind.IDEMPOTENCY_IN_PROGRESS;
            case IDEMPOTENCY_KEY_REUSED ->
                    OrganizationAdministrationFailureKind.IDEMPOTENCY_KEY_REUSED;
            case INVALID_JSON, INVALID_REQUEST ->
                    OrganizationAdministrationFailureKind.INVALID_REQUEST;
            case NOT_FOUND -> OrganizationAdministrationFailureKind.NOT_FOUND;
            case RATE_LIMITED -> OrganizationAdministrationFailureKind.RATE_LIMITED;
            case RESOURCE_CONFLICT -> OrganizationAdministrationFailureKind.CONFLICT;
            case SERVICE_UNAVAILABLE ->
                    OrganizationAdministrationFailureKind.SERVICE_UNAVAILABLE;
            default -> OrganizationAdministrationFailureKind.PROTOCOL;
        };
    }

    private static OrganizationAdministrationFailureKind status(int code) {
        if (code == HTTP_BAD_REQUEST || code == HTTP_UNPROCESSABLE) {
            return OrganizationAdministrationFailureKind.INVALID_REQUEST;
        }
        if (code == HTTP_UNAUTHORIZED) {
            return OrganizationAdministrationFailureKind.AUTH_REJECTED;
        }
        if (code == HTTP_FORBIDDEN) {
            return OrganizationAdministrationFailureKind.FORBIDDEN;
        }
        if (code == HTTP_NOT_FOUND) {
            return OrganizationAdministrationFailureKind.NOT_FOUND;
        }
        if (code == HTTP_CONFLICT) {
            return OrganizationAdministrationFailureKind.CONFLICT;
        }
        if (code == HTTP_UPDATE_REQUIRED) {
            return OrganizationAdministrationFailureKind.UPDATE_REQUIRED;
        }
        if (code == HTTP_TOO_MANY_REQUESTS) {
            return OrganizationAdministrationFailureKind.RATE_LIMITED;
        }
        if (code >= HTTP_SERVER_ERROR_START) {
            return OrganizationAdministrationFailureKind.SERVICE_UNAVAILABLE;
        }
        return OrganizationAdministrationFailureKind.PROTOCOL;
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

    private OrganizationAdministrationException protocol(
            IllegalArgumentException cause) {
        return new OrganizationAdministrationException(
                OrganizationAdministrationFailureKind.PROTOCOL,
                "The organization administration response was invalid.",
                cause);
    }
}
