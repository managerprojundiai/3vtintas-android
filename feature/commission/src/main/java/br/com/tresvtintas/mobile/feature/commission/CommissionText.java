package br.com.tresvtintas.mobile.feature.commission;

import android.content.Context;
import br.com.tresvtintas.mobile.core.commission.CommissionFailureKind;
import br.com.tresvtintas.mobile.core.commission.CommissionKind;
import br.com.tresvtintas.mobile.core.commission.CommissionPaymentMethod;
import br.com.tresvtintas.mobile.core.commission.CommissionRecipientRole;
import br.com.tresvtintas.mobile.core.commission.CommissionStatus;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

final class CommissionText {
    private static final Locale BRAZIL = Locale.forLanguageTag("pt-BR");
    private static final DateTimeFormatter DATE =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", BRAZIL)
                    .withZone(ZoneId.systemDefault());

    private CommissionText() {
    }

    static String money(BigDecimal value) {
        return NumberFormat.getCurrencyInstance(BRAZIL).format(value);
    }

    static String percentage(BigDecimal value) {
        return NumberFormat.getNumberInstance(BRAZIL).format(value) + "%";
    }

    static String date(Instant value) {
        return DATE.format(value);
    }

    static int status(CommissionStatus value) {
        return switch (value) {
            case PENDING -> R.string.commission_status_pending;
            case APPROVED -> R.string.commission_status_approved;
            case PAID -> R.string.commission_status_paid;
            case CANCELLED -> R.string.commission_status_cancelled;
        };
    }

    static int kind(CommissionKind value) {
        return value == CommissionKind.SELLER
                ? R.string.commission_kind_seller
                : R.string.commission_kind_global_admin;
    }

    static int role(CommissionRecipientRole value) {
        return switch (value) {
            case PAINTER -> R.string.commission_role_painter;
            case SALESPERSON -> R.string.commission_role_salesperson;
            case MASTER_ADMIN -> R.string.commission_role_master;
        };
    }

    static int paymentMethod(CommissionPaymentMethod value) {
        return switch (value) {
            case PIX -> R.string.commission_payment_pix;
            case TRANSFER -> R.string.commission_payment_transfer;
            case CASH -> R.string.commission_payment_cash;
            case BANK_SLIP -> R.string.commission_payment_bank_slip;
            case OTHER -> R.string.commission_payment_other;
        };
    }

    static int failure(CommissionFailureKind value) {
        return switch (value) {
            case AUTH_REJECTED, ACCESS_REVOKED ->
                R.string.commission_error_session;
            case FORBIDDEN -> R.string.commission_error_forbidden;
            case CONFLICT -> R.string.commission_action_conflict;
            case IDEMPOTENCY_IN_PROGRESS ->
                    R.string.commission_action_in_progress;
            case IDEMPOTENCY_KEY_REUSED ->
                    R.string.commission_action_key_reused;
            case NOT_FOUND -> R.string.commission_error_not_found;
            case NETWORK -> R.string.commission_error_network;
            case RATE_LIMITED -> R.string.commission_error_rate;
            case UPDATE_REQUIRED -> R.string.commission_error_update;
            case SERVICE_UNAVAILABLE -> R.string.commission_error_service;
            default -> R.string.commission_error_generic;
        };
    }

    static String reference(Context context, long id) {
        return context.getString(R.string.commission_reference, id);
    }
}
