package br.com.tresvtintas.mobile.feature.audit;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.recyclerview.widget.RecyclerView;
import br.com.tresvtintas.mobile.core.audit.AgentReplayModels.Step;
import br.com.tresvtintas.mobile.core.audit.AgentReplayModels.Turn;
import br.com.tresvtintas.mobile.feature.audit.databinding.AgentReplayItemBinding;
import java.time.Duration;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

final class AgentReplayAdapter
        extends RecyclerView.Adapter<AgentReplayAdapter.Holder> {
    private final DateTimeFormatter dateFormatter = DateTimeFormatter
            .ofLocalizedDateTime(FormatStyle.SHORT)
            .withLocale(Locale.getDefault())
            .withZone(ZoneId.systemDefault());
    private final Set<Integer> expanded = new HashSet<>();
    private List<Turn> items = List.of();

    void submit(List<Turn> values) {
        int previousSize = items.size();
        items = List.of();
        expanded.clear();
        if (previousSize > 0) {
            notifyItemRangeRemoved(0, previousSize);
        }
        items = List.copyOf(values);
        if (!items.isEmpty()) {
            notifyItemRangeInserted(0, items.size());
        }
    }

    @Override
    public Holder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new Holder(AgentReplayItemBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(Holder holder, int position) {
        holder.bind(items.get(position), dateFormatter, expanded.contains(position));
        holder.itemView.setOnClickListener(ignored -> toggle(holder));
        holder.binding.agentReplayExpand.setOnClickListener(ignored -> toggle(holder));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private void toggle(Holder holder) {
        int position = holder.getBindingAdapterPosition();
        if (position == RecyclerView.NO_POSITION) {
            return;
        }
        if (!expanded.add(position)) {
            expanded.remove(position);
        }
        notifyItemChanged(position);
    }

    static final class Holder extends RecyclerView.ViewHolder {
        private final AgentReplayItemBinding binding;

        Holder(AgentReplayItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Turn turn, DateTimeFormatter formatter, boolean isExpanded) {
            binding.agentReplayChannel.setText(
                    AgentReplayText.channel(turn.channel()));
            binding.agentReplayOutcome.setText(
                    AgentReplayText.outcome(turn.outcome()));
            binding.agentReplayTime.setText(formatter.format(turn.endedAt()));
            binding.agentReplayDuration.setText(duration(turn));
            binding.agentReplayStepCount.setText(
                    binding.getRoot().getResources().getQuantityString(
                            R.plurals.agent_replay_step_count,
                            turn.steps().size(),
                            turn.steps().size()));
            binding.agentReplayTruncated.setVisibility(
                    turn.stepsTruncated() ? View.VISIBLE : View.GONE);
            binding.agentReplaySteps.setText(stepDetails(turn.steps(), formatter));
            binding.agentReplaySteps.setVisibility(
                    isExpanded ? View.VISIBLE : View.GONE);
            binding.agentReplayExpand.setText(isExpanded
                    ? R.string.agent_replay_hide_steps
                    : R.string.agent_replay_show_steps);
        }

        private String duration(Turn turn) {
            long seconds = Math.max(
                    0,
                    Duration.between(turn.startedAt(), turn.endedAt()).getSeconds());
            return binding.getRoot().getContext().getString(
                    R.string.agent_replay_duration,
                    seconds);
        }

        private String stepDetails(
                List<Step> steps,
                DateTimeFormatter formatter) {
            if (steps.isEmpty()) {
                return binding.getRoot().getContext().getString(
                        R.string.agent_replay_no_public_steps);
            }
            StringBuilder result = new StringBuilder();
            for (Step step : steps) {
                if (result.length() > 0) {
                    result.append('\n');
                }
                result.append(binding.getRoot().getContext().getString(
                        R.string.agent_replay_step_format,
                        formatter.format(step.occurredAt()),
                        binding.getRoot().getContext().getString(
                                AgentReplayText.phase(step.phase())),
                        binding.getRoot().getContext().getString(
                                AgentReplayText.stepOutcome(step.outcome()))));
            }
            return result.toString();
        }
    }
}
