package br.com.tresvtintas.mobile.data.catalog.db;

import androidx.annotation.Nullable;
import androidx.annotation.NonNull;
import androidx.room.Entity;

@Entity(
        tableName = "catalog_products",
        primaryKeys = {"accountKey", "authorizationRevision", "productId"})
public final class CatalogProductEntity {
    @NonNull public final String accountKey;
    @NonNull public final String authorizationRevision;
    public final long productId;
    @Nullable public final Long categoryId;
    @Nullable public final String categoryName;
    @NonNull public final String name;
    @Nullable public final String description;
    @Nullable public final String imageUrl;
    @Nullable public final String sku;
    @Nullable public final String unit;
    @Nullable public final String volume;
    @Nullable public final String price;
    @Nullable public final Integer stock;
    @Nullable public final String brand;
    public final long updatedAtMillis;

    public CatalogProductEntity(
            String accountKey,
            String authorizationRevision,
            long productId,
            @Nullable Long categoryId,
            @Nullable String categoryName,
            String name,
            @Nullable String description,
            @Nullable String imageUrl,
            @Nullable String sku,
            @Nullable String unit,
            @Nullable String volume,
            @Nullable String price,
            @Nullable Integer stock,
            @Nullable String brand,
            long updatedAtMillis) {
        this.accountKey = accountKey;
        this.authorizationRevision = authorizationRevision;
        this.productId = productId;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.name = name;
        this.description = description;
        this.imageUrl = imageUrl;
        this.sku = sku;
        this.unit = unit;
        this.volume = volume;
        this.price = price;
        this.stock = stock;
        this.brand = brand;
        this.updatedAtMillis = updatedAtMillis;
    }
}
