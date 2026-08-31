package br.com.tresvtintas.mobile.feature.delivery;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import br.com.tresvtintas.mobile.core.delivery.DeliveryManagementSummary;
import br.com.tresvtintas.mobile.feature.delivery.databinding.DeliveryManagementItemSummaryBinding;

final class DeliveryManagementSummaryAdapter
        extends ListAdapter<
                DeliveryManagementSummary,
                DeliveryManagementSummaryAdapter.Holder> {
    @FunctionalInterface
    interface Listener {
        void onDeliveryManagementSelected(
                long organizationId,
                long orderId);
    }

    private static final DiffUtil.ItemCallback<DeliveryManagementSummary>
            DIFFERENCE = new DiffUtil.ItemCallback<>() {
                @Override
                public boolean areItemsTheSame(
                        @NonNull DeliveryManagementSummary oldItem,
                        @NonNull DeliveryManagementSummary newItem) {
                    return oldItem.order().id() == newItem.order().id();
                }

                @Override
                public boolean areContentsTheSame(
                        @NonNull DeliveryManagementSummary oldItem,
                        @NonNull DeliveryManagementSummary newItem) {
                    return oldItem.equals(newItem);
                }
            };

    private final Listener listener;

    DeliveryManagementSummaryAdapter(Listener listener) {
        super(DIFFERENCE);
        this.listener = listener;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType) {
        return new Holder(DeliveryManagementItemSummaryBinding.inflate(
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
        private final DeliveryManagementItemSummaryBinding binding;

        Holder(DeliveryManagementItemSummaryBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(
                DeliveryManagementSummary value,
                Listener listener) {
            binding.deliveryManagementItemTitle.setText(
                    binding.getRoot().getResources().getQuantityString(
                            R.plurals.delivery_management_order,
                            value.order().itemCount(),
                            value.order().id(),
                            value.order().itemCount()));
            binding.deliveryManagementItemCustomer.setText(
                    value.customer()
                            .map(DeliveryManagementSummary.Customer::name)
                            .orElseGet(() -> binding.getRoot()
                                    .getContext()
                                    .getString(
                                            R.string.delivery_not_informed)));
            binding.deliveryManagementItemSchedule.setText(
                    DeliveryText.schedule(
                            binding.getRoot().getContext(),
                            value.delivery().flatMap(
                                    DeliveryManagementSummary.ManagedDelivery
                                            ::scheduledAt)));
            binding.deliveryManagementItemDriver.setText(
                    value.delivery()
                            .flatMap(
                                    DeliveryManagementSummary.ManagedDelivery
                                        ::assignedDriver)
                            .map(driver -> binding.getRoot()
                                    .getContext()
                                    .getString(
                                            R.string.delivery_driver_value,
                                            driver.displayName()))
                            .orElseGet(() -> binding.getRoot()
                                    .getContext()
                                    .getString(
                                            R.string.delivery_unassigned)));
            binding.getRoot().setOnClickListener(ignored ->
                    listener.onDeliveryManagementSelected(
                            value.organization().id(),
                            value.order().id()));
        }
    }
}
