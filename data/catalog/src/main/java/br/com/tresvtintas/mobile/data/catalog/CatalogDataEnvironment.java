package br.com.tresvtintas.mobile.data.catalog;

import android.content.Context;
import br.com.tresvtintas.mobile.core.network.api.MobileApi;
import br.com.tresvtintas.mobile.data.catalog.cache.CatalogRoomCache;
import br.com.tresvtintas.mobile.data.catalog.db.CatalogRoomDatabase;
import br.com.tresvtintas.mobile.data.catalog.remote.RetrofitCatalogRemote;
import java.time.Clock;
import java.util.Objects;

/**
 * Owns the catalog persistence boundary so Room implementation types never leak into the app
 * composition root.
 */
public final class CatalogDataEnvironment implements AutoCloseable {
    private final CatalogRoomDatabase database;

    private CatalogDataEnvironment(CatalogRoomDatabase database) {
        this.database = database;
    }

    public static CatalogDataEnvironment create(Context context) {
        return new CatalogDataEnvironment(CatalogRoomDatabase.create(context));
    }

    public CachedCatalogRepository repository(
            CatalogAccountScope scope,
            MobileApi protectedApi,
            boolean canReadPrices) {
        return new CachedCatalogRepository(
                Objects.requireNonNull(scope, "Catalog account scope is required."),
                new CatalogRoomCache(database),
                new RetrofitCatalogRemote(Objects.requireNonNull(
                        protectedApi,
                        "Protected mobile API is required."),
                        canReadPrices,
                        scope.organizationId()),
                Clock.systemUTC());
    }

    @Override
    public void close() {
        database.close();
    }
}
