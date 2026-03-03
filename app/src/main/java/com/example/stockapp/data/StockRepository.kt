package com.example.stockapp.data

import com.example.stockapp.data.local.InventoryGroup
import com.example.stockapp.data.local.StockItem
import com.example.stockapp.data.local.StockItemDao
import com.example.stockapp.data.local.User
import com.example.stockapp.data.local.UserDao
import com.example.stockapp.data.remote.StockUploadClient
import com.example.stockapp.data.remote.StockUploadItemDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import java.security.MessageDigest

/**
 * A repository that handles data operations.
 */
class StockRepository(private val stockItemDao: StockItemDao, private val userDao: UserDao) {
    private val stockUploadClient = StockUploadClient()

    /**
     * A flow of all stock items from the database.
     */
    fun getAllStockItems(ownerUid: String): Flow<List<StockItem>> = stockItemDao.getAllStockItems(ownerUid)

    /**
     * A flow of inventory groups (Location + UID + SID).
     */
    fun getInventoryGroups(ownerUid: String): Flow<List<InventoryGroup>> = stockItemDao.getInventoryGroups(ownerUid)

    /**
     * Inserts a stock item into the database.
     * @param stockItem The stock item to be inserted.
     */
    suspend fun insert(stockItem: StockItem) {
        stockItemDao.insert(stockItem)
    }

    /**
     * Inserts a list of stock items into the database.
     * @param stockItems The list of stock items to be inserted.
     */
    suspend fun insertAll(stockItems: List<StockItem>) {
        stockItemDao.insertAll(stockItems)
    }

    /**
     * @return a flow of stock items for a specific inventory group.
     */
    fun getItemsForGroup(
        ownerUid: String,
        location: String,
        stockTakeId: String,
        stockCode: String
    ): Flow<List<StockItem>> {
        return stockItemDao.getItemsForGroup(ownerUid, location, stockTakeId, stockCode)
    }

    suspend fun updateGroup(
        ownerUid: String,
        oldLocation: String,
        oldStockTakeId: String,
        oldStockCode: String,
        newLocation: String,
        newStockTakeId: String,
        newStockCode: String
    ) {
        stockItemDao.updateGroup(
            ownerUid,
            oldLocation,
            oldStockTakeId,
            oldStockCode,
            newLocation,
            newStockTakeId,
            newStockCode
        )
    }

    suspend fun deleteGroup(
        ownerUid: String,
        location: String,
        stockTakeId: String,
        stockCode: String
    ) {
        stockItemDao.deleteGroup(ownerUid, location, stockTakeId, stockCode)
    }

    suspend fun updateStockItem(
        ownerUid: String,
        stockItem: StockItem
    ) {
        stockItemDao.updateStockItem(
            ownerUid = ownerUid,
            id = stockItem.id,
            itemId = stockItem.itemId,
            description = stockItem.description,
            quantity = stockItem.quantity,
            location = stockItem.location,
            stockCode = stockItem.stockCode,
            stockTakeId = stockItem.stockTakeId
        )
    }

    suspend fun deleteStockItem(
        ownerUid: String,
        stockItemId: Int
    ) {
        stockItemDao.deleteStockItem(ownerUid, stockItemId)
    }

    suspend fun uploadInventory(
        baseUrl: String,
        endpointPath: String,
        ownerUid: String,
        stockItems: List<StockItem>
    ): Result<String> {
        val scopedItems = stockItems.filter { it.ownerUid.isBlank() || it.ownerUid == ownerUid }
        if (scopedItems.isEmpty()) {
            return Result.failure(IllegalArgumentException("No stock items found to upload."))
        }

        val urlResult = stockUploadClient.buildUploadUrl(baseUrl, endpointPath)
        val uploadUrl = urlResult.getOrElse { return Result.failure(it) }

        val itemPayload = scopedItems.map { item -> item.toUploadDto(ownerUid) }
        return stockUploadClient.uploadInventory(uploadUrl, itemPayload)
    }

    /**
     * Creates a new user and inserts it into the database.
     * @param uid The unique identifier for the user.
     * @param password The user's password.
     */
    suspend fun createUser(uid: String, password: String) {
        val passwordHash = hashPassword(password)
        userDao.insert(User(uid = uid, passwordHash = passwordHash))
    }

    /**
     * Logs in a user.
     * @param uid The unique identifier for the user.
     * @param password The user's password.
     * @return a boolean indicating whether the login was successful.
     */
    suspend fun loginUser(uid: String, password: String): Boolean {
        val user = userDao.getUser(uid).firstOrNull()
        return if (user != null) {
            val passwordHash = hashPassword(password)
            user.passwordHash == passwordHash
        } else {
            false
        }
    }

    /**
     * Hashes a password using SHA-256.
     * @param password The password to be hashed.
     * @return The hashed password.
     */
    private fun hashPassword(password: String): String {
        val bytes = password.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.fold("") { str, it -> str + "%02x".format(it) }
    }
}

private fun StockItem.toUploadDto(ownerUid: String): StockUploadItemDto {
    return StockUploadItemDto(
        itemId = itemId,
        description = description,
        qty = quantity,
        location = location,
        stockCode = stockCode,
        stockTakeId = stockTakeId,
        ownerUid = ownerUid
    )
}
