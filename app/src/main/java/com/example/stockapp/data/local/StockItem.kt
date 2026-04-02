package com.example.stockapp.data.local

import android.annotation.SuppressLint
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * Represents a single stock item in the database with hybrid JSON storage.
 *
 * Table rules:
 * - Table name: stock_table
 * - Primary key: id
 *
 * @property id Unique record identifier.
 * @property sid Schema/session identifier (SID) used to group one stock-take table.
 * @property identifierKey Internal schema signature for the QR payload structure.
 * @property orderNo Legacy optional field kept for backward compatibility.
 * @property location Storage/location label used for grouping.
 * @property stockName User-provided stock take name used for display and uploads.
 * @property dateScanned Timestamp when the QR record was scanned.
 * @property variableData JSON payload that stores the scanned QR values.
 * @property ownerUid The account that owns this stock record.
 */

@SuppressLint("UnsafeOptInUsageError")
@Serializable
@Entity(
    tableName = "stock_table",
    indices = [
        Index(value = ["ownerUid", "dateScanned"]),
        Index(value = ["ownerUid", "location", "stockName", "identifierKey", "dateScanned"]),
        Index(value = ["ownerUid", "location", "sid", "identifierKey", "dateScanned"]),
        Index(value = ["ownerUid", "uploadedAt"])
    ]
)
data class StockItem(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val sid: String = "",
    val identifierKey: String = "",
    val orderNo: String? = null,
    val location: String = "",
    val stockName: String = "",
    val dateScanned: Long = System.currentTimeMillis(),
    val uploadedAt: Long? = null,
    val variableData: String = "{}",
    val ownerUid: String = ""
)
