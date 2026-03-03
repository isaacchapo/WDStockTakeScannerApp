package com.example.stockapp.ui.home

import android.os.SystemClock
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
import org.json.JSONObject

class CreateStockViewModel(
    private val repository: StockRepository,
    private val ownerUid: String
) : ViewModel() {
    private val scanJson = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val _scannedItems = MutableStateFlow<List<StockItem>>(emptyList())
    val scannedItems = _scannedItems.asStateFlow()
    private val _lastSid = MutableStateFlow("")
    val lastSid = _lastSid.asStateFlow()
    private val _isSaving = MutableStateFlow(false)
    val isSaving = _isSaving.asStateFlow()
    private var lastLocationForSid: String = ""
    private var lastAcceptedRawScan: String = ""
    private var lastAcceptedAtMs: Long = 0L

    fun addScannedItem(barcode: String) {
        val cleanedBarcode = barcode.trim()
        Log.d("Scanner", "Processing barcode: '$cleanedBarcode'")

        val nowMs = SystemClock.elapsedRealtime()
        if (cleanedBarcode == lastAcceptedRawScan && nowMs - lastAcceptedAtMs < 750L) {
            Log.d("Scanner", "Ignoring duplicate scan payload")
            return
        }

        val scannedData = parseScannedPayload(cleanedBarcode)
        if (scannedData == null) {
            Log.e("Scanner", "Error decoding barcode JSON. Raw data: '$cleanedBarcode'")
            return
        }

        val scannedItem = StockItem(
            itemId = scannedData.itemId,
            description = scannedData.description,
            quantity = scannedData.quantity,
            location = "",
            stockCode = "",
            stockTakeId = "",
            ownerUid = ownerUid
        )

        val currentList = _scannedItems.value.toMutableList()
        val existingItem = currentList.find { it.itemId == scannedItem.itemId }
        if (existingItem != null) {
            lastAcceptedRawScan = cleanedBarcode
            lastAcceptedAtMs = nowMs
            Log.d(
                "Scanner",
                "Item ${scannedItem.itemId} already exists in temporary table. Keeping quantity fixed at ${existingItem.quantity}."
            )
            return
        }

        currentList.add(scannedItem)
        _scannedItems.value = currentList
        lastAcceptedRawScan = cleanedBarcode
        lastAcceptedAtMs = nowMs
        Log.d("Scanner", "Staged item in temporary scan list: ${scannedItem.itemId}")
    }

    fun confirmScannedItems(
        location: String,
        stockTakeId: String,
        onComplete: (Boolean) -> Unit = {}
    ) {
        if (ownerUid.isBlank() || _isSaving.value) {
            onComplete(false)
            return
        }
        val tempItems = _scannedItems.value
        if (tempItems.isEmpty()) {
            onComplete(false)
            return
        }

        viewModelScope.launch {
            _isSaving.value = true
            try {
                val sidForThisBatch = if (_lastSid.value.isBlank()) generateSid() else _lastSid.value

                val itemsToSave = tempItems.map { item ->
                    item.copy(
                        location = location,
                        stockTakeId = stockTakeId,
                        stockCode = sidForThisBatch,
                        ownerUid = ownerUid
                    )
                }

                repository.insertAll(itemsToSave)
                _scannedItems.value = emptyList()
                // Prepare a fresh SID so the next "Confirm" creates a new inventory row/group.
                _lastSid.value = generateSid()
                onComplete(true)
            } catch (e: Exception) {
                Log.e("Scanner", "Error saving scanned items", e)
                onComplete(false)
            } finally {
                _isSaving.value = false
            }
        }
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

    private fun parseScannedPayload(rawValue: String): ScannedQrData? {
        val trimmed = rawValue.trim().removePrefix("\uFEFF")
        if (trimmed.isBlank()) return null

        val candidates = linkedSetOf<String>()
        candidates.add(trimmed)

        val noParens = unwrapSurroundingParentheses(trimmed)
        if (noParens.isNotBlank()) candidates.add(noParens)

        extractJsonObject(trimmed)?.let(candidates::add)
        extractJsonObject(noParens)?.let(candidates::add)

        decodeQuotedJson(trimmed)?.let { decoded ->
            candidates.add(decoded)
            extractJsonObject(decoded)?.let(candidates::add)
        }

        for (candidate in candidates) {
            parseJsonCandidate(candidate)?.let { return it }
        }
        return null
    }

    private fun parseJsonCandidate(candidate: String): ScannedQrData? {
        runCatching { scanJson.decodeFromString<ScannedQrData>(candidate) }
            .getOrNull()
            ?.let { return it }

        return runCatching {
            val obj = JSONObject(candidate)
            val itemId = obj.optString("itemId").trim()
            val description = obj.optString("itemDescription")
                .ifBlank { obj.optString("description") }
                .trim()
            val quantity = parseQuantity(obj.opt("currentStock"))
                ?: parseQuantity(obj.opt("quantity"))

            if (itemId.isBlank() || description.isBlank() || quantity == null) {
                null
            } else {
                ScannedQrData(
                    itemId = itemId,
                    description = description,
                    quantity = quantity
                )
            }
        }.getOrNull()
    }

    private fun parseQuantity(value: Any?): Int? {
        return when (value) {
            is Int -> value
            is Long -> value.toInt()
            is Double -> value.toInt()
            is Float -> value.toInt()
            is String -> value.trim().toIntOrNull()
            else -> null
        }
    }

    private fun decodeQuotedJson(value: String): String? {
        if (!(value.startsWith("\"") && value.endsWith("\""))) return null
        return runCatching { Json.decodeFromString<String>(value).trim() }.getOrNull()
    }

    private fun extractJsonObject(value: String): String? {
        val start = value.indexOf('{')
        val end = value.lastIndexOf('}')
        if (start == -1 || end == -1 || end <= start) return null
        return value.substring(start, end + 1).trim()
    }

    private fun unwrapSurroundingParentheses(value: String): String {
        var result = value.trim()
        while (result.length > 2 && result.startsWith("(") && result.endsWith(")")) {
            result = result.substring(1, result.length - 1).trim()
        }
        return result
    }
}
