package br.com.tresvtintas.mobile.data.accountaccess;

import br.com.tresvtintas.mobile.core.accountaccess.AccountAccessEntry;
import br.com.tresvtintas.mobile.core.accountaccess.AccountAccessException;
import br.com.tresvtintas.mobile.core.accountaccess.AccountAccessFailureKind;
import br.com.tresvtintas.mobile.core.accountaccess.AccountAccessView;
import br.com.tresvtintas.mobile.core.accountaccess.AccountDevice;
import br.com.tresvtintas.mobile.core.accountaccess.ManagedAccountRevocationChallenge;
import br.com.tresvtintas.mobile.core.accountaccess.ManagedAccountRevocationGrant;
import br.com.tresvtintas.mobile.core.accountaccess.ManagedAccountRevocationPreview;
import br.com.tresvtintas.mobile.core.accountaccess.ManagedAccountRevocationRepository;
import br.com.tresvtintas.mobile.core.accountaccess.ManagedAccountRevocationResult;
import br.com.tresvtintas.mobile.core.network.api.MobileApi;
import br.com.tresvtintas.mobile.core.network.dto.ManagedAccountRevocationDtos;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public final class RemoteManagedAccountRevocationRepository
        implements ManagedAccountRevocationRepository {
    private static final String DEVICE = "device";
    private final ManagedAccountAccessScope scope;
    private final MobileApi api;
    private final AccountAccessHttpClient http =
            new AccountAccessHttpClient();
    private final AtomicBoolean active = new AtomicBoolean(true);

    public RemoteManagedAccountRevocationRepository(
            ManagedAccountAccessScope scope,
            MobileApi api) {
        this.scope = Objects.requireNonNull(
                scope,
                "Managed revocation scope is required.");
        this.api = Objects.requireNonNull(
                api,
                "Managed revocation API is required.");
    }

    @Override
    public ManagedAccountRevocationPreview prepare(
            AccountAccessEntry entry) throws AccountAccessException {
        requireActive();
        Objects.requireNonNull(
                entry,
                "Managed revocation entry is required.");
        try {
            ManagedAccountRevocationDtos.PrepareRequest request =
                    ManagedAccountRevocationDtos.prepare(
                            entry.revision());
            ManagedAccountRevocationDtos.PreparedAction value =
                    entry instanceof AccountDevice
                            ? http.body(api.prepareManagedDeviceRevocation(
                                    scope.targetUserId(),
                                    entry.id(),
                                    request))
                            : http.body(api.prepareManagedSessionRevocation(
                                    scope.targetUserId(),
                                    entry.id(),
                                    request));
            AccountAccessView expected = view(entry);
            requirePrepared(value, entry, expected);
            return new ManagedAccountRevocationPreview(
                    value.actionId(),
                    expected,
                    value.target().id(),
                    value.target().name(),
                    value.resource().id(),
                    value.resource().label(),
                    value.resource().revision(),
                    value.consequence(),
                    Instant.parse(value.expiresAt()));
        } catch (IllegalArgumentException exception) {
            throw http.protocol(exception);
        }
    }

    @Override
    public ManagedAccountRevocationChallenge challenge(
            String actionId) throws AccountAccessException {
        requireActive();
        try {
            ManagedAccountRevocationDtos.ChallengeResponse value =
                    http.body(api.requestManagedRevocationStepUp(
                            actionId,
                            ManagedAccountRevocationDtos.ChallengeRequest
                                    .confirmed()));
            return new ManagedAccountRevocationChallenge(
                    value.challengeId(),
                    value.nonce(),
                    value.googleServerClientId(),
                    Instant.parse(value.expiresAt()));
        } catch (IllegalArgumentException exception) {
            throw http.protocol(exception);
        }
    }

    @Override
    public ManagedAccountRevocationGrant verify(
            String actionId,
            String challengeId,
            String credential) throws AccountAccessException {
        requireActive();
        try {
            ManagedAccountRevocationDtos.GrantResponse value =
                    http.body(api.verifyManagedRevocationStepUp(
                            actionId,
                            new ManagedAccountRevocationDtos.VerifyRequest(
                                    challengeId,
                                    credential)));
            return new ManagedAccountRevocationGrant(
                    value.stepUpToken(),
                    Instant.parse(value.expiresAt()));
        } catch (IllegalArgumentException exception) {
            throw http.protocol(exception);
        }
    }

    @Override
    public ManagedAccountRevocationResult execute(
            String actionId,
            String stepUpToken,
            String idempotencyKey) throws AccountAccessException {
        requireActive();
        try {
            ManagedAccountRevocationDtos.Result value =
                    http.body(api.executeManagedRevocation(
                            actionId,
                            idempotencyKey,
                            ManagedAccountRevocationDtos.ExecuteRequest
                                    .confirmed(stepUpToken)));
            if (value.targetUserId() != scope.targetUserId()) {
                throw new IllegalArgumentException(
                        "Managed revocation result changed the target.");
            }
            return new ManagedAccountRevocationResult(
                    value.actionId(),
                    DEVICE.equals(value.kind())
                            ? AccountAccessView.DEVICES
                            : AccountAccessView.SESSIONS,
                    value.targetUserId(),
                    value.resourceId(),
                    value.changed(),
                    value.revision());
        } catch (IllegalArgumentException exception) {
            throw http.protocol(exception);
        }
    }

    @Override
    public void close() {
        active.set(false);
    }

    private void requirePrepared(
            ManagedAccountRevocationDtos.PreparedAction value,
            AccountAccessEntry entry,
            AccountAccessView expected) {
        if (value.target().id() != scope.targetUserId()
                || !value.resource().id().equals(entry.id())
                || value.resource().revision() != entry.revision()
                || (DEVICE.equals(value.kind())
                        ? AccountAccessView.DEVICES
                        : AccountAccessView.SESSIONS) != expected) {
            throw new IllegalArgumentException(
                    "Managed revocation preview changed its scope.");
        }
    }

    private void requireActive() throws AccountAccessException {
        if (!active.get()) {
            throw new AccountAccessException(
                    AccountAccessFailureKind.ACCESS_REVOKED,
                    "Managed revocation scope is no longer active.");
        }
    }

    private static AccountAccessView view(AccountAccessEntry entry) {
        return entry instanceof AccountDevice
                ? AccountAccessView.DEVICES
                : AccountAccessView.SESSIONS;
    }

}
