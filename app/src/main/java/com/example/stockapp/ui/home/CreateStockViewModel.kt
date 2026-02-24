package com.example.stockapp.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.stockapp.data.StockRepository
import com.example.stockapp.data.local.StockItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class CreateStockViewModel(private val repository: StockRepository) : ViewModel() {

    private val _scannedItems = MutableStateFlow<List<StockItem>>(emptyList())
    val scannedItems = _scannedItems.asStateFlow()

    fun addScannedItem(barcode: String) {
        try {
            val item = Json.decodeFromString<StockItem>(barcode)
            val currentList = _scannedItems.value.toMutableList()
            if (currentList.none { it.itemId == item.itemId }) {
                currentList.add(item)
                _scannedItems.value = currentList
            }
        } catch (e: Exception) {
            // Handle decoding error if necessary
        }
    }

    fun saveScannedItems(location: String, stockTakeId: String, stockCode: String) {
        viewModelScope.launch {
            val itemsToSave = _scannedItems.value.map {
                it.copy(location = location, stockTakeId = stockTakeId, stockCode = stockCode)
            }
            repository.insertAll(itemsToSave)
            _scannedItems.value = emptyList()
        }
    }
}