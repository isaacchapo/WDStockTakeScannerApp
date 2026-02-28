package com.example.stockapp.data.remote

data class StockUploadItemDto(
    val itemId: String,
    val description: String,
    val quantity: Int,
    val location: String,
    val stockCode: String,
    val stockTakeId: String,
    val ownerUid: String
)

data class StockInventoryUploadRequest(
    val ownerUid: String,
    val location: String?,
    val stockTakeId: String?,
    val stockCode: String?,
    val items: List<StockUploadItemDto>
)

data class StockInventoryUploadResponse(
    val success: Boolean? = null,
    val message: String? = null
)
