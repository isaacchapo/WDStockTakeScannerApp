package com.example.stockapp.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.stockapp.data.StockRepository
import com.example.stockapp.data.local.InventoryGroup
import com.example.stockapp.data.local.StockItem
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * A ViewModel that provides data to the UI and survives configuration changes.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class StockViewModel(private val repository: StockRepository) : ViewModel() {
    private val _activeUserUid = MutableStateFlow<String?>(null)
    val activeUserUid = _activeUserUid.asStateFlow()

    /**
     * A flow of all stock items from the database.
     */
    val allStockItems: StateFlow<List<StockItem>> = activeUserUid.flatMapLatest { uid ->
        if (uid.isNullOrBlank()) {
            flowOf(emptyList())
        } else {
            repository.getAllStockItems(uid)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val inventoryGroups: StateFlow<List<InventoryGroup>> = activeUserUid.flatMapLatest { uid ->
        if (uid.isNullOrBlank()) {
            flowOf(emptyList())
        } else {
            repository.getInventoryGroups(uid)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    /**
     * A flow that represents the result of a login attempt.
     */
    private val _loginResult = MutableStateFlow<Boolean?>(null)
    val loginResult = _loginResult.asStateFlow()
    private val _loginInProgress = MutableStateFlow(false)
    val loginInProgress = _loginInProgress.asStateFlow()

    /**
     * Inserts a stock item into the database.
     * @param stockItem The stock item to be inserted.
     */
    fun insert(stockItem: StockItem) = viewModelScope.launch {
        val ownerUid = _activeUserUid.value ?: return@launch
        repository.insert(stockItem.copy(ownerUid = ownerUid))
    }

    /**
     * Inserts a list of stock items into the database.
     * @param stockItems The list of stock items to be inserted.
     */
    fun insertAll(stockItems: List<StockItem>) = viewModelScope.launch {
        val ownerUid = _activeUserUid.value ?: return@launch
        repository.insertAll(stockItems.map { it.copy(ownerUid = ownerUid) })
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
        if (_loginInProgress.value) return@launch
        if (uid.isBlank() || password.isBlank()) {
            _loginResult.value = null
            return@launch
        }
        _loginInProgress.value = true
        try {
            val loginSucceeded = repository.loginUser(uid, password)
            _loginResult.value = loginSucceeded
            if (loginSucceeded) {
                _activeUserUid.value = uid
            }
        } finally {
            _loginInProgress.value = false
        }
    }

    fun setActiveUser(uid: String) {
        _activeUserUid.value = uid.trim().ifBlank { null }
    }

    fun clearActiveUser() {
        _activeUserUid.value = null
    }

    /**
     * Resets the login result to null.
     */
    fun resetLoginResult() {
        _loginResult.value = null
    }

    fun getItemsForGroup(
        location: String,
        stockTakeId: String,
        stockCode: String
    ): Flow<List<StockItem>> {
        val ownerUid = _activeUserUid.value ?: return flowOf(emptyList())
        return repository.getItemsForGroup(ownerUid, location, stockTakeId, stockCode)
    }

    suspend fun getItemsForGroupSnapshot(
        location: String,
        stockTakeId: String,
        stockCode: String
    ): List<StockItem> {
        val ownerUid = _activeUserUid.value ?: return emptyList()
        return repository.getItemsForGroup(ownerUid, location, stockTakeId, stockCode).first()
    }

    suspend fun uploadInventory(
        baseUrl: String,
        endpointPath: String,
        inventoryGroup: InventoryGroup?,
        stockItems: List<StockItem>
    ): Result<String> {
        val ownerUid = _activeUserUid.value
            ?: return Result.failure(IllegalStateException("No active user found."))
        return repository.uploadInventory(
            baseUrl = baseUrl,
            endpointPath = endpointPath,
            ownerUid = ownerUid,
            inventoryGroup = inventoryGroup,
            stockItems = stockItems
        )
    }

    fun updateGroup(
        oldLocation: String,
        oldStockTakeId: String,
        oldStockCode: String,
        newLocation: String,
        newStockTakeId: String,
        newStockCode: String
    ) = viewModelScope.launch {
        val ownerUid = _activeUserUid.value ?: return@launch
        repository.updateGroup(
            ownerUid,
            oldLocation,
            oldStockTakeId,
            oldStockCode,
            newLocation,
            newStockTakeId,
            newStockCode
        )
    }

    fun deleteGroup(
        location: String,
        stockTakeId: String,
        stockCode: String
    ) = viewModelScope.launch {
        val ownerUid = _activeUserUid.value ?: return@launch
        repository.deleteGroup(ownerUid, location, stockTakeId, stockCode)
    }

    fun updateStockItem(stockItem: StockItem) = viewModelScope.launch {
        val ownerUid = _activeUserUid.value ?: return@launch
        repository.updateStockItem(ownerUid, stockItem.copy(ownerUid = ownerUid))
    }

    fun deleteStockItem(stockItemId: Int) = viewModelScope.launch {
        val ownerUid = _activeUserUid.value ?: return@launch
        repository.deleteStockItem(ownerUid, stockItemId)
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
