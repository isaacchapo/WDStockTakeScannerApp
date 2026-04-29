package com.example.stockapp.data

import com.example.stockapp.data.local.InventoryGroup
import com.example.stockapp.data.local.SavedLocation
import com.example.stockapp.data.local.SavedLocationDao
import com.example.stockapp.data.local.SchemaGroup
import com.example.stockapp.data.local.StockItem
import com.example.stockapp.data.local.StockItemDao
import com.example.stockapp.data.local.UploadDevice
import com.example.stockapp.data.local.UploadDeviceDao
import com.example.stockapp.data.local.User
import com.example.stockapp.data.local.UserDao
import com.example.stockapp.data.remote.StockUploadClient
import com.example.stockapp.data.remote.StockUploadItemDto
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.security.MessageDigest
import java.util.Locale

/**
 * A repository that handles data operations.
 */
class StockRepository(
    private val stockItemDao: StockItemDao,
    private val userDao: UserDao,
    private val savedLocationDao: SavedLocationDao,
    private val uploadDeviceDao: UploadDeviceDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default
) {
    private val stockUploadClient = StockUploadClient()

    /**
     * A flow of all stock items from the database.
     */
    fun getAllStockItems(ownerUid: String): Flow<List<StockItem>> = stockItemDao.getAllStockItems(ownerUid)

    suspend fun getAllStockItemsSnapshot(ownerUid: String): List<StockItem> =
        withContext(ioDispatcher) {
            stockItemDao.getAllStockItemsSnapshot(ownerUid)
        }

    suspend fun getMaxNumericSid(ownerUid: String): Int? =
        withContext(ioDispatcher) {
            stockItemDao.getMaxNumericSid(ownerUid)
        }

    /**
     * A flow of stock cards grouped by location + stock name + table rules.
     */
    fun getInventoryGroups(ownerUid: String): Flow<List<InventoryGroup>> = stockItemDao.getInventoryGroups(ownerUid)

    /**
     * A flow of saved locations for a specific user.
     */
    fun getSavedLocations(ownerUid: String): Flow<List<SavedLocation>> =
        savedLocationDao.getSavedLocations(ownerUid)

    /**
     * A flow of upload devices for a specific user.
     */
    fun getUploadDevices(ownerUid: String): Flow<List<UploadDevice>> =
        uploadDeviceDao.getUploadDevices(ownerUid)

    /**
     * Gets a specific saved location.
     */
    suspend fun getSavedLocation(ownerUid: String, locationNormalized: String): SavedLocation? =
        withContext(ioDispatcher) {
            savedLocationDao.getSavedLocation(ownerUid, locationNormalized)
        }

    /**
     * Creates or updates a saved location.
     */
    suspend fun upsertSavedLocation(savedLocation: SavedLocation) {
        withContext(ioDispatcher) {
            savedLocationDao.upsert(savedLocation)
        }
    }

    /**
     * Creates or updates an upload device.
     */
    suspend fun upsertUploadDevice(uploadDevice: UploadDevice) {
        withContext(ioDispatcher) {
            uploadDeviceDao.upsert(uploadDevice)
        }
    }

    suspend fun deleteUploadDevice(ownerUid: String, nameNormalized: String) {
        withContext(ioDispatcher) {
            uploadDeviceDao.deleteUploadDevice(ownerUid, nameNormalized)
        }
    }

    /**
     * Inserts a stock item into the database.
     * @param stockItem The stock item to be inserted.
     */
    suspend fun insert(stockItem: StockItem) {
        withContext(ioDispatcher) {
            stockItemDao.insert(stockItem)
        }
    }

    /**
     * Inserts a list of stock items into the database.
     * @param stockItems The list of stock items to be inserted.
     */
    suspend fun insertAll(stockItems: List<StockItem>) {
        withContext(ioDispatcher) {
            stockItemDao.insertAll(stockItems)
        }
    }

    /**
     * @return a flow of all records for a selected stock card.
     */
    fun getItemsForTable(
        ownerUid: String,
        location: String,
        stockName: String,
        identifierKey: String
    ): Flow<List<StockItem>> {
        return stockItemDao.getItemsForTable(ownerUid, location, stockName, identifierKey)
    }

    fun getItemsForTableAllSchemas(
        ownerUid: String,
        location: String,
        stockName: String
    ): Flow<List<StockItem>> {
        return stockItemDao.getItemsForTableAllSchemas(ownerUid, location, stockName)
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
        stockName: String,
        identifierKey: String,
        newLocation: String
    ) {
        withContext(ioDispatcher) {
            val normalizedOldLocation = normalizeLocation(oldLocation)
            val normalizedNewLocation = normalizeLocation(newLocation)
            val trimmedNewLocation = newLocation.trim()
            val oldSavedLocation = if (normalizedOldLocation.isBlank()) {
                null
            } else {
                savedLocationDao.getSavedLocation(ownerUid, normalizedOldLocation)
            }

            val existingSavedLocation = if (normalizedNewLocation.isBlank()) {
                null
            } else {
                savedLocationDao.getSavedLocation(ownerUid, normalizedNewLocation)
            }

            val locationLabel = existingSavedLocation?.location ?: trimmedNewLocation

            stockItemDao.updateTableLocation(
                ownerUid,
                oldLocation,
                stockName,
                identifierKey,
                locationLabel
            )

            if (normalizedNewLocation.isNotBlank()) {
                val resolvedSid = existingSavedLocation?.sid
                    ?.takeIf { it.isNotBlank() }
                    ?: oldSavedLocation?.sid.orEmpty()
                savedLocationDao.upsert(
                    SavedLocation(
                        ownerUid = ownerUid,
                        locationNormalized = normalizedNewLocation,
                        location = locationLabel,
                        sid = resolvedSid
                    )
                )
            }

            if (normalizedOldLocation != normalizedNewLocation) {
                deleteSavedLocationIfUnused(ownerUid, normalizedOldLocation)
            }
        }
    }

    suspend fun deleteTableGroup(
        ownerUid: String,
        location: String,
        stockName: String,
        identifierKey: String
    ) {
        withContext(ioDispatcher) {
            stockItemDao.deleteTableGroup(ownerUid, location, stockName, identifierKey)
            deleteSavedLocationIfUnused(ownerUid, normalizeLocation(location))
        }
    }

    suspend fun updateStockItem(
        ownerUid: String,
        stockItem: StockItem
    ) {
        withContext(ioDispatcher) {
            stockItemDao.updateStockItem(
                ownerUid = ownerUid,
                id = stockItem.id,
                sid = stockItem.sid,
                identifierKey = stockItem.identifierKey,
                orderNo = stockItem.orderNo,
                location = stockItem.location,
                stockName = stockItem.stockName,
                variableData = stockItem.variableData
            )
        }
    }

    suspend fun deleteStockItem(
        ownerUid: String,
        stockItemId: String
    ) {
        withContext(ioDispatcher) {
            stockItemDao.deleteStockItem(ownerUid, stockItemId)
        }
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
        withContext(ioDispatcher) {
            stockItemDao.markItemsUploaded(ownerUid, ids, uploadedAt)
        }
    }

    suspend fun uploadInventory(
        baseUrl: String,
        endpointPath: String,
        apiKey: String,
        ownerUid: String,
        userPassword: String,
        stockItems: List<StockItem>,
        onProgress: suspend (current: Int, total: Int) -> Unit = { _, _ -> }
    ): Result<String> {
        return withContext(ioDispatcher) {
            val trimmedOwnerUid = ownerUid.trim()
            val trimmedPassword = userPassword.trim()
            val scopedItems = stockItems.filter { it.ownerUid.isBlank() || it.ownerUid == ownerUid }
            if (scopedItems.isEmpty()) {
                return@withContext Result.failure(IllegalArgumentException("No stock items found to upload."))
            }
            if (apiKey.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("API key is required for upload."))
            }
            if (trimmedOwnerUid.isBlank() || trimmedPassword.isBlank()) {
                return@withContext Result.failure(
                    IllegalArgumentException("Active UID and password are required for upload.")
                )
            }

            val normalizedEndpointPath = normalizeUploadEndpointPath(endpointPath)
            val urlResult = stockUploadClient.buildUploadUrl(baseUrl, normalizedEndpointPath)
            val uploadUrl = urlResult.getOrElse { return@withContext Result.failure(it) }

            val itemPayload = scopedItems.map { item ->
                item.toUploadDto(ownerUid = trimmedOwnerUid, password = trimmedPassword)
            }
            stockUploadClient.uploadInventory(
                uploadUrl = uploadUrl,
                items = itemPayload,
                apiKey = apiKey.trim(),
                onProgress = onProgress
            )
        }
    }

    /**
     * Creates a new user and inserts it into the database.
     * @param uid The unique identifier for the user.
     * @param password The user's password.
     */
    suspend fun createUser(
        uid: String,
        email: String,
        password: String,
        securityKey: String
    ): Result<Unit> {
        return withContext(ioDispatcher) {
            val trimmedUid = uid.trim()
            val trimmedEmail = email.trim()
            val trimmedPassword = password.trim()
            val trimmedSecurityKey = securityKey.trim()
            if (trimmedUid.isBlank() || trimmedEmail.isBlank() || trimmedPassword.isBlank()) {
                return@withContext Result.failure(
                    IllegalArgumentException("UID, email, and password are required.")
                )
            }
            if (!looksLikeEmail(trimmedEmail)) {
                return@withContext Result.failure(
                    IllegalArgumentException("A valid email address is required.")
                )
            }
            if (trimmedSecurityKey != REQUIRED_CREATE_ACCOUNT_SECURITY_KEY) {
                return@withContext Result.failure(
                    SecurityException("Invalid security key.")
                )
            }
            if (userDao.userExists(trimmedUid)) {
                return@withContext Result.failure(
                    IllegalStateException("That UID already exists. Use a different UID.")
                )
            }
            val passwordHash = withContext(defaultDispatcher) { hashPassword(trimmedPassword) }
            userDao.insert(User(uid = trimmedUid, passwordHash = passwordHash))
            Result.success(Unit)
        }
    }

    /**
     * Logs in a user.
     * @param uid The unique identifier for the user.
     * @param password The user's password.
     * @return a boolean indicating whether the login was successful.
     */
    suspend fun loginUser(uid: String, password: String): Boolean {
        return withContext(ioDispatcher) {
            val trimmedUid = uid.trim()
            if (trimmedUid.isBlank()) return@withContext false
            val user = userDao.getUserSnapshot(trimmedUid)
            if (user != null) {
                val passwordHash = withContext(defaultDispatcher) { hashPassword(password) }
                user.passwordHash == passwordHash
            } else {
                false
            }
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
        val builder = StringBuilder(digest.size * 2)
        for (byte in digest) {
            val value = byte.toInt() and 0xFF
            builder.append(HEX_CHARS[value ushr 4])
            builder.append(HEX_CHARS[value and 0x0F])
        }
        return builder.toString()
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

    private companion object {
        private const val REQUIRED_CREATE_ACCOUNT_SECURITY_KEY = "stock123"
        private val HEX_CHARS = charArrayOf(
            '0', '1', '2', '3', '4', '5', '6', '7',
            '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'
        )
    }
}

private fun StockItem.toUploadDto(ownerUid: String, password: String): StockUploadItemDto {
    val resolvedUid = ownerUid.ifBlank { this.ownerUid }
    return StockUploadItemDto(
        id = id,
        sid = sid,
        identifierKey = identifierKey,
        orderNo = orderNo,
        location = location,
        stockName = stockName,
        dateScanned = dateScanned,
        variableData = enrichVariableData(variableData, sid, resolvedUid, password),
        ownerUid = resolvedUid,
        uid = resolvedUid,
        password = password
    )
}

private fun enrichVariableData(variableData: String, sid: String, uid: String, password: String): String {
    if (variableData.isBlank()) return variableData
    if (sid.isBlank() && uid.isBlank()) return variableData

    return runCatching {
        val json = JSONObject(variableData)
        val normalizedKeys = mutableSetOf<String>()
        val keys = json.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            normalizedKeys.add(normalizeColumnKey(key))
        }

        if (sid.isNotBlank() && "sid" !in normalizedKeys) {
            json.put("SID", sid)
        }
        if (uid.isNotBlank() && "uid" !in normalizedKeys && "owneruid" !in normalizedKeys) {
            json.put("UID", uid)
            normalizedKeys.add("uid")
        }
        val uidPresent = "uid" in normalizedKeys || "owneruid" in normalizedKeys
        if (uidPresent && password.isNotBlank() && "password" !in normalizedKeys) {
            json.put("Password", password)
        }

        json.toString()
    }.getOrElse { variableData }
}

private fun normalizeColumnKey(raw: String): String {
    return raw
        .trim()
        .lowercase(Locale.ROOT)
        .replace(Regex("[^a-z0-9]"), "")
}

private fun normalizeUploadEndpointPath(rawPath: String): String {
    val trimmed = rawPath.trim()
    if (trimmed.isBlank() || trimmed == "/") return PREFERRED_UPLOAD_ENDPOINT_PATH

    val withLeadingSlash = if (trimmed.startsWith("/")) trimmed else "/$trimmed"
    val normalized = withLeadingSlash.trimEnd('/')
    if (normalized.isBlank() || normalized == "/") return PREFERRED_UPLOAD_ENDPOINT_PATH

    return when {
        normalized.equals(LEGACY_UPLOAD_ENDPOINT_PATH, ignoreCase = true) -> PREFERRED_UPLOAD_ENDPOINT_PATH
        normalized.equals(LEGACY_UPLOAD_BULK_ENDPOINT_PATH, ignoreCase = true) -> PREFERRED_UPLOAD_ENDPOINT_PATH
        normalized.equals(LEGACY_PREFERRED_UPLOAD_ENDPOINT_PATH, ignoreCase = true) -> PREFERRED_UPLOAD_ENDPOINT_PATH
        normalized.equals(LEGACY_PREFERRED_BULK_UPLOAD_ENDPOINT_PATH, ignoreCase = true) -> PREFERRED_UPLOAD_ENDPOINT_PATH
        normalized.equals(LEGACY_UNIFIED_BULK_ENDPOINT_PATH, ignoreCase = true) -> PREFERRED_UPLOAD_ENDPOINT_PATH
        else -> normalized
    }
}

private fun looksLikeEmail(value: String): Boolean {
    val trimmed = value.trim()
    if (trimmed.isBlank()) return false
    val atIndex = trimmed.indexOf('@')
    val dotIndex = trimmed.lastIndexOf('.')
    return atIndex > 0 && dotIndex > atIndex + 1 && dotIndex < trimmed.lastIndex
}

private const val LEGACY_UPLOAD_ENDPOINT_PATH = "/api/stock/upload"
private const val LEGACY_UPLOAD_BULK_ENDPOINT_PATH = "/api/stock/upload/bulk"
private const val LEGACY_PREFERRED_UPLOAD_ENDPOINT_PATH = "/p/p"
private const val LEGACY_PREFERRED_BULK_UPLOAD_ENDPOINT_PATH = "/p/p/bulk"
private const val LEGACY_UNIFIED_BULK_ENDPOINT_PATH = "/api/app/bulk"
private const val PREFERRED_UPLOAD_ENDPOINT_PATH = "/api/app"
