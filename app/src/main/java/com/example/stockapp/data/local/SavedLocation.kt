package com.example.stockapp.data.local

import androidx.room.Entity

/**
 * Stores a user-specific location with a stable SID.
 */
@Entity(
    tableName = "saved_location_table",
    primaryKeys = ["ownerUid", "locationNormalized"]
)
data class SavedLocation(
    val ownerUid: String,
    val locationNormalized: String,
    val location: String,
    val sid: String
)
