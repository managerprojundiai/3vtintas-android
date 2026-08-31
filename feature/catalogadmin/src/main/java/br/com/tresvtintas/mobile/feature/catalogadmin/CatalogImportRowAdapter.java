package br.com.tresvtintas.mobile.feature.catalogadmin;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import br.com.tresvtintas.mobile.core.catalogadmin.CatalogImportModels.Row;
import br.com.tresvtintas.mobile.feature.catalogadmin.databinding.CatalogImportItemBinding;
import java.util.List;

final class CatalogImportRowAdapter
        extends ListAdapter<Row, CatalogImportRowAdapter.Holder> {
    private static final DiffUtil.ItemCallback<Row> DIFFER =
            new DiffUtil.ItemCallback<>() {
                @Override
                public boolean areItemsTheSame(
                        @NonNull Row oldItem,
                        @NonNull Row newItem) {
                    return oldItem.rowNumber() == newItem.rowNumber();
                }

                @Override
                public boolean areContentsTheSame(
                        @NonNull Row oldItem,
                        @NonNull Row newItem) {
                    return oldItem.equals(newItem);
                }
            };

    CatalogImportRowAdapter() {
        super(DIFFER);
    }

    void replace(List<Row> updated) {
        submitList(List.copyOf(updated));
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType) {
        return new Holder(CatalogImportItemBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false));
    }

    @Override
    public void onBindViewHolder(
            @NonNull Holder holder,
            int position) {
        holder.bind(getItem(position));
    }

    static final class Holder extends RecyclerView.ViewHolder {
        private final CatalogImportItemBinding binding;

        Holder(CatalogImportItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Row row) {
            binding.line.setText(binding.getRoot().getResources().getString(
                    R.string.catalog_import_row_number,
                    row.rowNumber()));
            binding.name.setText(row.preview().name());
            binding.action.setText(action(row));
            binding.details.setText(binding.getRoot().getResources().getString(
                    R.string.catalog_import_row_details,
                    row.preview().sku().orElse("—"),
                    row.preview().price().orElse("—"),
                    row.preview().stock()
                            .map(String::valueOf)
                            .orElse("—"),
                    row.preview().categoryName().orElse("—")));
            binding.message.setText(row.message().orElse(""));
            binding.message.setVisibility(
                    row.message().isPresent()
                            ? android.view.View.VISIBLE
                            : android.view.View.GONE);
        }

        private String action(Row row) {
            int resource = switch (row.action()) {
                case CREATE -> R.string.catalog_import_action_create;
                case UPDATE -> R.string.catalog_import_action_update;
                case SKIP -> R.string.catalog_import_action_skip;
            };
            return binding.getRoot().getResources().getString(resource);
        }
    }
}
