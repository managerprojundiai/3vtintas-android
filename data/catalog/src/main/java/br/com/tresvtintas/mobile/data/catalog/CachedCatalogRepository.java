package br.com.tresvtintas.mobile.data.catalog;

import android.database.sqlite.SQLiteException;
import br.com.tresvtintas.mobile.core.catalog.CatalogException;
import br.com.tresvtintas.mobile.core.catalog.CatalogFailureKind;
import br.com.tresvtintas.mobile.core.catalog.CatalogPage;
import br.com.tresvtintas.mobile.core.catalog.CatalogPricingContext;
import br.com.tresvtintas.mobile.core.catalog.CatalogQuery;
import br.com.tresvtintas.mobile.core.catalog.CatalogRepository;
import br.com.tresvtintas.mobile.core.catalog.CatalogSnapshot;
import br.com.tresvtintas.mobile.core.catalog.CatalogSource;
import br.com.tresvtintas.mobile.data.catalog.cache.CachedCatalogPage;
import br.com.tresvtintas.mobile.data.catalog.cache.CatalogCache;
import br.com.tresvtintas.mobile.data.catalog.remote.CatalogRemote;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public final class CachedCatalogRepository implements CatalogRepository {
    private final CatalogAccountScope scope;
    private final CatalogCache cache;
    private final CatalogRemote remote;
    private final Clock clock;

    public CachedCatalogRepository(
            CatalogAccountScope scope,
            CatalogCache cache,
            CatalogRemote remote,
            Clock clock) {
        this.scope = Objects.requireNonNull(scope, "Catalog account scope is required.");
        this.cache = Objects.requireNonNull(cache, "Catalog cache is required.");
        this.remote = Objects.requireNonNull(remote, "Catalog remote is required.");
        this.clock = Objects.requireNonNull(clock, "Catalog clock is required.");
    }

    @Override
    public Optional<CatalogPricingContext> pricingContext() throws CatalogException {
        return remote.pricingContext();
    }

    @Override
    public void selectPriceList(String versionPublicId) throws CatalogException {
        remote.selectPriceList(versionPublicId);
        clearAccount();
    }

    @Override
    public boolean allowsStalePrices() {
        return !remote.canReadPrices();
    }

    @Override
    public Optional<CatalogSnapshot> cached(CatalogQuery query)
            throws CatalogException {
        if (remote.canReadPrices()) {
            return Optional.empty();
        }
        try {
            return cache.read(scope, query).map(CachedCatalogRepository::cachedSnapshot);
        } catch (SQLiteException | IllegalStateException | IllegalArgumentException exception) {
            throw storageFailure("The catalog cache could not be read.", exception);
        }
    }

    @Override
    public CatalogSnapshot refresh(CatalogQuery query) throws CatalogException {
        CatalogPage page = remote.fetch(query, Optional.empty());
        Instant refreshedAt = clock.instant();
        try {
            return networkSnapshot(cache.replace(scope, query, page, refreshedAt));
        } catch (SQLiteException | IllegalStateException | IllegalArgumentException exception) {
            throw storageFailure("The catalog cache could not be replaced.", exception);
        }
    }

    @Override
    public CatalogSnapshot loadMore(CatalogQuery query) throws CatalogException {
        CachedCatalogPage existing;
        try {
            existing = cache.read(scope, query).orElseThrow(() ->
                    new IllegalStateException("Catalog cache is not initialized."));
        } catch (SQLiteException | IllegalStateException | IllegalArgumentException exception) {
            throw storageFailure("The catalog cursor could not be read.", exception);
        }
        if (existing.nextCursor().isEmpty()) {
            return cachedSnapshot(existing);
        }
        CatalogPage page = remote.fetch(query, existing.nextCursor());
        try {
            return networkSnapshot(cache.append(
                    scope,
                    query,
                    page,
                    clock.instant()));
        } catch (SQLiteException | IllegalStateException | IllegalArgumentException exception) {
            throw storageFailure("The catalog page could not be persisted.", exception);
        }
    }

    @Override
    public void clearAccount() throws CatalogException {
        try {
            cache.clearAccount(scope.accountKey());
        } catch (SQLiteException | IllegalStateException | IllegalArgumentException exception) {
            throw storageFailure("The catalog account cache could not be cleared.", exception);
        }
    }

    private static CatalogSnapshot cachedSnapshot(CachedCatalogPage page) {
        return new CatalogSnapshot(
                page.items(),
                page.nextCursor().isPresent(),
                true,
                CatalogSource.CACHE,
                page.refreshedAt());
    }

    private static CatalogSnapshot networkSnapshot(CachedCatalogPage page) {
        return new CatalogSnapshot(
                page.items(),
                page.nextCursor().isPresent(),
                false,
                CatalogSource.NETWORK,
                page.refreshedAt());
    }

    private static CatalogException storageFailure(
            String message,
            RuntimeException cause) {
        return new CatalogException(CatalogFailureKind.STORAGE, message, cause);
    }
}
