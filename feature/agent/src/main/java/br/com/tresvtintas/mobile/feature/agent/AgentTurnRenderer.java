package br.com.tresvtintas.mobile.feature.agent;

import android.view.View;
import br.com.tresvtintas.mobile.core.agent.AgentFailureKind;
import br.com.tresvtintas.mobile.core.agent.AgentTurnState;
import br.com.tresvtintas.mobile.feature.agent.databinding.AgentActivityConversationBinding;

final class AgentTurnRenderer {
    private final AgentActivityConversationBinding binding;

    AgentTurnRenderer(AgentActivityConversationBinding binding) {
        this.binding = binding;
    }

    void render(AgentTurnState state) {
        boolean busy = state.active();
        binding.agentTurnProgress.setVisibility(
                busy ? View.VISIBLE : View.INVISIBLE);
        binding.agentInput.setEnabled(state.canSend());
        binding.agentSend.setEnabled(state.canSend());
        binding.agentCancelTurn.setVisibility(
                state.canCancel() ? View.VISIBLE : View.GONE);
        binding.agentRetrySend.setVisibility(
                retrySend(state) ? View.VISIBLE : View.GONE);
        binding.agentPartialCard.setVisibility(
                state.partialText().isEmpty()
                        ? View.GONE
                        : View.VISIBLE);
        binding.agentPartialText.setText(state.partialText());
        int status = status(state);
        binding.agentTurnStatus.setVisibility(
                status == 0 ? View.GONE : View.VISIBLE);
        if (status != 0) {
            binding.agentTurnStatus.setText(
                    status == R.string.agent_turn_reconnecting
                            ? binding.getRoot().getContext().getString(
                                    status,
                                    state.reconnectAttempt())
                            : binding.getRoot().getContext().getString(
                                    status));
        }
        state.requestId().ifPresent(value -> {
            String support = binding.getRoot().getContext().getString(
                    R.string.agent_request_id,
                    value);
            CharSequence current = binding.agentTurnStatus.getText();
            binding.agentTurnStatus.setText(
                    current == null || current.length() == 0
                            ? support
                            : current + "\n" + support);
            binding.agentTurnStatus.setVisibility(View.VISIBLE);
        });
    }

    private static int status(AgentTurnState state) {
        return switch (state.phase()) {
            case IDLE, CLOSED -> 0;
            case SUBMITTING -> R.string.agent_turn_submitting;
            case QUEUED -> R.string.agent_turn_queued;
            case RUNNING -> state.toolActive()
                    ? R.string.agent_turn_tool
                    : R.string.agent_turn_running;
            case RECONNECTING -> R.string.agent_turn_reconnecting;
            case CANCELLING -> R.string.agent_turn_cancelling;
            case COMPLETED -> completed(state);
            case FAILED -> R.string.agent_turn_failed;
            case CANCELLED -> R.string.agent_turn_cancelled;
            case ERROR -> state.failure()
                    .map(AgentText::failure)
                    .orElse(R.string.agent_error_protocol);
        };
    }

    private static int completed(AgentTurnState state) {
        if (state.blocked()) {
            return R.string.agent_turn_blocked;
        }
        return state.requiresHuman()
                ? R.string.agent_turn_completed_human
                : R.string.agent_turn_completed;
    }

    private static boolean retrySend(AgentTurnState state) {
        if (state.phase() != AgentTurnState.Phase.ERROR
                || state.turn().isPresent()) {
            return false;
        }
        AgentFailureKind failure = state.failure().orElse(null);
        return failure != null && AgentText.retryable(failure);
    }
}
