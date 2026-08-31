package br.com.tresvtintas.mobile.feature.agent;

import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import br.com.tresvtintas.mobile.core.agent.AgentAction;
import br.com.tresvtintas.mobile.core.agent.AgentActionDecision;
import br.com.tresvtintas.mobile.core.agent.AgentActionDecisionController;
import br.com.tresvtintas.mobile.core.agent.AgentActionDecisionState;
import br.com.tresvtintas.mobile.core.agent.AgentActionKind;
import br.com.tresvtintas.mobile.core.agent.AgentActionResult;
import br.com.tresvtintas.mobile.core.agent.AgentFailureKind;
import br.com.tresvtintas.mobile.core.agent.AgentException;
import br.com.tresvtintas.mobile.core.agent.AgentMaterialQuoteAmendResult;
import br.com.tresvtintas.mobile.core.agent.AgentMaterialQuoteCreateResult;
import com.google.android.material.snackbar.Snackbar;
import java.time.Instant;
import java.util.Optional;

final class AgentActionUiCoordinator {
    private static final String STATE_ACTION_KEY =
            "agent_action_decision_key";
    private static final String STATE_ACTION_ID =
            "agent_action_decision_id";
    private static final String STATE_ACTION_DECISION =
            "agent_action_decision_value";
    private final AppCompatActivity activity;
    private final View anchor;
    private final AgentMessageAdapter adapter;
    private final AgentQuoteNavigator quoteNavigator;
    private final Runnable refreshHistory;
    private final AgentActionDecisionController.Listener listener =
            this::render;
    private Optional<AgentActionDecisionController> controller =
            Optional.empty();
    private Optional<AgentStepUpCoordinator> stepUp =
            Optional.empty();
    private AgentActionDecisionAttempt attempt =
            new AgentActionDecisionAttempt();

    AgentActionUiCoordinator(
            AppCompatActivity activity,
            View anchor,
            AgentMessageAdapter adapter,
            AgentQuoteNavigator quoteNavigator,
            Runnable refreshHistory) {
        this.activity = java.util.Objects.requireNonNull(
                activity,
                "Agent action activity is required.");
        this.anchor = java.util.Objects.requireNonNull(
                anchor,
                "Agent action notice anchor is required.");
        this.adapter = java.util.Objects.requireNonNull(
                adapter,
                "Agent action adapter is required.");
        this.quoteNavigator = java.util.Objects.requireNonNull(
                quoteNavigator,
                "Agent quote navigator is required.");
        this.refreshHistory = java.util.Objects.requireNonNull(
                refreshHistory,
                "Agent action refresh is required.");
    }

    void restore(Bundle state) {
        if (state == null) {
            return;
        }
        attempt = AgentActionDecisionAttempt.restored(
                state.getString(STATE_ACTION_KEY, ""),
                state.getString(STATE_ACTION_ID, ""),
                state.getString(STATE_ACTION_DECISION, ""));
    }

    void save(Bundle state) {
        state.putString(STATE_ACTION_KEY, attempt.key());
        state.putString(STATE_ACTION_ID, attempt.actionId());
        state.putString(
                STATE_ACTION_DECISION,
                attempt.decisionName());
    }

    void start(AgentFeatureRuntime runtime) {
        stop();
        AgentActionDecisionController next =
                new AgentActionDecisionController(
                        runtime.repository(),
                        runtime.workerExecutor(),
                        ContextCompat.getMainExecutor(activity));
        controller = Optional.of(next);
        stepUp = Optional.of(new AgentStepUpCoordinator(
                activity,
                runtime.repository(),
                runtime.googleIdTokenRequester(),
                runtime.workerExecutor(),
                ContextCompat.getMainExecutor(activity)));
        next.subscribe(listener);
    }

    void stop() {
        controller.ifPresent(value -> {
            value.unsubscribe(listener);
            value.close();
        });
        controller = Optional.empty();
        stepUp.ifPresent(AgentStepUpCoordinator::close);
        stepUp = Optional.empty();
        adapter.setBusyActionId(Optional.empty());
    }

    void review(AgentAction action) {
        if (controller.isEmpty()) {
            return;
        }
        if (!action.canDecide(Instant.now())) {
            notice(R.string.agent_action_expired);
            refreshHistory.run();
            return;
        }
        attempt.decisionFor(action.id()).ifPresentOrElse(
                decision -> AgentActionDialogs.retry(
                        activity,
                        decision,
                        value -> decide(action, value)),
                () -> AgentActionDialogs.review(
                        activity,
                        action,
                        value -> decide(action, value)));
    }

    private void decide(
            AgentAction action,
            AgentActionDecision decision) {
        String key;
        try {
            key = attempt.keyFor(action.id(), decision);
        } catch (IllegalStateException failure) {
            refreshHistory.run();
            return;
        }
        if (action.requiresStepUp()
                && decision == AgentActionDecision.CONFIRM) {
            authorize(action, decision, key);
            return;
        }
        decide(action, decision, key, Optional.empty());
    }

    private void authorize(
            AgentAction action,
            AgentActionDecision decision,
            String key) {
        adapter.setBusyActionId(Optional.of(action.id()));
        boolean started = stepUp.map(value -> value.authorize(
                action,
                new AgentStepUpCoordinator.Listener() {
                    @Override
                    public void onAuthorized(String token) {
                        decide(
                                action,
                                decision,
                                key,
                                Optional.of(token));
                    }

                    @Override
                    public void onFailure(AgentException failure) {
                        adapter.setBusyActionId(Optional.empty());
                        showFailure(
                                failure.kind(),
                                failure.requestId());
                    }

                    @Override
                    public void onCanceled() {
                        adapter.setBusyActionId(Optional.empty());
                        notice(R.string.agent_action_step_up_canceled);
                    }
                })).orElse(false);
        if (!started) {
            adapter.setBusyActionId(Optional.empty());
        }
    }

    private void decide(
            AgentAction action,
            AgentActionDecision decision,
            String key,
            Optional<String> stepUpToken) {
        controller.ifPresent(value -> value.decide(
                action,
                decision,
                key,
                stepUpToken));
    }

    private void render(AgentActionDecisionState state) {
        Optional<String> busyAction = state.phase()
                        == AgentActionDecisionState.Phase.RUNNING
                ? state.action().map(AgentAction::id)
                : Optional.empty();
        adapter.setBusyActionId(busyAction);
        if (state.phase()
                == AgentActionDecisionState.Phase.SUCCESS) {
            boolean replayed = state.result()
                    .orElseThrow()
                    .replayed();
            AgentAction completed = state.result()
                    .orElseThrow()
                    .action();
            attempt.reset();
            Optional<Long> quoteId = completed.result().flatMap(
                    AgentActionUiCoordinator::quoteId);
            if (quoteId.isPresent()) {
                Snackbar.make(
                                anchor,
                                replayed
                                        ? R.string.agent_action_replayed
                                        : successMessage(completed.kind()),
                                Snackbar.LENGTH_LONG)
                        .setAction(
                                R.string.agent_action_open_quote,
                                ignored -> quoteNavigator.open(
                                        activity,
                                        quoteId.orElseThrow()))
                        .show();
            } else {
                notice(replayed
                        ? R.string.agent_action_replayed
                        : successMessage(completed.kind()));
            }
            refreshHistory.run();
            return;
        }
        if (state.phase() != AgentActionDecisionState.Phase.ERROR) {
            return;
        }
        failure(state);
    }

    private void failure(AgentActionDecisionState state) {
        AgentFailureKind failure = state.failure().orElseThrow();
        if (!AgentText.retryable(failure)) {
            attempt.reset();
        }
        showFailure(failure, state.requestId());
        if (failure == AgentFailureKind.CONFLICT
                || failure == AgentFailureKind.NOT_FOUND
                || failure == AgentFailureKind.PROTOCOL) {
            refreshHistory.run();
        }
    }

    private void showFailure(
            AgentFailureKind failure,
            Optional<String> requestId) {
        String message = activity.getString(
                AgentText.failure(failure));
        if (requestId.isPresent()) {
            message = message
                    + "\n"
                    + activity.getString(
                            R.string.agent_request_id,
                            requestId.orElseThrow());
        }
        Snackbar.make(anchor, message, Snackbar.LENGTH_LONG).show();
    }

    private void notice(int message) {
        Snackbar.make(anchor, message, Snackbar.LENGTH_LONG).show();
    }

    private static int successMessage(AgentActionKind kind) {
        return switch (kind) {
            case MATERIAL_QUOTE_CREATE ->
                    R.string.agent_action_create_success;
            case MATERIAL_QUOTE_AMEND ->
                    R.string.agent_action_amend_success;
            case MATERIAL_QUOTE_SEND ->
                    R.string.agent_action_success;
            case ATTENDANCE_REPLY ->
                    R.string.agent_action_attendance_success;
            case APPOINTMENT_CREATE ->
                    R.string.agent_action_appointment_create_success;
            case APPOINTMENT_RESCHEDULE ->
                    R.string.agent_action_appointment_reschedule_success;
            case APPOINTMENT_CANCEL ->
                    R.string.agent_action_appointment_cancel_success;
            case DELIVERY_START ->
                    R.string.agent_action_delivery_start_success;
            case DELIVERY_COMPLETE ->
                    R.string.agent_action_delivery_complete_success;
            case ORDER_CONFIRM ->
                    R.string.agent_action_order_confirm_success;
            case ORDER_START_FULFILLMENT ->
                    R.string
                            .agent_action_order_start_fulfillment_success;
            case ORDER_COMPLETE ->
                    R.string.agent_action_order_complete_success;
            case PERSONAL_FINANCE_CREATE,
                    CORPORATE_FINANCE_CREATE ->
                    R.string.agent_action_finance_create_success;
            case PERSONAL_FINANCE_SETTLE,
                    CORPORATE_FINANCE_SETTLE ->
                    R.string.agent_action_finance_settle_success;
            case PERSONAL_FINANCE_CANCEL,
                    CORPORATE_FINANCE_CANCEL ->
                    R.string.agent_action_finance_cancel_success;
            case COMMISSION_APPROVE ->
                    R.string.agent_action_commission_approve_success;
            case COMMISSION_CANCEL ->
                    R.string.agent_action_commission_cancel_success;
            case COMMISSION_PAY ->
                    R.string.agent_action_commission_pay_success;
        };
    }

    private static Optional<Long> quoteId(
            AgentActionResult result) {
        if (result instanceof AgentMaterialQuoteCreateResult create) {
            return Optional.of(create.quoteId());
        }
        if (result instanceof AgentMaterialQuoteAmendResult amend) {
            return Optional.of(amend.quoteId());
        }
        return Optional.empty();
    }
}
