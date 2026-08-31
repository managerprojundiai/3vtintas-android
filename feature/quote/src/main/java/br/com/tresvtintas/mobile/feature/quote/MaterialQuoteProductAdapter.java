package br.com.tresvtintas.mobile.feature.quote;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import br.com.tresvtintas.mobile.core.catalog.CatalogProduct;
import br.com.tresvtintas.mobile.feature.quote.databinding.QuoteProductPickerItemBinding;
import java.text.NumberFormat;
import java.util.Locale;

final class MaterialQuoteProductAdapter extends ListAdapter<
        CatalogProduct,
        MaterialQuoteProductAdapter.ProductViewHolder> {
    @FunctionalInterface
    interface Listener {
        void onProductSelected(CatalogProduct product);
    }

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

    private final Listener listener;

    MaterialQuoteProductAdapter(Listener listener) {
        super(DIFFERENCE);
        this.listener = listener;
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType) {
        return new ProductViewHolder(QuoteProductPickerItemBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false));
    }

    @Override
    public void onBindViewHolder(
            @NonNull ProductViewHolder holder,
            int position) {
        holder.bind(getItem(position));
    }

    final class ProductViewHolder extends RecyclerView.ViewHolder {
        private final QuoteProductPickerItemBinding binding;
        private final NumberFormat currency = NumberFormat.getCurrencyInstance(
                Locale.forLanguageTag("pt-BR"));

        ProductViewHolder(QuoteProductPickerItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(CatalogProduct product) {
            String origin = product.brand().orElseGet(() ->
                    binding.getRoot().getContext().getString(
                            R.string.quote_product_brand_unknown));
            String category = product.category()
                    .map(value -> value.name())
                    .orElse("");
            String sku = product.sku()
                    .map(value -> binding.getRoot().getContext().getString(
                            R.string.quote_product_sku, value))
                    .orElse(binding.getRoot().getContext().getString(
                            R.string.quote_product_sku_unknown));
            String packageLabel = product.volume()
                    .orElseGet(() -> product.unit().orElse(""));
            String originLabel = category.isEmpty()
                    ? origin
                    : binding.getRoot().getContext().getString(
                            R.string.quote_product_meta,
                            origin,
                            category);
            String detailsLabel = packageLabel.isEmpty()
                    ? sku
                    : binding.getRoot().getContext().getString(
                            R.string.quote_product_meta,
                            sku,
                            packageLabel);
            binding.quoteProductItemOrigin.setText(originLabel);
            binding.quoteProductItemName.setText(product.name());
            binding.quoteProductItemDetails.setText(detailsLabel);
            binding.quoteProductItemPrice.setText(product.price()
                    .map(currency::format)
                    .orElse(binding.getRoot().getContext().getString(
                            R.string.quote_price_unavailable)));
            binding.getRoot().setOnClickListener(
                    ignored -> listener.onProductSelected(product));
            binding.getRoot().setContentDescription(
                    binding.getRoot().getContext().getString(
                            R.string.quote_product_accessibility,
                            product.name(),
                            sku,
                            binding.quoteProductItemPrice.getText()));
        }
    }
}
