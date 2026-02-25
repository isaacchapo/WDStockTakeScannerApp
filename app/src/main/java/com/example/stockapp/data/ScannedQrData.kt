package com.example.stockapp.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ScannedQrData(
    @SerialName("itemId") val itemId: String,
    @SerialName("itemDescription") val description: String,
    @SerialName("currentStock") val quantity: Int
)
