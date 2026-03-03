package com.example.stockapp.data.remote

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
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
        @Body request: StockUploadItemDto
    ): Response<ResponseBody>
}
