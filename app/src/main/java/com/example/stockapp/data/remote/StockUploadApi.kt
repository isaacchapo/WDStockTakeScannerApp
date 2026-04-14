package com.example.stockapp.data.remote

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Url

interface StockUploadApi {
    @Headers(
        "Accept: application/json",
        "Content-Type: application/json"
    )
    @POST
    suspend fun uploadInventory(
        @Url url: String,
        @Header("X-Api-Key") apiKey: String,
        @Body request: StockUploadItemDto
    ): Response<ResponseBody>

    @Headers(
        "Accept: application/json",
        "Content-Type: application/json"
    )
    @POST
    suspend fun uploadInventoryBulk(
        @Url url: String,
        @Header("X-Api-Key") apiKey: String,
        @Body request: List<StockUploadItemDto>
    ): Response<ResponseBody>
}
