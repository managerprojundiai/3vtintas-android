package br.com.tresvtintas.mobile.data.painteradmin;

import br.com.tresvtintas.mobile.core.network.AuthenticationRequiredException;
import br.com.tresvtintas.mobile.core.network.MobileApiFactory;
import br.com.tresvtintas.mobile.core.network.api.MobileApi;
import br.com.tresvtintas.mobile.core.network.dto.PainterAdministrationDtos;
import br.com.tresvtintas.mobile.core.network.problem.MobileProblemCode;
import br.com.tresvtintas.mobile.core.network.problem.ProblemDetails;
import br.com.tresvtintas.mobile.core.network.problem.ProblemDetailsParser;
import br.com.tresvtintas.mobile.core.painteradmin.PainterAdministrationException;
import br.com.tresvtintas.mobile.core.painteradmin.PainterAdministrationFailureKind;
import br.com.tresvtintas.mobile.core.painteradmin.PainterAdministrationModels.AccessRequest;
import br.com.tresvtintas.mobile.core.painteradmin.PainterAdministrationModels.AccessRequestDetail;
import br.com.tresvtintas.mobile.core.painteradmin.PainterAdministrationModels.Mutation;
import br.com.tresvtintas.mobile.core.painteradmin.PainterAdministrationModels.Options;
import br.com.tresvtintas.mobile.core.painteradmin.PainterAdministrationModels.Page;
import br.com.tresvtintas.mobile.core.painteradmin.PainterAdministrationModels.Painter;
import br.com.tresvtintas.mobile.core.painteradmin.PainterAdministrationModels.PainterDetail;
import br.com.tresvtintas.mobile.core.painteradmin.PainterAdministrationQuery;
import br.com.tresvtintas.mobile.core.painteradmin.PainterAdministrationRepository;
import br.com.tresvtintas.mobile.core.painteradmin.PainterAdministrationStatus;
import br.com.tresvtintas.mobile.core.painteradmin.PainterDraft;
import com.fasterxml.jackson.core.JacksonException;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.concurrent.atomic.AtomicBoolean;
import retrofit2.Call;
import retrofit2.Response;

public final class RemotePainterAdministrationRepository
        implements PainterAdministrationRepository {
    private static final int HTTP_BAD_REQUEST = 400;
    private static final int HTTP_UNAUTHORIZED = 401;
    private static final int HTTP_FORBIDDEN = 403;
    private static final int HTTP_NOT_FOUND = 404;
    private static final int HTTP_CONFLICT = 409;
    private static final int HTTP_UNPROCESSABLE = 422;
    private static final int HTTP_UPDATE_REQUIRED = 426;
    private static final int HTTP_TOO_MANY_REQUESTS = 429;
    private static final int HTTP_SERVER_ERROR_START = 500;

    private final PainterAdministrationAccountScope scope;
    private final MobileApi api;
    private final ProblemDetailsParser problemParser;
    private final AtomicBoolean active = new AtomicBoolean(true);

    public RemotePainterAdministrationRepository(
            PainterAdministrationAccountScope scope,
            MobileApi api) {
        this.scope = Objects.requireNonNull(scope, "Painter scope is required.");
        this.api = Objects.requireNonNull(api, "Mobile API is required.");
        this.problemParser = MobileApiFactory.problemDetailsParser();
    }

    public PainterAdministrationAccountScope scope() {
        return scope;
    }

    @Override
    public Options options() throws PainterAdministrationException {
        requireActive();
        try {
            return PainterAdministrationDtoMapper.options(
                    body(execute(api.painterAdministrationOptions())));
        } catch (IllegalArgumentException exception) {
            throw protocol(exception);
        }
    }

    @Override
    public Page<Painter> painters(
            PainterAdministrationQuery query,
            Optional<String> cursor) throws PainterAdministrationException {
        requireActive();
        Objects.requireNonNull(query, "Painter query is required.");
        try {
            Long organizationId = query.organizationId().isPresent()
                    ? query.organizationId().orElseThrow()
                    : null;
            return PainterAdministrationDtoMapper.painters(body(execute(
                    api.painterAdministrationPainters(
                            organizationId,
                            query.status()
                                    .map(PainterAdministrationStatus::wireValue)
                                    .orElse(null),
                            query.search().orElse(null),
                            cursor.orElse(null),
                            query.pageSize()))));
        } catch (IllegalArgumentException exception) {
            throw protocol(exception);
        }
    }

    @Override
    public PainterDetail painter(long painterId)
            throws PainterAdministrationException {
        requireActive();
        try {
            return PainterAdministrationDtoMapper.painterDetail(body(execute(
                    api.painterAdministrationPainter(painterId))));
        } catch (IllegalArgumentException exception) {
            throw protocol(exception);
        }
    }

    @Override
    public Page<AccessRequest> accessRequests(
            Optional<String> cursor,
            int pageSize) throws PainterAdministrationException {
        requireActive();
        try {
            return PainterAdministrationDtoMapper.requests(body(execute(
                    api.painterAdministrationAccessRequests(
                            cursor.orElse(null),
                            pageSize))));
        } catch (IllegalArgumentException exception) {
            throw protocol(exception);
        }
    }

    @Override
    public AccessRequestDetail accessRequest(long requestId)
            throws PainterAdministrationException {
        requireActive();
        try {
            return PainterAdministrationDtoMapper.requestDetail(body(execute(
                    api.painterAdministrationAccessRequest(requestId))));
        } catch (IllegalArgumentException exception) {
            throw protocol(exception);
        }
    }

    @Override
    public Mutation create(PainterDraft draft, String key)
            throws PainterAdministrationException {
        requireActive();
        Objects.requireNonNull(draft, "Painter draft is required.");
        try {
            return mutation(execute(api.createPainterAdministrationPainter(
                    key,
                    new PainterAdministrationDtos.CreateRequest(
                            draft.organizationId(),
                            draft.name(),
                            draft.email(),
                            nullable(draft.cpf()),
                            nullable(draft.rg()),
                            nullable(draft.phone()),
                            nullable(draft.company()),
                            nullable(draft.specialty()),
                            nullable(draft.serviceArea()),
                            nullable(draft.address()),
                            nullable(draft.city()),
                            nullable(draft.state()),
                            draft.commissionRate().doubleValue(),
                            nullable(draft.managerUserId()),
                            nullable(draft.notes()),
                            true))));
        } catch (IllegalArgumentException exception) {
            throw protocol(exception);
        }
    }

    @Override
    public Mutation updateStatus(
            long painterId,
            int revision,
            PainterAdministrationStatus status,
            String key) throws PainterAdministrationException {
        requireActive();
        try {
            return mutation(execute(api.updatePainterAdministrationStatus(
                    painterId,
                    key,
                    new PainterAdministrationDtos.StatusRequest(
                            "status",
                            revision,
                            status.wireValue(),
                            true))));
        } catch (IllegalArgumentException exception) {
            throw protocol(exception);
        }
    }

    @Override
    public Mutation updateCommission(
            long painterId,
            int revision,
            BigDecimal rate,
            String key) throws PainterAdministrationException {
        requireActive();
        try {
            return mutation(execute(api.updatePainterAdministrationCommission(
                    painterId,
                    key,
                    new PainterAdministrationDtos.CommissionRequest(
                            "commission",
                            revision,
                            rate.doubleValue(),
                            true))));
        } catch (IllegalArgumentException exception) {
            throw protocol(exception);
        }
    }

    @Override
    public Mutation updateManager(
            long painterId,
            int revision,
            OptionalLong managerUserId,
            String key) throws PainterAdministrationException {
        requireActive();
        try {
            return mutation(execute(api.updatePainterAdministrationManager(
                    painterId,
                    key,
                    new PainterAdministrationDtos.ManagerRequest(
                            "manager",
                            revision,
                            nullable(managerUserId),
                            true))));
        } catch (IllegalArgumentException exception) {
            throw protocol(exception);
        }
    }

    @Override
    public Mutation approvePainter(
            long requestId,
            int revision,
            long organizationId,
            OptionalLong managerUserId,
            BigDecimal rate,
            String key) throws PainterAdministrationException {
        requireActive();
        try {
            return mutation(execute(api.approvePainterAccessRequest(
                    requestId,
                    key,
                    new PainterAdministrationDtos.PainterDecisionRequest(
                            "painter",
                            revision,
                            organizationId,
                            nullable(managerUserId),
                            rate.doubleValue(),
                            true))));
        } catch (IllegalArgumentException exception) {
            throw protocol(exception);
        }
    }

    @Override
    public Mutation approveManager(
            long requestId,
            int revision,
            long organizationId,
            String key) throws PainterAdministrationException {
        requireActive();
        try {
            return mutation(execute(api.approveManagerAccessRequest(
                    requestId,
                    key,
                    new PainterAdministrationDtos.ManagerDecisionRequest(
                            "manager",
                            revision,
                            organizationId,
                            true))));
        } catch (IllegalArgumentException exception) {
            throw protocol(exception);
        }
    }

    @Override
    public Mutation reject(
            long requestId,
            int revision,
            String reason,
            String key) throws PainterAdministrationException {
        requireActive();
        try {
            return mutation(execute(api.rejectAccessRequest(
                    requestId,
                    key,
                    new PainterAdministrationDtos.RejectDecisionRequest(
                            "reject",
                            revision,
                            reason,
                            true))));
        } catch (IllegalArgumentException exception) {
            throw protocol(exception);
        }
    }

    public void close() {
        active.set(false);
    }

    private Mutation mutation(Response<PainterAdministrationDtos.Mutation> response)
            throws PainterAdministrationException {
        return PainterAdministrationDtoMapper.mutation(
                body(response),
                replayed(response));
    }

    private void requireActive() throws PainterAdministrationException {
        if (!active.get()) {
            throw new PainterAdministrationException(
                    PainterAdministrationFailureKind.ACCESS_REVOKED,
                    "Painter administration scope is inactive.");
        }
    }

    private <T> Response<T> execute(Call<T> call)
            throws PainterAdministrationException {
        try {
            Response<T> response = call.execute();
            if (!response.isSuccessful()) {
                throw failure(response);
            }
            return response;
        } catch (AuthenticationRequiredException exception) {
            throw new PainterAdministrationException(
                    PainterAdministrationFailureKind.AUTH_REJECTED,
                    "The protected painter session is unavailable.",
                    exception);
        } catch (IOException exception) {
            throw new PainterAdministrationException(
                    isProtocolFailure(exception)
                            ? PainterAdministrationFailureKind.PROTOCOL
                            : PainterAdministrationFailureKind.NETWORK,
                    "The painter administration response could not be read.",
                    exception);
        }
    }

    private static <T> T body(Response<T> response)
            throws PainterAdministrationException {
        if (response.body() == null) {
            throw new PainterAdministrationException(
                    PainterAdministrationFailureKind.PROTOCOL,
                    "The painter administration response has no body.");
        }
        return response.body();
    }

    private PainterAdministrationException failure(Response<?> response) {
        Optional<ProblemDetails> problem = problemParser.parse(response);
        if (problem.isPresent()) {
            ProblemDetails details = problem.orElseThrow();
            return new PainterAdministrationException(
                    map(details.code()),
                    "The painter administration request was rejected.",
                    Optional.ofNullable(details.requestId())
                            .filter(value -> !value.isBlank()),
                    null);
        }
        return new PainterAdministrationException(
                status(response.code()),
                "The painter administration error response is invalid.");
    }

    private static boolean replayed(Response<?> response)
            throws PainterAdministrationException {
        String value = response.headers().get("X-Idempotency-Replayed");
        if (!"true".equals(value) && !"false".equals(value)) {
            throw new PainterAdministrationException(
                    PainterAdministrationFailureKind.PROTOCOL,
                    "The painter idempotency response is invalid.");
        }
        return Boolean.parseBoolean(value);
    }

    private static PainterAdministrationFailureKind map(MobileProblemCode code) {
        return switch (code) {
            case AUTH_REQUIRED, AUTH_FAILED ->
                    PainterAdministrationFailureKind.AUTH_REJECTED;
            case APP_UPDATE_REQUIRED ->
                    PainterAdministrationFailureKind.UPDATE_REQUIRED;
            case FORBIDDEN -> PainterAdministrationFailureKind.FORBIDDEN;
            case IDEMPOTENCY_IN_PROGRESS ->
                    PainterAdministrationFailureKind.IDEMPOTENCY_IN_PROGRESS;
            case IDEMPOTENCY_KEY_REUSED ->
                    PainterAdministrationFailureKind.IDEMPOTENCY_KEY_REUSED;
            case INVALID_JSON, INVALID_REQUEST ->
                    PainterAdministrationFailureKind.INVALID_REQUEST;
            case NOT_FOUND -> PainterAdministrationFailureKind.NOT_FOUND;
            case RATE_LIMITED -> PainterAdministrationFailureKind.RATE_LIMITED;
            case RESOURCE_CONFLICT ->
                    PainterAdministrationFailureKind.CONFLICT;
            case SERVICE_UNAVAILABLE ->
                    PainterAdministrationFailureKind.SERVICE_UNAVAILABLE;
            default -> PainterAdministrationFailureKind.PROTOCOL;
        };
    }

    private static PainterAdministrationFailureKind status(int code) {
        if (code == HTTP_BAD_REQUEST || code == HTTP_UNPROCESSABLE) {
            return PainterAdministrationFailureKind.INVALID_REQUEST;
        }
        if (code == HTTP_UNAUTHORIZED) {
            return PainterAdministrationFailureKind.AUTH_REJECTED;
        }
        if (code == HTTP_FORBIDDEN) {
            return PainterAdministrationFailureKind.FORBIDDEN;
        }
        if (code == HTTP_NOT_FOUND) {
            return PainterAdministrationFailureKind.NOT_FOUND;
        }
        if (code == HTTP_CONFLICT) {
            return PainterAdministrationFailureKind.CONFLICT;
        }
        if (code == HTTP_UPDATE_REQUIRED) {
            return PainterAdministrationFailureKind.UPDATE_REQUIRED;
        }
        if (code == HTTP_TOO_MANY_REQUESTS) {
            return PainterAdministrationFailureKind.RATE_LIMITED;
        }
        if (code >= HTTP_SERVER_ERROR_START) {
            return PainterAdministrationFailureKind.SERVICE_UNAVAILABLE;
        }
        return PainterAdministrationFailureKind.PROTOCOL;
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

    private static String nullable(Optional<String> value) {
        return value.orElse(null);
    }

    private static Long nullable(OptionalLong value) {
        return value.isPresent() ? value.orElseThrow() : null;
    }

    private PainterAdministrationException protocol(
            IllegalArgumentException cause) {
        return new PainterAdministrationException(
                PainterAdministrationFailureKind.PROTOCOL,
                "The painter administration response was invalid.",
                cause);
    }
}
