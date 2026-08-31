package br.com.tresvtintas.mobile.feature.agent;

import android.view.View;
import br.com.tresvtintas.mobile.core.agent.AgentConversationListState;
import br.com.tresvtintas.mobile.core.agent.AgentFailureKind;
import br.com.tresvtintas.mobile.feature.agent.databinding.AgentActivityConversationListBinding;
import java.util.List;

final class AgentConversationListRenderer {
    private final AgentActivityConversationListBinding binding;
    private final AgentConversationAdapter adapter;

    AgentConversationListRenderer(
            AgentActivityConversationListBinding binding,
            AgentConversationAdapter adapter) {
        this.binding = binding;
        this.adapter = adapter;
    }

    void render(AgentConversationListState state) {
        boolean busy = switch (state.phase()) {
            case LOADING, REFRESHING, LOADING_MORE, CREATING -> true;
            default -> false;
        };
        binding.agentListProgress.setVisibility(
                busy ? View.VISIBLE : View.INVISIBLE);
        binding.agentListRefresh.setEnabled(!busy);
        binding.agentNewConversation.setEnabled(!busy);
        binding.agentListNotice.setVisibility(View.GONE);
        binding.agentListRetry.setVisibility(View.GONE);
        if (state.phase() == AgentConversationListState.Phase.ERROR) {
            error(state);
            return;
        }
        state.snapshot().ifPresentOrElse(
                value -> snapshot(state, value),
                () -> {
                    binding.agentConversationList.setVisibility(
                            View.GONE);
                    binding.agentListLoadMore.setVisibility(View.GONE);
                    binding.agentListEmptyGroup.setVisibility(
                            state.phase()
                                            == AgentConversationListState
                                                    .Phase.LOADING
                                    ? View.VISIBLE
                                    : View.GONE);
                });
    }

    private void snapshot(
            AgentConversationListState state,
            AgentConversationListState.Snapshot snapshot) {
        adapter.submitList(snapshot.items());
        boolean empty = snapshot.items().isEmpty();
        binding.agentConversationList.setVisibility(
                empty ? View.GONE : View.VISIBLE);
        binding.agentListEmptyGroup.setVisibility(
                empty ? View.VISIBLE : View.GONE);
        binding.agentListEmptyTitle.setText(
                R.string.agent_empty_title);
        binding.agentListEmptyMessage.setText(
                R.string.agent_empty_message);
        binding.agentListLoadMore.setVisibility(
                !empty && snapshot.hasMore()
                        ? View.VISIBLE
                        : View.GONE);
        binding.agentListLoadMore.setEnabled(
                state.phase()
                        != AgentConversationListState.Phase.LOADING_MORE);
        if (state.phase()
                == AgentConversationListState.Phase.WARNING) {
            binding.agentListNotice.setText(
                    R.string.agent_error_network);
            binding.agentListNotice.setVisibility(View.VISIBLE);
            support(state);
        }
    }

    private void error(AgentConversationListState state) {
        AgentFailureKind failure = state.failure().orElseThrow();
        adapter.submitList(List.of());
        binding.agentConversationList.setVisibility(View.GONE);
        binding.agentListLoadMore.setVisibility(View.GONE);
        binding.agentListEmptyGroup.setVisibility(View.VISIBLE);
        binding.agentListEmptyTitle.setText(
                R.string.agent_error_title);
        binding.agentListEmptyMessage.setText(
                AgentText.failure(failure));
        binding.agentListRetry.setVisibility(
                AgentText.retryable(failure)
                        ? View.VISIBLE
                        : View.GONE);
        support(state);
    }

    private void support(AgentConversationListState state) {
        state.requestId().ifPresent(value -> {
            binding.agentListNotice.setText(
                    binding.getRoot().getContext().getString(
                            R.string.agent_request_id,
                            value));
            binding.agentListNotice.setVisibility(View.VISIBLE);
        });
    }
}
