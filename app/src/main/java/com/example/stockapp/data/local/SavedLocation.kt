package com.example.stockapp.data.local

import androidx.room.Entity

/**
 * Stores a user-specific location with a stable SID.
 *
 * Table rules:
 * - Table name: saved_location_table
 * - Primary key: (ownerUid, locationNormalized)
 */
@Entity(
    tableName = "saved_location_table",
    primaryKeys = ["ownerUid", "locationNormalized"]
)
data class SavedLocation(
    /** Owning user id (part of composite primary key). */
    val ownerUid: String,
    /** Normalized location (lowercase/trimmed, part of composite primary key). */
    val locationNormalized: String,
    /** Display location string. */
    val location: String,
    /** Stable SID used for grouping stock records. */
    val sid: String
)
