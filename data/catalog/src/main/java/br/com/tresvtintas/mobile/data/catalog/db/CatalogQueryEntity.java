package br.com.tresvtintas.mobile.data.catalog.db;

import androidx.annotation.Nullable;
import androidx.annotation.NonNull;
import androidx.room.Entity;

@Entity(
        tableName = "catalog_queries",
        primaryKeys = {"accountKey", "authorizationRevision", "queryKey"})
public final class CatalogQueryEntity {
    @NonNull public final String accountKey;
    @NonNull public final String authorizationRevision;
    @NonNull public final String queryKey;
    @Nullable public final String nextCursor;
    public final long refreshedAtMillis;

    public CatalogQueryEntity(
            String accountKey,
            String authorizationRevision,
            String queryKey,
            @Nullable String nextCursor,
            long refreshedAtMillis) {
        this.accountKey = accountKey;
        this.authorizationRevision = authorizationRevision;
        this.queryKey = queryKey;
        this.nextCursor = nextCursor;
        this.refreshedAtMillis = refreshedAtMillis;
    }
}
