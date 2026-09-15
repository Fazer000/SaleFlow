package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "supply_batches")
data class SupplyBatchEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val supplierName: String = "",
    val invoiceNumber: String = "",
    val note: String = "",
    val totalCostPrice: Double = 0.0,
    val totalSellingPrice: Double = 0.0,
    val totalQuantity: Double = 0.0,
    val itemCount: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)
