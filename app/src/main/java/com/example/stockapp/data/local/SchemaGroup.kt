package com.example.stockapp.data.local

data class SchemaGroup(
    val sid: String,
    val ownerUid: String,
    val location: String,
    val schemaId: String,
    val sampleData: String,
    val totalRecords: Int,
    val lastScannedAt: Long
)
