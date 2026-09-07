package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "shifts")
data class ShiftEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val shiftNumber: Int,
    val openedAt: Long = System.currentTimeMillis(),
    val closedAt: Long? = null,
    val initialCash: Double = 0.0,
    val closingCash: Double? = null,
    val cashRevenue: Double = 0.0,
    val cardRevenue: Double = 0.0,
    val totalReturns: Double = 0.0,
    val totalProfit: Double = 0.0,
    val status: String = "OPEN" // OPEN or CLOSED
)
