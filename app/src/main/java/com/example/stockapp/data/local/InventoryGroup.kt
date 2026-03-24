package com.example.stockapp.data.local

data class InventoryGroup(
    val ownerUid: String,
    val location: String,
    val sid: String,
    val stockName: String,
    val totalRecords: Int,
    val schemaCount: Int,
    val isUploaded: Boolean,
    val lastScannedAt: Long
)
