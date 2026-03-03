package com.example.stockapp.data.remote

import com.google.gson.annotations.SerializedName

data class StockUploadItemDto(
    @SerializedName("itemId")
    val itemId: String,
    @SerializedName("description")
    val description: String,
    @SerializedName("qty")
    val qty: Int,
    @SerializedName("location")
    val location: String,
    @SerializedName("stockCode")
    val stockCode: String,
    @SerializedName("stockTakeId")
    val stockTakeId: String,
    @SerializedName("ownerUid")
    val ownerUid: String
)

data class StockInventoryUploadResponse(
    val success: Boolean? = null,
    val message: String? = null
)
