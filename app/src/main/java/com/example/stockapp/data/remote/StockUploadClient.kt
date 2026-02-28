package com.example.stockapp.data.remote

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class StockUploadClient {
    private val api: StockUploadApi by lazy {
        Retrofit.Builder()
            .baseUrl("http://localhost/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(StockUploadApi::class.java)
    }

    fun buildUploadUrl(baseUrl: String, endpointPath: String): Result<String> {
        val trimmedBase = baseUrl.trim().removeSuffix("/")
        if (trimmedBase.isBlank()) {
            return Result.failure(IllegalArgumentException("Base URL is required."))
        }

        val normalizedPath = endpointPath.trim().let { path ->
            when {
                path.isBlank() -> ""
                path.startsWith("/") -> path
                else -> "/$path"
            }
        }

        val targetUrl = "$trimmedBase$normalizedPath"
        return if (targetUrl.toHttpUrlOrNull() != null) {
            Result.success(targetUrl)
        } else {
            Result.failure(IllegalArgumentException("Invalid URL: $targetUrl"))
        }
    }

    suspend fun uploadInventory(
        uploadUrl: String,
        request: StockInventoryUploadRequest
    ): Result<String> {
        return try {
            val response = api.uploadInventory(uploadUrl, request)
            if (!response.isSuccessful) {
                val errorBody = response.errorBody()?.string()?.take(300)
                val details = if (errorBody.isNullOrBlank()) "" else ": $errorBody"
                Result.failure(
                    IllegalStateException("Upload failed with HTTP ${response.code()}$details")
                )
            } else {
                val body = response.body()
                if (body?.success == false) {
                    Result.failure(
                        IllegalStateException(body.message ?: "Server rejected the upload request.")
                    )
                } else {
                    Result.success(body?.message ?: "Stock data uploaded successfully.")
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
