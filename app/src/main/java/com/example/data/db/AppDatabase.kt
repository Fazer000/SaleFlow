package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.entity.CustomerEntity
import com.example.data.entity.ProductEntity
import com.example.data.entity.ShiftEntity
import com.example.data.entity.SupplyEntity
import com.example.data.entity.TransactionEntity
import com.example.data.entity.TransactionItemEntity

@Database(
    entities = [
        ProductEntity::class,
        SupplyEntity::class,
        ShiftEntity::class,
        TransactionEntity::class,
        TransactionItemEntity::class,
        CustomerEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun supplyDao(): SupplyDao
    abstract fun shiftDao(): ShiftDao
    abstract fun transactionDao(): TransactionDao
    abstract fun customerDao(): CustomerDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "pos_terminal_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
