package com.example.stockapp.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Url

interface StockUploadApi {
    @POST
    suspend fun uploadInventory(
        @Url url: String,
        @Body request: StockInventoryUploadRequest
    ): Response<StockInventoryUploadResponse>
}
