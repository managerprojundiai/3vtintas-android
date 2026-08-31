package br.com.tresvtintas.mobile.core.catalogadmin;

import br.com.tresvtintas.mobile.core.catalogadmin.CatalogAdministrationModels.Category;
import br.com.tresvtintas.mobile.core.catalogadmin.CatalogAdministrationModels.CategoryMutation;
import br.com.tresvtintas.mobile.core.catalogadmin.CatalogAdministrationModels.KnowledgeDraft;
import br.com.tresvtintas.mobile.core.catalogadmin.CatalogAdministrationModels.Mutation;
import br.com.tresvtintas.mobile.core.catalogadmin.CatalogAdministrationModels.Page;
import br.com.tresvtintas.mobile.core.catalogadmin.CatalogAdministrationModels.Product;
import br.com.tresvtintas.mobile.core.catalogadmin.CatalogAdministrationModels.ProductDraft;
import java.util.List;
import java.util.Optional;

public interface CatalogAdministrationRepository {
    Page products(
            CatalogAdministrationQuery query,
            Optional<String> cursor) throws CatalogAdministrationException;

    Product product(long productId) throws CatalogAdministrationException;

    List<Category> categories() throws CatalogAdministrationException;

    Mutation create(ProductDraft product, String key)
            throws CatalogAdministrationException;

    Mutation update(
            long productId,
            int revision,
            ProductDraft product,
            String key) throws CatalogAdministrationException;

    Mutation setActive(
            long productId,
            int revision,
            boolean active,
            String key) throws CatalogAdministrationException;

    Mutation updateKnowledge(
            long productId,
            int revision,
            KnowledgeDraft knowledge,
            String key) throws CatalogAdministrationException;

    CategoryMutation createCategory(
            String name,
            Optional<String> description,
            String key) throws CatalogAdministrationException;
}
