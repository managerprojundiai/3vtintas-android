package br.com.tresvtintas.mobile.feature.order;

import android.content.Context;
import br.com.tresvtintas.mobile.core.order.OrderAction;
import br.com.tresvtintas.mobile.core.order.OrderFailureKind;
import br.com.tresvtintas.mobile.core.order.OrderPaymentMethod;
import br.com.tresvtintas.mobile.core.order.OrderStatus;
import br.com.tresvtintas.mobile.core.order.OrderType;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

final class OrderText {
    private static final Locale BRAZIL = Locale.forLanguageTag("pt-BR");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", BRAZIL)
            .withZone(ZoneId.systemDefault());

    private OrderText() { }

    static String money(BigDecimal value) {
        return NumberFormat.getCurrencyInstance(BRAZIL).format(value);
    }

    static String date(Instant value) {
        return DATE.format(value);
    }

    static int status(OrderStatus value) {
        return switch (value) {
            case PENDING -> R.string.order_status_pending;
            case CONFIRMED -> R.string.order_status_confirmed;
            case IN_PROGRESS -> R.string.order_status_in_progress;
            case DELIVERED -> R.string.order_status_delivered;
            case CANCELLED -> R.string.order_status_cancelled;
        };
    }

    static int type(OrderType value) {
        return value == OrderType.MATERIAL
                ? R.string.order_type_material
                : R.string.order_type_labor;
    }

    static int action(OrderAction value) {
        return switch (value) {
            case CONFIRM -> R.string.order_action_confirm;
            case START_FULFILLMENT -> R.string.order_action_start;
            case COMPLETE -> R.string.order_action_complete;
            case CANCEL -> R.string.order_action_cancel;
            case RECORD_PAYMENT -> R.string.order_action_payment;
        };
    }

    static int actionWarning(OrderAction value) {
        return switch (value) {
            case CONFIRM -> R.string.order_action_confirm_warning;
            case START_FULFILLMENT -> R.string.order_action_start_warning;
            case COMPLETE -> R.string.order_action_complete_warning;
            case CANCEL -> R.string.order_action_cancel_warning;
            case RECORD_PAYMENT -> R.string.order_action_payment_warning;
        };
    }

    static int paymentMethod(OrderPaymentMethod value) {
        return switch (value) {
            case PIX -> R.string.order_payment_method_pix;
            case TRANSFER -> R.string.order_payment_method_transfer;
            case CASH -> R.string.order_payment_method_cash;
            case BANK_SLIP -> R.string.order_payment_method_bank_slip;
            case OTHER -> R.string.order_payment_method_other;
        };
    }

    static int failure(OrderFailureKind value) {
        return switch (value) {
            case AUTH_REJECTED, ACCESS_REVOKED -> R.string.order_error_session;
            case FORBIDDEN -> R.string.order_error_forbidden;
            case NOT_FOUND -> R.string.order_error_not_found;
            case NETWORK -> R.string.order_error_network;
            case RATE_LIMITED -> R.string.order_error_rate;
            case UPDATE_REQUIRED -> R.string.order_error_update;
            case SERVICE_UNAVAILABLE -> R.string.order_error_service;
            default -> R.string.order_error_generic;
        };
    }

    static String reference(Context context, long id) {
        return context.getString(R.string.order_reference, id);
    }
}
