package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.entity.SupplyBatchEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SupplyBatchDao {
    @Query("SELECT * FROM supply_batches ORDER BY timestamp DESC")
    fun getAllBatches(): Flow<List<SupplyBatchEntity>>

    @Query("SELECT * FROM supply_batches WHERE id = :batchId")
    suspend fun getBatchById(batchId: Long): SupplyBatchEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBatch(batch: SupplyBatchEntity): Long

    @Query("DELETE FROM supply_batches WHERE id = :batchId")
    suspend fun deleteBatch(batchId: Long)
}
