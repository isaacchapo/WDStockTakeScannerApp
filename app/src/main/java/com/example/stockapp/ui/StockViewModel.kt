package com.example.stockapp.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.stockapp.data.StockRepository
import com.example.stockapp.data.local.InventoryGroup
import com.example.stockapp.data.local.StockItem
import com.example.stockapp.data.local.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * A ViewModel that provides data to the UI and survives configuration changes.
 */
class StockViewModel(private val repository: StockRepository) : ViewModel() {

    /**
     * A flow of all stock items from the database.
     */
    val allStockItems: StateFlow<List<StockItem>> = repository.allStockItems.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val inventoryGroups: StateFlow<List<InventoryGroup>> = repository.inventoryGroups.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _scannedItems = MutableStateFlow<List<StockItem>>(emptyList())
    val scannedItems = _scannedItems.asStateFlow()

    /**
     * A flow that represents the result of a login attempt.
     */
    private val _loginResult = MutableStateFlow<Boolean?>(null)
    val loginResult = _loginResult.asStateFlow()

    /**
     * Inserts a stock item into the database.
     * @param stockItem The stock item to be inserted.
     */
    fun insert(stockItem: StockItem) = viewModelScope.launch {
        repository.insert(stockItem)
    }

    /**
     * Inserts a list of stock items into the database.
     * @param stockItems The list of stock items to be inserted.
     */
    fun insertAll(stockItems: List<StockItem>) = viewModelScope.launch {
        repository.insertAll(stockItems)
    }

    /**
     * Creates a new user and inserts it into the database.
     * @param uid The unique identifier for the user.
     * @param password The user's password.
     */
    fun createUser(uid: String, password: String) = viewModelScope.launch {
        repository.createUser(uid, password)
    }

    /**
     * Logs in a user.
     * @param uid The unique identifier for the user.
     * @param password The user's password.
     */
    fun loginUser(uid: String, password: String) = viewModelScope.launch {
        _loginResult.value = repository.loginUser(uid, password)
    }

    /**
     * Resets the login result to null.
     */
    fun resetLoginResult() {
        _loginResult.value = null
    }

    /**
     * @param uid The unique identifier of the user.
     * @return a flow of the user from the database.
     */
    fun getUser(uid: String): Flow<User?> {
        return repository.getUser(uid)
    }

    fun getItemsForGroup(
        location: String,
        stockTakeId: String,
        stockCode: String
    ): Flow<List<StockItem>> {
        return repository.getItemsForGroup(location, stockTakeId, stockCode)
    }

    fun addScannedItem(item: StockItem) {
        val currentList = _scannedItems.value.toMutableList()
        if (currentList.none { it.itemId == item.itemId }) {
            currentList.add(item)
            _scannedItems.value = currentList
        }
    }

    fun saveScannedItems(location: String, stockTakeId: String, stockCode: String) = viewModelScope.launch {
        val itemsToSave = _scannedItems.value.map {
            it.copy(location = location, stockTakeId = stockTakeId, stockCode = stockCode)
        }
        repository.insertAll(itemsToSave)
        _scannedItems.value = emptyList()
    }
}

/**
 * A factory for creating a StockViewModel with a constructor that takes a StockRepository.
 */
class StockViewModelFactory(private val repository: StockRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StockViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return StockViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
