package br.com.tresvtintas.mobile.data.catalog.cache;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import br.com.tresvtintas.mobile.core.catalog.CatalogCategory;
import br.com.tresvtintas.mobile.core.catalog.CatalogPage;
import br.com.tresvtintas.mobile.core.catalog.CatalogProduct;
import br.com.tresvtintas.mobile.core.catalog.CatalogQuery;
import br.com.tresvtintas.mobile.data.catalog.CatalogAccountScope;
import br.com.tresvtintas.mobile.data.catalog.db.CatalogRoomDatabase;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Runs inside the app test APK on the CI emulator matrix. */
@RunWith(AndroidJUnit4.class)
public final class CatalogRoomCacheInstrumentedTest {
    private CatalogRoomDatabase database;
    private CatalogRoomCache cache;

    @Before
    public void createDatabase() {
        Context context = ApplicationProvider.getApplicationContext();
        database = Room.inMemoryDatabaseBuilder(
                        context,
                        CatalogRoomDatabase.class)
                .allowMainThreadQueries()
                .build();
        cache = new CatalogRoomCache(database);
    }

    @After
    public void closeDatabase() {
        database.close();
    }

    @Test
    public void replacesAppendsDeduplicatesAndPreservesOrder() {
        CatalogAccountScope scope = scope(42, "a");
        CatalogQuery query = CatalogQuery.initial();
        Instant firstRefresh = Instant.parse("2026-07-25T12:00:00Z");
        cache.replace(
                scope,
                query,
                page(List.of(product(1, "Primeiro")), "next"),
                firstRefresh);

        CachedCatalogPage combined = cache.append(
                scope,
                query,
                page(List.of(
                        product(1, "Primeiro atualizado"),
                        product(2, "Segundo")),
                        null),
                firstRefresh.plusSeconds(60));

        assertEquals("Duplicate product membership must be suppressed.", 2, combined.items().size());
        assertEquals("Existing position must remain first.", 1, combined.items().get(0).id());
        assertEquals("Upserted commercial data must be refreshed.",
                "Primeiro atualizado", combined.items().get(0).name());
        assertEquals("New product must append after existing page.", 2, combined.items().get(1).id());
        assertTrue("Final null cursor closes pagination.", combined.nextCursor().isEmpty());
    }

    @Test
    public void isolatesAccountAndAuthorizationRevisionThenClearsAccount() {
        CatalogQuery query = CatalogQuery.initial();
        CatalogAccountScope first = scope(42, "a");
        CatalogAccountScope otherAccount = scope(43, "a");
        CatalogAccountScope otherRevision = scope(42, "b");
        cache.replace(
                first,
                query,
                page(List.of(product(1, "Privado")), null),
                Instant.parse("2026-07-25T12:00:00Z"));

        assertTrue("Owning account may read its cache.", cache.read(first, query).isPresent());
        assertFalse("Another account must not read prices or stock.",
                cache.read(otherAccount, query).isPresent());
        assertFalse("A new authorization revision must not reuse old data.",
                cache.read(otherRevision, query).isPresent());

        cache.clearAccount(first.accountKey());

        assertTrue("Logout cleanup removes every revision for the account.",
                cache.read(first, query).isEmpty());
    }

    private static CatalogAccountScope scope(long userId, String revisionCharacter) {
        return CatalogAccountScope.from(userId, revisionCharacter.repeat(64), 9);
    }

    private static CatalogPage page(
            List<CatalogProduct> products,
            String cursor) {
        return new CatalogPage(products, Optional.ofNullable(cursor));
    }

    private static CatalogProduct product(long id, String name) {
        return new CatalogProduct(
                id,
                Optional.of(new CatalogCategory(3, "Esmaltes")),
                name,
                Optional.of("Descrição"),
                Optional.empty(),
                Optional.of("SKU-" + id),
                Optional.of("UN"),
                Optional.of("3,6 L"),
                Optional.of(new BigDecimal("119.90")),
                Optional.of(9),
                Optional.of("Marca"),
                Instant.parse("2026-07-25T12:00:00Z"));
    }
}
