package br.com.tresvtintas.mobile.feature.painteradmin;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.recyclerview.widget.RecyclerView;
import br.com.tresvtintas.mobile.core.painteradmin.PainterAdministrationModels.AccessRequest;
import br.com.tresvtintas.mobile.feature.painteradmin.databinding.PainterAdminItemBinding;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.LongConsumer;

final class AccessRequestSummaryAdapter
        extends RecyclerView.Adapter<AccessRequestSummaryAdapter.Holder> {
    private static final DateTimeFormatter DATE =
            DateTimeFormatter.ofPattern("dd/MM/yyyy")
                    .withZone(ZoneId.of("America/Sao_Paulo"));
    private final LongConsumer selected;
    private List<AccessRequest> items = List.of();

    AccessRequestSummaryAdapter(LongConsumer selected) {
        this.selected = selected;
    }

    void submit(List<AccessRequest> values) {
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

        void bind(AccessRequest item, LongConsumer selected) {
            binding.painterAdminItemName.setText(item.name());
            binding.painterAdminItemContext.setText(
                    item.email().orElse(
                            binding.getRoot().getContext().getString(
                                    R.string.painter_admin_no_email)));
            binding.painterAdminItemMeta.setText(
                    binding.getRoot().getContext().getString(
                            R.string.painter_admin_requested_at,
                            DATE.format(item.createdAt())));
            binding.getRoot().setOnClickListener(
                    ignored -> selected.accept(item.id()));
        }
    }
}
