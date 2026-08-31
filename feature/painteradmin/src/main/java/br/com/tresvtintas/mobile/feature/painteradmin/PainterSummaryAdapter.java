package br.com.tresvtintas.mobile.feature.painteradmin;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.recyclerview.widget.RecyclerView;
import br.com.tresvtintas.mobile.core.painteradmin.PainterAdministrationModels.Painter;
import br.com.tresvtintas.mobile.feature.painteradmin.databinding.PainterAdminItemBinding;
import java.math.RoundingMode;
import java.util.List;
import java.util.function.LongConsumer;

final class PainterSummaryAdapter
        extends RecyclerView.Adapter<PainterSummaryAdapter.Holder> {
    private final LongConsumer selected;
    private List<Painter> items = List.of();

    PainterSummaryAdapter(LongConsumer selected) {
        this.selected = selected;
    }

    void submit(List<Painter> values) {
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
        return new Holder(PainterAdminItemBinding.inflate(
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
        private final PainterAdminItemBinding binding;

        Holder(PainterAdminItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Painter item, LongConsumer selected) {
            binding.painterAdminItemName.setText(item.name());
            binding.painterAdminItemContext.setText(
                    binding.getRoot().getContext().getString(
                            R.string.painter_admin_item_context_format,
                            item.organization().name(),
                            item.specialty().orElse(
                                    binding.getRoot().getContext().getString(
                                            R.string.painter_admin_no_specialty))));
            binding.painterAdminItemMeta.setText(
                    binding.getRoot().getContext().getString(
                            R.string.painter_admin_rate_and_status,
                            item.commissionRate()
                                    .setScale(2, RoundingMode.UNNECESSARY)
                                    .toPlainString(),
                            binding.getRoot().getContext().getString(
                                    PainterAdministrationText.status(
                                            item.status()))));
            binding.getRoot().setOnClickListener(
                    ignored -> selected.accept(item.id()));
        }
    }
}
