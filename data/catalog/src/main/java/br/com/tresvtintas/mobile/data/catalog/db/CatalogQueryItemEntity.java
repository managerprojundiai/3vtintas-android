package br.com.tresvtintas.mobile.data.catalog.db;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.annotation.NonNull;

@Entity(
        tableName = "catalog_query_items",
        primaryKeys = {
            "accountKey",
            "authorizationRevision",
            "queryKey",
            "productId"
        },
        indices = {
            @Index(
                    value = {
                        "accountKey",
                        "authorizationRevision",
                        "queryKey",
                        "sortPosition"
                    },
                    unique = true)
        })
public final class CatalogQueryItemEntity {
    @NonNull public final String accountKey;
    @NonNull public final String authorizationRevision;
    @NonNull public final String queryKey;
    public final long productId;
    public final int sortPosition;

    public CatalogQueryItemEntity(
            String accountKey,
            String authorizationRevision,
            String queryKey,
            long productId,
            int sortPosition) {
        this.accountKey = accountKey;
        this.authorizationRevision = authorizationRevision;
        this.queryKey = queryKey;
        this.productId = productId;
        this.sortPosition = sortPosition;
    }
}
