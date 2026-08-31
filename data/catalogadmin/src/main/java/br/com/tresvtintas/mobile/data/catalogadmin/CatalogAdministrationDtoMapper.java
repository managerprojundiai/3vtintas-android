package br.com.tresvtintas.mobile.data.catalogadmin;

import br.com.tresvtintas.mobile.core.catalogadmin.CatalogAdministrationModels.Category;
import br.com.tresvtintas.mobile.core.catalogadmin.CatalogAdministrationModels.CategoryMutation;
import br.com.tresvtintas.mobile.core.catalogadmin.CatalogAdministrationModels.Knowledge;
import br.com.tresvtintas.mobile.core.catalogadmin.CatalogAdministrationModels.Mutation;
import br.com.tresvtintas.mobile.core.catalogadmin.CatalogAdministrationModels.Page;
import br.com.tresvtintas.mobile.core.catalogadmin.CatalogAdministrationModels.Product;
import br.com.tresvtintas.mobile.core.network.dto.CatalogAdministrationDtos;
import java.time.Instant;
import java.util.Optional;

final class CatalogAdministrationDtoMapper {
    private CatalogAdministrationDtoMapper() {
        throw new AssertionError("No instances.");
    }

    static Page page(CatalogAdministrationDtos.Page dto) {
        return new Page(
                dto.items().stream()
                        .map(CatalogAdministrationDtoMapper::product)
                        .toList(),
                Optional.ofNullable(dto.nextCursor()));
    }

    static Product product(CatalogAdministrationDtos.Product dto) {
        return new Product(
                dto.id(),
                Optional.ofNullable(dto.category())
                        .map(category -> new Category(
                                category.id(),
                                category.name(),
                                Optional.empty())),
                dto.name(),
                Optional.ofNullable(dto.description()),
                Optional.ofNullable(dto.imageUrl()),
                Optional.ofNullable(dto.imagePageUrl()),
                Optional.ofNullable(dto.sku()),
                Optional.ofNullable(dto.unit()),
                Optional.ofNullable(dto.volume()),
                dto.price(),
                dto.stock(),
                Optional.ofNullable(dto.brand()),
                dto.isActive(),
                dto.revision(),
                knowledge(dto.knowledge()),
                Instant.parse(dto.createdAt()),
                Instant.parse(dto.updatedAt()));
    }

    static Category category(CatalogAdministrationDtos.Category dto) {
        return new Category(
                dto.id(),
                dto.name(),
                Optional.ofNullable(dto.description()));
    }

    static Mutation mutation(
            CatalogAdministrationDtos.Mutation dto,
            boolean replayed) {
        return new Mutation(
                dto.resourceId(),
                dto.revision(),
                dto.changed(),
                replayed);
    }

    static CategoryMutation categoryMutation(
            CatalogAdministrationDtos.CategoryMutation dto,
            boolean replayed) {
        return new CategoryMutation(
                dto.resourceId(),
                dto.changed(),
                replayed);
    }

    private static Knowledge knowledge(
            CatalogAdministrationDtos.Knowledge dto) {
        return new Knowledge(
                Optional.ofNullable(dto.sourceType()),
                Optional.ofNullable(dto.sourceDescription()),
                dto.synonyms(),
                Optional.ofNullable(dto.application()),
                Optional.ofNullable(dto.modeOfUse()),
                Optional.ofNullable(dto.dilution()),
                Optional.ofNullable(dto.yield()),
                dto.intents(),
                Optional.ofNullable(dto.salesArgument()),
                Optional.ofNullable(dto.questions()),
                Optional.ofNullable(dto.avoidSelling()),
                Optional.ofNullable(dto.whatsappReply()),
                dto.confidence());
    }
}
