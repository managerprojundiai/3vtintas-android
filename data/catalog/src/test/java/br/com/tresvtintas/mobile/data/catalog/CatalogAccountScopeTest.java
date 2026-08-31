package br.com.tresvtintas.mobile.data.catalog;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

public final class CatalogAccountScopeTest {
    @Test
    public void derivesOpaqueStableAccountPartition() {
        CatalogAccountScope first = CatalogAccountScope.from(42, "a".repeat(64), 9);
        CatalogAccountScope same = CatalogAccountScope.from(42, "a".repeat(64), 9);
        CatalogAccountScope other = CatalogAccountScope.from(43, "a".repeat(64), 9);
        CatalogAccountScope otherStore = CatalogAccountScope.from(42, "a".repeat(64), 10);

        assertEquals("Same account must resolve to same partition.", first, same);
        assertEquals("Account partition uses SHA-256.", 64, first.accountKey().length());
        assertNotEquals("Different accounts must be isolated.", first.accountKey(), other.accountKey());
        assertNotEquals("Different stores must be isolated.", first.accountKey(), otherStore.accountKey());
        assertNotEquals("Raw account ID must not be persisted.", "42", first.accountKey());
    }

    @Test
    public void rejectsInvalidIdentityAndRevision() {
        assertThrows(
                "Non-positive user must fail.",
                IllegalArgumentException.class,
                () -> CatalogAccountScope.from(0, "a".repeat(64), 9));
        assertThrows(
                "Invalid authorization revision must fail.",
                IllegalArgumentException.class,
                () -> CatalogAccountScope.from(42, "short", 9));
        assertThrows(
                "Non-positive organization must fail.",
                IllegalArgumentException.class,
                () -> CatalogAccountScope.from(42, "a".repeat(64), 0));
    }
}
