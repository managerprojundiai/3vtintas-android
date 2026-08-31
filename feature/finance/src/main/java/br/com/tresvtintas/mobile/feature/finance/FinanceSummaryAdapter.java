package br.com.tresvtintas.mobile.feature.finance;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import br.com.tresvtintas.mobile.core.finance.FinanceSummary;
import br.com.tresvtintas.mobile.feature.finance.databinding.FinanceItemSummaryBinding;
import java.util.Objects;

final class FinanceSummaryAdapter
        extends ListAdapter<
                FinanceSummary,
                FinanceSummaryAdapter.Holder> {
    private static final DiffUtil.ItemCallback<FinanceSummary> DIFFERENCE =
            new DiffUtil.ItemCallback<>() {
                @Override
                public boolean areItemsTheSame(
                        @NonNull FinanceSummary oldItem,
                        @NonNull FinanceSummary newItem) {
                    return oldItem.id() == newItem.id();
                }

                @Override
                public boolean areContentsTheSame(
                        @NonNull FinanceSummary oldItem,
                        @NonNull FinanceSummary newItem) {
                    return oldItem.equals(newItem);
                }
            };

    @FunctionalInterface
    interface SelectionListener {
        void onSelected(long entryId);
    }

    private final SelectionListener listener;

    FinanceSummaryAdapter(SelectionListener listener) {
        super(DIFFERENCE);
        this.listener = Objects.requireNonNull(
                listener,
                "Finance selection listener is required.");
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
        return new Holder(FinanceItemSummaryBinding.inflate(
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
        private final FinanceItemSummaryBinding binding;

        Holder(FinanceItemSummaryBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(
                FinanceSummary entry,
                SelectionListener listener) {
            var context = binding.getRoot().getContext();
            String status = context.getString(FinanceText.status(entry.status()));
            String amount = FinanceText.money(entry.amount());
            binding.financeItemTitle.setText(entry.title());
            binding.financeItemStatus.setText(status);
            binding.financeItemAmount.setText(amount);
            binding.financeItemType.setText(
                    FinanceText.type(entry.type()));
            String customer = entry.customer()
                    .map(value -> context.getString(
                            R.string.finance_item_customer,
                            value.name()))
                    .orElseGet(() -> context.getString(
                            R.string.finance_item_no_customer));
            String due = entry.dueAt()
                    .map(value -> context.getString(
                            R.string.finance_item_due,
                            FinanceText.date(value)))
                    .orElseGet(() -> context.getString(
                            R.string.finance_item_no_due));
            String organization = entry.organization()
                    .map(value -> context.getString(
                            R.string.finance_item_organization,
                            value.name()) + "\n")
                    .orElse("");
            binding.financeItemContext.setText(context.getString(
                    R.string.finance_item_context,
                    organization + customer,
                    due));
            binding.getRoot().setContentDescription(
                    entry.title() + ". " + status + ". " + amount);
            binding.getRoot().setOnClickListener(
                    ignored -> listener.onSelected(entry.id()));
        }
    }
}
