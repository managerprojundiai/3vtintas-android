package br.com.tresvtintas.mobile.feature.catalog;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import br.com.tresvtintas.mobile.core.catalog.CatalogProduct;
import br.com.tresvtintas.mobile.feature.catalog.databinding.CatalogItemProductBinding;
import java.text.NumberFormat;
import java.util.Locale;

final class CatalogProductAdapter
        extends ListAdapter<CatalogProduct, CatalogProductAdapter.ProductViewHolder> {
    private static final DiffUtil.ItemCallback<CatalogProduct> DIFFERENCE =
            new DiffUtil.ItemCallback<>() {
                @Override
                public boolean areItemsTheSame(
                        @NonNull CatalogProduct oldItem,
                        @NonNull CatalogProduct newItem) {
                    return oldItem.id() == newItem.id();
                }

                @Override
                public boolean areContentsTheSame(
                        @NonNull CatalogProduct oldItem,
                        @NonNull CatalogProduct newItem) {
                    return oldItem.equals(newItem);
                }
            };

    CatalogProductAdapter() {
        super(DIFFERENCE);
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType) {
        CatalogItemProductBinding binding = CatalogItemProductBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false);
        return new ProductViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(
            @NonNull ProductViewHolder holder,
            int position) {
        holder.bind(getItem(position));
    }

    static final class ProductViewHolder extends RecyclerView.ViewHolder {
        private final CatalogItemProductBinding binding;
        private final NumberFormat currency =
                NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR"));

        ProductViewHolder(CatalogItemProductBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(CatalogProduct product) {
            String price = product.price()
                    .map(currency::format)
                    .orElse(binding.getRoot().getContext().getString(
                            R.string.catalog_price_restricted));
            String category = product.category()
                    .map(item -> item.name())
                    .orElse(binding.getRoot().getContext().getString(
                            R.string.catalog_without_category));
            String stock = product.stock()
                    .map(value -> binding.getRoot().getContext().getString(
                            R.string.catalog_stock_value,
                            value))
                    .orElse(binding.getRoot().getContext().getString(
                            R.string.catalog_stock_unavailable));
            String brand = product.brand()
                    .orElse(binding.getRoot().getContext().getString(
                            R.string.catalog_without_brand));
            binding.productName.setText(product.name());
            binding.productCategory.setText(binding.getRoot().getContext().getString(
                    R.string.catalog_product_origin,
                    brand,
                    category));
            binding.productPrice.setText(price);
            binding.productPrice.setImportantForAccessibility(
                    product.price().isPresent()
                            ? View.IMPORTANT_FOR_ACCESSIBILITY_YES
                            : View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            binding.productStock.setText(stock);
            binding.productStock.setVisibility(
                    product.stock().isPresent() ? View.VISIBLE : View.GONE);
            String sku = product.sku()
                    .map(value -> binding.getRoot().getContext().getString(
                            R.string.catalog_sku_value, value))
                    .orElse(binding.getRoot().getContext().getString(
                            R.string.catalog_sku_unavailable));
            String volume = product.volume()
                    .map(value -> binding.getRoot().getContext().getString(
                            R.string.catalog_product_volume, value))
                    .orElse("");
            String unit = product.unit()
                    .map(value -> binding.getRoot().getContext().getString(
                            R.string.catalog_product_unit, value))
                    .orElse("");
            binding.productSku.setText(binding.getRoot().getContext().getString(
                    R.string.catalog_product_details,
                    sku,
                    volume,
                    unit));
            binding.getRoot().setContentDescription(
                    binding.getRoot().getContext().getString(
                            R.string.catalog_product_accessibility,
                            product.name(),
                            category,
                            price,
                            stock));
        }
    }
}
