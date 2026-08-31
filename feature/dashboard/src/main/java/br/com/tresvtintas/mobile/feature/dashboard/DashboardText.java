package br.com.tresvtintas.mobile.feature.dashboard;

import br.com.tresvtintas.mobile.core.dashboard.DashboardFailureKind;
import br.com.tresvtintas.mobile.core.dashboard.DashboardSnapshot;
import br.com.tresvtintas.mobile.core.dashboard.DashboardVisibility;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

final class DashboardText {
    private static final Locale BRAZIL = Locale.forLanguageTag("pt-BR");
    private static final DateTimeFormatter DATE =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", BRAZIL)
                    .withZone(ZoneId.systemDefault());

    private DashboardText() {
    }

    static String money(BigDecimal value) {
        return NumberFormat.getCurrencyInstance(BRAZIL).format(value);
    }

    static String date(Instant value) {
        return DATE.format(value);
    }

    static int visibility(DashboardVisibility value) {
        return switch (value) {
            case SELF -> R.string.dashboard_visibility_self;
            case TEAM -> R.string.dashboard_visibility_team;
            case ALL -> R.string.dashboard_visibility_all;
        };
    }

    static int commissionScope(
            DashboardSnapshot.CommissionScope value) {
        return value == DashboardSnapshot.CommissionScope.SELF
                ? R.string.dashboard_scope_self
                : R.string.dashboard_scope_team;
    }

    static int appointmentScope(
            DashboardSnapshot.AppointmentScope value) {
        return switch (value) {
            case SELF -> R.string.dashboard_scope_self;
            case TEAM -> R.string.dashboard_scope_team;
            case ALL -> R.string.dashboard_scope_all;
        };
    }

    static int financeScope(DashboardSnapshot.FinanceScope value) {
        return value == DashboardSnapshot.FinanceScope.PERSONAL
                ? R.string.dashboard_scope_personal
                : R.string.dashboard_scope_corporate;
    }

    static int failure(DashboardFailureKind value) {
        return switch (value) {
            case AUTH_REJECTED, ACCESS_REVOKED ->
                R.string.dashboard_error_session;
            case FORBIDDEN -> R.string.dashboard_error_forbidden;
            case NETWORK -> R.string.dashboard_error_network;
            case RATE_LIMITED -> R.string.dashboard_error_rate;
            case UPDATE_REQUIRED -> R.string.dashboard_error_update;
            case SERVICE_UNAVAILABLE -> R.string.dashboard_error_service;
            default -> R.string.dashboard_error_generic;
        };
    }
}
