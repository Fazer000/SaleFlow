package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val shiftId: Long,
    val type: String, // SALE or RETURN
    val paymentMethod: String, // CASH, CARD, QR
    val totalAmount: Double,
    val totalCostPrice: Double,
    val discountAmount: Double = 0.0,
    val timestamp: Long = System.currentTimeMillis(),
    val relatedTransactionId: Long? = null // if RETURN, references original SALE transaction
)
