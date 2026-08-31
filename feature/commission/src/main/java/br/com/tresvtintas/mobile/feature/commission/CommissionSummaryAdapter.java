package br.com.tresvtintas.mobile.feature.commission;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import br.com.tresvtintas.mobile.core.commission.CommissionSummary;
import br.com.tresvtintas.mobile.feature.commission.databinding.CommissionItemSummaryBinding;
import java.util.Objects;

final class CommissionSummaryAdapter
        extends ListAdapter<
                CommissionSummary,
                CommissionSummaryAdapter.Holder> {
    private static final DiffUtil.ItemCallback<CommissionSummary> DIFFERENCE =
            new DiffUtil.ItemCallback<>() {
                @Override
                public boolean areItemsTheSame(
                        @NonNull CommissionSummary oldItem,
                        @NonNull CommissionSummary newItem) {
                    return oldItem.id() == newItem.id();
                }

                @Override
                public boolean areContentsTheSame(
                        @NonNull CommissionSummary oldItem,
                        @NonNull CommissionSummary newItem) {
                    return oldItem.equals(newItem);
                }
            };

    @FunctionalInterface
    interface SelectionListener {
        void onSelected(long commissionId);
    }

    private final SelectionListener listener;

    CommissionSummaryAdapter(SelectionListener listener) {
        super(DIFFERENCE);
        this.listener = Objects.requireNonNull(
                listener,
                "Commission selection listener is required.");
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
        return new Holder(CommissionItemSummaryBinding.inflate(
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
        private final CommissionItemSummaryBinding binding;

        Holder(CommissionItemSummaryBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(
                CommissionSummary commission,
                SelectionListener listener) {
            var context = binding.getRoot().getContext();
            String reference = CommissionText.reference(
                    context,
                    commission.id());
            String status = context.getString(
                    CommissionText.status(commission.status()));
            String amount = CommissionText.money(
                    commission.calculation().amount());
            String recipientName = commission.recipient().name()
                    .orElseGet(() -> context.getString(
                            CommissionText.role(commission.recipient().role())));
            binding.commissionItemReference.setText(reference);
            binding.commissionItemStatus.setText(status);
            binding.commissionItemAmount.setText(amount);
            binding.commissionItemRecipient.setText(context.getString(
                    R.string.commission_item_recipient,
                    recipientName,
                    context.getString(
                            CommissionText.role(
                                    commission.recipient().role()))));
            String origin = commission.order()
                    .map(order -> context.getString(
                            R.string.commission_item_origin,
                            order.id(),
                            context.getString(
                                    CommissionText.kind(commission.kind()))))
                    .orElseGet(() -> context.getString(
                            R.string.commission_item_without_order));
            if (commission.organization().isPresent()) {
                origin = origin
                        + "\n"
                        + context.getString(
                                R.string.commission_item_store,
                                commission.organization()
                                        .orElseThrow()
                                        .name());
            }
            binding.commissionItemOrigin.setText(origin);
            binding.getRoot().setContentDescription(
                    reference + ". " + status + ". " + amount);
            binding.getRoot().setOnClickListener(
                    ignored -> listener.onSelected(commission.id()));
        }
    }
}
