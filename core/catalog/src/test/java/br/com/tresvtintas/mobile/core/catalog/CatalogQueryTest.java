package br.com.tresvtintas.mobile.core.catalog;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;

import java.util.Optional;
import java.util.OptionalLong;
import org.junit.Test;

public final class CatalogQueryTest {
    @Test
    public void normalizesSearchAndBuildsOpaqueStableCacheKey() {
        CatalogQuery first = new CatalogQuery(
                Optional.of("  Tinta   PREMIUM "),
                OptionalLong.of(4),
                30);
        CatalogQuery equivalent = new CatalogQuery(
                Optional.of("tinta premium"),
                OptionalLong.of(4),
                30);

        assertEquals(
                "Search must be normalized deterministically.",
                "tinta premium",
                first.search().orElseThrow());
        assertEquals(
                "Equivalent queries must share a cache key.",
                first.cacheKey(),
                equivalent.cacheKey());
        assertEquals("SHA-256 key must be fixed length.", 64, first.cacheKey().length());
        assertFalse(
                "Cache key must not persist plaintext search.",
                first.cacheKey().contains("tinta"));
    }

    @Test
    public void isolatesCategoryAndPageSizeInCacheKey() {
        CatalogQuery original = CatalogQuery.initial();

        assertNotEquals(
                "Category filters must not share cache membership.",
                original.cacheKey(),
                original.withCategory(OptionalLong.of(2)).cacheKey());
        assertNotEquals(
                "Page sizes must not share pagination metadata.",
                original.cacheKey(),
                new CatalogQuery(Optional.empty(), OptionalLong.empty(), 50).cacheKey());
    }

    @Test
    public void rejectsOversizedSearchAndInvalidCategory() {
        assertThrows(
                "Search over the API limit must fail locally.",
                IllegalArgumentException.class,
                () -> assertNotNull(
                        "Invalid search must not produce a query.",
                        CatalogQuery.initial().withSearch("a".repeat(121))));
        assertThrows(
                "Non-positive category IDs must fail locally.",
                IllegalArgumentException.class,
                () -> assertNotNull(
                        "Invalid category must not produce a query.",
                        CatalogQuery.initial().withCategory(OptionalLong.of(0))));
    }
}
