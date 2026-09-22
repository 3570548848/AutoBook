package com.example.autobook.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Query("SELECT * FROM txn ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<Transaction>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(t: Transaction): Long

    @Delete
    suspend fun delete(t: Transaction)
}
