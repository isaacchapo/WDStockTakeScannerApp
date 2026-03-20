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

    /**
     * @return a flow of table groups identified by SID + UID + location.
     */
    @Query(
        "SELECT ownerUid, location, sid, " +
            "COUNT(*) AS totalRecords, " +
            "COUNT(DISTINCT identifierKey) AS schemaCount, " +
            "MAX(dateScanned) AS lastScannedAt " +
            "FROM stock_table " +
            "WHERE ownerUid = :ownerUid " +
            "GROUP BY ownerUid, location, sid " +
            "ORDER BY lastScannedAt DESC, sid ASC, location COLLATE NOCASE ASC"
    )
    fun getInventoryGroups(ownerUid: String): Flow<List<InventoryGroup>>

    /**
     * @return a flow of all records for a table group.
     */
    @Query(
        "SELECT * FROM stock_table " +
            "WHERE ownerUid = :ownerUid AND location = :location " +
            "AND sid = :sid " +
            "ORDER BY dateScanned DESC"
    )
    fun getItemsForTable(
        ownerUid: String,
        location: String,
        sid: String
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
            "AND sid = :oldSid"
    )
    suspend fun updateTableLocation(
        ownerUid: String,
        oldLocation: String,
        oldSid: String,
        newLocation: String
    )

    @Query(
        "DELETE FROM stock_table WHERE ownerUid = :ownerUid AND location = :location " +
            "AND sid = :sid"
    )
    suspend fun deleteTableGroup(
        ownerUid: String,
        location: String,
        sid: String
    )

    @Query(
        "UPDATE stock_table SET sid = :sid, identifierKey = :identifierKey, orderNo = :orderNo, location = :location, variableData = :variableData " +
            "WHERE ownerUid = :ownerUid AND id = :id"
    )
    suspend fun updateStockItem(
        ownerUid: String,
        id: String,
        sid: String,
        identifierKey: String,
        orderNo: String?,
        location: String,
        variableData: String
    )

    @Query("DELETE FROM stock_table WHERE ownerUid = :ownerUid AND id = :id")
    suspend fun deleteStockItem(
        ownerUid: String,
        id: String
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
