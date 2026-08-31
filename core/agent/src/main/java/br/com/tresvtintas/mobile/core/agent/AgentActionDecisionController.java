package br.com.tresvtintas.mobile.core.agent;

import java.time.Clock;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public final class AgentActionDecisionController {
    @FunctionalInterface
    public interface Listener {
        void onAgentActionDecisionStateChanged(
                AgentActionDecisionState state);
    }

    private final AgentRepository repository;
    private final Executor worker;
    private final Executor main;
    private final Clock clock;
    private final Set<Listener> listeners =
            new CopyOnWriteArraySet<>();
    private final AtomicBoolean busy = new AtomicBoolean();
    private final AtomicLong generation = new AtomicLong();
    private volatile AgentActionDecisionState current =
            AgentActionDecisionState.idle();

    public AgentActionDecisionController(
            AgentRepository repository,
            Executor worker,
            Executor main) {
        this(repository, worker, main, Clock.systemUTC());
    }

    AgentActionDecisionController(
            AgentRepository repository,
            Executor worker,
            Executor main,
            Clock clock) {
        this.repository = Objects.requireNonNull(
                repository,
                "Agent repository is required.");
        this.worker = Objects.requireNonNull(
                worker,
                "Agent worker is required.");
        this.main = Objects.requireNonNull(
                main,
                "Agent main executor is required.");
        this.clock = Objects.requireNonNull(
                clock,
                "Agent action clock is required.");
    }

    public void subscribe(Listener listener) {
        Listener required = Objects.requireNonNull(
                listener,
                "Agent action decision listener is required.");
        listeners.add(required);
        AgentActionDecisionState snapshot = current;
        main.execute(() ->
                required.onAgentActionDecisionStateChanged(snapshot));
    }

    public void unsubscribe(Listener listener) {
        listeners.remove(listener);
    }

    public void decide(
            AgentAction action,
            AgentActionDecision decision,
            String idempotencyKey) {
        decide(action, decision, idempotencyKey, Optional.empty());
    }

    public void decide(
            AgentAction action,
            AgentActionDecision decision,
            String idempotencyKey,
            Optional<String> stepUpToken) {
        if (!valid(
                action,
                decision,
                idempotencyKey,
                stepUpToken)) {
            AgentAction safeAction = Objects.requireNonNull(
                    action,
                    "Agent action is required.");
            AgentActionDecision safeDecision = Objects.requireNonNull(
                    decision,
                    "Agent action decision is required.");
            publish(AgentActionDecisionState.error(
                    safeAction,
                    safeDecision,
                    new AgentException(
                            AgentFailureKind.INVALID_REQUEST,
                            "Agent action decision request is invalid.")));
            return;
        }
        if (!action.canDecide(clock.instant())) {
            publish(AgentActionDecisionState.error(
                    action,
                    decision,
                    new AgentException(
                            AgentFailureKind.CONFLICT,
                            "Agent action is no longer pending.")));
            return;
        }
        if (!busy.compareAndSet(false, true)) {
            return;
        }
        long operation = generation.incrementAndGet();
        publish(AgentActionDecisionState.running(action, decision));
        worker.execute(() -> execute(
                operation,
                action,
                decision,
                idempotencyKey,
                stepUpToken));
    }

    public void close() {
        generation.incrementAndGet();
        busy.set(false);
        publish(AgentActionDecisionState.closed());
        listeners.clear();
    }

    private void execute(
            long operation,
            AgentAction action,
            AgentActionDecision decision,
            String idempotencyKey,
            Optional<String> stepUpToken) {
        AgentActionDecisionState next;
        try {
            AgentActionDecisionResult result =
                    repository.decideAction(
                            action.id(),
                            decision,
                            idempotencyKey,
                            stepUpToken);
            if (!action.id().equals(result.action().id())) {
                throw new AgentException(
                        AgentFailureKind.PROTOCOL,
                        "Agent action decision identifiers do not match.");
            }
            next = AgentActionDecisionState.success(
                    action,
                    decision,
                    result);
        } catch (AgentException failure) {
            next = AgentActionDecisionState.error(
                    action,
                    decision,
                    failure);
        }
        if (generation.get() == operation
                && current.phase()
                        != AgentActionDecisionState.Phase.CLOSED) {
            busy.set(false);
            publish(next);
        }
    }

    private void publish(AgentActionDecisionState state) {
        current = state;
        main.execute(() -> listeners.forEach(listener ->
                listener.onAgentActionDecisionStateChanged(state)));
    }

    private static boolean valid(
            AgentAction action,
            AgentActionDecision decision,
            String idempotencyKey,
            Optional<String> stepUpToken) {
        if (stepUpToken == null) {
            return false;
        }
        boolean tokenRequired = action != null
                && action.requiresStepUp()
                && decision == AgentActionDecision.CONFIRM;
        return action != null
                && decision != null
                && idempotencyKey != null
                && idempotencyKey.length() >= 16
                && idempotencyKey.length() <= 255
                && idempotencyKey.matches("^[\\x21-\\x7e]+$")
                && (!tokenRequired
                        || stepUpToken.filter(
                                token -> token.matches(
                                        "^3vsu1_[A-Za-z0-9_-]{43}$"))
                                .isPresent())
                && (tokenRequired || stepUpToken.isEmpty());
    }
}
