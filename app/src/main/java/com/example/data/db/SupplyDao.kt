package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.entity.SupplyEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SupplyDao {
    @Query("SELECT * FROM supplies ORDER BY timestamp DESC")
    fun getAllSupplies(): Flow<List<SupplyEntity>>

    @Query("SELECT * FROM supplies")
    suspend fun getAllSuppliesList(): List<SupplyEntity>

    @Query("SELECT * FROM supplies WHERE productId = :productId ORDER BY timestamp DESC")
    fun getSuppliesForProduct(productId: Long): Flow<List<SupplyEntity>>

    @Query("SELECT * FROM supplies WHERE batchId = :batchId ORDER BY id ASC")
    fun getSuppliesForBatch(batchId: Long): Flow<List<SupplyEntity>>

    @Query("SELECT * FROM supplies WHERE batchId = :batchId ORDER BY id ASC")
    suspend fun getSuppliesForBatchSync(batchId: Long): List<SupplyEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSupply(supply: SupplyEntity): Long
}
