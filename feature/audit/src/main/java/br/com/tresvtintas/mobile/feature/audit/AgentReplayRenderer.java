package br.com.tresvtintas.mobile.feature.audit;

import android.view.View;
import br.com.tresvtintas.mobile.core.audit.AgentReplayState;
import br.com.tresvtintas.mobile.core.audit.AuditException;
import br.com.tresvtintas.mobile.feature.audit.databinding.AgentReplayActivityBinding;
import java.util.List;

final class AgentReplayRenderer {
    private final AgentReplayActivityBinding binding;
    private final AgentReplayAdapter adapter;

    AgentReplayRenderer(
            AgentReplayActivityBinding binding,
            AgentReplayAdapter adapter) {
        this.binding = binding;
        this.adapter = adapter;
    }

    void render(AgentReplayState state) {
        boolean busy = state.phase() == AgentReplayState.Phase.LOADING
                || state.phase() == AgentReplayState.Phase.REFRESHING
                || state.phase() == AgentReplayState.Phase.LOADING_MORE;
        binding.agentReplayProgress.setVisibility(
                busy ? View.VISIBLE : View.INVISIBLE);
        binding.agentReplayRefresh.setEnabled(!busy);
        binding.agentReplayApplyFilter.setEnabled(!busy);
        binding.agentReplayLoadMore.setEnabled(!busy);
        if (state.snapshot().isPresent()) {
            AgentReplayState.Snapshot snapshot = state.snapshot().orElseThrow();
            adapter.submit(snapshot.turns());
            binding.agentReplayCount.setText(
                    binding.getRoot().getResources().getQuantityString(
                            R.plurals.agent_replay_count,
                            snapshot.turns().size(),
                            snapshot.turns().size()));
            binding.agentReplayList.setVisibility(
                    snapshot.turns().isEmpty() ? View.GONE : View.VISIBLE);
            binding.agentReplayEmpty.setVisibility(
                    snapshot.turns().isEmpty() ? View.VISIBLE : View.GONE);
            binding.agentReplayLoadMore.setVisibility(
                    snapshot.nextCursor().isPresent() ? View.VISIBLE : View.GONE);
            binding.agentReplayErrorGroup.setVisibility(View.GONE);
        } else if (state.phase() == AgentReplayState.Phase.ERROR) {
            adapter.submit(List.of());
            binding.agentReplayList.setVisibility(View.GONE);
            binding.agentReplayEmpty.setVisibility(View.GONE);
            binding.agentReplayLoadMore.setVisibility(View.GONE);
            binding.agentReplayErrorGroup.setVisibility(View.VISIBLE);
            binding.agentReplayError.setText(
                    failureText(state.failure().orElseThrow()));
            binding.agentReplayCount.setText(R.string.agent_replay_count_empty);
        } else {
            binding.agentReplayList.setVisibility(View.GONE);
            binding.agentReplayEmpty.setVisibility(View.GONE);
            binding.agentReplayErrorGroup.setVisibility(View.GONE);
        }
        binding.agentReplayStaleCard.setVisibility(
                state.phase() == AgentReplayState.Phase.STALE
                        ? View.VISIBLE
                        : View.GONE);
    }

    private String failureText(AuditException failure) {
        String message = binding.getRoot().getContext().getString(
                AuditText.failure(failure.kind()));
        return failure.requestId().map(value -> message + "\n\n"
                + binding.getRoot().getContext().getString(
                        R.string.audit_support_code,
                        value)).orElse(message);
    }
}
