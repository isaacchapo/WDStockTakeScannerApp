package com.example.stockapp.data

import com.example.stockapp.data.local.InventoryGroup
import com.example.stockapp.data.local.StockItem
import com.example.stockapp.data.local.StockItemDao
import com.example.stockapp.data.local.User
import com.example.stockapp.data.local.UserDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import java.security.MessageDigest

/**
 * A repository that handles data operations.
 */
class StockRepository(private val stockItemDao: StockItemDao, private val userDao: UserDao) {

    /**
     * A flow of all stock items from the database.
     */
    val allStockItems: Flow<List<StockItem>> = stockItemDao.getAllStockItems()

    /**
     * A flow of inventory groups (Location + UID + SID).
     */
    val inventoryGroups: Flow<List<InventoryGroup>> = stockItemDao.getInventoryGroups()

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
        location: String,
        stockTakeId: String,
        stockCode: String
    ): Flow<List<StockItem>> {
        return stockItemDao.getItemsForGroup(location, stockTakeId, stockCode)
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
     * @param uid The unique identifier of the user.
     * @return a flow of the user from the database.
     */
    fun getUser(uid: String): Flow<User?> {
        return userDao.getUser(uid)
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
