package br.com.tresvtintas.mobile.data.accountaccess;

import br.com.tresvtintas.mobile.core.accountaccess.AccountAccessException;
import br.com.tresvtintas.mobile.core.accountaccess.AccountAccessFailureKind;
import br.com.tresvtintas.mobile.core.accountaccess.AccountAccessPage;
import br.com.tresvtintas.mobile.core.accountaccess.AccountAccessView;
import br.com.tresvtintas.mobile.core.accountaccess.ManagedAccountAccessReader;
import br.com.tresvtintas.mobile.core.network.api.MobileApi;
import br.com.tresvtintas.mobile.core.network.dto.ManagedAccountDevicePageDto;
import br.com.tresvtintas.mobile.core.network.dto.ManagedAccountSessionPageDto;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

public final class RemoteManagedAccountAccessReader
        extends ManagedAccountAccessReader {
    private final ManagedAccountAccessScope scope;
    private final MobileApi api;
    private final AccountAccessHttpClient http =
            new AccountAccessHttpClient();
    private final AtomicBoolean active = new AtomicBoolean(true);

    public RemoteManagedAccountAccessReader(
            ManagedAccountAccessScope scope,
            MobileApi api) {
        this.scope = Objects.requireNonNull(
                scope,
                "Managed account access scope is required.");
        this.api = Objects.requireNonNull(
                api,
                "Managed account access API is required.");
    }

    public ManagedAccountAccessScope scope() {
        return scope;
    }

    @Override
    public AccountAccessPage page(
            AccountAccessView view,
            Optional<String> cursor,
            int limit) throws AccountAccessException {
        requireActive();
        if (view == null || cursor == null || limit < 1 || limit > 100) {
            throw new AccountAccessException(
                    AccountAccessFailureKind.INVALID_REQUEST,
                    "Managed account access page request is invalid.");
        }
        try {
            if (view == AccountAccessView.DEVICES) {
                ManagedAccountDevicePageDto page = http.body(
                        api.managedAccountDevices(
                                scope.targetUserId(),
                                cursor.orElse(null),
                                limit));
                requireTarget(page.target().id());
                return AccountAccessDtoMapper.devices(
                        page.items(),
                        page.nextCursor());
            }
            ManagedAccountSessionPageDto page = http.body(
                    api.managedAccountSessions(
                            scope.targetUserId(),
                            cursor.orElse(null),
                            limit));
            requireTarget(page.target().id());
            return AccountAccessDtoMapper.sessions(
                    page.items(),
                    page.nextCursor());
        } catch (IllegalArgumentException exception) {
            throw http.protocol(exception);
        }
    }

    @Override
    public void close() {
        active.set(false);
    }

    private void requireActive() throws AccountAccessException {
        if (!active.get()) {
            throw new AccountAccessException(
                    AccountAccessFailureKind.ACCESS_REVOKED,
                    "Managed account access scope is no longer active.");
        }
    }

    private void requireTarget(long returnedTarget) {
        if (returnedTarget != scope.targetUserId()) {
            throw new IllegalArgumentException(
                    "Managed account access response changed the target.");
        }
    }
}
