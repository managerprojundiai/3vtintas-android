package br.com.tresvtintas.mobile.feature.delivery;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import br.com.tresvtintas.mobile.core.delivery.DeliverySummary;
import br.com.tresvtintas.mobile.feature.delivery.databinding.DeliveryItemSummaryBinding;
import java.util.Objects;

final class DeliverySummaryAdapter
        extends ListAdapter<DeliverySummary, DeliverySummaryAdapter.Holder> {
    private static final DiffUtil.ItemCallback<DeliverySummary> DIFFERENCE =
            new DiffUtil.ItemCallback<>() {
                @Override
                public boolean areItemsTheSame(
                        @NonNull DeliverySummary oldItem,
                        @NonNull DeliverySummary newItem) {
                    return oldItem.id() == newItem.id();
                }

                @Override
                public boolean areContentsTheSame(
                        @NonNull DeliverySummary oldItem,
                        @NonNull DeliverySummary newItem) {
                    return oldItem.equals(newItem);
                }
            };

    @FunctionalInterface
    interface SelectionListener {
        void onSelected(long deliveryId);
    }

    private final SelectionListener listener;

    DeliverySummaryAdapter(SelectionListener listener) {
        super(DIFFERENCE);
        this.listener = Objects.requireNonNull(
                listener,
                "Delivery listener is required.");
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
        return new Holder(DeliveryItemSummaryBinding.inflate(
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
        private final DeliveryItemSummaryBinding binding;

        Holder(DeliveryItemSummaryBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(
                DeliverySummary delivery,
                SelectionListener listener) {
            var context = binding.getRoot().getContext();
            String reference = context.getString(
                    R.string.delivery_order_reference,
                    delivery.order().id(),
                    delivery.id());
            String status = DeliveryText.status(
                    context,
                    delivery.status());
            String customer = delivery.customer()
                    .map(value -> context.getString(
                            R.string.delivery_customer_value,
                            value.name(),
                            DeliveryText.place(
                                    context,
                                    value.city(),
                                    value.state())))
                    .orElseGet(() -> context.getString(
                            R.string.delivery_not_informed));
            String organization = delivery.organization()
                    .map(DeliverySummary.Organization::name)
                    .orElseGet(() -> context.getString(
                            R.string.delivery_not_informed));
            String driver = delivery.assignedDriver()
                    .flatMap(DeliverySummary.Driver::name)
                    .orElseGet(() -> context.getString(
                            R.string.delivery_unassigned));
            binding.deliveryItemReference.setText(reference);
            binding.deliveryItemStatus.setText(status);
            binding.deliveryItemCustomer.setText(customer);
            binding.deliveryItemSchedule.setText(context.getString(
                    R.string.delivery_schedule_value,
                    DeliveryText.schedule(
                            context,
                            delivery.scheduledAt())));
            binding.deliveryItemMeta.setText(context.getString(
                    R.string.delivery_list_meta_value,
                    organization,
                    driver));
            binding.getRoot().setContentDescription(
                    reference + ". " + status + ". " + customer);
            binding.getRoot().setOnClickListener(
                    ignored -> listener.onSelected(delivery.id()));
        }
    }
}
