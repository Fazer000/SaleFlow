package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val sku: String, // Штрихкод или артикул
    val category: String,
    val costPrice: Double, // Себестоимость
    val sellingPrice: Double, // Цена продажи
    val currentStock: Double, // Актуальный остаток
    val unit: String = "шт", // шт, кг, л, упак
    val createdAt: Long = System.currentTimeMillis()
)
