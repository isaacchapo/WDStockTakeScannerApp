package com.example.stockapp.data

import com.example.stockapp.data.local.InventoryGroup
import com.example.stockapp.data.local.SavedLocation
import com.example.stockapp.data.local.SavedLocationDao
import com.example.stockapp.data.local.SchemaGroup
import com.example.stockapp.data.local.StockItem
import com.example.stockapp.data.local.StockItemDao
import com.example.stockapp.data.local.User
import com.example.stockapp.data.local.UserDao
import com.example.stockapp.data.remote.StockUploadClient
import com.example.stockapp.data.remote.StockUploadItemDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import java.security.MessageDigest
import java.util.Locale

/**
 * A repository that handles data operations.
 */
class StockRepository(
    private val stockItemDao: StockItemDao,
    private val userDao: UserDao,
    private val savedLocationDao: SavedLocationDao
) {
    private val stockUploadClient = StockUploadClient()

    /**
     * A flow of all stock items from the database.
     */
    fun getAllStockItems(ownerUid: String): Flow<List<StockItem>> = stockItemDao.getAllStockItems(ownerUid)

    suspend fun getAllStockItemsSnapshot(ownerUid: String): List<StockItem> =
        stockItemDao.getAllStockItemsSnapshot(ownerUid)

    /**
     * A flow of table groups (UID + location + SID).
     */
    fun getInventoryGroups(ownerUid: String): Flow<List<InventoryGroup>> = stockItemDao.getInventoryGroups(ownerUid)

    /**
     * A flow of saved locations for a specific user.
     */
    fun getSavedLocations(ownerUid: String): Flow<List<SavedLocation>> =
        savedLocationDao.getSavedLocations(ownerUid)

    /**
     * Gets a specific saved location.
     */
    suspend fun getSavedLocation(ownerUid: String, locationNormalized: String): SavedLocation? =
        savedLocationDao.getSavedLocation(ownerUid, locationNormalized)

    /**
     * Creates or updates a saved location.
     */
    suspend fun upsertSavedLocation(savedLocation: SavedLocation) {
        savedLocationDao.upsert(savedLocation)
    }

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
     * @return a flow of all records for a selected table group (SID + UID + location).
     */
    fun getItemsForTable(
        ownerUid: String,
        location: String,
        sid: String
    ): Flow<List<StockItem>> {
        return stockItemDao.getItemsForTable(ownerUid, location, sid)
    }

    /**
     * @return schema groups found inside a selected table.
     */
    fun getSchemaGroups(
        ownerUid: String,
        location: String,
        sid: String
    ): Flow<List<SchemaGroup>> {
        return stockItemDao.getSchemaGroups(ownerUid, location, sid)
    }

    /**
     * @return a flow of items that belong to a specific schema in a table.
     */
    fun getItemsForSchema(
        ownerUid: String,
        location: String,
        sid: String,
        schemaId: String
    ): Flow<List<StockItem>> {
        return stockItemDao.getItemsForSchema(ownerUid, location, sid, schemaId)
    }

    suspend fun updateTableLocation(
        ownerUid: String,
        oldLocation: String,
        oldSid: String,
        newLocation: String
    ) {
        val normalizedOldLocation = normalizeLocation(oldLocation)
        val normalizedNewLocation = normalizeLocation(newLocation)
        val trimmedNewLocation = newLocation.trim()

        val existingSavedLocation = if (normalizedNewLocation.isBlank()) {
            null
        } else {
            savedLocationDao.getSavedLocation(ownerUid, normalizedNewLocation)
        }

        val locationLabel = existingSavedLocation?.location ?: trimmedNewLocation

        stockItemDao.updateTableLocation(
            ownerUid,
            oldLocation,
            oldSid,
            locationLabel
        )

        if (normalizedNewLocation.isNotBlank()) {
            savedLocationDao.upsert(
                SavedLocation(
                    ownerUid = ownerUid,
                    locationNormalized = normalizedNewLocation,
                    location = locationLabel,
                    sid = oldSid
                )
            )
        }

        if (normalizedOldLocation != normalizedNewLocation) {
            deleteSavedLocationIfUnused(ownerUid, normalizedOldLocation)
        }
    }

    suspend fun deleteTableGroup(
        ownerUid: String,
        location: String,
        sid: String
    ) {
        stockItemDao.deleteTableGroup(ownerUid, location, sid)
        deleteSavedLocationIfUnused(ownerUid, normalizeLocation(location))
    }

    suspend fun updateStockItem(
        ownerUid: String,
        stockItem: StockItem
    ) {
        stockItemDao.updateStockItem(
            ownerUid = ownerUid,
            id = stockItem.id,
            sid = stockItem.sid,
            identifierKey = stockItem.identifierKey,
            orderNo = stockItem.orderNo,
            location = stockItem.location,
            variableData = stockItem.variableData
        )
    }

    suspend fun deleteStockItem(
        ownerUid: String,
        stockItemId: String
    ) {
        stockItemDao.deleteStockItem(ownerUid, stockItemId)
    }

    suspend fun markItemsUploaded(
        ownerUid: String,
        stockItemIds: List<String>,
        uploadedAt: Long = System.currentTimeMillis()
    ) {
        val ids = stockItemIds
            .map(String::trim)
            .filter(String::isNotBlank)
            .distinct()
        if (ids.isEmpty()) return
        stockItemDao.markItemsUploaded(ownerUid, ids, uploadedAt)
    }

    suspend fun uploadInventory(
        baseUrl: String,
        endpointPath: String,
        apiKey: String,
        ownerUid: String,
        stockItems: List<StockItem>,
        onProgress: suspend (current: Int, total: Int) -> Unit = { _, _ -> }
    ): Result<String> {
        val scopedItems = stockItems.filter { it.ownerUid.isBlank() || it.ownerUid == ownerUid }
        if (scopedItems.isEmpty()) {
            return Result.failure(IllegalArgumentException("No stock items found to upload."))
        }
        if (apiKey.isBlank()) {
            return Result.failure(IllegalArgumentException("API key is required for upload."))
        }

        val urlResult = stockUploadClient.buildUploadUrl(baseUrl, endpointPath)
        val uploadUrl = urlResult.getOrElse { return Result.failure(it) }

        val itemPayload = scopedItems.map { item -> item.toUploadDto(ownerUid) }
        return stockUploadClient.uploadInventory(
            uploadUrl = uploadUrl,
            items = itemPayload,
            apiKey = apiKey.trim(),
            onProgress = onProgress
        )
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

    private suspend fun deleteSavedLocationIfUnused(ownerUid: String, normalizedLocation: String) {
        if (normalizedLocation.isBlank()) return
        val count = stockItemDao.countItemsByNormalizedLocation(ownerUid, normalizedLocation)
        if (count == 0) {
            savedLocationDao.deleteSavedLocation(ownerUid, normalizedLocation)
        }
    }

    private fun normalizeLocation(rawLocation: String): String {
        return rawLocation.trim().lowercase(Locale.ROOT)
    }

}

private fun StockItem.toUploadDto(ownerUid: String): StockUploadItemDto {
    return StockUploadItemDto(
        id = id,
        sid = sid,
        identifierKey = identifierKey,
        orderNo = orderNo,
        location = location,
        dateScanned = dateScanned,
        variableData = variableData,
        ownerUid = ownerUid
    )
}
