package br.com.tresvtintas.mobile.data.accountaccess;

import br.com.tresvtintas.mobile.core.accountaccess.AccountAccessException;
import br.com.tresvtintas.mobile.core.accountaccess.AccountAccessFailureKind;
import br.com.tresvtintas.mobile.core.accountaccess.AccountAccessPage;
import br.com.tresvtintas.mobile.core.accountaccess.AccountAccessRepository;
import br.com.tresvtintas.mobile.core.accountaccess.AccountAccessView;
import br.com.tresvtintas.mobile.core.accountaccess.AccountRevocation;
import br.com.tresvtintas.mobile.core.network.api.MobileApi;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

public final class RemoteAccountAccessRepository
        implements AccountAccessRepository {
    private final AccountAccessScope scope;
    private final MobileApi api;
    private final AccountAccessHttpClient http =
            new AccountAccessHttpClient();
    private final AtomicBoolean active = new AtomicBoolean(true);

    public RemoteAccountAccessRepository(
            AccountAccessScope scope,
            MobileApi api) {
        this.scope = Objects.requireNonNull(
                scope,
                "Account access scope is required.");
        this.api = Objects.requireNonNull(
                api,
                "Mobile API is required.");
    }

    public AccountAccessScope scope() {
        return scope;
    }

    @Override
    public AccountAccessPage page(
            AccountAccessView view,
            Optional<String> cursor,
            int limit) throws AccountAccessException {
        requireActive();
        if (view == null || limit < 1 || limit > 100) {
            throw new AccountAccessException(
                    AccountAccessFailureKind.INVALID_REQUEST,
                    "Account access page request is invalid.");
        }
        try {
            if (view == AccountAccessView.DEVICES) {
                return AccountAccessDtoMapper.devices(
                        http.body(api.accountDevices(
                                cursor.orElse(null),
                                limit)));
            }
            return AccountAccessDtoMapper.sessions(
                    http.body(api.accountSessions(
                            cursor.orElse(null),
                            limit)));
        } catch (IllegalArgumentException exception) {
            throw http.protocol(exception);
        }
    }

    @Override
    public AccountRevocation revoke(
            AccountAccessView view,
            String targetId) throws AccountAccessException {
        requireActive();
        if (view == null || targetId == null) {
            throw new AccountAccessException(
                    AccountAccessFailureKind.INVALID_REQUEST,
                    "Account access revocation is invalid.");
        }
        try {
            AccountRevocation result;
            if (view == AccountAccessView.DEVICES) {
                result = AccountAccessDtoMapper.revocation(
                        http.body(api.revokeAccountDevice(
                                targetId)));
            } else {
                result = AccountAccessDtoMapper.revocation(
                        http.body(api.revokeAccountSession(
                                targetId)));
            }
            if (!targetId.equals(result.targetId())) {
                throw new IllegalArgumentException(
                        "Revocation response changed the target.");
            }
            return result;
        } catch (IllegalArgumentException exception) {
            throw http.protocol(exception);
        }
    }

    public void close() {
        active.set(false);
    }

    private void requireActive() throws AccountAccessException {
        if (!active.get()) {
            throw new AccountAccessException(
                    AccountAccessFailureKind.ACCESS_REVOKED,
                    "Account access scope is no longer active.");
        }
    }

}
