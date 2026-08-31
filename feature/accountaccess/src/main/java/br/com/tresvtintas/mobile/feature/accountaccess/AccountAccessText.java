package br.com.tresvtintas.mobile.feature.accountaccess;

import android.content.Context;
import br.com.tresvtintas.mobile.core.accountaccess.AccountAccessEntry;
import br.com.tresvtintas.mobile.core.accountaccess.AccountAccessFailureKind;
import br.com.tresvtintas.mobile.core.accountaccess.AccountAccessStatus;
import br.com.tresvtintas.mobile.core.accountaccess.AccountDevice;
import br.com.tresvtintas.mobile.core.accountaccess.AccountSession;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class AccountAccessText {
    private static final int SHORT_ID_LENGTH = 8;
    private static final Locale BRAZIL =
            Locale.forLanguageTag("pt-BR");
    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern(
                            "dd/MM/yyyy HH:mm",
                            BRAZIL)
                    .withZone(ZoneId.systemDefault());

    private AccountAccessText() {
    }

    static int status(AccountAccessStatus value) {
        return switch (value) {
            case ACTIVE -> R.string.account_access_status_active;
            case REVOKED -> R.string.account_access_status_revoked;
            case EXPIRED -> R.string.account_access_status_expired;
        };
    }

    static int failure(AccountAccessFailureKind value) {
        return switch (value) {
            case NETWORK -> R.string.account_access_error_network;
            case AUTH_REJECTED, ACCESS_REVOKED ->
                    R.string.account_access_error_session;
            case FORBIDDEN -> R.string.account_access_error_forbidden;
            case NOT_FOUND, CONFLICT ->
                    R.string.account_access_error_not_found;
            case RATE_LIMITED -> R.string.account_access_error_rate;
            case UPDATE_REQUIRED -> R.string.account_access_error_update;
            case SERVICE_UNAVAILABLE ->
                    R.string.account_access_error_service;
            default -> R.string.account_access_error_generic;
        };
    }

    static boolean retryable(AccountAccessFailureKind value) {
        return value == AccountAccessFailureKind.NETWORK
                || value == AccountAccessFailureKind.RATE_LIMITED
                || value
                        == AccountAccessFailureKind.SERVICE_UNAVAILABLE;
    }

    static String title(Context context, AccountAccessEntry entry) {
        if (entry instanceof AccountDevice device) {
            return device.displayName();
        }
        return context.getString(
                R.string.account_access_session_title,
                shortId(entry.id()));
    }

    static String details(Context context, AccountAccessEntry entry) {
        if (entry instanceof AccountDevice device) {
            return context.getString(
                    R.string.account_access_device_details,
                    deviceName(context, device),
                    device.androidApi(),
                    device.appVersion());
        }
        AccountSession session = (AccountSession) entry;
        return context.getString(
                R.string.account_access_session_device,
                shortId(session.deviceId()));
    }

    static String lastSeen(Context context, AccountAccessEntry entry) {
        return context.getString(
                R.string.account_access_last_seen,
                date(entry.lastSeenAt()));
    }

    static String timing(Context context, AccountAccessEntry entry) {
        if (entry instanceof AccountDevice device) {
            return context.getString(
                    R.string.account_access_registered,
                    date(device.registeredAt()));
        }
        AccountSession session = (AccountSession) entry;
        Instant expiry = session.idleExpiresAt().isBefore(
                session.absoluteExpiresAt())
                ? session.idleExpiresAt()
                : session.absoluteExpiresAt();
        return context.getString(
                R.string.account_access_session_expiry,
                date(expiry));
    }

    static int revokeLabel(AccountAccessEntry entry) {
        return entry instanceof AccountDevice
                ? R.string.account_access_revoke_device
                : R.string.account_access_revoke_session;
    }

    static String shortId(String value) {
        if (value == null || value.length() < SHORT_ID_LENGTH) {
            throw new IllegalArgumentException(
                    "Account access identifier is invalid.");
        }
        return value.substring(0, SHORT_ID_LENGTH);
    }

    static String date(Instant value) {
        return DATE_TIME.format(value);
    }

    private static String deviceName(
            Context context,
            AccountDevice device) {
        List<String> parts = new ArrayList<>();
        device.manufacturer().ifPresent(parts::add);
        device.model()
                .filter(value -> !parts.contains(value))
                .ifPresent(parts::add);
        return parts.isEmpty()
                ? context.getString(
                        R.string.account_access_device_unknown)
                : String.join(" ", parts);
    }
}
