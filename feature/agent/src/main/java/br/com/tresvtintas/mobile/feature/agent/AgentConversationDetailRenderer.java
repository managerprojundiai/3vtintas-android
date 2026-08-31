package br.com.tresvtintas.mobile.feature.agent;

import android.view.View;
import br.com.tresvtintas.mobile.core.agent.AgentConversationDetailState;
import br.com.tresvtintas.mobile.core.agent.AgentFailureKind;
import br.com.tresvtintas.mobile.feature.agent.databinding.AgentActivityConversationBinding;
import java.util.List;

final class AgentConversationDetailRenderer {
    private final AgentActivityConversationBinding binding;
    private final AgentMessageAdapter adapter;

    AgentConversationDetailRenderer(
            AgentActivityConversationBinding binding,
            AgentMessageAdapter adapter) {
        this.binding = binding;
        this.adapter = adapter;
    }

    void render(AgentConversationDetailState state) {
        boolean busy = switch (state.phase()) {
            case LOADING, REFRESHING, LOADING_MORE -> true;
            default -> false;
        };
        binding.agentDetailProgress.setVisibility(
                busy ? View.VISIBLE : View.INVISIBLE);
        binding.agentDetailRefresh.setEnabled(!busy);
        binding.agentDetailNotice.setVisibility(View.GONE);
        binding.agentDetailRetry.setVisibility(View.GONE);
        if (state.phase() == AgentConversationDetailState.Phase.ERROR) {
            error(state);
            return;
        }
        state.snapshot().ifPresentOrElse(
                value -> snapshot(state, value),
                () -> {
                    binding.agentMessageList.setVisibility(View.GONE);
                    binding.agentDetailLoadMore.setVisibility(View.GONE);
                    binding.agentDetailEmptyGroup.setVisibility(
                            state.phase()
                                            == AgentConversationDetailState
                                                    .Phase.LOADING
                                    ? View.VISIBLE
                                    : View.GONE);
                });
    }

    private void snapshot(
            AgentConversationDetailState state,
            AgentConversationDetailState.Snapshot snapshot) {
        binding.agentDetailToolbar.setTitle(
                snapshot.conversation().title());
        adapter.submitList(snapshot.messages());
        boolean empty = snapshot.messages().isEmpty();
        binding.agentMessageList.setVisibility(
                empty ? View.GONE : View.VISIBLE);
        binding.agentDetailEmptyGroup.setVisibility(
                empty ? View.VISIBLE : View.GONE);
        binding.agentDetailEmptyTitle.setText(
                R.string.agent_detail_empty_title);
        binding.agentDetailEmptyMessage.setText(
                R.string.agent_detail_empty_message);
        binding.agentDetailLoadMore.setVisibility(
                !empty && snapshot.hasMore()
                        ? View.VISIBLE
                        : View.GONE);
        binding.agentDetailLoadMore.setEnabled(
                state.phase()
                        != AgentConversationDetailState.Phase.LOADING_MORE);
        if (state.phase()
                == AgentConversationDetailState.Phase.WARNING) {
            binding.agentDetailNotice.setText(
                    R.string.agent_error_network);
            binding.agentDetailNotice.setVisibility(View.VISIBLE);
            support(state);
        }
    }

    private void error(AgentConversationDetailState state) {
        AgentFailureKind failure = state.failure().orElseThrow();
        adapter.submitList(List.of());
        binding.agentMessageList.setVisibility(View.GONE);
        binding.agentDetailLoadMore.setVisibility(View.GONE);
        binding.agentDetailEmptyGroup.setVisibility(View.VISIBLE);
        binding.agentDetailEmptyTitle.setText(
                R.string.agent_error_title);
        binding.agentDetailEmptyMessage.setText(
                AgentText.failure(failure));
        binding.agentDetailRetry.setVisibility(
                AgentText.retryable(failure)
                        ? View.VISIBLE
                        : View.GONE);
        support(state);
    }

    private void support(AgentConversationDetailState state) {
        state.requestId().ifPresent(value -> {
            binding.agentDetailNotice.setText(
                    binding.getRoot().getContext().getString(
                            R.string.agent_request_id,
                            value));
            binding.agentDetailNotice.setVisibility(View.VISIBLE);
        });
    }
}
