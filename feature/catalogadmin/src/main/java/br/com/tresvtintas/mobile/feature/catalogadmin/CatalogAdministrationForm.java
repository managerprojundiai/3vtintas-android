package br.com.tresvtintas.mobile.feature.catalogadmin;

import android.content.Context;
import android.view.View;
import android.widget.ArrayAdapter;
import br.com.tresvtintas.mobile.core.catalogadmin.CatalogAdministrationModels.Category;
import br.com.tresvtintas.mobile.core.catalogadmin.CatalogAdministrationModels.Knowledge;
import br.com.tresvtintas.mobile.core.catalogadmin.CatalogAdministrationModels.KnowledgeDraft;
import br.com.tresvtintas.mobile.core.catalogadmin.CatalogAdministrationModels.Product;
import br.com.tresvtintas.mobile.core.catalogadmin.CatalogAdministrationModels.ProductDraft;
import br.com.tresvtintas.mobile.feature.catalogadmin.databinding.CatalogAdminKnowledgeDialogBinding;
import br.com.tresvtintas.mobile.feature.catalogadmin.databinding.CatalogAdminProductDialogBinding;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalLong;

final class CatalogAdministrationForm {
    private CatalogAdministrationForm() {
        throw new AssertionError("No instances.");
    }

    static void configureProduct(
            Context context,
            CatalogAdminProductDialogBinding binding,
            List<Category> categories,
            Product product) {
        List<String> labels = new ArrayList<>();
        labels.add(context.getString(R.string.catalog_admin_no_category));
        categories.stream().map(Category::name).forEach(labels::add);
        binding.category.setAdapter(new ArrayAdapter<>(
                context,
                android.R.layout.simple_spinner_dropdown_item,
                labels));
        if (product == null) {
            binding.stock.setText("0");
            return;
        }
        binding.name.setText(product.name());
        binding.price.setText(product.price());
        binding.stock.setText(String.format(
                Locale.getDefault(),
                "%d",
                product.stock()));
        binding.sku.setText(product.sku().orElse(""));
        binding.brand.setText(product.brand().orElse(""));
        binding.unit.setText(product.unit().orElse(""));
        binding.volume.setText(product.volume().orElse(""));
        binding.description.setText(product.description().orElse(""));
        binding.imageUrl.setText(product.imageUrl().orElse(""));
        binding.imagePageUrl.setText(product.imagePageUrl().orElse(""));
        product.category().ifPresent(selected -> {
            for (int index = 0; index < categories.size(); index++) {
                if (categories.get(index).id() == selected.id()) {
                    binding.category.setSelection(index + 1);
                    break;
                }
            }
        });
        binding.knowledge.setVisibility(View.VISIBLE);
        binding.changeStatus.setVisibility(View.VISIBLE);
        binding.changeStatus.setText(product.active()
                ? R.string.catalog_admin_deactivate
                : R.string.catalog_admin_activate);
    }

    static Optional<ProductDraft> productDraft(
            CatalogAdminProductDialogBinding binding,
            List<Category> categories) {
        String name = text(binding.name);
        String price = text(binding.price).replace(',', '.');
        String stockText = text(binding.stock);
        if (name.isEmpty()
                || !price.matches("\\d{1,8}\\.\\d{2}")
                || !stockText.matches("\\d{1,10}")) {
            return Optional.empty();
        }
        try {
            int stock = Integer.parseInt(stockText);
            int position = binding.category.getSelectedItemPosition();
            OptionalLong categoryId = position > 0
                    && position <= categories.size()
                    ? OptionalLong.of(categories.get(position - 1).id())
                    : OptionalLong.empty();
            return Optional.of(new ProductDraft(
                    categoryId,
                    name,
                    optional(binding.description),
                    optional(binding.imageUrl),
                    optional(binding.imagePageUrl),
                    optional(binding.sku),
                    optional(binding.unit),
                    optional(binding.volume),
                    price,
                    stock,
                    optional(binding.brand)));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    static void configureKnowledge(
            CatalogAdminKnowledgeDialogBinding binding,
            Knowledge knowledge) {
        binding.sourceDescription.setText(
                knowledge.sourceDescription().orElse(""));
        binding.synonyms.setText(String.join(", ", knowledge.synonyms()));
        binding.application.setText(knowledge.application().orElse(""));
        binding.modeOfUse.setText(knowledge.modeOfUse().orElse(""));
        binding.dilution.setText(knowledge.dilution().orElse(""));
        binding.yield.setText(knowledge.yield().orElse(""));
        binding.intents.setText(String.join(", ", knowledge.intents()));
        binding.salesArgument.setText(knowledge.salesArgument().orElse(""));
        binding.questions.setText(knowledge.questions().orElse(""));
        binding.avoidSelling.setText(knowledge.avoidSelling().orElse(""));
        binding.whatsappReply.setText(knowledge.whatsappReply().orElse(""));
    }

    static KnowledgeDraft knowledgeDraft(
            CatalogAdminKnowledgeDialogBinding binding) {
        return new KnowledgeDraft(
                optional(binding.sourceDescription),
                optional(binding.synonyms),
                optional(binding.application),
                optional(binding.modeOfUse),
                optional(binding.dilution),
                optional(binding.yield),
                optional(binding.intents),
                optional(binding.salesArgument),
                optional(binding.questions),
                optional(binding.avoidSelling),
                optional(binding.whatsappReply));
    }

    private static Optional<String> optional(android.widget.EditText input) {
        return Optional.of(text(input)).filter(value -> !value.isEmpty());
    }

    private static String text(android.widget.EditText input) {
        return input.getText() == null
                ? ""
                : input.getText().toString().strip();
    }
}
