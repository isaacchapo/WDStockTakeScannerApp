package com.example.stockapp.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedLocationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(savedLocation: SavedLocation)

    @Query(
        "SELECT * FROM saved_location_table " +
            "WHERE ownerUid = :ownerUid " +
            "ORDER BY location COLLATE NOCASE ASC"
    )
    fun getSavedLocations(ownerUid: String): Flow<List<SavedLocation>>

    @Query(
        "SELECT * FROM saved_location_table " +
            "WHERE ownerUid = :ownerUid AND locationNormalized = :locationNormalized " +
            "LIMIT 1"
    )
    suspend fun getSavedLocation(ownerUid: String, locationNormalized: String): SavedLocation?

    @Query(
        "DELETE FROM saved_location_table " +
            "WHERE ownerUid = :ownerUid AND locationNormalized = :locationNormalized"
    )
    suspend fun deleteSavedLocation(ownerUid: String, locationNormalized: String)
}
