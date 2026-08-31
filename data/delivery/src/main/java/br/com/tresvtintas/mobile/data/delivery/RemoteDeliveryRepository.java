package br.com.tresvtintas.mobile.data.delivery;

import br.com.tresvtintas.mobile.core.delivery.DeliveryAction;
import br.com.tresvtintas.mobile.core.delivery.DeliveryDetail;
import br.com.tresvtintas.mobile.core.delivery.DeliveryException;
import br.com.tresvtintas.mobile.core.delivery.DeliveryFailureKind;
import br.com.tresvtintas.mobile.core.delivery.DeliveryMutationResult;
import br.com.tresvtintas.mobile.core.delivery.DeliveryPage;
import br.com.tresvtintas.mobile.core.delivery.DeliveryQuery;
import br.com.tresvtintas.mobile.core.delivery.DeliveryRepository;
import br.com.tresvtintas.mobile.core.delivery.DeliveryRoutePage;
import br.com.tresvtintas.mobile.core.delivery.DeliveryRoutePlan;
import br.com.tresvtintas.mobile.core.delivery.DeliveryRouteRepository;
import br.com.tresvtintas.mobile.core.delivery.DeliveryStatus;
import br.com.tresvtintas.mobile.core.network.AuthenticationRequiredException;
import br.com.tresvtintas.mobile.core.network.MobileApiFactory;
import br.com.tresvtintas.mobile.core.network.api.MobileApi;
import br.com.tresvtintas.mobile.core.network.dto.DeliveryCompletionRequest;
import br.com.tresvtintas.mobile.core.network.dto.DeliveryMutationResponse;
import br.com.tresvtintas.mobile.core.network.dto.DeliveryStartRequest;
import br.com.tresvtintas.mobile.core.network.problem.MobileProblemCode;
import br.com.tresvtintas.mobile.core.network.problem.ProblemDetails;
import br.com.tresvtintas.mobile.core.network.problem.ProblemDetailsParser;
import com.fasterxml.jackson.core.JacksonException;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import retrofit2.Call;
import retrofit2.Response;

public final class RemoteDeliveryRepository implements
        DeliveryRepository,
        DeliveryRouteRepository {
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

    public RemoteDeliveryRepository(
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
    public DeliveryPage page(
            DeliveryQuery query,
            Optional<String> cursor) throws DeliveryException {
        requireActive();
        try {
            Long organizationId = query.organizationId().isPresent()
                    ? query.organizationId().orElseThrow()
                    : null;
            return DeliveryDtoMapper.page(body(execute(api.deliveries(
                    query.search().orElse(null),
                    organizationId,
                    query.status().map(value -> wire(value.name())).orElse(null),
                    query.view().wireValue(),
                    query.scheduledFrom()
                            .map(java.time.Instant::toString)
                            .orElse(null),
                    query.scheduledToExclusive()
                            .map(java.time.Instant::toString)
                            .orElse(null),
                    cursor.orElse(null),
                    query.pageSize()))));
        } catch (IllegalArgumentException exception) {
            throw protocol(exception);
        }
    }

    @Override
    public DeliveryDetail detail(long deliveryId) throws DeliveryException {
        requireActive();
        try {
            return DeliveryDtoMapper.detail(body(execute(api.delivery(deliveryId))));
        } catch (IllegalArgumentException exception) {
            throw protocol(exception);
        }
    }

    @Override
    public DeliveryRoutePage routes(LocalDate serviceDate) throws DeliveryException {
        requireActive();
        try {
            return DeliveryRouteDtoMapper.page(body(execute(api.deliveryRoutes(
                    Objects.requireNonNull(
                            serviceDate,
                            "Delivery route date is required.").toString(),
                    null))));
        } catch (IllegalArgumentException exception) {
            throw protocol(exception);
        }
    }

    @Override
    public DeliveryRoutePlan route(String routeKey) throws DeliveryException {
        requireActive();
        try {
            return DeliveryRouteDtoMapper.plan(body(execute(
                    api.deliveryRoute(routeKey))));
        } catch (IllegalArgumentException exception) {
            throw protocol(exception);
        }
    }

    @Override
    public DeliveryMutationResult start(
            long deliveryId,
            int expectedOrderRevision,
            String idempotencyKey) throws DeliveryException {
        requireActive();
        try {
            Response<DeliveryMutationResponse> response = execute(
                    api.startDelivery(
                            deliveryId,
                            idempotencyKey,
                            new DeliveryStartRequest(expectedOrderRevision)));
            return mutation(body(response), response);
        } catch (IllegalArgumentException exception) {
            throw protocol(exception);
        }
    }

    @Override
    public DeliveryMutationResult complete(
            long deliveryId,
            int expectedOrderRevision,
            String idempotencyKey) throws DeliveryException {
        requireActive();
        try {
            Response<DeliveryMutationResponse> response = execute(
                    api.completeDelivery(
                            deliveryId,
                            idempotencyKey,
                            new DeliveryCompletionRequest(expectedOrderRevision)));
            return mutation(body(response), response);
        } catch (IllegalArgumentException exception) {
            throw protocol(exception);
        }
    }

    public void close() {
        active.set(false);
    }

    private void requireActive() throws DeliveryException {
        if (!active.get()) {
            throw new DeliveryException(
                    DeliveryFailureKind.ACCESS_REVOKED,
                    "Delivery account scope is no longer active.");
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
                    "The protected delivery session is unavailable.",
                    exception);
        } catch (IOException exception) {
            throw new DeliveryException(
                    isProtocolFailure(exception)
                            ? DeliveryFailureKind.PROTOCOL
                            : DeliveryFailureKind.NETWORK,
                    "The delivery response could not be read.",
                    exception);
        }
    }

    private static <T> T body(Response<T> response) throws DeliveryException {
        if (response.body() == null) {
            throw new DeliveryException(
                    DeliveryFailureKind.PROTOCOL,
                    "The delivery response did not contain a body.");
        }
        return response.body();
    }

    private DeliveryException failure(Response<?> response) {
        Optional<ProblemDetails> problem = problemParser.parse(response);
        if (problem.isPresent()) {
            ProblemDetails details = problem.orElseThrow();
            return new DeliveryException(
                    map(details.code()),
                    "The delivery request was rejected.",
                    Optional.ofNullable(details.requestId())
                            .filter(value -> !value.isBlank()),
                    null);
        }
        return new DeliveryException(
                status(response.code()),
                "The delivery service returned an invalid error response.");
    }

    private static DeliveryMutationResult mutation(
            DeliveryMutationResponse value,
            Response<?> response) throws DeliveryException {
        return new DeliveryMutationResult(
                enumValue(DeliveryAction.class, value.action()),
                value.deliveryId(),
                enumValue(DeliveryStatus.class, value.deliveryStatus()),
                value.orderId(),
                value.orderStatus(),
                value.orderRevision(),
                value.changed(),
                replayed(response));
    }

    private static boolean replayed(Response<?> response)
            throws DeliveryException {
        String value = response.headers().get("X-Idempotency-Replayed");
        if (!"true".equals(value) && !"false".equals(value)) {
            throw new DeliveryException(
                    DeliveryFailureKind.PROTOCOL,
                    "The delivery idempotency response is invalid.");
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

    private static String wire(String value) {
        return value.toLowerCase(Locale.ROOT);
    }

    private static <T extends Enum<T>> T enumValue(
            Class<T> type,
            String value) {
        return Enum.valueOf(type, value.toUpperCase(Locale.ROOT));
    }

    private static DeliveryException protocol(
            IllegalArgumentException exception) {
        return new DeliveryException(
                DeliveryFailureKind.PROTOCOL,
                "The delivery response was invalid.",
                exception);
    }
}
