package com.example.stockapp.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for the stock_table.
 */
@Dao
interface StockItemDao {

    /**
     * Inserts a stock item into the table, replacing it if it already exists.
     * @param stockItem The stock item to be inserted.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(stockItem: StockItem)

    /**
     * Inserts a list of stock items into the table, replacing them if they already exist.
     * @param stockItems The list of stock items to be inserted.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(stockItems: List<StockItem>)

    /**
     * @return a flow of all stock items from the table, ordered by description in descending order.
     */
    @Query("SELECT * FROM stock_table ORDER BY description DESC")
    fun getAllStockItems(): Flow<List<StockItem>>

    /**
     * @return a flow of inventory groups (Location + UID + SID).
     */
    @Query("SELECT DISTINCT location, stockTakeId, stockCode FROM stock_table ORDER BY location, stockCode")
    fun getInventoryGroups(): Flow<List<InventoryGroup>>

    /**
     * @return a flow of stock items for a specific inventory group.
     */
    @Query(
        "SELECT * FROM stock_table " +
            "WHERE location = :location AND stockTakeId = :stockTakeId AND stockCode = :stockCode " +
            "ORDER BY description DESC"
    )
    fun getItemsForGroup(
        location: String,
        stockTakeId: String,
        stockCode: String
    ): Flow<List<StockItem>>
}
