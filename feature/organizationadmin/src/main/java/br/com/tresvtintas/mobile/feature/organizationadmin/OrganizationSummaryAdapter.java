package br.com.tresvtintas.mobile.feature.organizationadmin;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import br.com.tresvtintas.mobile.core.organizationadmin.OrganizationAdministrationModels.Organization;
import br.com.tresvtintas.mobile.feature.organizationadmin.databinding.OrganizationAdminItemBinding;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

final class OrganizationSummaryAdapter
        extends RecyclerView.Adapter<OrganizationSummaryAdapter.Holder> {
    @FunctionalInterface
    interface Listener {
        void onOrganizationSelected(Organization organization);
    }

    private final List<Organization> items = new ArrayList<>();
    private final Listener listener;

    OrganizationSummaryAdapter(Listener listener) {
        this.listener = Objects.requireNonNull(listener, "Listener is required.");
    }

    void replace(List<Organization> organizations) {
        int previousSize = items.size();
        items.clear();
        if (previousSize > 0) {
            notifyItemRangeRemoved(0, previousSize);
        }
        items.addAll(organizations);
        if (!items.isEmpty()) {
            notifyItemRangeInserted(0, items.size());
        }
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(OrganizationAdminItemBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    final class Holder extends RecyclerView.ViewHolder {
        private final OrganizationAdminItemBinding binding;

        Holder(OrganizationAdminItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Organization organization) {
            binding.organizationName.setText(organization.name());
            binding.organizationSlug.setText(organization.slug());
            binding.organizationStatus.setText(OrganizationAdministrationText.status(
                    binding.getRoot().getContext(), organization.status()));
            binding.organizationMembers.setText(binding.getRoot().getContext().getString(
                    R.string.organization_admin_members_format,
                    organization.activeMemberCount(),
                    organization.managerCount(),
                    organization.salespersonCount(),
                    organization.deliveryDriverCount()));
            binding.getRoot().setOnClickListener(
                    ignored -> listener.onOrganizationSelected(organization));
        }
    }
}
