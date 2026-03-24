package com.example.stockapp.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface UploadDeviceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(uploadDevice: UploadDevice)

    @Query(
        "SELECT * FROM upload_device_table " +
            "WHERE ownerUid = :ownerUid " +
            "ORDER BY name COLLATE NOCASE ASC"
    )
    fun getUploadDevices(ownerUid: String): Flow<List<UploadDevice>>
}
