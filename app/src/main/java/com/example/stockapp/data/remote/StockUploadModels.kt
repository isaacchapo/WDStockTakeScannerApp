package com.example.stockapp.data.remote

import com.google.gson.annotations.SerializedName

data class StockUploadItemDto(
    @SerializedName("id")
    val id: String,
    @SerializedName("sid")
    val sid: String,
    @SerializedName("identifierKey")
    val identifierKey: String,
    @SerializedName("orderNo")
    val orderNo: String?,
    @SerializedName("location")
    val location: String,
    @SerializedName("stockName")
    val stockName: String,
    @SerializedName("dateScanned")
    val dateScanned: Long,
    @SerializedName("variableData")
    val variableData: String,
    @SerializedName("ownerUid")
    val ownerUid: String,
    @SerializedName("uid")
    val uid: String,
    @SerializedName("password")
    val password: String
)

data class UnifiedStockUploadRequest(
    @SerializedName("action")
    val action: String,
    @SerializedName("payload")
    val payload: StockUploadItemDto
)

data class UnifiedStockBulkPayload(
    @SerializedName("items")
    val items: List<StockUploadItemDto>
)

data class UnifiedStockBulkUploadRequest(
    @SerializedName("action")
    val action: String,
    @SerializedName("payload")
    val payload: UnifiedStockBulkPayload
)

data class StockInventoryUploadResponse(
    val success: Boolean? = null,
    val message: String? = null
)
