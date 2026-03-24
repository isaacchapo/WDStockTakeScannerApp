package com.example.stockapp.ui.home

import android.os.SystemClock
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.stockapp.data.QrDataParser
import com.example.stockapp.data.StockRepository
import com.example.stockapp.data.local.SavedLocation
import com.example.stockapp.data.local.StockItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.security.SecureRandom
import java.util.Locale

class CreateStockViewModel(
    private val repository: StockRepository,
    private val ownerUid: String
) : ViewModel() {
    data class ScanAddResult(
        val accepted: Boolean,
        val message: String? = null
    )

    data class ConfirmSaveResult(
        val success: Boolean,
        val savedCount: Int = 0,
        val duplicateCount: Int = 0
    )

    private val _scannedItems = MutableStateFlow<List<StockItem>>(emptyList())
    val scannedItems = _scannedItems.asStateFlow()
    private val _currentSid = MutableStateFlow(generateSid())
    val currentSid = _currentSid.asStateFlow()
    private val _savedLocations = MutableStateFlow<List<SavedLocation>>(emptyList())
    val savedLocations = _savedLocations.asStateFlow()
    private val _isSaving = MutableStateFlow(false)
    val isSaving = _isSaving.asStateFlow()
    private var lastAcceptedRawScan: String = ""
    private var lastAcceptedAtMs: Long = 0L
    private val seenScanFingerprints = linkedSetOf<String>()
    private val scanMutex = Mutex()

    init {
        viewModelScope.launch {
            repository.getSavedLocations(ownerUid).collectLatest { locations ->
                _savedLocations.value = locations
            }
        }
    }

    fun addScannedItem(
        barcode: String,
        onResult: (ScanAddResult) -> Unit = {}
    ) {
        viewModelScope.launch {
            val result = scanMutex.withLock {
                addScannedItemInternal(barcode)
            }
            onResult(result)
        }
    }

    private suspend fun addScannedItemInternal(barcode: String): ScanAddResult {
        val cleanedBarcode = barcode.trim()
        Log.d("Scanner", "Processing barcode: '$cleanedBarcode'")

        val nowMs = SystemClock.elapsedRealtime()
        // Ignore only immediate scanner bounce repeats from a single trigger pull.
        if (cleanedBarcode == lastAcceptedRawScan && nowMs - lastAcceptedAtMs < 250L) {
            Log.d("Scanner", "Ignoring duplicate scan payload")
            return ScanAddResult(
                accepted = false,
                message = "Duplicate scan ignored."
            )
        }

        val parsedData = withContext(Dispatchers.Default) {
            QrDataParser.parseQrData(cleanedBarcode)
        }
        if (parsedData == null) {
            Log.e("Scanner", "Error decoding barcode JSON. Raw data: '$cleanedBarcode'")
            return ScanAddResult(
                accepted = false,
                message = "Invalid QR payload. Scan a valid product QR code."
            )
        }

        val fingerprint = withContext(Dispatchers.Default) {
            buildScanFingerprint(parsedData.fields, parsedData.variableData)
        }
        if (fingerprint.isNotBlank() && seenScanFingerprints.contains(fingerprint)) {
            Log.d("Scanner", "Ignoring already buffered QR values")
            return ScanAddResult(
                accepted = false,
                message = "Duplicate QR values ignored."
            )
        }

        val activeSid = _currentSid.value.ifBlank {
            val generated = generateSid()
            _currentSid.value = generated
            generated
        }

        val scannedItem = StockItem(
            sid = activeSid,
            identifierKey = buildIdentifierKey(
                fields = parsedData.fields,
                fallbackPayload = parsedData.variableData
            ),
            orderNo = null,
            location = "",
            dateScanned = System.currentTimeMillis(),
            variableData = parsedData.variableData,
            ownerUid = ownerUid
        )

        _scannedItems.value = _scannedItems.value + scannedItem
        if (fingerprint.isNotBlank()) {
            seenScanFingerprints.add(fingerprint)
        }
        lastAcceptedRawScan = cleanedBarcode
        lastAcceptedAtMs = nowMs
        Log.d("Scanner", "Staged item in temporary scan list")
        return ScanAddResult(accepted = true)
    }

    fun confirmScannedItems(
        location: String,
        stockName: String,
        onComplete: (ConfirmSaveResult) -> Unit = {}
    ) {
        val normalizedLocation = normalizeLocation(location)
        val trimmedStockName = stockName.trim()
        if (
            ownerUid.isBlank() ||
            normalizedLocation.isBlank() ||
            trimmedStockName.isBlank() ||
            _isSaving.value
        ) {
            onComplete(ConfirmSaveResult(success = false))
            return
        }
        val tempItems = _scannedItems.value
        val activeSid = _currentSid.value
        if (
            tempItems.isEmpty() ||
            activeSid.isBlank()
        ) {
            onComplete(ConfirmSaveResult(success = false))
            return
        }

        viewModelScope.launch {
            _isSaving.value = true
            try {
                val persistedItems = repository.getAllStockItemsSnapshot(ownerUid)
                val savedLocationsSnapshot = _savedLocations.value
                val prepared = withContext(Dispatchers.Default) {
                    val persistedFingerprints = persistedItems
                        .mapNotNull { persistedItem ->
                            buildFingerprintForItem(persistedItem).takeIf { it.isNotBlank() }
                        }
                        .toHashSet()
                    val existingLocation = savedLocationsSnapshot
                        .firstOrNull { it.locationNormalized == normalizedLocation }
                    val sessionSid = existingLocation?.sid
                        ?.takeIf { it.isNotBlank() }
                        ?: activeSid
                    val locationDisplayName = existingLocation?.location ?: location.trim()
                    val stockNameDisplayName = trimmedStockName

                    val itemsToSave = tempItems.map { item ->
                        item.copy(
                            sid = sessionSid,
                            identifierKey = buildIdentifierKeyFromItem(item),
                            location = locationDisplayName,
                            stockName = stockNameDisplayName,
                            ownerUid = ownerUid
                        )
                    }
                    val newItemsToSave = mutableListOf<StockItem>()
                    var duplicateCount = 0

                    itemsToSave.forEach { item ->
                        val fingerprint = buildFingerprintForItem(item)
                        val alreadyExists = fingerprint.isNotBlank() && persistedFingerprints.contains(fingerprint)
                        if (alreadyExists) {
                            duplicateCount += 1
                        } else {
                            newItemsToSave += item
                            if (fingerprint.isNotBlank()) {
                                persistedFingerprints.add(fingerprint)
                            }
                        }
                    }

                    PreparedSaveResult(
                        sessionSid = sessionSid,
                        locationDisplayName = locationDisplayName,
                        newItemsToSave = newItemsToSave,
                        duplicateCount = duplicateCount
                    )
                }

                if (prepared.newItemsToSave.isNotEmpty()) {
                    repository.upsertSavedLocation(
                        SavedLocation(
                            ownerUid = ownerUid,
                            locationNormalized = normalizedLocation,
                            location = prepared.locationDisplayName,
                            sid = prepared.sessionSid
                        )
                    )

                    repository.insertAll(prepared.newItemsToSave)
                }
                resetActiveScanSession()
                onComplete(
                    ConfirmSaveResult(
                        success = true,
                        savedCount = prepared.newItemsToSave.size,
                        duplicateCount = prepared.duplicateCount
                    )
                )
            } catch (e: Exception) {
                Log.e("Scanner", "Error saving scanned items", e)
                onComplete(ConfirmSaveResult(success = false))
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun removeScannedItem(itemId: String) {
        val currentList = _scannedItems.value.toMutableList()
        currentList.removeAll { it.id == itemId }
        _scannedItems.value = currentList
        rebuildScanFingerprints(currentList)
        if (currentList.isEmpty()) {
            resetActiveScanSession()
        }
    }

    fun clearScannedItems(resetSession: Boolean = false) {
        _scannedItems.value = emptyList()
        seenScanFingerprints.clear()
        if (resetSession) {
            resetActiveScanSession()
        }
    }

    fun addLocation(rawLocation: String, onComplete: (Boolean) -> Unit = {}) {
        if (ownerUid.isBlank()) {
            onComplete(false)
            return
        }

        val trimmedLocation = rawLocation.trim()
        val normalized = normalizeLocation(trimmedLocation)
        if (normalized.isBlank()) {
            onComplete(false)
            return
        }

        val existing = _savedLocations.value.firstOrNull { it.locationNormalized == normalized }
        if (existing != null) {
            onComplete(true)
            return
        }

        viewModelScope.launch {
            runCatching {
                repository.upsertSavedLocation(
                    SavedLocation(
                        ownerUid = ownerUid,
                        locationNormalized = normalized,
                        location = trimmedLocation,
                        sid = _currentSid.value
                    )
                )
            }.onSuccess {
                onComplete(true)
            }.onFailure { error ->
                Log.e("Scanner", "Error saving location", error)
                onComplete(false)
            }
        }
    }

    private fun normalizeLocation(rawLocation: String): String {
        return rawLocation.trim().lowercase(Locale.ROOT)
    }

    private fun resetActiveScanSession() {
        _scannedItems.value = emptyList()
        _currentSid.value = generateSid()
        lastAcceptedRawScan = ""
        lastAcceptedAtMs = 0L
        seenScanFingerprints.clear()
    }

    private fun rebuildScanFingerprints(items: List<StockItem>) {
        seenScanFingerprints.clear()
        items.forEach { item ->
            val fingerprint = buildFingerprintForItem(item)
            if (fingerprint.isNotBlank()) {
                seenScanFingerprints.add(fingerprint)
            }
        }
    }

    private fun buildFingerprintForItem(item: StockItem): String {
        val parsed = QrDataParser.parseQrData(item.variableData)
        return buildScanFingerprint(
            fields = parsed?.fields.orEmpty(),
            fallbackPayload = item.variableData
        )
    }

    private fun buildIdentifierKeyFromItem(item: StockItem): String {
        val parsed = QrDataParser.parseQrData(item.variableData)
        return buildIdentifierKey(
            fields = parsed?.fields.orEmpty(),
            fallbackPayload = item.variableData
        )
    }

    private fun buildScanFingerprint(
        fields: Map<String, String>,
        fallbackPayload: String
    ): String {
        if (fields.isEmpty()) {
            return normalizeFingerprintValue(fallbackPayload)
        }

        val dedupeKeys = resolveDedupeKeys(fields)

        return dedupeKeys
            .map { rawKey ->
                val key = normalizeFingerprintKey(rawKey)
                val value = normalizeFingerprintValue(QrDataParser.getFieldValue(fields, rawKey))
                key to value
            }
            .sortedWith(compareBy<Pair<String, String>> { it.first }.thenBy { it.second })
            .joinToString(separator = "|") { (key, value) -> "$key=$value" }
    }

    private fun resolveDedupeKeys(fields: Map<String, String>): List<String> {
        val preferred = QrDataParser.selectMajorIdentifierKeys(
            fields = fields,
            requiredCount = 4
        )
        if (preferred.isNotEmpty()) return preferred

        return fields.keys
            .filter { it.isNotBlank() }
            .take(4)
    }

    private fun normalizeFingerprintKey(raw: String): String {
        return raw.trim()
            .lowercase(Locale.ROOT)
            .replace(WHITESPACE_REGEX, " ")
            .replace(KEY_SYMBOL_REGEX, "")
    }

    private fun normalizeFingerprintValue(raw: String): String {
        return raw.trim()
            .lowercase(Locale.ROOT)
            .replace(WHITESPACE_REGEX, " ")
    }

    private fun buildIdentifierKey(
        fields: Map<String, String>,
        fallbackPayload: String
    ): String {
        val normalizedKeys = fields.keys
            .map(::normalizeFingerprintKey)
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()

        if (normalizedKeys.isNotEmpty()) {
            return normalizedKeys.joinToString("|")
        }

        return normalizeFingerprintValue(fallbackPayload).ifBlank { "data" }
    }

    private fun generateSid(length: Int = 6): String {
        return buildString(length) {
            repeat(length) {
                append(SID_ALPHABET[sidRandom.nextInt(SID_ALPHABET.length)])
            }
        }
    }

    private data class PreparedSaveResult(
        val sessionSid: String,
        val locationDisplayName: String,
        val newItemsToSave: List<StockItem>,
        val duplicateCount: Int
    )

    private companion object {
        const val SID_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        val WHITESPACE_REGEX = Regex("\\s+")
        val KEY_SYMBOL_REGEX = Regex("[^a-z0-9]")
        val sidRandom = SecureRandom()
    }
}
