package br.com.tresvtintas.mobile.data.order;

import br.com.tresvtintas.mobile.core.network.AuthenticationRequiredException;
import br.com.tresvtintas.mobile.core.network.MobileApiFactory;
import br.com.tresvtintas.mobile.core.network.api.MobileApi;
import br.com.tresvtintas.mobile.core.network.dto.OrderConversionRequest;
import br.com.tresvtintas.mobile.core.network.dto.OrderConversionResponse;
import br.com.tresvtintas.mobile.core.network.dto.OrderCancellationRequest;
import br.com.tresvtintas.mobile.core.network.dto.OrderMutationResponse;
import br.com.tresvtintas.mobile.core.network.dto.OrderPaymentReceiptRequest;
import br.com.tresvtintas.mobile.core.network.dto.OrderStatusMutationRequest;
import br.com.tresvtintas.mobile.core.network.problem.MobileProblemCode;
import br.com.tresvtintas.mobile.core.network.problem.ProblemDetails;
import br.com.tresvtintas.mobile.core.network.problem.ProblemDetailsParser;
import br.com.tresvtintas.mobile.core.order.OrderDetail;
import br.com.tresvtintas.mobile.core.order.OrderAction;
import br.com.tresvtintas.mobile.core.order.OrderActionResult;
import br.com.tresvtintas.mobile.core.order.OrderConversionResult;
import br.com.tresvtintas.mobile.core.order.OrderException;
import br.com.tresvtintas.mobile.core.order.OrderFailureKind;
import br.com.tresvtintas.mobile.core.order.OrderPage;
import br.com.tresvtintas.mobile.core.order.OrderPaymentMethod;
import br.com.tresvtintas.mobile.core.order.OrderPaymentStatus;
import br.com.tresvtintas.mobile.core.order.OrderQuery;
import br.com.tresvtintas.mobile.core.order.OrderRepository;
import br.com.tresvtintas.mobile.core.order.OrderStatus;
import com.fasterxml.jackson.core.JacksonException;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import retrofit2.Call;
import retrofit2.Response;

public final class RemoteOrderRepository implements OrderRepository {
    private static final int HTTP_BAD_REQUEST = 400;
    private static final int HTTP_UNAUTHORIZED = 401;
    private static final int HTTP_FORBIDDEN = 403;
    private static final int HTTP_NOT_FOUND = 404;
    private static final int HTTP_CONFLICT = 409;
    private static final int HTTP_UPDATE_REQUIRED = 426;
    private static final int HTTP_TOO_MANY_REQUESTS = 429;
    private static final int HTTP_SERVER_ERROR_START = 500;

    private final OrderAccountScope scope;
    private final MobileApi api;
    private final boolean canReadPrices;
    private final ProblemDetailsParser problemParser;
    private final AtomicBoolean active = new AtomicBoolean(true);

    public RemoteOrderRepository(
            OrderAccountScope scope,
            MobileApi api,
            boolean canReadPrices) {
        this.scope = Objects.requireNonNull(scope, "Order scope is required.");
        this.api = Objects.requireNonNull(api, "Mobile API is required.");
        this.canReadPrices = canReadPrices;
        this.problemParser = MobileApiFactory.problemDetailsParser();
    }

    public OrderAccountScope scope() { return scope; }

    @Override
    public OrderPage page(OrderQuery query, Optional<String> cursor) throws OrderException {
        requireActive();
        try {
            Call<br.com.tresvtintas.mobile.core.network.dto.OrderPageDto> call = canReadPrices
                    ? api.orders(query.search().orElse(null),
                    query.type().map(value -> value.name().toLowerCase(Locale.ROOT)).orElse(null),
                    query.status().map(value -> value.name().toLowerCase(Locale.ROOT)).orElse(null),
                    query.view() == br.com.tresvtintas.mobile.core.order.OrderView.ALL
                            ? null : query.view().queryValue(),
                    cursor.orElse(null), query.pageSize())
                    : api.orderInformation(query.search().orElse(null),
                    query.type().map(value -> value.name().toLowerCase(Locale.ROOT)).orElse(null),
                    query.status().map(value -> value.name().toLowerCase(Locale.ROOT)).orElse(null),
                    query.view() == br.com.tresvtintas.mobile.core.order.OrderView.ALL
                            ? null : query.view().queryValue(),
                    cursor.orElse(null), query.pageSize());
            return OrderDtoMapper.page(body(execute(call)), canReadPrices);
        } catch (IllegalArgumentException exception) {
            throw protocol(exception);
        }
    }

    @Override
    public OrderDetail detail(long orderId) throws OrderException {
        requireActive();
        try {
            return OrderDtoMapper.detail(body(execute(canReadPrices
                    ? api.pricedOrder(orderId)
                    : api.orderInformation(orderId))), canReadPrices);
        } catch (IllegalArgumentException exception) {
            throw protocol(exception);
        }
    }

    @Override
    public OrderConversionResult convertMaterialQuote(long quoteId, int expectedRevision,
            String idempotencyKey) throws OrderException {
        requireActive();
        try {
            Response<OrderConversionResponse> response = execute(
                    api.convertMaterialQuoteToOrder(quoteId, idempotencyKey,
                            new OrderConversionRequest(expectedRevision, true)));
            OrderConversionResponse value = body(response);
            return new OrderConversionResult(value.quoteId(), value.quoteRevision(), value.orderId(),
                    value.orderRevision(), OrderStatus.valueOf(value.status().toUpperCase(Locale.ROOT)),
                    new BigDecimal(value.total()), value.deliveryId(), value.seller().userId(),
                    value.seller().role(), replayed(response));
        } catch (IllegalArgumentException exception) {
            throw protocol(exception);
        }
    }

    @Override
    public OrderActionResult transitionStatus(
            long orderId,
            int expectedRevision,
            OrderStatus status,
            String idempotencyKey) throws OrderException {
        if (status != OrderStatus.CONFIRMED
                && status != OrderStatus.IN_PROGRESS
                && status != OrderStatus.DELIVERED) {
            throw new OrderException(
                    OrderFailureKind.INVALID_REQUEST,
                    "The order status target is invalid.");
        }
        requireActive();
        try {
            Response<OrderMutationResponse> response = execute(
                    api.transitionOrderStatus(
                            orderId,
                            idempotencyKey,
                            new OrderStatusMutationRequest(
                                    expectedRevision,
                                    status.name().toLowerCase(Locale.ROOT),
                                    true)));
            return action(body(response), response);
        } catch (IllegalArgumentException exception) {
            throw protocol(exception);
        }
    }

    @Override
    public OrderActionResult cancel(
            long orderId,
            int expectedRevision,
            Optional<String> reason,
            String idempotencyKey) throws OrderException {
        requireActive();
        try {
            Response<OrderMutationResponse> response = execute(
                    api.cancelOrder(
                            orderId,
                            idempotencyKey,
                            new OrderCancellationRequest(
                                    expectedRevision,
                                    optionalText(reason),
                                    true)));
            return action(body(response), response);
        } catch (IllegalArgumentException exception) {
            throw protocol(exception);
        }
    }

    @Override
    public OrderActionResult recordPayment(
            long orderId,
            int expectedRevision,
            OrderPaymentMethod paymentMethod,
            Optional<String> paymentReference,
            String idempotencyKey) throws OrderException {
        if (paymentMethod == null) {
            throw new OrderException(
                    OrderFailureKind.INVALID_REQUEST,
                    "The order payment method is required.");
        }
        requireActive();
        try {
            Response<OrderMutationResponse> response = execute(
                    api.recordOrderPayment(
                            orderId,
                            idempotencyKey,
                            new OrderPaymentReceiptRequest(
                                    expectedRevision,
                                    paymentMethod.wireValue(),
                                    optionalText(paymentReference),
                                    true)));
            return action(body(response), response);
        } catch (IllegalArgumentException exception) {
            throw protocol(exception);
        }
    }

    public void close() { active.set(false); }

    private void requireActive() throws OrderException {
        if (!active.get()) {
            throw new OrderException(OrderFailureKind.ACCESS_REVOKED,
                    "Order account scope is no longer active.");
        }
    }

    private <T> Response<T> execute(Call<T> call) throws OrderException {
        try {
            Response<T> response = call.execute();
            if (!response.isSuccessful()) {
                throw failure(response);
            }
            return response;
        } catch (AuthenticationRequiredException exception) {
            throw new OrderException(OrderFailureKind.AUTH_REJECTED,
                    "The protected order session is unavailable.", exception);
        } catch (IOException exception) {
            throw new OrderException(isProtocolFailure(exception)
                    ? OrderFailureKind.PROTOCOL : OrderFailureKind.NETWORK,
                    "The order response could not be read.", exception);
        } catch (IllegalArgumentException exception) {
            throw new OrderException(OrderFailureKind.PROTOCOL,
                    "The order response was invalid.", exception);
        }
    }

    private static <T> T body(Response<T> response) throws OrderException {
        if (response.body() == null) {
            throw new OrderException(OrderFailureKind.PROTOCOL,
                    "The order response did not contain a body.");
        }
        return response.body();
    }

    private OrderException failure(Response<?> response) {
        Optional<ProblemDetails> problem = problemParser.parse(response);
        if (problem.isPresent()) {
            ProblemDetails details = problem.orElseThrow();
            return new OrderException(map(details.code()), "The order request was rejected.",
                    details.requestId(), details.detail(), null);
        }
        return new OrderException(status(response.code()),
                "The order service returned an invalid error response.");
    }

    private static OrderFailureKind map(MobileProblemCode code) {
        return switch (code) {
            case AUTH_REQUIRED, AUTH_FAILED -> OrderFailureKind.AUTH_REJECTED;
            case APP_UPDATE_REQUIRED -> OrderFailureKind.UPDATE_REQUIRED;
            case FORBIDDEN -> OrderFailureKind.FORBIDDEN;
            case IDEMPOTENCY_IN_PROGRESS -> OrderFailureKind.IDEMPOTENCY_IN_PROGRESS;
            case IDEMPOTENCY_KEY_REUSED -> OrderFailureKind.IDEMPOTENCY_KEY_REUSED;
            case INVALID_JSON, INVALID_REQUEST -> OrderFailureKind.INVALID_REQUEST;
            case NOT_FOUND -> OrderFailureKind.NOT_FOUND;
            case RATE_LIMITED -> OrderFailureKind.RATE_LIMITED;
            case RESOURCE_CONFLICT -> OrderFailureKind.CONFLICT;
            case SERVICE_UNAVAILABLE -> OrderFailureKind.SERVICE_UNAVAILABLE;
            default -> OrderFailureKind.PROTOCOL;
        };
    }

    private static OrderFailureKind status(int code) {
        if (code == HTTP_BAD_REQUEST) {
            return OrderFailureKind.INVALID_REQUEST;
        }
        if (code == HTTP_UNAUTHORIZED) {
            return OrderFailureKind.AUTH_REJECTED;
        }
        if (code == HTTP_FORBIDDEN) {
            return OrderFailureKind.FORBIDDEN;
        }
        if (code == HTTP_NOT_FOUND) {
            return OrderFailureKind.NOT_FOUND;
        }
        if (code == HTTP_CONFLICT) {
            return OrderFailureKind.CONFLICT;
        }
        if (code == HTTP_UPDATE_REQUIRED) {
            return OrderFailureKind.UPDATE_REQUIRED;
        }
        if (code == HTTP_TOO_MANY_REQUESTS) {
            return OrderFailureKind.RATE_LIMITED;
        }
        if (code >= HTTP_SERVER_ERROR_START) {
            return OrderFailureKind.SERVICE_UNAVAILABLE;
        }
        return OrderFailureKind.PROTOCOL;
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

    private static boolean replayed(Response<?> response) throws OrderException {
        String value = response.headers().get("X-Idempotency-Replayed");
        if (!"true".equals(value) && !"false".equals(value)) {
            throw new OrderException(OrderFailureKind.PROTOCOL,
                    "The order idempotency response is invalid.");
        }
        return Boolean.parseBoolean(value);
    }

    private static OrderActionResult action(
            OrderMutationResponse value,
            Response<?> response) throws OrderException {
        return new OrderActionResult(
                enumValue(OrderAction.class, value.action()),
                value.orderId(),
                value.revision(),
                enumValue(OrderStatus.class, value.status()),
                enumValue(OrderPaymentStatus.class, value.paymentStatus()),
                value.changed(),
                replayed(response));
    }

    private static String optionalText(Optional<String> value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        String text = value.orElseThrow().trim();
        return text.isEmpty() ? null : text;
    }

    private static <T extends Enum<T>> T enumValue(
            Class<T> type,
            String value) {
        return Enum.valueOf(
                type,
                value.toUpperCase(Locale.ROOT));
    }

    private static OrderException protocol(IllegalArgumentException exception) {
        return new OrderException(OrderFailureKind.PROTOCOL,
                "The order response was invalid.", exception);
    }
}
