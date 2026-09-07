package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "supplies")
data class SupplyEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val productId: Long,
    val productName: String,
    val stockBefore: Double, // Сколько было до поступления
    val quantityAdded: Double, // Сколько поступило
    val stockAfter: Double, // Сколько стало после
    val costPriceAtSupply: Double,
    val supplierNote: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
