package com.example.stockapp.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.stockapp.data.StockRepository
import com.example.stockapp.data.local.InventoryGroup
import com.example.stockapp.data.local.SchemaGroup
import com.example.stockapp.data.local.StockItem
import com.example.stockapp.data.local.UploadDevice
import android.util.Log
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
import java.util.Locale

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

    val uploadDevices: StateFlow<List<UploadDevice>> = activeUserUid.flatMapLatest { uid ->
        if (uid.isNullOrBlank()) {
            flowOf(emptyList())
        } else {
            repository.getUploadDevices(uid)
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
    private val _createInProgress = MutableStateFlow(false)
    val createInProgress = _createInProgress.asStateFlow()

    /**
     * Creates a new user and inserts it into the database.
     * @param uid The unique identifier for the user.
     * @param password The user's password.
     */
    fun createUser(
        uid: String,
        password: String,
        onComplete: (Boolean) -> Unit = {}
    ) = viewModelScope.launch {
        if (_createInProgress.value) return@launch
        _createInProgress.value = true
        try {
            val created = repository.createUser(uid, password)
            onComplete(created)
        } catch (e: Exception) {
            Log.e("StockViewModel", "Create user failed", e)
            onComplete(false)
        } finally {
            _createInProgress.value = false
        }
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
        } catch (e: Exception) {
            Log.e("StockViewModel", "Login failed", e)
            _loginResult.value = false
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

    fun getItemsForTable(
        location: String,
        sid: String
    ): Flow<List<StockItem>> {
        val ownerUid = _activeUserUid.value ?: return flowOf(emptyList())
        return repository.getItemsForTable(ownerUid, location, sid)
    }

    suspend fun getItemsForTableSnapshot(
        location: String,
        sid: String
    ): List<StockItem> {
        val ownerUid = _activeUserUid.value ?: return emptyList()
        return repository
            .getItemsForTable(ownerUid, location, sid)
            .first()
    }

    fun getSchemaGroups(
        location: String,
        sid: String
    ): Flow<List<SchemaGroup>> {
        val ownerUid = _activeUserUid.value ?: return flowOf(emptyList())
        return repository.getSchemaGroups(ownerUid, location, sid)
    }

    fun getItemsForSchema(
        location: String,
        sid: String,
        schemaId: String
    ): Flow<List<StockItem>> {
        val ownerUid = _activeUserUid.value ?: return flowOf(emptyList())
        return repository.getItemsForSchema(ownerUid, location, sid, schemaId)
    }

    suspend fun uploadInventory(
        baseUrl: String,
        endpointPath: String,
        apiKey: String,
        stockItems: List<StockItem>,
        onProgress: suspend (current: Int, total: Int) -> Unit = { _, _ -> }
    ): Result<String> {
        val ownerUid = _activeUserUid.value
            ?: return Result.failure(IllegalStateException("No active user found."))
        return repository.uploadInventory(
            baseUrl = baseUrl,
            endpointPath = endpointPath,
            apiKey = apiKey,
            ownerUid = ownerUid,
            stockItems = stockItems,
            onProgress = onProgress
        )
    }

    suspend fun markItemsUploaded(
        stockItems: List<StockItem>,
        uploadedAt: Long = System.currentTimeMillis()
    ) {
        val ownerUid = _activeUserUid.value ?: return
        repository.markItemsUploaded(
            ownerUid = ownerUid,
            stockItemIds = stockItems.map { it.id },
            uploadedAt = uploadedAt
        )
    }

    fun updateTableLocation(
        oldLocation: String,
        oldSid: String,
        newLocation: String
    ) = viewModelScope.launch {
        val ownerUid = _activeUserUid.value ?: return@launch
        repository.updateTableLocation(
            ownerUid,
            oldLocation,
            oldSid,
            newLocation
        )
    }

    fun deleteTableGroup(
        location: String,
        sid: String
    ) = viewModelScope.launch {
        val ownerUid = _activeUserUid.value ?: return@launch
        repository.deleteTableGroup(ownerUid, location, sid)
    }

    fun updateStockItem(stockItem: StockItem) = viewModelScope.launch {
        val ownerUid = _activeUserUid.value ?: return@launch
        repository.updateStockItem(ownerUid, stockItem.copy(ownerUid = ownerUid))
    }

    fun deleteStockItem(stockItemId: String) = viewModelScope.launch {
        val ownerUid = _activeUserUid.value ?: return@launch
        repository.deleteStockItem(ownerUid, stockItemId)
    }

    fun addUploadDevice(
        name: String,
        baseUrl: String,
        endpointPath: String,
        apiKey: String,
        onComplete: (Result<Unit>) -> Unit = {}
    ) = viewModelScope.launch {
        val ownerUid = _activeUserUid.value
            ?: return@launch onComplete(Result.failure(IllegalStateException("No active user found.")))

        val trimmedName = name.trim()
        val trimmedBaseUrl = baseUrl.trim()
        val trimmedEndpointPath = endpointPath.trim().ifBlank { DEFAULT_UPLOAD_ENDPOINT_PATH }
        val trimmedApiKey = apiKey.trim()
        if (trimmedName.isBlank()) {
            return@launch onComplete(Result.failure(IllegalArgumentException("Device name is required.")))
        }
        if (trimmedBaseUrl.isBlank()) {
            return@launch onComplete(Result.failure(IllegalArgumentException("Base URL is required.")))
        }
        if (trimmedApiKey.isBlank()) {
            return@launch onComplete(Result.failure(IllegalArgumentException("API key is required.")))
        }

        runCatching {
            repository.upsertUploadDevice(
                UploadDevice(
                    ownerUid = ownerUid,
                    nameNormalized = trimmedName.lowercase(Locale.ROOT),
                    name = trimmedName,
                    baseUrl = trimmedBaseUrl,
                    endpointPath = trimmedEndpointPath,
                    apiKey = trimmedApiKey
                )
            )
        }.onSuccess {
            onComplete(Result.success(Unit))
        }.onFailure { error ->
            onComplete(Result.failure(error))
        }
    }

    companion object {
        private const val DEFAULT_UPLOAD_ENDPOINT_PATH = "/api/stock/upload"
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
