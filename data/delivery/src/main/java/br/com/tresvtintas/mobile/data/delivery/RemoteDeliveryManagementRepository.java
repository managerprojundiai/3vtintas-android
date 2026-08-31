package br.com.tresvtintas.mobile.data.delivery;

import br.com.tresvtintas.mobile.core.delivery.DeliveryException;
import br.com.tresvtintas.mobile.core.delivery.DeliveryFailureKind;
import br.com.tresvtintas.mobile.core.delivery.DeliveryManagementDetail;
import br.com.tresvtintas.mobile.core.delivery.DeliveryManagementDriver;
import br.com.tresvtintas.mobile.core.delivery.DeliveryManagementMutationResult;
import br.com.tresvtintas.mobile.core.delivery.DeliveryManagementOrganization;
import br.com.tresvtintas.mobile.core.delivery.DeliveryManagementPage;
import br.com.tresvtintas.mobile.core.delivery.DeliveryManagementQuery;
import br.com.tresvtintas.mobile.core.delivery.DeliveryManagementRepository;
import br.com.tresvtintas.mobile.core.network.AuthenticationRequiredException;
import br.com.tresvtintas.mobile.core.network.MobileApiFactory;
import br.com.tresvtintas.mobile.core.network.api.MobileApi;
import br.com.tresvtintas.mobile.core.network.dto.DeliveryManagementAssignmentRequest;
import br.com.tresvtintas.mobile.core.network.dto.DeliveryManagementCompletionRequest;
import br.com.tresvtintas.mobile.core.network.dto.DeliveryManagementMutationResponse;
import br.com.tresvtintas.mobile.core.network.dto.DeliveryManagementScheduleRequest;
import br.com.tresvtintas.mobile.core.network.problem.MobileProblemCode;
import br.com.tresvtintas.mobile.core.network.problem.ProblemDetails;
import br.com.tresvtintas.mobile.core.network.problem.ProblemDetailsParser;
import com.fasterxml.jackson.core.JacksonException;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.concurrent.atomic.AtomicBoolean;
import retrofit2.Call;
import retrofit2.Response;

public final class RemoteDeliveryManagementRepository
        implements DeliveryManagementRepository {
    private static final int HTTP_BAD_REQUEST = 400;
    private static final int HTTP_UNAUTHORIZED = 401;
    private static final int HTTP_FORBIDDEN = 403;
    private static final int HTTP_NOT_FOUND = 404;
    private static final int HTTP_CONFLICT = 409;
    private static final int HTTP_UPDATE_REQUIRED = 426;
    private static final int HTTP_TOO_MANY_REQUESTS = 429;
    private static final int HTTP_SERVER_ERROR_START = 500;

    private final DeliveryAccountScope scope;
    private final MobileApi api;
    private final ProblemDetailsParser problemParser;
    private final AtomicBoolean active = new AtomicBoolean(true);

    public RemoteDeliveryManagementRepository(
            DeliveryAccountScope scope,
            MobileApi api) {
        this.scope = Objects.requireNonNull(scope, "Delivery scope is required.");
        this.api = Objects.requireNonNull(api, "Mobile API is required.");
        this.problemParser = MobileApiFactory.problemDetailsParser();
    }

    public DeliveryAccountScope scope() {
        return scope;
    }

    @Override
    public List<DeliveryManagementOrganization> organizations()
            throws DeliveryException {
        requireActive();
        try {
            return body(execute(api.deliveryManagementOrganizations()))
                    .items()
                    .stream()
                    .map(value -> new DeliveryManagementOrganization(
                            value.id(),
                            value.name()))
                    .toList();
        } catch (IllegalArgumentException exception) {
            throw protocol(exception);
        }
    }

    @Override
    public List<DeliveryManagementDriver> drivers(long organizationId)
            throws DeliveryException {
        requireActive();
        try {
            return body(execute(api.deliveryManagementDrivers(organizationId)))
                    .items()
                    .stream()
                    .map(DeliveryManagementDtoMapper::driver)
                    .toList();
        } catch (IllegalArgumentException exception) {
            throw protocol(exception);
        }
    }

    @Override
    public DeliveryManagementPage page(
            DeliveryManagementQuery query,
            Optional<String> cursor) throws DeliveryException {
        requireActive();
        try {
            return DeliveryManagementDtoMapper.page(body(execute(
                    api.deliveryManagementOrders(
                            query.organizationId(),
                            query.search().orElse(null),
                            query.view().wireValue(),
                            cursor.orElse(null),
                            query.pageSize()))));
        } catch (IllegalArgumentException exception) {
            throw protocol(exception);
        }
    }

    @Override
    public DeliveryManagementDetail detail(
            long organizationId,
            long orderId) throws DeliveryException {
        requireActive();
        try {
            return DeliveryManagementDtoMapper.detail(body(execute(
                    api.deliveryManagementOrder(orderId, organizationId))));
        } catch (IllegalArgumentException exception) {
            throw protocol(exception);
        }
    }

    @Override
    public DeliveryManagementMutationResult schedule(
            long organizationId,
            long orderId,
            int expectedOrderRevision,
            Instant scheduledAt,
            int durationMinutes,
            String idempotencyKey) throws DeliveryException {
        requireActive();
        try {
            Response<DeliveryManagementMutationResponse> response = execute(
                    api.scheduleManagedDelivery(
                            orderId,
                            idempotencyKey,
                            new DeliveryManagementScheduleRequest(
                                    organizationId,
                                    expectedOrderRevision,
                                    scheduledAt.toString(),
                                    durationMinutes)));
            return mutation(response);
        } catch (IllegalArgumentException exception) {
            throw protocol(exception);
        }
    }

    @Override
    public DeliveryManagementMutationResult assign(
            long organizationId,
            long orderId,
            int expectedOrderRevision,
            OptionalLong driverUserId,
            String idempotencyKey) throws DeliveryException {
        requireActive();
        try {
            Long driver = driverUserId.isPresent()
                    ? driverUserId.getAsLong()
                    : null;
            Response<DeliveryManagementMutationResponse> response = execute(
                    api.assignManagedDeliveryDriver(
                            orderId,
                            idempotencyKey,
                            new DeliveryManagementAssignmentRequest(
                                    organizationId,
                                    expectedOrderRevision,
                                    driver)));
            return mutation(response);
        } catch (IllegalArgumentException exception) {
            throw protocol(exception);
        }
    }

    @Override
    public DeliveryManagementMutationResult complete(
            long organizationId,
            long orderId,
            int expectedOrderRevision,
            String idempotencyKey) throws DeliveryException {
        requireActive();
        try {
            Response<DeliveryManagementMutationResponse> response = execute(
                    api.completeManagedDelivery(
                            orderId,
                            idempotencyKey,
                            new DeliveryManagementCompletionRequest(
                                    organizationId,
                                    expectedOrderRevision)));
            return mutation(response);
        } catch (IllegalArgumentException exception) {
            throw protocol(exception);
        }
    }

    public void close() {
        active.set(false);
    }

    private DeliveryManagementMutationResult mutation(
            Response<DeliveryManagementMutationResponse> response)
            throws DeliveryException {
        return DeliveryManagementDtoMapper.mutation(
                body(response),
                replayed(response));
    }

    private void requireActive() throws DeliveryException {
        if (!active.get()) {
            throw new DeliveryException(
                    DeliveryFailureKind.ACCESS_REVOKED,
                    "Delivery management account scope is inactive.");
        }
    }

    private <T> Response<T> execute(Call<T> call) throws DeliveryException {
        try {
            Response<T> response = call.execute();
            if (!response.isSuccessful()) {
                throw failure(response);
            }
            return response;
        } catch (AuthenticationRequiredException exception) {
            throw new DeliveryException(
                    DeliveryFailureKind.AUTH_REJECTED,
                    "The protected delivery management session is unavailable.",
                    exception);
        } catch (IOException exception) {
            throw new DeliveryException(
                    isProtocolFailure(exception)
                            ? DeliveryFailureKind.PROTOCOL
                            : DeliveryFailureKind.NETWORK,
                    "The delivery management response could not be read.",
                    exception);
        }
    }

    private static <T> T body(Response<T> response)
            throws DeliveryException {
        if (response.body() == null) {
            throw new DeliveryException(
                    DeliveryFailureKind.PROTOCOL,
                    "The delivery management response has no body.");
        }
        return response.body();
    }

    private DeliveryException failure(Response<?> response) {
        Optional<ProblemDetails> problem = problemParser.parse(response);
        if (problem.isPresent()) {
            ProblemDetails details = problem.orElseThrow();
            return new DeliveryException(
                    map(details.code()),
                    "The delivery management request was rejected.",
                    Optional.ofNullable(details.requestId())
                            .filter(value -> !value.isBlank()),
                    null);
        }
        return new DeliveryException(
                status(response.code()),
                "The delivery management error response is invalid.");
    }

    private static boolean replayed(Response<?> response)
            throws DeliveryException {
        String value = response.headers().get("X-Idempotency-Replayed");
        if (!"true".equals(value) && !"false".equals(value)) {
            throw new DeliveryException(
                    DeliveryFailureKind.PROTOCOL,
                    "The management idempotency response is invalid.");
        }
        return Boolean.parseBoolean(value);
    }

    private static DeliveryFailureKind map(MobileProblemCode code) {
        return switch (code) {
            case AUTH_REQUIRED, AUTH_FAILED -> DeliveryFailureKind.AUTH_REJECTED;
            case APP_UPDATE_REQUIRED -> DeliveryFailureKind.UPDATE_REQUIRED;
            case FORBIDDEN -> DeliveryFailureKind.FORBIDDEN;
            case IDEMPOTENCY_IN_PROGRESS ->
                    DeliveryFailureKind.IDEMPOTENCY_IN_PROGRESS;
            case IDEMPOTENCY_KEY_REUSED ->
                    DeliveryFailureKind.IDEMPOTENCY_KEY_REUSED;
            case INVALID_JSON, INVALID_REQUEST ->
                    DeliveryFailureKind.INVALID_REQUEST;
            case NOT_FOUND -> DeliveryFailureKind.NOT_FOUND;
            case RATE_LIMITED -> DeliveryFailureKind.RATE_LIMITED;
            case RESOURCE_CONFLICT -> DeliveryFailureKind.CONFLICT;
            case SERVICE_UNAVAILABLE -> DeliveryFailureKind.SERVICE_UNAVAILABLE;
            default -> DeliveryFailureKind.PROTOCOL;
        };
    }

    private static DeliveryFailureKind status(int code) {
        if (code == HTTP_BAD_REQUEST) {
            return DeliveryFailureKind.INVALID_REQUEST;
        }
        if (code == HTTP_UNAUTHORIZED) {
            return DeliveryFailureKind.AUTH_REJECTED;
        }
        if (code == HTTP_FORBIDDEN) {
            return DeliveryFailureKind.FORBIDDEN;
        }
        if (code == HTTP_NOT_FOUND) {
            return DeliveryFailureKind.NOT_FOUND;
        }
        if (code == HTTP_CONFLICT) {
            return DeliveryFailureKind.CONFLICT;
        }
        if (code == HTTP_UPDATE_REQUIRED) {
            return DeliveryFailureKind.UPDATE_REQUIRED;
        }
        if (code == HTTP_TOO_MANY_REQUESTS) {
            return DeliveryFailureKind.RATE_LIMITED;
        }
        if (code >= HTTP_SERVER_ERROR_START) {
            return DeliveryFailureKind.SERVICE_UNAVAILABLE;
        }
        return DeliveryFailureKind.PROTOCOL;
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

    private static DeliveryException protocol(
            IllegalArgumentException exception) {
        return new DeliveryException(
                DeliveryFailureKind.PROTOCOL,
                "The delivery management response was invalid.",
                exception);
    }
}
