package com.example.stockapp.data.remote

import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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
import kotlin.math.max

data class UploadPolicy(
    val maxRetries: Int = 2,
    val baseDelayMs: Long = 400,
    val maxDelayMs: Long = 2000
)

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
        val trimmedBase = baseUrl.trim()
        if (trimmedBase.isBlank()) {
            return Result.failure(IllegalArgumentException("Base URL is required."))
        }

        val parsedBase = trimmedBase.toHttpUrlOrNull()
            ?: return Result.failure(IllegalArgumentException("Invalid URL: $trimmedBase"))

        if (parsedBase.scheme != "http" && parsedBase.scheme != "https") {
            return Result.failure(
                IllegalArgumentException("URL must start with http:// or https://")
            )
        }

        if (isLocalHost(parsedBase.host)) {
            return Result.failure(
                IllegalArgumentException(
                    "Do not use localhost from Android. Use the API server LAN IP (for example: http://192.168.1.15:5000). For Android Emulator use http://10.0.2.2:<port>."
                )
            )
        }

        fun normalizePath(path: String): String {
            if (path.isBlank() || path == "/") return "/"
            return path.trimEnd('/')
        }

        val rawEndpointPath = endpointPath.trim()
        val normalizedEndpoint = rawEndpointPath.takeIf { it.isNotBlank() }?.let { path ->
            val withSlash = if (path.startsWith("/")) path else "/$path"
            normalizePath(withSlash)
        }.orEmpty()

        val basePath = normalizePath(parsedBase.encodedPath)
        val finalPath = when {
            normalizedEndpoint.isBlank() -> basePath
            basePath != "/" && (basePath == normalizedEndpoint || basePath.endsWith(normalizedEndpoint)) -> basePath
            basePath != "/" && normalizedEndpoint.startsWith("$basePath/") -> normalizedEndpoint
            basePath == "/" -> normalizedEndpoint
            else -> basePath + normalizedEndpoint
        }

        val targetUrl = parsedBase.newBuilder()
            .encodedPath(finalPath)
            .build()
            .toString()

        return Result.success(targetUrl)
    }

    suspend fun uploadInventory(
        uploadUrl: String,
        items: List<StockUploadItemDto>,
        apiKey: String,
        policy: UploadPolicy = UploadPolicy(),
        onProgress: suspend (current: Int, total: Int) -> Unit = { _, _ -> }
    ): Result<String> {
        if (items.isEmpty()) {
            return Result.failure(IllegalArgumentException("No stock items found to upload."))
        }
        if (apiKey.isBlank()) {
            return Result.failure(IllegalArgumentException("API key is required for upload."))
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

                val itemResult = uploadSingleWithRetry(
                    uploadUrl = uploadUrl,
                    apiKey = apiKey,
                    item = item,
                    policy = policy,
                    itemIndex = index + 1,
                    totalItems = items.size
                )
                val successMessage = itemResult.getOrElse { return Result.failure(it) }
                lastSuccessMessage = successMessage
                onProgress(index + 1, items.size)
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

    private suspend fun uploadSingleWithRetry(
        uploadUrl: String,
        apiKey: String,
        item: StockUploadItemDto,
        policy: UploadPolicy,
        itemIndex: Int,
        totalItems: Int
    ): Result<String> {
        var attempt = 0
        while (true) {
            try {
                val response = api.uploadInventory(uploadUrl, apiKey, item)
                if (response.isSuccessful) {
                    return Result.success(parseSuccessMessage(response))
                }

                if (shouldRetry(response.code()) && attempt < policy.maxRetries) {
                    delay(backoffDelayMs(attempt, policy.baseDelayMs, policy.maxDelayMs))
                    attempt++
                    continue
                }

                val errorBody = response.errorBody()?.string() ?: "No error body"
                Log.e(TAG, "HTTP Error ${response.code()}: $errorBody")
                return Result.failure(
                    buildHttpError(
                        uploadUrl = uploadUrl,
                        response = response,
                        rawErrorBody = errorBody,
                        itemIndex = itemIndex,
                        totalItems = totalItems
                    )
                )
            } catch (e: IOException) {
                if (attempt < policy.maxRetries) {
                    delay(backoffDelayMs(attempt, policy.baseDelayMs, policy.maxDelayMs))
                    attempt++
                    continue
                }
                return Result.failure(
                    IllegalStateException(
                        "Network error while uploading item $itemIndex/$totalItems to $uploadUrl.",
                        e
                    )
                )
            } catch (e: Exception) {
                return Result.failure(e)
            }
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

    private fun shouldRetry(statusCode: Int): Boolean {
        return statusCode == 408 || statusCode == 429 || statusCode == 500 ||
            statusCode == 502 || statusCode == 503 || statusCode == 504
    }

    private fun backoffDelayMs(attempt: Int, baseDelayMs: Long, maxDelayMs: Long): Long {
        val exponential = baseDelayMs * (1 shl attempt)
        return maxDelayMs.coerceAtMost(max(exponential, baseDelayMs))
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
