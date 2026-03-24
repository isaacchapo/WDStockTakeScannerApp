package com.example.stockapp.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for the user_table.
 */
@Dao
interface UserDao {

    /**
     * Inserts a user into the table, replacing them if they already exist.
     * @param user The user to be inserted.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(user: User)

    @Query("SELECT EXISTS(SELECT 1 FROM user_table WHERE uid = :uid)")
    suspend fun userExists(uid: String): Boolean

    /**
     * @param uid The unique identifier of the user.
     * @return a flow of the user from the table.
     */
    @Query("SELECT * FROM user_table WHERE uid = :uid")
    fun getUser(uid: String): Flow<User?>

    @Query("SELECT * FROM user_table WHERE uid = :uid LIMIT 1")
    suspend fun getUserSnapshot(uid: String): User?
}
