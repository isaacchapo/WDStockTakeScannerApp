package com.example.stockapp.data.local

import androidx.room.Entity

/**
 * Stores a user-specific upload target/device configuration.
 *
 * Table rules:
 * - Table name: upload_device_table
 * - Primary key: (ownerUid, nameNormalized)
 */
@Entity(
    tableName = "upload_device_table",
    primaryKeys = ["ownerUid", "nameNormalized"]
)
data class UploadDevice(
    /** Owning user id (part of composite primary key). */
    val ownerUid: String,
    /** Normalized device name (lowercase/trimmed, part of composite primary key). */
    val nameNormalized: String,
    /** Display device name. */
    val name: String,
    /** Base URL for the upload endpoint. */
    val baseUrl: String,
    /** Relative path for the upload endpoint. */
    val endpointPath: String,
    /** API key used for uploads. */
    val apiKey: String
)
