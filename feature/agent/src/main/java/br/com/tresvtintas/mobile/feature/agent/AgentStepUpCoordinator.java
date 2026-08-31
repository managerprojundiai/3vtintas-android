package br.com.tresvtintas.mobile.feature.agent;

import androidx.appcompat.app.AppCompatActivity;
import br.com.tresvtintas.mobile.core.agent.AgentAction;
import br.com.tresvtintas.mobile.core.agent.AgentException;
import br.com.tresvtintas.mobile.core.agent.AgentFailureKind;
import br.com.tresvtintas.mobile.core.agent.AgentRepository;
import br.com.tresvtintas.mobile.core.agent.AgentStepUpChallenge;
import br.com.tresvtintas.mobile.core.agent.AgentStepUpGrant;
import br.com.tresvtintas.mobile.core.auth.AuthCallback;
import br.com.tresvtintas.mobile.core.auth.AuthException;
import br.com.tresvtintas.mobile.core.auth.AuthFailureKind;
import br.com.tresvtintas.mobile.core.auth.GoogleIdTokenRequester;
import java.time.Clock;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

final class AgentStepUpCoordinator implements AutoCloseable {
    interface Listener {
        void onAuthorized(String stepUpToken);

        void onFailure(AgentException failure);

        void onCanceled();
    }

    private final AppCompatActivity activity;
    private final AgentRepository repository;
    private final GoogleIdTokenRequester credentialRequester;
    private final Executor worker;
    private final Executor main;
    private final Clock clock;
    private final AtomicBoolean busy = new AtomicBoolean();
    private final AtomicLong generation = new AtomicLong();

    AgentStepUpCoordinator(
            AppCompatActivity activity,
            AgentRepository repository,
            GoogleIdTokenRequester credentialRequester,
            Executor worker,
            Executor main) {
        this(
                activity,
                repository,
                credentialRequester,
                worker,
                main,
                Clock.systemUTC());
    }

    AgentStepUpCoordinator(
            AppCompatActivity activity,
            AgentRepository repository,
            GoogleIdTokenRequester credentialRequester,
            Executor worker,
            Executor main,
            Clock clock) {
        this.activity = Objects.requireNonNull(
                activity,
                "Agent step-up activity is required.");
        this.repository = Objects.requireNonNull(
                repository,
                "Agent step-up repository is required.");
        this.credentialRequester = Objects.requireNonNull(
                credentialRequester,
                "Agent step-up credential requester is required.");
        this.worker = Objects.requireNonNull(
                worker,
                "Agent step-up worker is required.");
        this.main = Objects.requireNonNull(
                main,
                "Agent step-up main executor is required.");
        this.clock = Objects.requireNonNull(
                clock,
                "Agent step-up clock is required.");
    }

    boolean authorize(AgentAction action, Listener listener) {
        Objects.requireNonNull(
                action,
                "Agent step-up action is required.");
        Listener required = Objects.requireNonNull(
                listener,
                "Agent step-up listener is required.");
        if (!action.requiresStepUp()
                || !busy.compareAndSet(false, true)) {
            return false;
        }
        long operation = generation.incrementAndGet();
        worker.execute(() -> requestChallenge(
                operation,
                action,
                required));
        return true;
    }

    private void requestChallenge(
            long operation,
            AgentAction action,
            Listener listener) {
        try {
            AgentStepUpChallenge challenge =
                    repository.requestStepUp(action.id());
            if (challenge.isExpired(clock.instant())) {
                throw new AgentException(
                        AgentFailureKind.CONFLICT,
                        "Agent step-up challenge has expired.");
            }
            main.execute(() -> requestCredential(
                    operation,
                    action,
                    challenge,
                    listener));
        } catch (AgentException failure) {
            fail(operation, listener, failure);
        }
    }

    private void requestCredential(
            long operation,
            AgentAction action,
            AgentStepUpChallenge challenge,
            Listener listener) {
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
                        if (!active(operation)) {
                            return;
                        }
                        worker.execute(() -> verify(
                                operation,
                                action,
                                challenge,
                                credential,
                                listener));
                    }

                    @Override
                    public void onFailure(AuthException failure) {
                        if (failure.kind() == AuthFailureKind.CANCELED
                                || failure.kind()
                                        == AuthFailureKind.NO_CREDENTIAL) {
                            cancel(operation, listener);
                            return;
                        }
                        fail(
                                operation,
                                listener,
                                new AgentException(
                                        map(failure.kind()),
                                        "Agent step-up credential failed.",
                                        failure.requestId(),
                                        failure));
                    }
                });
    }

    private void verify(
            long operation,
            AgentAction action,
            AgentStepUpChallenge challenge,
            String credential,
            Listener listener) {
        try {
            AgentStepUpGrant grant = repository.verifyStepUp(
                    action.id(),
                    challenge.id(),
                    credential);
            if (grant.isExpired(clock.instant())) {
                throw new AgentException(
                        AgentFailureKind.CONFLICT,
                        "Agent step-up grant has expired.");
            }
            complete(operation, () ->
                    listener.onAuthorized(grant.token()));
        } catch (AgentException failure) {
            fail(operation, listener, failure);
        }
    }

    private void fail(
            long operation,
            Listener listener,
            AgentException failure) {
        complete(operation, () -> listener.onFailure(failure));
    }

    private void cancel(long operation, Listener listener) {
        complete(operation, listener::onCanceled);
    }

    private void complete(long operation, Runnable callback) {
        if (!active(operation)) {
            return;
        }
        busy.set(false);
        main.execute(() -> {
            if (generation.get() == operation) {
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
        busy.set(false);
    }

    private static AgentFailureKind map(AuthFailureKind kind) {
        return switch (kind) {
            case AUTH_REJECTED -> AgentFailureKind.AUTH_REJECTED;
            case NETWORK -> AgentFailureKind.NETWORK;
            case RATE_LIMITED -> AgentFailureKind.RATE_LIMITED;
            case SERVICE_UNAVAILABLE ->
                    AgentFailureKind.SERVICE_UNAVAILABLE;
            case UPDATE_REQUIRED -> AgentFailureKind.UPDATE_REQUIRED;
            case CANCELED, NO_CREDENTIAL, CONFIGURATION, PROTOCOL,
                    STORAGE -> AgentFailureKind.PROTOCOL;
        };
    }
}
