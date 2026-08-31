package br.com.tresvtintas.mobile.data.catalog.db;

import static org.junit.Assert.assertTrue;

import android.database.Cursor;
import androidx.room.testing.MigrationTestHelper;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.io.IOException;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public final class CatalogRoomMigrationInstrumentedTest {
    private static final String DATABASE_NAME = "catalog-price-redaction-migration.db";

    @Rule
    public final MigrationTestHelper helper = new MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            CatalogRoomDatabase.class);

    @After
    public void tearDown() {
        InstrumentationRegistry.getInstrumentation()
                .getTargetContext()
                .deleteDatabase(DATABASE_NAME);
    }

    @Test
    public void migrationClearsPreviouslyCachedPrice() throws IOException {
        SupportSQLiteDatabase versionOne = helper.createDatabase(DATABASE_NAME, 1);
        versionOne.execSQL("INSERT INTO `catalog_products` "
                + "(`accountKey`, `authorizationRevision`, `productId`, `categoryId`, "
                + "`categoryName`, `name`, `description`, `imageUrl`, `sku`, `unit`, "
                + "`volume`, `price`, `stock`, `brand`, `updatedAtMillis`) VALUES "
                + "('restricted-account', 'revision-1', 10, 2, 'Premium', "
                + "'Tinta Premium', NULL, NULL, 'SKU-10', 'UN', '18 L', "
                + "'999.90', 5, '3V', 1)");
        versionOne.close();

        SupportSQLiteDatabase versionTwo = helper.runMigrationsAndValidate(
                DATABASE_NAME,
                2,
                true,
                CatalogRoomDatabase.MIGRATION_1_2);

        try (Cursor cursor = versionTwo.query(
                "SELECT `price` FROM `catalog_products` WHERE `productId` = 10")) {
            assertTrue("The migrated product must remain available.", cursor.moveToFirst());
            assertTrue(
                    "A legacy cached price must be erased during the security migration.",
                    cursor.isNull(0));
        }
        versionTwo.close();
    }
}
