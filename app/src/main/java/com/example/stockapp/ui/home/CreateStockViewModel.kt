package com.example.stockapp.ui.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.stockapp.data.ScannedQrData
import com.example.stockapp.data.StockRepository
import com.example.stockapp.data.local.StockItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class CreateStockViewModel(
    private val repository: StockRepository,
    private val ownerUid: String
) : ViewModel() {

    private val _scannedItems = MutableStateFlow<List<StockItem>>(emptyList())
    val scannedItems = _scannedItems.asStateFlow()

    private val _pendingItem = MutableStateFlow<StockItem?>(null)
    val pendingItem = _pendingItem.asStateFlow()
    private val _lastSid = MutableStateFlow("")
    val lastSid = _lastSid.asStateFlow()
    private var lastLocationForSid: String = ""

    fun addScannedItem(barcode: String) {
        if (_pendingItem.value != null) {
            return
        }

        Log.d("Scanner", "Scanned barcode: $barcode")
        try {
            val scannedData = Json.decodeFromString<ScannedQrData>(barcode)
            val newItem = StockItem(
                itemId = scannedData.itemId,
                description = scannedData.description,
                quantity = scannedData.quantity,
                location = "",
                stockCode = "",
                stockTakeId = "",
                ownerUid = ownerUid
            )
            _pendingItem.value = newItem
        } catch (e: Exception) {
            Log.e("Scanner", "Error decoding barcode", e)
        }
    }

    fun confirmPendingItem(location: String, stockTakeId: String) {
        if (ownerUid.isBlank()) {
            return
        }
        val pending = _pendingItem.value ?: return

        viewModelScope.launch {
            val sid = if (_lastSid.value.isBlank()) generateSid() else _lastSid.value
            _lastSid.value = sid
            val itemToSave = pending.copy(
                location = location,
                stockTakeId = stockTakeId,
                stockCode = sid,
                ownerUid = ownerUid
            )

            repository.insert(itemToSave)

            val currentList = _scannedItems.value.toMutableList()
            val existingItem = currentList.find { it.itemId == itemToSave.itemId }

            if (existingItem != null) {
                val updatedItem = existingItem.copy(quantity = existingItem.quantity + itemToSave.quantity)
                val index = currentList.indexOf(existingItem)
                currentList[index] = updatedItem
            } else {
                currentList.add(itemToSave)
            }
            _scannedItems.value = currentList
            _pendingItem.value = null
            _lastSid.value = sid
        }
    }

    fun clearScannedItems() {
        _pendingItem.value = null
        _scannedItems.value = emptyList()
        _lastSid.value = ""
        lastLocationForSid = ""
    }

    fun updateSidForLocation(location: String) {
        if (location.isBlank()) {
            _lastSid.value = ""
            lastLocationForSid = ""
            return
        }

        if (location != lastLocationForSid) {
            _lastSid.value = generateSid()
            lastLocationForSid = location
        }
    }

    private fun generateSid(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..6)
            .map { chars.random() }
            .joinToString("")
    }
}
