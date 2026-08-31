package br.com.tresvtintas.mobile.feature.accountaccess;

import androidx.appcompat.app.AppCompatActivity;
import br.com.tresvtintas.mobile.core.accountaccess.AccountAccessEntry;
import br.com.tresvtintas.mobile.core.accountaccess.AccountAccessException;
import br.com.tresvtintas.mobile.core.accountaccess.AccountAccessFailureKind;
import br.com.tresvtintas.mobile.core.accountaccess.ManagedAccountRevocationChallenge;
import br.com.tresvtintas.mobile.core.accountaccess.ManagedAccountRevocationGrant;
import br.com.tresvtintas.mobile.core.accountaccess.ManagedAccountRevocationPreview;
import br.com.tresvtintas.mobile.core.accountaccess.ManagedAccountRevocationRepository;
import br.com.tresvtintas.mobile.core.accountaccess.ManagedAccountRevocationResult;
import br.com.tresvtintas.mobile.core.auth.AuthCallback;
import br.com.tresvtintas.mobile.core.auth.AuthException;
import br.com.tresvtintas.mobile.core.auth.AuthFailureKind;
import br.com.tresvtintas.mobile.core.auth.GoogleIdTokenRequester;
import br.com.tresvtintas.mobile.feature.accountaccess.databinding.ManagedRevocationReviewDialogBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.time.Clock;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

final class ManagedAccountRevocationCoordinator implements AutoCloseable {
    interface Listener {
        void onBusy(Optional<String> resourceId);

        void onSuccess(ManagedAccountRevocationResult result);

        void onFailure(
                AccountAccessException failure,
                boolean executionRetryAvailable);

        void onCanceled();
    }

    private final AppCompatActivity activity;
    private final ManagedAccountRevocationRepository repository;
    private final GoogleIdTokenRequester credentialRequester;
    private final Executor worker;
    private final Executor main;
    private final Listener listener;
    private final Clock clock;
    private final AtomicBoolean busy = new AtomicBoolean();
    private final AtomicLong generation = new AtomicLong();
    private volatile Optional<ExecutionAttempt> retryAttempt =
            Optional.empty();

    ManagedAccountRevocationCoordinator(
            AppCompatActivity activity,
            ManagedAccountRevocationRepository repository,
            GoogleIdTokenRequester credentialRequester,
            Executor worker,
            Executor main,
            Listener listener) {
        this(
                activity,
                repository,
                credentialRequester,
                worker,
                main,
                listener,
                Clock.systemUTC());
    }

    ManagedAccountRevocationCoordinator(
            AppCompatActivity activity,
            ManagedAccountRevocationRepository repository,
            GoogleIdTokenRequester credentialRequester,
            Executor worker,
            Executor main,
            Listener listener,
            Clock clock) {
        this.activity = Objects.requireNonNull(
                activity,
                "Managed revocation activity is required.");
        this.repository = Objects.requireNonNull(
                repository,
                "Managed revocation repository is required.");
        this.credentialRequester = Objects.requireNonNull(
                credentialRequester,
                "Managed revocation credential requester is required.");
        this.worker = Objects.requireNonNull(
                worker,
                "Managed revocation worker is required.");
        this.main = Objects.requireNonNull(
                main,
                "Managed revocation main executor is required.");
        this.listener = Objects.requireNonNull(
                listener,
                "Managed revocation listener is required.");
        this.clock = Objects.requireNonNull(
                clock,
                "Managed revocation clock is required.");
    }

    boolean review(AccountAccessEntry entry) {
        Objects.requireNonNull(
                entry,
                "Managed revocation entry is required.");
        if (!busy.compareAndSet(false, true)) {
            return false;
        }
        retryAttempt = Optional.empty();
        long operation = generation.incrementAndGet();
        listener.onBusy(Optional.of(entry.id()));
        worker.execute(() -> prepare(operation, entry));
        return true;
    }

    boolean retryExecution() {
        Optional<ExecutionAttempt> available = retryAttempt;
        if (available.isEmpty()) {
            return false;
        }
        ExecutionAttempt attempt = available.orElseThrow();
        if (attempt.grant().isExpired(clock.instant())
                || !busy.compareAndSet(false, true)) {
            retryAttempt = Optional.empty();
            return false;
        }
        long operation = generation.incrementAndGet();
        listener.onBusy(Optional.of(
                attempt.preview().resourceId()));
        worker.execute(() -> execute(operation, attempt));
        return true;
    }

    private void prepare(
            long operation,
            AccountAccessEntry entry) {
        try {
            ManagedAccountRevocationPreview preview =
                    repository.prepare(entry);
            if (preview.isExpired(clock.instant())) {
                throw conflict("Managed revocation preview has expired.");
            }
            main.execute(() -> showReview(operation, preview));
        } catch (AccountAccessException failure) {
            fail(operation, failure, false);
        }
    }

    private void showReview(
            long operation,
            ManagedAccountRevocationPreview preview) {
        if (!active(operation)) {
            return;
        }
        ManagedRevocationReviewDialogBinding binding =
                ManagedRevocationReviewDialogBinding.inflate(
                        activity.getLayoutInflater());
        binding.managedRevocationTarget.setText(preview.targetName());
        binding.managedRevocationResource.setText(
                preview.resourceLabel());
        binding.managedRevocationConsequence.setText(
                preview.consequence());
        new MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.account_access_managed_confirm_title)
                .setView(binding.getRoot())
                .setNegativeButton(
                        R.string.account_access_cancel_action,
                        (dialog, which) -> cancel(operation))
                .setPositiveButton(
                        R.string.account_access_managed_continue,
                        (dialog, which) -> challenge(operation, preview))
                .setOnCancelListener(dialog -> cancel(operation))
                .show();
    }

    private void challenge(
            long operation,
            ManagedAccountRevocationPreview preview) {
        if (!active(operation)) {
            return;
        }
        worker.execute(() -> requestChallenge(operation, preview));
    }

    private void requestChallenge(
            long operation,
            ManagedAccountRevocationPreview preview) {
        try {
            ManagedAccountRevocationChallenge challenge =
                    repository.challenge(preview.actionId());
            if (challenge.isExpired(clock.instant())) {
                throw conflict("Managed revocation challenge has expired.");
            }
            main.execute(() -> requestCredential(
                    operation,
                    preview,
                    challenge));
        } catch (AccountAccessException failure) {
            fail(operation, failure, false);
        }
    }

    private void requestCredential(
            long operation,
            ManagedAccountRevocationPreview preview,
            ManagedAccountRevocationChallenge challenge) {
        if (!active(operation)) {
            return;
        }
        credentialRequester.requestExplicit(
                activity,
                challenge.googleServerClientId(),
                challenge.nonce(),
                new AuthCallback<>() {
                    @Override
                    public void onSuccess(String credential) {
                        if (active(operation)) {
                            worker.execute(() -> verify(
                                    operation,
                                    preview,
                                    challenge,
                                    credential));
                        }
                    }

                    @Override
                    public void onFailure(AuthException failure) {
                        if (failure.kind() == AuthFailureKind.CANCELED
                                || failure.kind()
                                        == AuthFailureKind.NO_CREDENTIAL) {
                            cancel(operation);
                            return;
                        }
                        fail(
                                operation,
                                authFailure(failure),
                                false);
                    }
                });
    }

    private void verify(
            long operation,
            ManagedAccountRevocationPreview preview,
            ManagedAccountRevocationChallenge challenge,
            String credential) {
        try {
            ManagedAccountRevocationGrant grant = repository.verify(
                    preview.actionId(),
                    challenge.id(),
                    credential);
            if (grant.isExpired(clock.instant())) {
                throw conflict("Managed revocation grant has expired.");
            }
            execute(
                    operation,
                    new ExecutionAttempt(
                            preview,
                            grant,
                            UUID.randomUUID().toString()));
        } catch (AccountAccessException failure) {
            fail(operation, failure, false);
        }
    }

    private void execute(
            long operation,
            ExecutionAttempt attempt) {
        try {
            ManagedAccountRevocationResult result = repository.execute(
                    attempt.preview().actionId(),
                    attempt.grant().token(),
                    attempt.idempotencyKey());
            requireSameResource(attempt.preview(), result);
            retryAttempt = Optional.empty();
            complete(operation, () -> listener.onSuccess(result));
        } catch (AccountAccessException failure) {
            boolean retryable = retryable(failure.kind())
                    && !attempt.grant().isExpired(clock.instant());
            retryAttempt = retryable
                    ? Optional.of(attempt)
                    : Optional.empty();
            fail(operation, failure, retryable);
        }
    }

    private void requireSameResource(
            ManagedAccountRevocationPreview preview,
            ManagedAccountRevocationResult result)
            throws AccountAccessException {
        if (!preview.actionId().equals(result.actionId())
                || preview.view() != result.view()
                || preview.targetUserId() != result.targetUserId()
                || !preview.resourceId().equals(result.resourceId())
                || result.revision() <= preview.revision()) {
            throw new AccountAccessException(
                    AccountAccessFailureKind.PROTOCOL,
                    "Managed revocation result changed its confirmed scope.");
        }
    }

    private void fail(
            long operation,
            AccountAccessException failure,
            boolean retryAvailable) {
        complete(operation, () -> listener.onFailure(
                failure,
                retryAvailable));
    }

    private void cancel(long operation) {
        retryAttempt = Optional.empty();
        complete(operation, listener::onCanceled);
    }

    private void complete(long operation, Runnable callback) {
        if (!active(operation)) {
            return;
        }
        busy.set(false);
        main.execute(() -> {
            if (generation.get() == operation) {
                listener.onBusy(Optional.empty());
                callback.run();
            }
        });
    }

    private boolean active(long operation) {
        return generation.get() == operation && busy.get();
    }

    @Override
    public void close() {
        generation.incrementAndGet();
        retryAttempt = Optional.empty();
        busy.set(false);
        listener.onBusy(Optional.empty());
    }

    private static AccountAccessException conflict(String message) {
        return new AccountAccessException(
                AccountAccessFailureKind.CONFLICT,
                message);
    }

    private static boolean retryable(AccountAccessFailureKind kind) {
        return kind == AccountAccessFailureKind.NETWORK
                || kind == AccountAccessFailureKind.RATE_LIMITED
                || kind == AccountAccessFailureKind.SERVICE_UNAVAILABLE;
    }

    private static AccountAccessException authFailure(
            AuthException failure) {
        AccountAccessFailureKind kind = switch (failure.kind()) {
            case AUTH_REJECTED -> AccountAccessFailureKind.AUTH_REJECTED;
            case NETWORK -> AccountAccessFailureKind.NETWORK;
            case RATE_LIMITED -> AccountAccessFailureKind.RATE_LIMITED;
            case SERVICE_UNAVAILABLE ->
                    AccountAccessFailureKind.SERVICE_UNAVAILABLE;
            case UPDATE_REQUIRED ->
                    AccountAccessFailureKind.UPDATE_REQUIRED;
            case CANCELED, NO_CREDENTIAL, CONFIGURATION, PROTOCOL,
                    STORAGE -> AccountAccessFailureKind.PROTOCOL;
        };
        return new AccountAccessException(
                kind,
                "Managed revocation credential failed.",
                failure.requestId().orElse(null),
                failure);
    }

    private record ExecutionAttempt(
            ManagedAccountRevocationPreview preview,
            ManagedAccountRevocationGrant grant,
            String idempotencyKey) {
        private ExecutionAttempt {
            Objects.requireNonNull(preview, "Preview is required.");
            Objects.requireNonNull(grant, "Grant is required.");
            Objects.requireNonNull(
                    idempotencyKey,
                    "Idempotency key is required.");
        }
    }
}
