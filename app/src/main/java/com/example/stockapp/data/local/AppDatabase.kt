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
@Database(entities = [StockItem::class, User::class], version = 8, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    /**
     * @return The DAO for the stock_table.
     */
    abstract fun stockItemDao(): StockItemDao

    /**
     * @return The DAO for the user_table.
     */
    abstract fun userDao(): UserDao

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
                .addMigrations(MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8)
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
    }
}
