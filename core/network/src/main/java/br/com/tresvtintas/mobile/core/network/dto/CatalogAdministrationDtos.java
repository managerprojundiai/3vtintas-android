package br.com.tresvtintas.mobile.core.network.dto;

import java.util.List;
import java.util.Set;

public final class CatalogAdministrationDtos {
    private static final int MAXIMUM_TEXT = 20_000;
    private static final int MINIMUM_REVISION = 1;
    private static final Set<String> SOURCE_TYPES = Set.of(
            "import",
            "manual",
            "backfill",
            "sync");

    private CatalogAdministrationDtos() {
        throw new AssertionError("No instances.");
    }

    public static boolean supportsSourceType(String sourceType) {
        return sourceType == null || SOURCE_TYPES.contains(sourceType);
    }

    public record Category(long id, String name, String description) {
        public Category {
            id = DtoValidation.requirePositive(id, "Catalog category ID");
            name = DtoValidation.requireText(name, "Catalog category name", 100);
            description = DtoValidation.optionalText(
                    description,
                    "Catalog category description",
                    MAXIMUM_TEXT);
        }
    }

    public record ProductCategory(long id, String name) {
        public ProductCategory {
            id = DtoValidation.requirePositive(id, "Product category ID");
            name = DtoValidation.requireText(name, "Product category name", 100);
        }
    }

    public record Knowledge(
            String sourceType,
            String sourceDescription,
            List<String> synonyms,
            String application,
            String modeOfUse,
            String dilution,
            String yield,
            List<String> intents,
            String salesArgument,
            String questions,
            String avoidSelling,
            String whatsappReply,
            int confidence) {
        public Knowledge {
            sourceType = DtoValidation.optionalText(
                    sourceType,
                    "Knowledge source type",
                    20);
            if (!supportsSourceType(sourceType)) {
                throw new IllegalArgumentException(
                        "Knowledge source type is invalid.");
            }
            sourceDescription = optionalKnowledge(
                    sourceDescription,
                    "Knowledge source description");
            synonyms = requireStringList(synonyms, "Knowledge synonyms");
            application = optionalKnowledge(application, "Knowledge application");
            modeOfUse = optionalKnowledge(modeOfUse, "Knowledge mode of use");
            dilution = optionalKnowledge(dilution, "Knowledge dilution");
            yield = optionalKnowledge(yield, "Knowledge yield");
            intents = requireStringList(intents, "Knowledge intents");
            salesArgument = optionalKnowledge(
                    salesArgument,
                    "Knowledge sales argument");
            questions = optionalKnowledge(questions, "Knowledge questions");
            avoidSelling = optionalKnowledge(
                    avoidSelling,
                    "Knowledge selling restrictions");
            whatsappReply = optionalKnowledge(
                    whatsappReply,
                    "Knowledge WhatsApp reply");
            if (confidence < 0 || confidence > 100) {
                throw new IllegalArgumentException(
                        "Knowledge confidence is invalid.");
            }
        }

        @Override
        public List<String> synonyms() {
            return List.copyOf(synonyms);
        }

        @Override
        public List<String> intents() {
            return List.copyOf(intents);
        }
    }

    public record Product(
            long id,
            ProductCategory category,
            String name,
            String description,
            String imageUrl,
            String imagePageUrl,
            String sku,
            String unit,
            String volume,
            String price,
            int stock,
            String brand,
            boolean isActive,
            int revision,
            Knowledge knowledge,
            String createdAt,
            String updatedAt) {
        public Product {
            id = DtoValidation.requirePositive(id, "Catalog product ID");
            name = DtoValidation.requireText(name, "Catalog product name", 200);
            description = optionalKnowledge(description, "Product description");
            imageUrl = DtoValidation.optionalText(
                    imageUrl,
                    "Product image URL",
                    2_048);
            imagePageUrl = DtoValidation.optionalText(
                    imagePageUrl,
                    "Product image page URL",
                    2_048);
            sku = DtoValidation.optionalText(sku, "Product SKU", 50);
            unit = DtoValidation.optionalText(unit, "Product unit", 20);
            volume = DtoValidation.optionalText(volume, "Product volume", 20);
            price = DtoValidation.requireText(price, "Product price", 11);
            if (!price.matches("\\d{1,8}\\.\\d{2}") || stock < 0) {
                throw new IllegalArgumentException(
                        "Catalog product values are invalid.");
            }
            brand = DtoValidation.optionalText(brand, "Product brand", 100);
            if (revision < MINIMUM_REVISION || knowledge == null) {
                throw new IllegalArgumentException(
                        "Catalog product revision is invalid.");
            }
            createdAt = DtoValidation.requireInstant(
                    createdAt,
                    "Product creation");
            updatedAt = DtoValidation.requireInstant(
                    updatedAt,
                    "Product update");
        }
    }

    public record Page(List<Product> items, String nextCursor) {
        public Page {
            if (items == null
                    || items.size() > 100
                    || items.stream().anyMatch(item -> item == null)) {
                throw new IllegalArgumentException("Catalog page is invalid.");
            }
            items = List.copyOf(items);
            nextCursor = DtoValidation.optionalText(
                    nextCursor,
                    "Catalog cursor",
                    256);
        }

        @Override
        public List<Product> items() {
            return List.copyOf(items);
        }
    }

    public record Categories(List<Category> items) {
        public Categories {
            if (items == null
                    || items.size() > 1_000
                    || items.stream().anyMatch(item -> item == null)) {
                throw new IllegalArgumentException(
                        "Catalog categories are invalid.");
            }
            items = List.copyOf(items);
        }

        @Override
        public List<Category> items() {
            return List.copyOf(items);
        }
    }

    public record Mutation(long resourceId, int revision, boolean changed) {
        public Mutation {
            resourceId = DtoValidation.requirePositive(
                    resourceId,
                    "Catalog mutation resource ID");
            if (revision < MINIMUM_REVISION) {
                throw new IllegalArgumentException(
                        "Catalog mutation revision is invalid.");
            }
        }
    }

    public record CategoryMutation(long resourceId, boolean changed) {
        public CategoryMutation {
            resourceId = DtoValidation.requirePositive(
                    resourceId,
                    "Category mutation resource ID");
            if (!changed) {
                throw new IllegalArgumentException(
                        "Category mutation is invalid.");
            }
        }
    }

    public record ProductWrite(
            Long categoryId,
            String name,
            String description,
            String imageUrl,
            String imagePageUrl,
            String sku,
            String unit,
            String volume,
            String price,
            int stock,
            String brand) {
        public ProductWrite {
            if (categoryId != null && categoryId < 1L) {
                throw new IllegalArgumentException(
                        "Product category is invalid.");
            }
            name = DtoValidation.requireText(name, "Product name", 200);
            description = optionalKnowledge(description, "Product description");
            imageUrl = DtoValidation.optionalText(
                    imageUrl,
                    "Product image URL",
                    2_048);
            imagePageUrl = DtoValidation.optionalText(
                    imagePageUrl,
                    "Product image page URL",
                    2_048);
            sku = DtoValidation.optionalText(sku, "Product SKU", 50);
            unit = DtoValidation.optionalText(unit, "Product unit", 20);
            volume = DtoValidation.optionalText(volume, "Product volume", 20);
            price = DtoValidation.requireText(price, "Product price", 11);
            brand = DtoValidation.optionalText(brand, "Product brand", 100);
            if (!price.matches("\\d{1,8}\\.\\d{2}") || stock < 0) {
                throw new IllegalArgumentException(
                        "Product write values are invalid.");
            }
        }
    }

    public record CreateRequest(ProductWrite product, boolean confirmed) {
        public CreateRequest {
            if (product == null || !confirmed) {
                throw new IllegalArgumentException(
                        "Product creation is invalid.");
            }
        }
    }

    public record UpdateRequest(
            String action,
            int expectedRevision,
            ProductWrite changes,
            boolean confirmed) {
        public UpdateRequest {
            requireCommand(action, "update", expectedRevision, confirmed);
            if (changes == null) {
                throw new IllegalArgumentException(
                        "Product update changes are required.");
            }
        }
    }

    public record StatusRequest(
            String action,
            int expectedRevision,
            boolean isActive,
            boolean confirmed) {
        public StatusRequest {
            requireCommand(action, "status", expectedRevision, confirmed);
        }
    }

    public record KnowledgeWrite(
            String sourceDescription,
            String synonyms,
            String application,
            String modeOfUse,
            String dilution,
            String yield,
            String intents,
            String salesArgument,
            String questions,
            String avoidSelling,
            String whatsappReply) {
        public KnowledgeWrite {
            sourceDescription = optionalKnowledge(
                    sourceDescription,
                    "Knowledge source description");
            synonyms = optionalKnowledge(synonyms, "Knowledge synonyms");
            application = optionalKnowledge(application, "Knowledge application");
            modeOfUse = optionalKnowledge(modeOfUse, "Knowledge mode of use");
            dilution = optionalKnowledge(dilution, "Knowledge dilution");
            yield = optionalKnowledge(yield, "Knowledge yield");
            intents = optionalKnowledge(intents, "Knowledge intents");
            salesArgument = optionalKnowledge(
                    salesArgument,
                    "Knowledge sales argument");
            questions = optionalKnowledge(questions, "Knowledge questions");
            avoidSelling = optionalKnowledge(
                    avoidSelling,
                    "Knowledge selling restrictions");
            whatsappReply = optionalKnowledge(
                    whatsappReply,
                    "Knowledge WhatsApp reply");
        }
    }

    public record KnowledgeRequest(
            String action,
            int expectedRevision,
            KnowledgeWrite knowledge,
            boolean confirmed) {
        public KnowledgeRequest {
            requireCommand(action, "knowledge", expectedRevision, confirmed);
            if (knowledge == null) {
                throw new IllegalArgumentException(
                        "Product knowledge is required.");
            }
        }
    }

    public record CategoryCreateRequest(
            String name,
            String description,
            boolean confirmed) {
        public CategoryCreateRequest {
            name = DtoValidation.requireText(name, "Category name", 100);
            description = DtoValidation.optionalText(
                    description,
                    "Category description",
                    MAXIMUM_TEXT);
            if (!confirmed) {
                throw new IllegalArgumentException(
                        "Category creation is not confirmed.");
            }
        }
    }

    private static void requireCommand(
            String action,
            String expected,
            int revision,
            boolean confirmed) {
        if (!expected.equals(action)
                || revision < MINIMUM_REVISION
                || !confirmed) {
            throw new IllegalArgumentException(
                    "Catalog administration command is invalid.");
        }
    }

    private static String optionalKnowledge(String value, String label) {
        return DtoValidation.optionalText(value, label, MAXIMUM_TEXT);
    }

    private static List<String> requireStringList(
            List<String> values,
            String label) {
        if (values == null
                || values.size() > 500
                || values.stream().anyMatch(value ->
                        value == null || value.length() > MAXIMUM_TEXT)) {
            throw new IllegalArgumentException(label + " are invalid.");
        }
        return List.copyOf(values);
    }
}
