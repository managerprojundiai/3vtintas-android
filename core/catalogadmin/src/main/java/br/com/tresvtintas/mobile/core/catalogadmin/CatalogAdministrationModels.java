package br.com.tresvtintas.mobile.core.catalogadmin;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;

public final class CatalogAdministrationModels {
    private static final long MINIMUM_ID = 1L;
    private static final int MINIMUM_REVISION = 1;

    private CatalogAdministrationModels() {
        throw new AssertionError("No instances.");
    }

    public static Knowledge emptyKnowledge() {
        return new Knowledge(
                Optional.empty(),
                Optional.empty(),
                List.of(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                List.of(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                0);
    }

    public record Category(long id, String name, Optional<String> description) {
        public Category {
            requireId(id);
            name = requireText(name, "Category name");
            description = description == null ? Optional.empty() : description;
        }
    }

    public record Knowledge(
            Optional<String> sourceType,
            Optional<String> sourceDescription,
            List<String> synonyms,
            Optional<String> application,
            Optional<String> modeOfUse,
            Optional<String> dilution,
            Optional<String> yield,
            List<String> intents,
            Optional<String> salesArgument,
            Optional<String> questions,
            Optional<String> avoidSelling,
            Optional<String> whatsappReply,
            int confidence) {
        public Knowledge {
            sourceType = optional(sourceType);
            sourceDescription = optional(sourceDescription);
            synonyms = copyStrings(synonyms, "Knowledge synonyms");
            application = optional(application);
            modeOfUse = optional(modeOfUse);
            dilution = optional(dilution);
            yield = optional(yield);
            intents = copyStrings(intents, "Knowledge intents");
            salesArgument = optional(salesArgument);
            questions = optional(questions);
            avoidSelling = optional(avoidSelling);
            whatsappReply = optional(whatsappReply);
            if (confidence < 0 || confidence > 100) {
                throw new IllegalArgumentException("Knowledge confidence is invalid.");
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
            Optional<Category> category,
            String name,
            Optional<String> description,
            Optional<String> imageUrl,
            Optional<String> imagePageUrl,
            Optional<String> sku,
            Optional<String> unit,
            Optional<String> volume,
            String price,
            int stock,
            Optional<String> brand,
            boolean active,
            int revision,
            Knowledge knowledge,
            Instant createdAt,
            Instant updatedAt) {
        public Product {
            requireId(id);
            category = category == null ? Optional.empty() : category;
            name = requireText(name, "Product name");
            description = optional(description);
            imageUrl = optional(imageUrl);
            imagePageUrl = optional(imagePageUrl);
            sku = optional(sku);
            unit = optional(unit);
            volume = optional(volume);
            price = requireText(price, "Product price");
            brand = optional(brand);
            requireRevision(revision);
            if (stock < 0) {
                throw new IllegalArgumentException("Product stock is invalid.");
            }
            Objects.requireNonNull(knowledge, "Product knowledge is required.");
            Objects.requireNonNull(createdAt, "Product creation is required.");
            Objects.requireNonNull(updatedAt, "Product update is required.");
        }
    }

    public record Page(List<Product> items, Optional<String> nextCursor) {
        public Page {
            if (items == null || items.stream().anyMatch(Objects::isNull)) {
                throw new IllegalArgumentException("Catalog page is invalid.");
            }
            items = List.copyOf(items);
            nextCursor = nextCursor == null ? Optional.empty() : nextCursor;
        }

        @Override
        public List<Product> items() {
            return List.copyOf(items);
        }
    }

    public record Mutation(
            long resourceId,
            int revision,
            boolean changed,
            boolean replayed) {
        public Mutation {
            requireId(resourceId);
            requireRevision(revision);
        }
    }

    public record CategoryMutation(
            long resourceId,
            boolean changed,
            boolean replayed) {
        public CategoryMutation {
            requireId(resourceId);
        }
    }

    public record ProductDraft(
            OptionalLong categoryId,
            String name,
            Optional<String> description,
            Optional<String> imageUrl,
            Optional<String> imagePageUrl,
            Optional<String> sku,
            Optional<String> unit,
            Optional<String> volume,
            String price,
            int stock,
            Optional<String> brand) {
        public ProductDraft {
            categoryId = categoryId == null ? OptionalLong.empty() : categoryId;
            name = requireText(name, "Product name");
            description = optional(description);
            imageUrl = optional(imageUrl);
            imagePageUrl = optional(imagePageUrl);
            sku = optional(sku);
            unit = optional(unit);
            volume = optional(volume);
            price = requireText(price, "Product price");
            brand = optional(brand);
            if ((categoryId.isPresent() && categoryId.getAsLong() < 1L)
                    || stock < 0) {
                throw new IllegalArgumentException("Product draft is invalid.");
            }
        }
    }

    public record KnowledgeDraft(
            Optional<String> sourceDescription,
            Optional<String> synonyms,
            Optional<String> application,
            Optional<String> modeOfUse,
            Optional<String> dilution,
            Optional<String> yield,
            Optional<String> intents,
            Optional<String> salesArgument,
            Optional<String> questions,
            Optional<String> avoidSelling,
            Optional<String> whatsappReply) {
        public KnowledgeDraft {
            sourceDescription = optional(sourceDescription);
            synonyms = optional(synonyms);
            application = optional(application);
            modeOfUse = optional(modeOfUse);
            dilution = optional(dilution);
            yield = optional(yield);
            intents = optional(intents);
            salesArgument = optional(salesArgument);
            questions = optional(questions);
            avoidSelling = optional(avoidSelling);
            whatsappReply = optional(whatsappReply);
        }
    }

    private static Optional<String> optional(Optional<String> value) {
        return value == null
                ? Optional.empty()
                : value.map(String::strip).filter(text -> !text.isEmpty());
    }

    private static List<String> copyStrings(List<String> values, String label) {
        if (values == null || values.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException(label + " are invalid.");
        }
        return List.copyOf(values);
    }

    private static void requireId(long value) {
        if (value < MINIMUM_ID) {
            throw new IllegalArgumentException("Resource ID is invalid.");
        }
    }

    private static void requireRevision(int value) {
        if (value < MINIMUM_REVISION) {
            throw new IllegalArgumentException("Revision is invalid.");
        }
    }

    private static String requireText(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " is invalid.");
        }
        return value.strip();
    }
}
