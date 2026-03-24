package com.example.stockapp.data.local

import androidx.room.Entity

/**
 * Stores a user-specific upload target/device configuration.
 */
@Entity(
    tableName = "upload_device_table",
    primaryKeys = ["ownerUid", "nameNormalized"]
)
data class UploadDevice(
    val ownerUid: String,
    val nameNormalized: String,
    val name: String,
    val baseUrl: String,
    val endpointPath: String,
    val apiKey: String
)
