package com.example.stockapp.ui.upload

import android.content.Context

private const val PREFS_NAME = "stock_upload_prefs"
private const val KEY_BASE_URL = "base_url"
private const val KEY_ENDPOINT_PATH = "endpoint_path"

const val DEFAULT_UPLOAD_ENDPOINT_PATH = "/api/stock/upload"

data class ApiUploadConfig(
    val baseUrl: String,
    val endpointPath: String
)

object ApiUploadPreferences {
    fun load(context: Context): ApiUploadConfig {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return ApiUploadConfig(
            baseUrl = prefs.getString(KEY_BASE_URL, "") ?: "",
            endpointPath = prefs.getString(KEY_ENDPOINT_PATH, DEFAULT_UPLOAD_ENDPOINT_PATH)
                ?: DEFAULT_UPLOAD_ENDPOINT_PATH
        )
    }

    fun save(
        context: Context,
        baseUrl: String,
        endpointPath: String
    ) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_BASE_URL, baseUrl)
            .putString(KEY_ENDPOINT_PATH, endpointPath)
            .apply()
    }
}
