package br.com.tresvtintas.mobile.feature.corporatefinance;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import br.com.tresvtintas.mobile.core.corporatefinance.CorporateFinanceOrganization;
import br.com.tresvtintas.mobile.feature.corporatefinance.databinding.CorporateFinanceItemOrganizationBinding;
import java.util.Objects;

final class CorporateFinanceOrganizationAdapter
        extends ListAdapter<
                CorporateFinanceOrganization,
                CorporateFinanceOrganizationAdapter.Holder> {
    private static final DiffUtil.ItemCallback<CorporateFinanceOrganization>
            DIFFERENCE = new DiffUtil.ItemCallback<>() {
                @Override
                public boolean areItemsTheSame(
                        @NonNull CorporateFinanceOrganization oldItem,
                        @NonNull CorporateFinanceOrganization newItem) {
                    return oldItem.id() == newItem.id();
                }

                @Override
                public boolean areContentsTheSame(
                        @NonNull CorporateFinanceOrganization oldItem,
                        @NonNull CorporateFinanceOrganization newItem) {
                    return oldItem.equals(newItem);
                }
            };

    @FunctionalInterface
    interface SelectionListener {
        void onSelected(CorporateFinanceOrganization organization);
    }

    private final SelectionListener listener;

    CorporateFinanceOrganizationAdapter(SelectionListener listener) {
        super(DIFFERENCE);
        this.listener = Objects.requireNonNull(
                listener,
                "Corporate finance organization listener is required.");
        setHasStableIds(true);
    }

    @Override
    public long getItemId(int position) {
        return getItem(position).id();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType) {
        return new Holder(CorporateFinanceItemOrganizationBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false));
    }

    @Override
    public void onBindViewHolder(
            @NonNull Holder holder,
            int position) {
        holder.bind(getItem(position), listener);
    }

    static final class Holder extends RecyclerView.ViewHolder {
        private final CorporateFinanceItemOrganizationBinding binding;

        Holder(CorporateFinanceItemOrganizationBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(
                CorporateFinanceOrganization organization,
                SelectionListener listener) {
            binding.corporateFinanceOrganizationName.setText(
                    organization.name());
            binding.getRoot().setContentDescription(
                    binding.getRoot().getContext().getString(
                            R.string.corporate_finance_open_organization,
                            organization.name()));
            binding.getRoot().setOnClickListener(
                    ignored -> listener.onSelected(organization));
        }
    }
}
