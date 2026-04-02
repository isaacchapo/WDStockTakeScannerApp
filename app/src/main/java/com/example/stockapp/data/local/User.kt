package com.example.stockapp.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a single user in the database.
 *
 * Table rules:
 * - Table name: user_table
 * - Primary key: uid
 *
 * @property uid The unique identifier for the user.
 * @property passwordHash The user's hashed password for security.
 */
@Entity(tableName = "user_table")
data class User(
    @PrimaryKey val uid: String,
    val passwordHash: String
)
