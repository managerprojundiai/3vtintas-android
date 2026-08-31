package br.com.tresvtintas.mobile.data.catalog;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import br.com.tresvtintas.mobile.core.catalog.CatalogException;
import br.com.tresvtintas.mobile.core.catalog.CatalogFailureKind;
import br.com.tresvtintas.mobile.core.catalog.CatalogPage;
import br.com.tresvtintas.mobile.core.catalog.CatalogPricingContext;
import br.com.tresvtintas.mobile.core.catalog.CatalogProduct;
import br.com.tresvtintas.mobile.core.catalog.CatalogQuery;
import br.com.tresvtintas.mobile.core.catalog.CatalogSnapshot;
import br.com.tresvtintas.mobile.core.catalog.CatalogSource;
import br.com.tresvtintas.mobile.data.catalog.cache.CachedCatalogPage;
import br.com.tresvtintas.mobile.data.catalog.cache.CatalogCache;
import br.com.tresvtintas.mobile.data.catalog.remote.CatalogRemote;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.Test;

public final class CachedCatalogRepositoryTest {
    private static final Instant NOW = Instant.parse("2026-07-25T15:00:00Z");

    @Test
    public void coldCacheRefreshesAndPersistsFirstPage() throws Exception {
        FakeCache cache = new FakeCache();
        FakeRemote remote = new FakeRemote();
        remote.pages.add(CatalogDataFixtures.page(
                List.of(CatalogDataFixtures.product(1)),
                "next"));
        CachedCatalogRepository repository = repository(cache, remote);

        assertTrue("Cold cache must be empty.", repository.cached(CatalogQuery.initial()).isEmpty());
        CatalogSnapshot snapshot = repository.refresh(CatalogQuery.initial());

        assertEquals("Refresh must use network source.", CatalogSource.NETWORK, snapshot.source());
        assertTrue("Remote cursor must enable pagination.", snapshot.hasMore());
        assertFalse("Successful refresh must be fresh.", snapshot.stale());
        assertEquals("First page must be persisted.", 1,
                cache.page.orElseThrow().items().size());
    }

    @Test
    public void appendsUsingOpaqueCursorAndReturnsCombinedPage() throws Exception {
        FakeCache cache = new FakeCache();
        cache.page = Optional.of(new CachedCatalogPage(
                List.of(CatalogDataFixtures.product(1)),
                Optional.of("cursor-1"),
                NOW.minusSeconds(60)));
        FakeRemote remote = new FakeRemote();
        remote.pages.add(CatalogDataFixtures.page(
                List.of(CatalogDataFixtures.product(2)),
                null));
        CachedCatalogRepository repository = repository(cache, remote);

        CatalogSnapshot snapshot = repository.loadMore(CatalogQuery.initial());

        assertEquals("Opaque cursor must be forwarded.", "cursor-1", remote.lastCursor.orElseThrow());
        assertEquals("Appended snapshot combines both pages.", 2, snapshot.items().size());
        assertFalse("Final page must close pagination.", snapshot.hasMore());
    }

    @Test
    public void mapsStorageFailureWithoutInventingNetworkFailure() {
        FakeCache cache = new FakeCache();
        cache.failReads = true;
        CachedCatalogRepository repository = repository(cache, new FakeRemote());

        CatalogException exception = assertThrows(
                "Storage exception must be typed.",
                CatalogException.class,
                () -> repository.cached(CatalogQuery.initial()));

        assertEquals(
                "Storage failure must remain distinct.",
                CatalogFailureKind.STORAGE,
                exception.kind());
    }

    @Test
    public void clearsOnlyTheScopedAccountPartition() throws Exception {
        FakeCache cache = new FakeCache();
        CachedCatalogRepository repository = repository(cache, new FakeRemote());

        repository.clearAccount();

        assertEquals(
                "Repository must clear its opaque account key.",
                CatalogDataFixtures.scope().accountKey(),
                cache.clearedAccount);
    }

    @Test
    public void neverReturnsCachedPricesForAnAuthorizedPricedCatalog() throws Exception {
        FakeCache cache = new FakeCache();
        cache.page = Optional.of(new CachedCatalogPage(
                List.of(CatalogDataFixtures.product(1)),
                Optional.empty(),
                NOW.minusSeconds(60)));
        FakeRemote remote = new FakeRemote(true);
        CachedCatalogRepository repository = repository(cache, remote);

        assertTrue(
                "A priced catalog must revalidate against the server before display.",
                repository.cached(CatalogQuery.initial()).isEmpty());
        assertFalse(
                "Prices must never be declared safe while offline.",
                repository.allowsStalePrices());
    }

    private static CachedCatalogRepository repository(
            FakeCache cache,
            FakeRemote remote) {
        return new CachedCatalogRepository(
                CatalogDataFixtures.scope(),
                cache,
                remote,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private static final class FakeRemote implements CatalogRemote {
        private final List<CatalogPage> pages = new ArrayList<>();
        private final boolean priced;
        private Optional<String> lastCursor = Optional.empty();

        private FakeRemote() {
            this(false);
        }

        private FakeRemote(boolean priced) {
            this.priced = priced;
        }

        @Override
        public Optional<CatalogPricingContext> pricingContext() {
            return Optional.empty();
        }

        @Override
        public void selectPriceList(String versionPublicId) {
            // No selection is required by this cache-focused fake.
        }

        @Override
        public boolean canReadPrices() {
            return priced;
        }

        @Override
        public CatalogPage fetch(CatalogQuery query, Optional<String> cursor) {
            lastCursor = cursor;
            return pages.remove(0);
        }
    }

    private static final class FakeCache implements CatalogCache {
        private Optional<CachedCatalogPage> page = Optional.empty();
        private boolean failReads;
        private String clearedAccount;

        @Override
        public Optional<CachedCatalogPage> read(
                CatalogAccountScope scope,
                CatalogQuery query) {
            if (failReads) {
                throw new IllegalStateException("storage");
            }
            return page;
        }

        @Override
        public CachedCatalogPage replace(
                CatalogAccountScope scope,
                CatalogQuery query,
                CatalogPage value,
                Instant refreshedAt) {
            page = Optional.of(new CachedCatalogPage(
                    value.items(),
                    value.nextCursor(),
                    refreshedAt));
            return page.orElseThrow();
        }

        @Override
        public CachedCatalogPage append(
                CatalogAccountScope scope,
                CatalogQuery query,
                CatalogPage value,
                Instant refreshedAt) {
            List<CatalogProduct> combined = new ArrayList<>(
                    page.orElseThrow().items());
            combined.addAll(value.items());
            page = Optional.of(new CachedCatalogPage(
                    combined,
                    value.nextCursor(),
                    refreshedAt));
            return page.orElseThrow();
        }

        @Override
        public void clearAccount(String accountKey) {
            clearedAccount = accountKey;
            page = Optional.empty();
        }
    }
}
