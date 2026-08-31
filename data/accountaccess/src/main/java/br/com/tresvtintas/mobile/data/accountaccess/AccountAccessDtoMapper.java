package br.com.tresvtintas.mobile.data.accountaccess;

import br.com.tresvtintas.mobile.core.accountaccess.AccountAccessEntry;
import br.com.tresvtintas.mobile.core.accountaccess.AccountAccessPage;
import br.com.tresvtintas.mobile.core.accountaccess.AccountAccessStatus;
import br.com.tresvtintas.mobile.core.accountaccess.AccountAccessView;
import br.com.tresvtintas.mobile.core.accountaccess.AccountAuthMethod;
import br.com.tresvtintas.mobile.core.accountaccess.AccountDevice;
import br.com.tresvtintas.mobile.core.accountaccess.AccountRevocation;
import br.com.tresvtintas.mobile.core.accountaccess.AccountSession;
import br.com.tresvtintas.mobile.core.network.dto.AccountDeviceDto;
import br.com.tresvtintas.mobile.core.network.dto.AccountDevicePageDto;
import br.com.tresvtintas.mobile.core.network.dto.AccountDeviceRevocationDto;
import br.com.tresvtintas.mobile.core.network.dto.AccountSessionDto;
import br.com.tresvtintas.mobile.core.network.dto.AccountSessionPageDto;
import br.com.tresvtintas.mobile.core.network.dto.AccountSessionRevocationDto;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

final class AccountAccessDtoMapper {
    private AccountAccessDtoMapper() {
        throw new AssertionError("No instances.");
    }

    static AccountAccessPage devices(AccountDevicePageDto value) {
        return devices(value.items(), value.nextCursor());
    }

    static AccountAccessPage devices(
            List<AccountDeviceDto> values,
            String nextCursor) {
        List<AccountAccessEntry> items = values.stream()
                .map(AccountAccessDtoMapper::device)
                .map(AccountAccessEntry.class::cast)
                .toList();
        return new AccountAccessPage(
                items,
                Optional.ofNullable(nextCursor));
    }

    static AccountAccessPage sessions(AccountSessionPageDto value) {
        return sessions(value.items(), value.nextCursor());
    }

    static AccountAccessPage sessions(
            List<AccountSessionDto> values,
            String nextCursor) {
        List<AccountAccessEntry> items = values.stream()
                .map(AccountAccessDtoMapper::session)
                .map(AccountAccessEntry.class::cast)
                .toList();
        return new AccountAccessPage(
                items,
                Optional.ofNullable(nextCursor));
    }

    static AccountRevocation revocation(
            AccountDeviceRevocationDto value) {
        return new AccountRevocation(
                AccountAccessView.DEVICES,
                value.deviceId(),
                value.changed(),
                value.current());
    }

    static AccountRevocation revocation(
            AccountSessionRevocationDto value) {
        return new AccountRevocation(
                AccountAccessView.SESSIONS,
                value.sessionId(),
                value.changed(),
                value.current());
    }

    private static AccountDevice device(AccountDeviceDto value) {
        return new AccountDevice(
                value.id(),
                value.displayName(),
                Optional.ofNullable(value.manufacturer()),
                Optional.ofNullable(value.model()),
                value.androidApi(),
                value.appVersion(),
                status(value.status()),
                Instant.parse(value.registeredAt()),
                Instant.parse(value.lastSeenAt()),
                instant(value.revokedAt()),
                value.current(),
                value.revision());
    }

    private static AccountSession session(AccountSessionDto value) {
        return new AccountSession(
                value.id(),
                value.deviceId(),
                AccountAuthMethod.valueOf(
                        value.authMethod().toUpperCase(Locale.ROOT)),
                status(value.status()),
                Instant.parse(value.issuedAt()),
                Instant.parse(value.lastSeenAt()),
                Instant.parse(value.idleExpiresAt()),
                Instant.parse(value.absoluteExpiresAt()),
                instant(value.endedAt()),
                value.current(),
                value.revision());
    }

    private static AccountAccessStatus status(String value) {
        return AccountAccessStatus.valueOf(
                value.toUpperCase(Locale.ROOT));
    }

    private static Optional<Instant> instant(String value) {
        return Optional.ofNullable(value).map(Instant::parse);
    }
}
