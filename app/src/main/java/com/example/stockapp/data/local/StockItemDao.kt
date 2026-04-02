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
     * @return a flow of all stock items from the table, ordered by date scanned in descending order.
     */
    @Query("SELECT * FROM stock_table WHERE ownerUid = :ownerUid ORDER BY dateScanned DESC")
    fun getAllStockItems(ownerUid: String): Flow<List<StockItem>>

    @Query("SELECT * FROM stock_table WHERE ownerUid = :ownerUid")
    suspend fun getAllStockItemsSnapshot(ownerUid: String): List<StockItem>

    @Query(
        "SELECT MAX(CAST(trim(sid) AS INTEGER)) FROM stock_table " +
            "WHERE ownerUid = :ownerUid AND trim(sid) <> '' " +
            "AND trim(sid) NOT GLOB '*[^0-9]*'"
    )
    suspend fun getMaxNumericSid(ownerUid: String): Int?

    /**
     * @return a flow of stock cards identified by UID + location + stock name + table rules.
     */
    @Query(
        "SELECT ownerUid, location, " +
            "COALESCE(NULLIF(trim(stockName), ''), '') AS stockName, " +
            "identifierKey, " +
            "COUNT(*) AS totalRecords, " +
            "COUNT(DISTINCT identifierKey) AS schemaCount, " +
            "CASE WHEN COUNT(*) > 0 AND COUNT(*) = SUM(CASE WHEN uploadedAt IS NOT NULL THEN 1 ELSE 0 END) " +
            "THEN 1 ELSE 0 END AS isUploaded, " +
            "MAX(dateScanned) AS lastScannedAt " +
            "FROM stock_table " +
            "WHERE ownerUid = :ownerUid " +
            "GROUP BY ownerUid, location, COALESCE(NULLIF(trim(stockName), ''), ''), identifierKey " +
            "ORDER BY lastScannedAt DESC, location COLLATE NOCASE ASC, stockName COLLATE NOCASE ASC"
    )
    fun getInventoryGroups(ownerUid: String): Flow<List<InventoryGroup>>

    /**
     * @return a flow of all records for a stock card.
     */
    @Query(
        "SELECT * FROM stock_table " +
            "WHERE ownerUid = :ownerUid AND location = :location " +
            "AND COALESCE(NULLIF(trim(stockName), ''), '') = :stockName " +
            "AND identifierKey = :identifierKey " +
            "ORDER BY dateScanned DESC"
    )
    fun getItemsForTable(
        ownerUid: String,
        location: String,
        stockName: String,
        identifierKey: String
    ): Flow<List<StockItem>>

    @Query(
        "SELECT * FROM stock_table " +
            "WHERE ownerUid = :ownerUid AND location = :location " +
            "AND COALESCE(NULLIF(trim(stockName), ''), '') = :stockName " +
            "ORDER BY dateScanned DESC"
    )
    fun getItemsForTableAllSchemas(
        ownerUid: String,
        location: String,
        stockName: String
    ): Flow<List<StockItem>>

    /**
     * @return a flow of schema groups inside a selected table.
     */
    @Query(
        "SELECT sid, ownerUid, location, " +
            "identifierKey AS schemaId, " +
            "MIN(variableData) AS sampleData, " +
            "COUNT(*) AS totalRecords, " +
            "MAX(dateScanned) AS lastScannedAt " +
            "FROM stock_table " +
            "WHERE ownerUid = :ownerUid AND location = :location AND sid = :sid " +
            "GROUP BY sid, ownerUid, location, identifierKey " +
            "ORDER BY lastScannedAt DESC, schemaId ASC"
    )
    fun getSchemaGroups(
        ownerUid: String,
        location: String,
        sid: String
    ): Flow<List<SchemaGroup>>

    /**
     * @return a flow of stock items for a specific schema inside a table.
     */
    @Query(
        "SELECT * FROM stock_table " +
            "WHERE ownerUid = :ownerUid AND location = :location " +
            "AND sid = :sid AND identifierKey = :schemaId " +
            "ORDER BY dateScanned DESC"
    )
    fun getItemsForSchema(
        ownerUid: String,
        location: String,
        sid: String,
        schemaId: String
    ): Flow<List<StockItem>>

    @Query(
        "UPDATE stock_table SET location = :newLocation " +
            "WHERE ownerUid = :ownerUid AND location = :oldLocation " +
            "AND COALESCE(NULLIF(trim(stockName), ''), '') = :stockName " +
            "AND identifierKey = :identifierKey"
    )
    suspend fun updateTableLocation(
        ownerUid: String,
        oldLocation: String,
        stockName: String,
        identifierKey: String,
        newLocation: String
    )

    @Query(
        "DELETE FROM stock_table WHERE ownerUid = :ownerUid AND location = :location " +
            "AND COALESCE(NULLIF(trim(stockName), ''), '') = :stockName " +
            "AND identifierKey = :identifierKey"
    )
    suspend fun deleteTableGroup(
        ownerUid: String,
        location: String,
        stockName: String,
        identifierKey: String
    )

    @Query(
        "UPDATE stock_table SET sid = :sid, identifierKey = :identifierKey, orderNo = :orderNo, location = :location, stockName = :stockName, variableData = :variableData " +
            "WHERE ownerUid = :ownerUid AND id = :id"
    )
    suspend fun updateStockItem(
        ownerUid: String,
        id: String,
        sid: String,
        identifierKey: String,
        orderNo: String?,
        location: String,
        stockName: String,
        variableData: String
    )

    @Query("DELETE FROM stock_table WHERE ownerUid = :ownerUid AND id = :id")
    suspend fun deleteStockItem(
        ownerUid: String,
        id: String
    )

    @Query(
        "UPDATE stock_table SET uploadedAt = :uploadedAt " +
            "WHERE ownerUid = :ownerUid AND id IN (:ids)"
    )
    suspend fun markItemsUploaded(
        ownerUid: String,
        ids: List<String>,
        uploadedAt: Long
    )

    @Query(
        "SELECT COUNT(*) FROM stock_table " +
            "WHERE ownerUid = :ownerUid AND lower(trim(location)) = :locationNormalized"
    )
    suspend fun countItemsByNormalizedLocation(
        ownerUid: String,
        locationNormalized: String
    ): Int
}
