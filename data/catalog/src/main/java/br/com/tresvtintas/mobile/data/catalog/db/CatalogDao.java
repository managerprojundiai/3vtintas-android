package br.com.tresvtintas.mobile.data.catalog.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import java.util.List;

@Dao
public interface CatalogDao {
    @Query("""
            SELECT p.* FROM catalog_query_items AS i
            INNER JOIN catalog_products AS p
              ON p.accountKey = i.accountKey
             AND p.authorizationRevision = i.authorizationRevision
             AND p.productId = i.productId
            WHERE i.accountKey = :accountKey
              AND i.authorizationRevision = :revision
              AND i.queryKey = :queryKey
            ORDER BY i.sortPosition ASC
            """)
    List<CatalogProductEntity> readProducts(
            String accountKey,
            String revision,
            String queryKey);

    @Query("""
            SELECT * FROM catalog_queries
            WHERE accountKey = :accountKey
              AND authorizationRevision = :revision
              AND queryKey = :queryKey
            LIMIT 1
            """)
    CatalogQueryEntity readQuery(
            String accountKey,
            String revision,
            String queryKey);

    @Query("""
            SELECT productId FROM catalog_query_items
            WHERE accountKey = :accountKey
              AND authorizationRevision = :revision
              AND queryKey = :queryKey
            ORDER BY sortPosition ASC
            """)
    List<Long> readProductIds(
            String accountKey,
            String revision,
            String queryKey);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsertProducts(List<CatalogProductEntity> products);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsertQuery(CatalogQueryEntity query);

    @Insert(onConflict = OnConflictStrategy.ABORT)
    void insertQueryItems(List<CatalogQueryItemEntity> items);

    @Query("""
            DELETE FROM catalog_query_items
            WHERE accountKey = :accountKey
              AND authorizationRevision = :revision
              AND queryKey = :queryKey
            """)
    void deleteQueryItems(String accountKey, String revision, String queryKey);

    @Query("DELETE FROM catalog_query_items WHERE accountKey = :accountKey")
    void deleteAccountQueryItems(String accountKey);

    @Query("DELETE FROM catalog_queries WHERE accountKey = :accountKey")
    void deleteAccountQueries(String accountKey);

    @Query("DELETE FROM catalog_products WHERE accountKey = :accountKey")
    void deleteAccountProducts(String accountKey);
}
