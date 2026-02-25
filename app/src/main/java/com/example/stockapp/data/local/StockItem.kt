package com.example.stockapp.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents a single stock item in the database.
 *
 * @property id The unique identifier for the stock item.
 * @property description The description of the stock item.
 * @property quantity The current quantity of the stock item.
 * @property location The location of the stock item in the warehouse.
 * @property stockCode The stock code of the item.
 * @property stockTakeId The unique identifier for the stock take.
 */
@Serializable
@Entity(tableName = "stock_table")
data class StockItem(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    @SerialName("itemId") val itemId: String,
    @SerialName("itemName") val description: String,
    @SerialName("currentStock") val quantity: Int,
    val location: String,
    val stockCode: String,
    val stockTakeId: String
)
