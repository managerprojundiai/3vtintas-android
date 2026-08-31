package br.com.tresvtintas.mobile.feature.team;

import android.content.Context;
import br.com.tresvtintas.mobile.core.team.TeamFailureKind;
import br.com.tresvtintas.mobile.core.team.TeamMember;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

final class TeamText {
    private static final Locale BRAZIL = Locale.forLanguageTag("pt-BR");
    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", BRAZIL)
                    .withZone(ZoneId.systemDefault());

    private TeamText() {
    }

    static String money(BigDecimal value) {
        return NumberFormat.getCurrencyInstance(BRAZIL).format(value);
    }

    static String percent(int basisPoints) {
        return NumberFormat.getPercentInstance(BRAZIL)
                .format(BigDecimal.valueOf(basisPoints, 4));
    }

    static String rate(BigDecimal value) {
        return NumberFormat.getPercentInstance(BRAZIL)
                .format(value.movePointLeft(2));
    }

    static String date(Instant value) {
        return DATE_TIME.format(value);
    }

    static String status(Context context, TeamMember.Status status) {
        return switch (status) {
            case ACTIVE -> context.getString(R.string.team_status_active);
            case PENDING -> context.getString(R.string.team_status_pending);
            case BLOCKED -> context.getString(R.string.team_status_blocked);
        };
    }

    static int failure(TeamFailureKind kind) {
        return switch (kind) {
            case AUTH_REJECTED, ACCESS_REVOKED ->
                R.string.team_error_session;
            case FORBIDDEN -> R.string.team_error_forbidden;
            case UPDATE_REQUIRED -> R.string.team_error_update;
            case NETWORK -> R.string.team_error_network;
            case RATE_LIMITED -> R.string.team_error_rate_limited;
            case SERVICE_UNAVAILABLE ->
                R.string.team_error_service_unavailable;
            case INVALID_REQUEST, PROTOCOL ->
                R.string.team_error_protocol;
        };
    }
}
