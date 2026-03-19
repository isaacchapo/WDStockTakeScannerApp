package com.example.stockapp.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * The Room database for this app.
 */
@Database(
    entities = [StockItem::class, User::class, SavedLocation::class],
    version = 12,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    /**
     * @return The DAO for the stock_table.
     */
    abstract fun stockItemDao(): StockItemDao

    /**
     * @return The DAO for the user_table.
     */
    abstract fun userDao(): UserDao

    /**
     * @return The DAO for the saved_location_table.
     */
    abstract fun savedLocationDao(): SavedLocationDao

    companion object {
        /**
         * The singleton instance of the AppDatabase.
         */
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Returns the singleton instance of the AppDatabase.
         * @param context The application context.
         * @return The singleton instance of the AppDatabase.
         */
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "stock_database"
                )
                .addMigrations(
                    MIGRATION_5_6,
                    MIGRATION_6_7,
                    MIGRATION_7_8,
                    MIGRATION_8_9,
                    MIGRATION_9_10,
                    MIGRATION_10_11,
                    MIGRATION_11_12
                )
                .build()
                INSTANCE = instance
                instance
            }
        }

        /**
         * Migration from version 5 to 6.
         */
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE stock_table RENAME COLUMN section TO location")
            }
        }

        /**
         * Migration from version 6 to 7.
         */
        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE stock_table ADD COLUMN stockCode TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE stock_table ADD COLUMN stockTakeId TEXT NOT NULL DEFAULT ''")
                db.execSQL("CREATE TABLE new_stock_table (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, itemId TEXT NOT NULL, description TEXT NOT NULL, quantity INTEGER NOT NULL, location TEXT NOT NULL, stockCode TEXT NOT NULL, stockTakeId TEXT NOT NULL)")
                db.execSQL("INSERT INTO new_stock_table (itemId, description, quantity, location, stockCode, stockTakeId) SELECT id, name, quantity, location, stockCode, stockTakeId FROM stock_table")
                db.execSQL("DROP TABLE stock_table")
                db.execSQL("ALTER TABLE new_stock_table RENAME TO stock_table")
            }
        }

        /**
         * Migration from version 7 to 8.
         */
        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE stock_table RENAME COLUMN name TO description")
            }
        }

        /**
         * Migration from version 8 to 9.
         */
        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE stock_table ADD COLUMN ownerUid TEXT NOT NULL DEFAULT ''")
                db.execSQL("UPDATE stock_table SET ownerUid = stockTakeId WHERE ownerUid = ''")
            }
        }

        /**
         * Migration from version 9 to 10.
         */
        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS saved_location_table (" +
                        "ownerUid TEXT NOT NULL, " +
                        "locationNormalized TEXT NOT NULL, " +
                        "location TEXT NOT NULL, " +
                        "sid TEXT NOT NULL, " +
                        "PRIMARY KEY(ownerUid, locationNormalized))"
                )

                db.execSQL(
                    "INSERT OR IGNORE INTO saved_location_table " +
                        "(ownerUid, locationNormalized, location, sid) " +
                        "SELECT ownerUid, " +
                        "lower(trim(location)) AS locationNormalized, " +
                        "trim(location) AS location, " +
                        "CASE WHEN trim(MIN(stockCode)) = '' " +
                        "THEN substr(upper(hex(randomblob(4))), 1, 6) " +
                        "ELSE trim(MIN(stockCode)) END AS sid " +
                        "FROM stock_table " +
                        "WHERE trim(location) <> '' AND trim(ownerUid) <> '' " +
                        "GROUP BY ownerUid, lower(trim(location))"
                )
            }
        }

        /**
         * Migration from version 10 to 11: Convert to hybrid JSON schema.
         */
        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Create new table with hybrid schema
                db.execSQL(
                    "CREATE TABLE new_stock_table (" +
                        "id TEXT PRIMARY KEY NOT NULL, " +
                        "orderNo TEXT, " +
                        "location TEXT NOT NULL, " +
                        "dateScanned INTEGER NOT NULL, " +
                        "variableData TEXT NOT NULL, " +
                        "ownerUid TEXT NOT NULL)"
                )

                // Migrate data: convert old fields to JSON
                db.execSQL(
                    "INSERT INTO new_stock_table (id, orderNo, location, dateScanned, variableData, ownerUid) " +
                        "SELECT " +
                        "lower(hex(randomblob(16))), " +
                        "stockTakeId, " +
                        "location, " +
                        "strftime('%s', 'now') * 1000, " +
                        "json_object('itemId', itemId, 'description', description, 'quantity', quantity, 'stockCode', stockCode), " +
                        "ownerUid " +
                        "FROM stock_table"
                )

                // Drop old table and rename
                db.execSQL("DROP TABLE stock_table")
                db.execSQL("ALTER TABLE new_stock_table RENAME TO stock_table")
            }
        }

        /**
         * Migration from version 11 to 12: add SID and primary identifier key columns.
         */
        private val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE stock_table ADD COLUMN sid TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE stock_table ADD COLUMN identifierKey TEXT NOT NULL DEFAULT ''")
                db.execSQL(
                    "UPDATE stock_table SET identifierKey = 'identifier' " +
                        "WHERE trim(identifierKey) = ''"
                )
            }
        }
    }
}
