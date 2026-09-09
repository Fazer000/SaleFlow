package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.entity.TransactionEntity
import com.example.data.entity.TransactionItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE shiftId = :shiftId ORDER BY timestamp DESC")
    fun getTransactionsForShift(shiftId: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE shiftId = :shiftId")
    suspend fun getTransactionsForShiftSync(shiftId: Long): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE customerId = :customerId ORDER BY timestamp DESC")
    fun getTransactionsForCustomer(customerId: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE customerId = :customerId")
    suspend fun getTransactionsForCustomerSync(customerId: Long): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionById(id: Long): TransactionEntity?

    @Query("SELECT * FROM transaction_items WHERE transactionId = :transactionId")
    fun getItemsForTransaction(transactionId: Long): Flow<List<TransactionItemEntity>>

    @Query("SELECT * FROM transaction_items WHERE transactionId = :transactionId")
    suspend fun getItemsForTransactionSync(transactionId: Long): List<TransactionItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactionItems(items: List<TransactionItemEntity>)

    @Query("UPDATE transactions SET isPaid = :isPaid WHERE id = :transactionId")
    suspend fun updatePaidStatus(transactionId: Long, isPaid: Boolean)
}
