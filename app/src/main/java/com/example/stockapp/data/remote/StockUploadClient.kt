package com.example.stockapp.data.remote

import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.Response
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

class StockUploadClient {
    private companion object {
        const val TAG = "StockUploadClient"
        const val HOST_REACHABILITY_TIMEOUT_MS = 4000
    }

    private val gson = Gson()
    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .callTimeout(90, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    private val api: StockUploadApi by lazy {
        Retrofit.Builder()
            .baseUrl("http://localhost/")
            .client(httpClient)
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
        val parsedUrl = targetUrl.toHttpUrlOrNull()
            ?: return Result.failure(IllegalArgumentException("Invalid URL: $targetUrl"))

        if (parsedUrl.scheme != "http" && parsedUrl.scheme != "https") {
            return Result.failure(
                IllegalArgumentException("URL must start with http:// or https://")
            )
        }

        if (isLocalHost(parsedUrl.host)) {
            return Result.failure(
                IllegalArgumentException(
                    "Do not use localhost from Android. Use the API server LAN IP (for example: http://192.168.1.15:5000). For Android Emulator use http://10.0.2.2:<port>."
                )
            )
        }

        return Result.success(targetUrl)
    }

    suspend fun uploadInventory(
        uploadUrl: String,
        items: List<StockUploadItemDto>
    ): Result<String> {
        if (items.isEmpty()) {
            return Result.failure(IllegalArgumentException("No stock items found to upload."))
        }

        val parsedUrl = uploadUrl.toHttpUrlOrNull()
            ?: return Result.failure(IllegalArgumentException("Invalid URL: $uploadUrl"))

        val reachabilityCheck = verifyHostReachable(parsedUrl)
        val reachabilityError = reachabilityCheck.exceptionOrNull()
        if (reachabilityError != null) {
            return Result.failure(reachabilityError)
        }

        return try {
            var lastSuccessMessage = "Stock data uploaded successfully."

            // Upload items one by one because the server expects a single object, not an array
            items.forEachIndexed { index, item ->
                val jsonPayload = gson.toJson(item)
                Log.d(TAG, "Uploading item ${index + 1}/${items.size} to $uploadUrl")
                Log.d(TAG, "Payload: $jsonPayload")

                val response = api.uploadInventory(uploadUrl, item)
                if (!response.isSuccessful) {
                    val errorBody = response.errorBody()?.string() ?: "No error body"
                    Log.e(TAG, "HTTP Error ${response.code()}: $errorBody")
                    
                    return Result.failure(
                        buildHttpError(
                            uploadUrl = uploadUrl,
                            response = response,
                            rawErrorBody = errorBody,
                            itemIndex = index + 1,
                            totalItems = items.size
                        )
                    )
                }
                lastSuccessMessage = parseSuccessMessage(response)
            }

            if (items.size > 1) {
                Result.success(
                    "Uploaded ${items.size} item(s) successfully. ${lastSuccessMessage.trim()}"
                )
            } else {
                Result.success(lastSuccessMessage)
            }
        } catch (e: IOException) {
            Log.e(TAG, "Network error", e)
            Result.failure(
                IllegalStateException(
                    "Network timeout/error while uploading to ${parsedUrl.host}:${parsedUrl.port}. Verify phone and API server are on the same network and firewall allows this port.",
                    e
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error", e)
            Result.failure(e)
        }
    }

    private fun parseSuccessMessage(response: Response<okhttp3.ResponseBody>): String {
        val raw = response.body()?.string()?.trim().orEmpty()
        if (raw.isBlank()) {
            return "Stock data uploaded successfully."
        }

        return try {
            val parsed = gson.fromJson(raw, StockInventoryUploadResponse::class.java)
            if (parsed?.success == false) {
                throw IllegalStateException(parsed.message ?: "Server rejected the upload request.")
            } else {
                parsed?.message?.takeIf { it.isNotBlank() } ?: raw
            }
        } catch (_: Exception) {
            raw
        }
    }

    private fun buildHttpError(
        uploadUrl: String,
        response: Response<okhttp3.ResponseBody>,
        rawErrorBody: String,
        itemIndex: Int? = null,
        totalItems: Int? = null
    ): IllegalStateException {
        val details = if (rawErrorBody.isBlank()) "" else ": $rawErrorBody"
        val itemDetails = if (itemIndex != null && totalItems != null) {
            " (item $itemIndex/$totalItems)"
        } else {
            ""
        }
        return IllegalStateException(
            "Upload failed at $uploadUrl$itemDetails with HTTP ${response.code()}$details"
        )
    }

    private suspend fun verifyHostReachable(uploadUrl: HttpUrl): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            Socket().use { socket ->
                socket.connect(
                    InetSocketAddress(uploadUrl.host, uploadUrl.port),
                    HOST_REACHABILITY_TIMEOUT_MS
                )
            }
            Result.success(Unit)
        } catch (e: UnknownHostException) {
            Result.failure(
                IllegalStateException(
                    "Cannot resolve ${uploadUrl.host}. Use the API server LAN IP that is reachable from this device.",
                    e
                )
            )
        } catch (e: IOException) {
            Result.failure(
                IllegalStateException(
                    "Cannot reach ${uploadUrl.host}:${uploadUrl.port} from this device. Check same Wi-Fi/LAN, port exposure, and firewall rules.",
                    e
                )
            )
        }
    }

    private fun isLocalHost(host: String): Boolean {
        return host.equals("localhost", ignoreCase = true) || host == "127.0.0.1"
    }
}
