package br.com.tresvtintas.mobile.feature.customer;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import br.com.tresvtintas.mobile.core.customer.CustomerSummary;
import br.com.tresvtintas.mobile.feature.customer.databinding.CustomerItemSummaryBinding;
import java.util.Objects;

final class CustomerSummaryAdapter
        extends ListAdapter<CustomerSummary, CustomerSummaryAdapter.CustomerViewHolder> {
    private static final DiffUtil.ItemCallback<CustomerSummary> DIFFERENCE =
            new DiffUtil.ItemCallback<>() {
                @Override
                public boolean areItemsTheSame(
                        @NonNull CustomerSummary oldItem,
                        @NonNull CustomerSummary newItem) {
                    return oldItem.id() == newItem.id();
                }

                @Override
                public boolean areContentsTheSame(
                        @NonNull CustomerSummary oldItem,
                        @NonNull CustomerSummary newItem) {
                    return oldItem.equals(newItem);
                }
            };
    @FunctionalInterface
    interface CustomerSelectionListener {
        void onCustomerSelected(long customerId);
    }

    private final CustomerSelectionListener selectionListener;
    CustomerSummaryAdapter(CustomerSelectionListener selectionListener) {
        super(DIFFERENCE);
        this.selectionListener = Objects.requireNonNull(
                selectionListener,
                "Customer selection listener is required.");
        setHasStableIds(true);
    }

    @Override
    public long getItemId(int position) {
        return getItem(position).id();
    }

    @NonNull
    @Override
    public CustomerViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType) {
        return new CustomerViewHolder(CustomerItemSummaryBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false));
    }

    @Override
    public void onBindViewHolder(
            @NonNull CustomerViewHolder holder,
            int position) {
        holder.bind(getItem(position), selectionListener);
    }

    static final class CustomerViewHolder extends RecyclerView.ViewHolder {
        private final CustomerItemSummaryBinding binding;

        CustomerViewHolder(CustomerItemSummaryBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(
                CustomerSummary customer,
                CustomerSelectionListener listener) {
            binding.customerItemName.setText(customer.name());
            String phone = customer.phone().orElse("");
            String email = customer.email().orElse("");
            String contact = contact(binding, phone, email);
            binding.customerItemContact.setText(contact);
            binding.customerItemLocation.setText(location(binding, customer));
            binding.getRoot().setContentDescription(
                    customer.name() + ". " + contact);
            binding.getRoot().setOnClickListener(
                    ignored -> listener.onCustomerSelected(customer.id()));
        }

        private static String contact(
                CustomerItemSummaryBinding binding,
                String phone,
                String email) {
            if (!phone.isBlank() && !email.isBlank()) {
                return binding.getRoot().getContext().getString(
                        R.string.customer_contact_value,
                        phone,
                        email);
            }
            if (!phone.isBlank()) {
                return phone;
            }
            if (!email.isBlank()) {
                return email;
            }
            return binding.getRoot().getContext().getString(
                    R.string.customer_not_informed);
        }

        private static String location(
                CustomerItemSummaryBinding binding,
                CustomerSummary customer) {
            String city = customer.city().orElse("");
            String state = customer.state().orElse("");
            if (!city.isBlank() && !state.isBlank()) {
                return binding.getRoot().getContext().getString(
                        R.string.customer_city_state_value,
                        city,
                        state);
            }
            if (!city.isBlank()) {
                return city;
            }
            if (!state.isBlank()) {
                return state;
            }
            return binding.getRoot().getContext().getString(
                    R.string.customer_not_informed);
        }
    }
}
