package br.com.tresvtintas.mobile.data.catalog.db;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

@Database(
        entities = {
            CatalogProductEntity.class,
            CatalogQueryEntity.class,
            CatalogQueryItemEntity.class
        },
        version = 2,
        exportSchema = true)
public abstract class CatalogRoomDatabase extends RoomDatabase {
    private static final String DATABASE_NAME = "3v_operational_cache.db";
    public static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS `catalog_products_v2` "
                    + "(`accountKey` TEXT NOT NULL, `authorizationRevision` TEXT NOT NULL, "
                    + "`productId` INTEGER NOT NULL, `categoryId` INTEGER, "
                    + "`categoryName` TEXT, `name` TEXT NOT NULL, `description` TEXT, "
                    + "`imageUrl` TEXT, `sku` TEXT, `unit` TEXT, `volume` TEXT, "
                    + "`price` TEXT, `stock` INTEGER, `brand` TEXT, "
                    + "`updatedAtMillis` INTEGER NOT NULL, "
                    + "PRIMARY KEY(`accountKey`, `authorizationRevision`, `productId`))");
            database.execSQL("INSERT INTO `catalog_products_v2` "
                    + "(`accountKey`, `authorizationRevision`, `productId`, `categoryId`, "
                    + "`categoryName`, `name`, `description`, `imageUrl`, `sku`, `unit`, "
                    + "`volume`, `price`, `stock`, `brand`, `updatedAtMillis`) "
                    + "SELECT `accountKey`, `authorizationRevision`, `productId`, `categoryId`, "
                    + "`categoryName`, `name`, `description`, `imageUrl`, `sku`, `unit`, "
                    + "`volume`, NULL, `stock`, `brand`, `updatedAtMillis` "
                    + "FROM `catalog_products`");
            database.execSQL("DROP TABLE `catalog_products`");
            database.execSQL("ALTER TABLE `catalog_products_v2` RENAME TO `catalog_products`");
        }
    };

    public abstract CatalogDao catalogDao();

    public static CatalogRoomDatabase create(Context context) {
        if (context == null) {
            throw new IllegalArgumentException("Application context is required.");
        }
        return Room.databaseBuilder(
                        context.getApplicationContext(),
                        CatalogRoomDatabase.class,
                        DATABASE_NAME)
                .addMigrations(MIGRATION_1_2)
                .build();
    }
}
