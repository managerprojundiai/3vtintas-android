package br.com.tresvtintas.mobile.feature.quote;

import androidx.annotation.StringRes;
import br.com.tresvtintas.mobile.core.order.OrderException;
import br.com.tresvtintas.mobile.core.order.OrderFailureKind;
import java.util.Optional;

record MaterialQuoteOrderFailurePresentation(
        Optional<String> serverDetail,
        Optional<String> requestId,
        @StringRes int fallbackMessage,
        boolean resetIdempotency) {

    MaterialQuoteOrderFailurePresentation {
        serverDetail = serverDetail == null
                ? Optional.empty()
                : serverDetail.map(String::trim).filter(value -> !value.isEmpty());
        requestId = requestId == null ? Optional.empty() : requestId;
    }

    static MaterialQuoteOrderFailurePresentation from(
            OrderException exception) {
        OrderFailureKind kind = exception.kind();
        return new MaterialQuoteOrderFailurePresentation(
                exception.userDetail(),
                exception.requestId(),
                fallback(kind),
                resetsAfter(kind));
    }

    private static int fallback(OrderFailureKind kind) {
        return switch (kind) {
            case NETWORK -> R.string.quote_order_failure_network;
            case SERVICE_UNAVAILABLE, RATE_LIMITED,
                    IDEMPOTENCY_IN_PROGRESS ->
                R.string.quote_order_failure_temporary;
            case UPDATE_REQUIRED -> R.string.quote_order_failure_update;
            case AUTH_REJECTED, ACCESS_REVOKED ->
                R.string.quote_order_failure_session;
            case FORBIDDEN -> R.string.quote_order_failure_forbidden;
            case CONFLICT, IDEMPOTENCY_KEY_REUSED ->
                R.string.quote_order_failure_conflict;
            case INVALID_REQUEST, NOT_FOUND, PROTOCOL ->
                R.string.quote_failure;
        };
    }

    private static boolean resetsAfter(OrderFailureKind kind) {
        return switch (kind) {
            case NETWORK, SERVICE_UNAVAILABLE, RATE_LIMITED,
                    IDEMPOTENCY_IN_PROGRESS, PROTOCOL -> false;
            case ACCESS_REVOKED, AUTH_REJECTED, CONFLICT, FORBIDDEN,
                    IDEMPOTENCY_KEY_REUSED, INVALID_REQUEST, NOT_FOUND,
                    UPDATE_REQUIRED -> true;
        };
    }
}
