package com.example.stockapp.data

import android.util.LruCache
import org.json.JSONObject

/**
 * Utility to extract fields from JSON stored in StockItem.variableData
 */
object JsonFieldExtractor {
    private const val MAX_CACHE_ENTRIES = 200
    private val cacheLock = Any()
    private val parsedCache = object : LruCache<String, Map<String, String>>(MAX_CACHE_ENTRIES) {}
    
    /**
     * Extracts all fields from JSON string
     */
    fun extractAllFields(jsonString: String): Map<String, String> {
        if (jsonString.isBlank()) return emptyMap()

        val cached = synchronized(cacheLock) { parsedCache.get(jsonString) }
        if (cached != null) return cached

        val parsed = try {
            val json = JSONObject(jsonString)
            val fields = mutableMapOf<String, String>()
            json.keys().forEach { key ->
                val value = json.opt(key)
                fields[key] = value?.toString() ?: ""
            }
            fields
        } catch (e: Exception) {
            emptyMap()
        }

        synchronized(cacheLock) { parsedCache.put(jsonString, parsed) }
        return parsed
    }

    /**
     * Gets all unique keys from multiple JSON strings
     */
    fun getUniqueKeys(jsonStrings: List<String>): Set<String> {
        val keys = mutableSetOf<String>()
        jsonStrings.forEach { jsonString ->
            keys.addAll(extractAllFields(jsonString).keys)
        }
        return keys
    }

    /**
     * Safely gets a field value, returns empty string if not found
     */
    fun getFieldValue(jsonString: String, fieldName: String): String {
        return try {
            val json = JSONObject(jsonString)
            json.optString(fieldName, "")
        } catch (e: Exception) {
            ""
        }
    }
}
