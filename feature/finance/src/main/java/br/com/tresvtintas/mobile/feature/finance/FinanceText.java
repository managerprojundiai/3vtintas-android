package br.com.tresvtintas.mobile.feature.finance;

import android.content.Context;
import androidx.annotation.StringRes;
import br.com.tresvtintas.mobile.core.finance.FinanceEntrySource;
import br.com.tresvtintas.mobile.core.finance.FinanceEntryStatus;
import br.com.tresvtintas.mobile.core.finance.FinanceEntryType;
import br.com.tresvtintas.mobile.core.finance.FinanceFailureKind;
import br.com.tresvtintas.mobile.core.finance.FinancePaymentMethod;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

final class FinanceText {
    private static final Locale BRAZIL = Locale.forLanguageTag("pt-BR");
    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", BRAZIL)
                    .withZone(ZoneId.systemDefault());

    private FinanceText() {
    }

    static String money(BigDecimal value) {
        return NumberFormat.getCurrencyInstance(BRAZIL).format(value);
    }

    static String date(Instant value) {
        return DATE_TIME.format(value);
    }

    @StringRes
    static int type(FinanceEntryType value) {
        return switch (value) {
            case EXPENSE -> R.string.finance_type_expense;
            case PAYABLE -> R.string.finance_type_payable;
            case RECEIVABLE -> R.string.finance_type_receivable;
        };
    }

    @StringRes
    static int status(FinanceEntryStatus value) {
        return switch (value) {
            case PENDING -> R.string.finance_status_pending;
            case SETTLED -> R.string.finance_status_settled;
            case CANCELLED -> R.string.finance_status_cancelled;
        };
    }

    @StringRes
    static int source(FinanceEntrySource value) {
        return switch (value) {
            case MANUAL -> R.string.finance_source_manual;
            case MATERIAL_ORDER -> R.string.finance_source_material_order;
            case COMMISSION -> R.string.finance_source_commission;
            case SYSTEM -> R.string.finance_source_system;
        };
    }

    @StringRes
    static int payment(FinancePaymentMethod value) {
        return switch (value) {
            case PIX -> R.string.finance_payment_pix;
            case TRANSFER -> R.string.finance_payment_transfer;
            case CASH -> R.string.finance_payment_cash;
            case BANK_SLIP -> R.string.finance_payment_bank_slip;
            case OTHER -> R.string.finance_payment_other;
        };
    }

    static String failure(Context context, FinanceFailureKind value) {
        return context.getString(switch (value) {
            case ACCESS_REVOKED, AUTH_REJECTED ->
                R.string.finance_error_session;
            case FORBIDDEN -> R.string.finance_error_forbidden;
            case NOT_FOUND -> R.string.finance_error_not_found;
            case CONFLICT -> R.string.finance_error_conflict;
            case IDEMPOTENCY_IN_PROGRESS, IDEMPOTENCY_KEY_REUSED ->
                R.string.finance_error_idempotency;
            case INVALID_REQUEST -> R.string.finance_error_invalid;
            case NETWORK -> R.string.finance_error_network;
            case RATE_LIMITED -> R.string.finance_error_rate;
            case UPDATE_REQUIRED -> R.string.finance_error_update;
            case SERVICE_UNAVAILABLE -> R.string.finance_error_service;
            case PROTOCOL -> R.string.finance_error_generic;
        });
    }
}
