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
    @Query("SELECT * FROM stock_table WHERE ownerUid = :ownerUid ORDER BY description DESC")
    fun getAllStockItems(ownerUid: String): Flow<List<StockItem>>

    /**
     * @return a flow of inventory groups (Location + UID + SID).
     */
    @Query(
        "SELECT DISTINCT location, stockTakeId, stockCode FROM stock_table " +
            "WHERE ownerUid = :ownerUid ORDER BY location, stockCode"
    )
    fun getInventoryGroups(ownerUid: String): Flow<List<InventoryGroup>>

    /**
     * @return a flow of stock items for a specific inventory group.
     */
    @Query(
        "SELECT * FROM stock_table " +
            "WHERE ownerUid = :ownerUid AND location = :location " +
            "AND stockTakeId = :stockTakeId AND stockCode = :stockCode " +
            "ORDER BY description DESC"
    )
    fun getItemsForGroup(
        ownerUid: String,
        location: String,
        stockTakeId: String,
        stockCode: String
    ): Flow<List<StockItem>>

    @Query(
        "UPDATE stock_table SET location = :newLocation, stockTakeId = :newStockTakeId, stockCode = :newStockCode " +
            "WHERE ownerUid = :ownerUid AND location = :oldLocation " +
            "AND stockTakeId = :oldStockTakeId AND stockCode = :oldStockCode"
    )
    suspend fun updateGroup(
        ownerUid: String,
        oldLocation: String,
        oldStockTakeId: String,
        oldStockCode: String,
        newLocation: String,
        newStockTakeId: String,
        newStockCode: String
    )

    @Query(
        "DELETE FROM stock_table WHERE ownerUid = :ownerUid AND location = :location " +
            "AND stockTakeId = :stockTakeId AND stockCode = :stockCode"
    )
    suspend fun deleteGroup(
        ownerUid: String,
        location: String,
        stockTakeId: String,
        stockCode: String
    )

    @Query(
        "UPDATE stock_table SET itemId = :itemId, description = :description, quantity = :quantity, " +
            "location = :location, stockCode = :stockCode, stockTakeId = :stockTakeId " +
            "WHERE ownerUid = :ownerUid AND id = :id"
    )
    suspend fun updateStockItem(
        ownerUid: String,
        id: Int,
        itemId: String,
        description: String,
        quantity: Int,
        location: String,
        stockCode: String,
        stockTakeId: String
    )

    @Query("DELETE FROM stock_table WHERE ownerUid = :ownerUid AND id = :id")
    suspend fun deleteStockItem(
        ownerUid: String,
        id: Int
    )
}
