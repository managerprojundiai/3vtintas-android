package br.com.tresvtintas.mobile.feature.order;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import br.com.tresvtintas.mobile.core.order.OrderSummary;
import br.com.tresvtintas.mobile.feature.order.databinding.OrderItemSummaryBinding;
import java.util.Objects;

final class OrderSummaryAdapter extends ListAdapter<OrderSummary, OrderSummaryAdapter.Holder> {
    private static final DiffUtil.ItemCallback<OrderSummary> DIFFERENCE = new DiffUtil.ItemCallback<>() {
        @Override
        public boolean areItemsTheSame(
                @NonNull OrderSummary oldItem,
                @NonNull OrderSummary newItem) {
            return oldItem.id() == newItem.id();
        }

        @Override
        public boolean areContentsTheSame(
                @NonNull OrderSummary oldItem,
                @NonNull OrderSummary newItem) {
            return oldItem.equals(newItem);
        }
    };

    @FunctionalInterface
    interface SelectionListener {
        void onSelected(long orderId);
    }

    private final SelectionListener listener;

    OrderSummaryAdapter(SelectionListener listener) {
        super(DIFFERENCE);
        this.listener = Objects.requireNonNull(listener, "Order listener is required.");
        setHasStableIds(true);
    }

    @Override
    public long getItemId(int position) {
        return getItem(position).id();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(OrderItemSummaryBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        holder.bind(getItem(position), listener);
    }

    static final class Holder extends RecyclerView.ViewHolder {
        private final OrderItemSummaryBinding binding;

        Holder(OrderItemSummaryBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(OrderSummary order, SelectionListener listener) {
            var context = binding.getRoot().getContext();
            binding.orderItemReference.setText(OrderText.reference(context, order.id()));
            binding.orderItemStatus.setText(OrderText.status(order.status()));
            binding.orderItemTotal.setText(order.total().map(total ->
                    context.getResources().getQuantityString(
                            R.plurals.order_total_items,
                            order.itemCount(),
                            OrderText.money(total),
                            order.itemCount())).orElseGet(() ->
                    context.getResources().getQuantityString(
                            R.plurals.order_restricted_total_items,
                            order.itemCount(),
                            order.itemCount())));
            binding.orderItemCustomer.setText(order.customer().map(value ->
                    context.getString(R.string.order_customer_value, value.name()))
                    .orElseGet(() -> context.getString(R.string.order_not_informed)));
            binding.orderItemMeta.setText(context.getString(R.string.order_created_value,
                    OrderText.date(order.createdAt())));
            binding.getRoot().setContentDescription(OrderText.reference(context, order.id()) + ". "
                    + context.getString(OrderText.status(order.status())) + ". "
                    + binding.orderItemTotal.getText());
            binding.getRoot().setOnClickListener(ignored -> listener.onSelected(order.id()));
        }
    }
}
