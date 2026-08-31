package br.com.tresvtintas.mobile.app;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.recyclerview.widget.RecyclerView;
import br.com.tresvtintas.mobile.app.databinding.MainShellActionItemBinding;
import java.util.ArrayList;
import java.util.List;

final class ShellActionAdapter
        extends RecyclerView.Adapter<ShellActionAdapter.ActionViewHolder> {
    @FunctionalInterface
    interface Listener {
        void onActionSelected(ShellAction action);
    }

    private final Listener listener;
    private final boolean showSections;
    private final List<ShellAction> actions = new ArrayList<>();

    ShellActionAdapter(Listener listener) {
        this(listener, false);
    }

    ShellActionAdapter(Listener listener, boolean showSections) {
        if (listener == null) {
            throw new IllegalArgumentException("Action listener is required.");
        }
        this.listener = listener;
        this.showSections = showSections;
    }

    void submit(List<ShellAction> updatedActions) {
        int previousSize = actions.size();
        if (previousSize > 0) {
            actions.clear();
            notifyItemRangeRemoved(0, previousSize);
        }
        int updatedSize = updatedActions.size();
        if (updatedSize == 0) {
            return;
        }
        actions.addAll(updatedActions);
        notifyItemRangeInserted(0, updatedSize);
    }

    @Override
    public ActionViewHolder onCreateViewHolder(
            ViewGroup parent,
            int viewType) {
        MainShellActionItemBinding binding =
                MainShellActionItemBinding.inflate(
                        LayoutInflater.from(parent.getContext()),
                        parent,
                        false);
        return new ActionViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(ActionViewHolder holder, int position) {
        holder.bind(actions.get(position));
    }

    @Override
    public int getItemCount() {
        return actions.size();
    }

    final class ActionViewHolder extends RecyclerView.ViewHolder {
        private final MainShellActionItemBinding binding;

        ActionViewHolder(MainShellActionItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(ShellAction action) {
            int position = getBindingAdapterPosition();
            boolean firstInSection = showSections
                    && position >= 0
                    && (position == 0
                            || actions.get(position - 1).section() != action.section());
            binding.shellActionSection.setVisibility(
                    firstInSection ? View.VISIBLE : View.GONE);
            if (firstInSection) {
                binding.shellActionSection.setText(action.section().titleResource());
            }
            binding.shellActionIcon.setImageResource(action.iconResource());
            binding.shellActionTitle.setText(action.titleResource());
            binding.shellActionDescription.setText(
                    action.descriptionResource());
            binding.shellActionRow.setContentDescription(
                    binding.shellActionRow.getContext().getString(
                            R.string.shell_action_open,
                            binding.shellActionRow.getContext().getString(
                                    action.titleResource())));
            binding.shellActionRow.setOnClickListener(
                    ignored -> listener.onActionSelected(action));
        }
    }
}
