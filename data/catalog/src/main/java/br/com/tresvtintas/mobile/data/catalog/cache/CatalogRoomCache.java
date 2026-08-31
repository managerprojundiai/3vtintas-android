package br.com.tresvtintas.mobile.data.catalog.cache;

import br.com.tresvtintas.mobile.core.catalog.CatalogPage;
import br.com.tresvtintas.mobile.core.catalog.CatalogProduct;
import br.com.tresvtintas.mobile.core.catalog.CatalogQuery;
import br.com.tresvtintas.mobile.data.catalog.CatalogAccountScope;
import br.com.tresvtintas.mobile.data.catalog.db.CatalogDao;
import br.com.tresvtintas.mobile.data.catalog.db.CatalogProductEntity;
import br.com.tresvtintas.mobile.data.catalog.db.CatalogQueryEntity;
import br.com.tresvtintas.mobile.data.catalog.db.CatalogQueryItemEntity;
import br.com.tresvtintas.mobile.data.catalog.db.CatalogRoomDatabase;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public final class CatalogRoomCache implements CatalogCache {
    private final CatalogRoomDatabase database;
    private final CatalogDao dao;

    public CatalogRoomCache(CatalogRoomDatabase database) {
        if (database == null) {
            throw new IllegalArgumentException("Catalog database is required.");
        }
        this.database = database;
        this.dao = database.catalogDao();
    }

    @Override
    public Optional<CachedCatalogPage> read(
            CatalogAccountScope scope,
            CatalogQuery query) {
        String queryKey = query.cacheKey();
        CatalogQueryEntity metadata = dao.readQuery(
                scope.accountKey(),
                scope.authorizationRevision(),
                queryKey);
        if (metadata == null) {
            return Optional.empty();
        }
        List<CatalogProduct> products = dao.readProducts(
                        scope.accountKey(),
                        scope.authorizationRevision(),
                        queryKey)
                .stream()
                .map(CatalogEntityMapper::toDomain)
                .collect(Collectors.toList());
        return Optional.of(new CachedCatalogPage(
                products,
                Optional.ofNullable(metadata.nextCursor),
                Instant.ofEpochMilli(metadata.refreshedAtMillis)));
    }

    @Override
    public CachedCatalogPage replace(
            CatalogAccountScope scope,
            CatalogQuery query,
            CatalogPage page,
            Instant refreshedAt) {
        database.runInTransaction(() -> {
            String queryKey = query.cacheKey();
            dao.upsertProducts(productEntities(scope, page.items()));
            dao.deleteQueryItems(
                    scope.accountKey(),
                    scope.authorizationRevision(),
                    queryKey);
            dao.insertQueryItems(queryItems(scope, queryKey, page.items(), 0));
            dao.upsertQuery(queryEntity(scope, queryKey, page, refreshedAt));
        });
        return read(scope, query).orElseThrow();
    }

    @Override
    public CachedCatalogPage append(
            CatalogAccountScope scope,
            CatalogQuery query,
            CatalogPage page,
            Instant refreshedAt) {
        database.runInTransaction(() -> {
            String queryKey = query.cacheKey();
            List<Long> existingIds = dao.readProductIds(
                    scope.accountKey(),
                    scope.authorizationRevision(),
                    queryKey);
            Set<Long> seen = new HashSet<>(existingIds);
            List<CatalogProduct> unique = page.items()
                    .stream()
                    .filter(product -> seen.add(product.id()))
                    .collect(Collectors.toList());
            dao.upsertProducts(productEntities(scope, page.items()));
            dao.insertQueryItems(queryItems(
                    scope,
                    queryKey,
                    unique,
                    existingIds.size()));
            dao.upsertQuery(queryEntity(scope, queryKey, page, refreshedAt));
        });
        return read(scope, query).orElseThrow();
    }

    @Override
    public void clearAccount(String accountKey) {
        database.runInTransaction(() -> {
            dao.deleteAccountQueryItems(accountKey);
            dao.deleteAccountQueries(accountKey);
            dao.deleteAccountProducts(accountKey);
        });
    }

    private static List<CatalogProductEntity> productEntities(
            CatalogAccountScope scope,
            List<CatalogProduct> products) {
        return products.stream()
                .map(product -> CatalogEntityMapper.toEntity(scope, product))
                .collect(Collectors.toList());
    }

    private static List<CatalogQueryItemEntity> queryItems(
            CatalogAccountScope scope,
            String queryKey,
            List<CatalogProduct> products,
            int initialPosition) {
        List<CatalogQueryItemEntity> items = new ArrayList<>(products.size());
        int position = initialPosition;
        for (CatalogProduct product : products) {
            items.add(new CatalogQueryItemEntity(
                    scope.accountKey(),
                    scope.authorizationRevision(),
                    queryKey,
                    product.id(),
                    position));
            position++;
        }
        return items;
    }

    private static CatalogQueryEntity queryEntity(
            CatalogAccountScope scope,
            String queryKey,
            CatalogPage page,
            Instant refreshedAt) {
        return new CatalogQueryEntity(
                scope.accountKey(),
                scope.authorizationRevision(),
                queryKey,
                page.nextCursor().orElse(null),
                refreshedAt.toEpochMilli());
    }
}
