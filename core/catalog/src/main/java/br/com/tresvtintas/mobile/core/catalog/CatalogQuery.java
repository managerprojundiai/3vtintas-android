package br.com.tresvtintas.mobile.core.catalog;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalLong;

public record CatalogQuery(
        Optional<String> search,
        OptionalLong categoryId,
        int pageSize) {
    public static final int DEFAULT_PAGE_SIZE = 30;
    private static final int MAXIMUM_SEARCH_LENGTH = 120;
    private static final int MAXIMUM_PAGE_SIZE = 100;

    public CatalogQuery {
        search = normalizeSearch(search);
        categoryId = categoryId == null ? OptionalLong.empty() : categoryId;
        if (categoryId.isPresent() && categoryId.getAsLong() < 1) {
            throw new IllegalArgumentException("Catalog category ID must be positive.");
        }
        if (pageSize < 1 || pageSize > MAXIMUM_PAGE_SIZE) {
            throw new IllegalArgumentException("Catalog page size is invalid.");
        }
    }

    public static CatalogQuery initial() {
        return new CatalogQuery(
                Optional.empty(),
                OptionalLong.empty(),
                DEFAULT_PAGE_SIZE);
    }

    public CatalogQuery withSearch(String value) {
        return new CatalogQuery(
                Optional.ofNullable(value),
                categoryId,
                pageSize);
    }

    public CatalogQuery withCategory(OptionalLong value) {
        return new CatalogQuery(search, value, pageSize);
    }

    public String cacheKey() {
        String canonical = search.orElse("")
                + "\n"
                + (categoryId.isPresent() ? categoryId.getAsLong() : "")
                + "\n"
                + pageSize;
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(canonical.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(digest.length * 2);
            for (byte item : digest) {
                result.append(Character.forDigit((item >>> 4) & 0x0f, 16))
                        .append(Character.forDigit(item & 0x0f, 16));
            }
            return result.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new AssertionError("SHA-256 is required by the Java runtime.", exception);
        }
    }

    private static Optional<String> normalizeSearch(Optional<String> value) {
        if (value == null || value.isEmpty()) {
            return Optional.empty();
        }
        String normalized = value.orElseThrow()
                .trim()
                .replaceAll("\\s+", " ");
        if (normalized.isEmpty()) {
            return Optional.empty();
        }
        if (normalized.length() > MAXIMUM_SEARCH_LENGTH) {
            throw new IllegalArgumentException("Catalog search is too long.");
        }
        return Optional.of(normalized.toLowerCase(Locale.ROOT));
    }
}
