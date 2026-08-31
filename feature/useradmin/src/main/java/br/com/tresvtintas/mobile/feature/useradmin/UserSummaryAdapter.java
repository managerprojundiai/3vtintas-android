package br.com.tresvtintas.mobile.feature.useradmin;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.recyclerview.widget.RecyclerView;
import br.com.tresvtintas.mobile.core.useradmin.UserAdministrationModels.Assignment;
import br.com.tresvtintas.mobile.core.useradmin.UserAdministrationModels.User;
import br.com.tresvtintas.mobile.feature.useradmin.databinding.UserAdminItemBinding;
import java.util.List;
import java.util.function.LongConsumer;

final class UserSummaryAdapter extends RecyclerView.Adapter<UserSummaryAdapter.Holder> {
    private final LongConsumer selected;
    private List<User> items = List.of();

    UserSummaryAdapter(LongConsumer selected) {
        this.selected = selected;
    }

    void submit(List<User> values) {
        int previousSize = items.size();
        items = List.of();
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
        return new Holder(UserAdminItemBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false));
    }

    @Override
    public void onBindViewHolder(Holder holder, int position) {
        holder.bind(items.get(position), selected);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static final class Holder extends RecyclerView.ViewHolder {
        private final UserAdminItemBinding binding;

        Holder(UserAdminItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(User user, LongConsumer selected) {
            binding.userAdminItemName.setText(user.name());
            binding.userAdminItemEmail.setText(user.email().orElse(
                    binding.getRoot().getContext().getString(
                            R.string.user_admin_no_email)));
            String organization = user.assignments().stream()
                    .findFirst()
                    .map(Assignment::organizationName)
                    .orElse(binding.getRoot().getContext().getString(
                            R.string.user_admin_no_assignment));
            binding.userAdminItemMeta.setText(binding.getRoot().getContext().getString(
                    R.string.user_admin_item_meta,
                    binding.getRoot().getContext().getString(
                            UserAdministrationText.role(user.role())),
                    organization,
                    binding.getRoot().getContext().getString(
                            user.blocked()
                                    ? R.string.user_admin_status_blocked
                                    : R.string.user_admin_status_active)));
            binding.getRoot().setOnClickListener(ignored -> selected.accept(user.id()));
        }
    }
}
