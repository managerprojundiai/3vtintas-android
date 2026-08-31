package br.com.tresvtintas.mobile.data.catalog.cache;

import br.com.tresvtintas.mobile.core.catalog.CatalogPage;
import br.com.tresvtintas.mobile.core.catalog.CatalogQuery;
import br.com.tresvtintas.mobile.data.catalog.CatalogAccountScope;
import java.time.Instant;
import java.util.Optional;

public interface CatalogCache {
    Optional<CachedCatalogPage> read(
            CatalogAccountScope scope,
            CatalogQuery query);

    CachedCatalogPage replace(
            CatalogAccountScope scope,
            CatalogQuery query,
            CatalogPage page,
            Instant refreshedAt);

    CachedCatalogPage append(
            CatalogAccountScope scope,
            CatalogQuery query,
            CatalogPage page,
            Instant refreshedAt);

    void clearAccount(String accountKey);
}
