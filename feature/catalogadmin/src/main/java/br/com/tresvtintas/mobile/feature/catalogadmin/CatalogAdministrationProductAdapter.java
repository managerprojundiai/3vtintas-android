package br.com.tresvtintas.mobile.feature.catalogadmin;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import br.com.tresvtintas.mobile.core.catalogadmin.CatalogAdministrationModels.Product;
import br.com.tresvtintas.mobile.feature.catalogadmin.databinding.CatalogAdminItemBinding;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

final class CatalogAdministrationProductAdapter
        extends RecyclerView.Adapter<CatalogAdministrationProductAdapter.Holder> {
    @FunctionalInterface
    interface Listener {
        void onProductSelected(Product product);
    }

    private final List<Product> items = new ArrayList<>();
    private final Listener listener;

    CatalogAdministrationProductAdapter(Listener listener) {
        this.listener = Objects.requireNonNull(listener, "Listener is required.");
    }

    void replace(List<Product> products) {
        int previousSize = items.size();
        items.clear();
        if (previousSize > 0) {
            notifyItemRangeRemoved(0, previousSize);
        }
        items.addAll(products);
        if (!items.isEmpty()) {
            notifyItemRangeInserted(0, items.size());
        }
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType) {
        return new Holder(CatalogAdminItemBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    final class Holder extends RecyclerView.ViewHolder {
        private final CatalogAdminItemBinding binding;

        Holder(CatalogAdminItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Product product) {
            String sku = product.sku().orElse("—");
            String brand = product.brand().orElse("Sem marca");
            binding.productName.setText(product.name());
            binding.productStatus.setText(product.active()
                    ? R.string.catalog_admin_active
                    : R.string.catalog_admin_inactive);
            binding.productSummary.setText(binding.getRoot().getContext().getString(
                    R.string.catalog_admin_product_summary,
                    sku,
                    brand,
                    product.stock()));
            binding.productPrice.setText(binding.getRoot().getContext().getString(
                    R.string.catalog_admin_price,
                    product.price().replace('.', ',')));
            binding.getRoot().setOnClickListener(
                    ignored -> listener.onProductSelected(product));
        }
    }
}
