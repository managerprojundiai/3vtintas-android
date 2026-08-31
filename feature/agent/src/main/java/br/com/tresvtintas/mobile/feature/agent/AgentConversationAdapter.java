package br.com.tresvtintas.mobile.feature.agent;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import br.com.tresvtintas.mobile.core.agent.AgentConversation;
import br.com.tresvtintas.mobile.feature.agent.databinding.AgentItemConversationBinding;

final class AgentConversationAdapter
        extends ListAdapter<
                AgentConversation,
                AgentConversationAdapter.Holder> {
    private static final DiffUtil.ItemCallback<AgentConversation>
            DIFFERENCE = new DiffUtil.ItemCallback<>() {
                @Override
                public boolean areItemsTheSame(
                        @NonNull AgentConversation oldItem,
                        @NonNull AgentConversation newItem) {
                    return oldItem.id().equals(newItem.id());
                }

                @Override
                public boolean areContentsTheSame(
                        @NonNull AgentConversation oldItem,
                        @NonNull AgentConversation newItem) {
                    return oldItem.equals(newItem);
                }
            };
    private final ConversationListener listener;

    AgentConversationAdapter(ConversationListener listener) {
        super(DIFFERENCE);
        this.listener = listener;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType) {
        return new Holder(
                AgentItemConversationBinding.inflate(
                        LayoutInflater.from(parent.getContext()),
                        parent,
                        false),
                listener);
    }

    @Override
    public void onBindViewHolder(
            @NonNull Holder holder,
            int position) {
        holder.bind(getItem(position));
    }

    static final class Holder extends RecyclerView.ViewHolder {
        private final AgentItemConversationBinding binding;
        private final ConversationListener listener;

        Holder(
                AgentItemConversationBinding binding,
                ConversationListener listener) {
            super(binding.getRoot());
            this.binding = binding;
            this.listener = listener;
        }

        void bind(AgentConversation value) {
            var context = binding.getRoot().getContext();
            binding.agentConversationTitle.setText(value.title());
            binding.agentConversationOrigin.setText(
                    value.createdOnThisDevice()
                            ? R.string.agent_created_here
                            : R.string.agent_created_elsewhere);
            binding.agentConversationActivity.setText(
                    context.getString(
                            R.string.agent_activity_time,
                            AgentText.activity(
                                    value.lastActivityAt())));
            binding.getRoot().setOnClickListener(
                    ignored -> listener.onConversationSelected(value));
        }
    }

    @FunctionalInterface
    interface ConversationListener {
        void onConversationSelected(AgentConversation conversation);
    }
}
