package com.example.data.sync

import android.content.Context
import android.util.Log
import com.example.data.db.CustomerDao
import com.example.data.db.ProductDao
import com.example.data.db.ShiftDao
import com.example.data.db.SupplyDao
import com.example.data.db.TransactionDao
import com.example.data.entity.CustomerEntity
import com.example.data.entity.ProductEntity
import com.example.data.entity.ShiftEntity
import com.example.data.entity.SupplyEntity
import com.example.data.entity.TransactionEntity
import com.example.data.entity.TransactionItemEntity
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class SyncState(val label: String) {
    SYNCED("Синхронизировано с Firebase"),
    SYNCING("Синхронизация..."),
    OFFLINE("Локальный режим (Без облака)"),
    ERROR("Ошибка синхронизации")
}

class FirebaseSyncManager(
    private val context: Context,
    private val productDao: ProductDao,
    private val customerDao: CustomerDao,
    private val shiftDao: ShiftDao,
    private val supplyDao: SupplyDao,
    private val transactionDao: TransactionDao
) {
    private val TAG = "FirebaseSyncManager"
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _syncState = MutableStateFlow(SyncState.OFFLINE)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    private var firestore: FirebaseFirestore? = null
    private val activeListeners = mutableListOf<ListenerRegistration>()

    private var isSuppressingRemoteSync = false

    init {
        initFirebase()
    }

    private fun initFirebase() {
        try {
            val app = if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
            } else {
                FirebaseApp.getInstance()
            }

            if (app != null) {
                firestore = FirebaseFirestore.getInstance()
                _syncState.value = SyncState.SYNCED
                Log.d(TAG, "Firebase initialized successfully")
                startRealtimeListeners()
            } else {
                _syncState.value = SyncState.OFFLINE
                Log.w(TAG, "FirebaseApp is null, operating in offline Room mode")
            }
        } catch (e: Exception) {
            _syncState.value = SyncState.OFFLINE
            Log.e(TAG, "Firebase initialization error: ${e.message}")
        }
    }

    // --- REALTIME LISTENERS FROM FIRESTORE TO ROOM ---
    fun startRealtimeListeners() {
        val db = firestore ?: return

        try {
            // 1. PRODUCTS LISTENER
            val productsRegistration = db.collection("products")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "Products snapshot error: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshot != null && !snapshot.isEmpty) {
                        scope.launch {
                            isSuppressingRemoteSync = true
                            for (doc in snapshot.documents) {
                                val id = doc.getLong("id") ?: doc.id.toLongOrNull() ?: continue
                                val name = doc.getString("name") ?: ""
                                val sku = doc.getString("sku") ?: ""
                                val category = doc.getString("category") ?: "Общее"
                                val costPrice = doc.getDouble("costPrice") ?: 0.0
                                val sellingPrice = doc.getDouble("sellingPrice") ?: 0.0
                                val currentStock = doc.getDouble("currentStock") ?: 0.0
                                val unit = doc.getString("unit") ?: "шт"
                                val createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()

                                val product = ProductEntity(
                                    id = id,
                                    name = name,
                                    sku = sku,
                                    category = category,
                                    costPrice = costPrice,
                                    sellingPrice = sellingPrice,
                                    currentStock = currentStock,
                                    unit = unit,
                                    createdAt = createdAt
                                )
                                productDao.insertProduct(product)
                            }
                            isSuppressingRemoteSync = false
                        }
                    }
                }
            activeListeners.add(productsRegistration)

            // 2. CUSTOMERS LISTENER
            val customersRegistration = db.collection("customers")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) return@addSnapshotListener
                    if (snapshot != null && !snapshot.isEmpty) {
                        scope.launch {
                            isSuppressingRemoteSync = true
                            for (doc in snapshot.documents) {
                                val id = doc.getLong("id") ?: doc.id.toLongOrNull() ?: continue
                                val name = doc.getString("name") ?: ""
                                val phone = doc.getString("phone") ?: ""
                                val note = doc.getString("note") ?: ""
                                val createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()

                                val customer = CustomerEntity(
                                    id = id,
                                    name = name,
                                    phone = phone,
                                    note = note,
                                    createdAt = createdAt
                                )
                                customerDao.insertCustomer(customer)
                            }
                            isSuppressingRemoteSync = false
                        }
                    }
                }
            activeListeners.add(customersRegistration)

            // 3. SHIFTS LISTENER
            val shiftsRegistration = db.collection("shifts")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) return@addSnapshotListener
                    if (snapshot != null && !snapshot.isEmpty) {
                        scope.launch {
                            isSuppressingRemoteSync = true
                            for (doc in snapshot.documents) {
                                val id = doc.getLong("id") ?: doc.id.toLongOrNull() ?: continue
                                val shiftNumber = doc.getLong("shiftNumber")?.toInt() ?: 1
                                val openedAt = doc.getLong("openedAt") ?: System.currentTimeMillis()
                                val closedAt = doc.getLong("closedAt")
                                val initialCash = doc.getDouble("initialCash") ?: 0.0
                                val closingCash = doc.getDouble("closingCash")
                                val cashRevenue = doc.getDouble("cashRevenue") ?: 0.0
                                val cardRevenue = doc.getDouble("cardRevenue") ?: 0.0
                                val totalReturns = doc.getDouble("totalReturns") ?: 0.0
                                val totalProfit = doc.getDouble("totalProfit") ?: 0.0
                                val status = doc.getString("status") ?: "OPEN"

                                val shift = ShiftEntity(
                                    id = id,
                                    shiftNumber = shiftNumber,
                                    openedAt = openedAt,
                                    closedAt = closedAt,
                                    initialCash = initialCash,
                                    closingCash = closingCash,
                                    cashRevenue = cashRevenue,
                                    cardRevenue = cardRevenue,
                                    totalReturns = totalReturns,
                                    totalProfit = totalProfit,
                                    status = status
                                )
                                shiftDao.insertShift(shift)
                            }
                            isSuppressingRemoteSync = false
                        }
                    }
                }
            activeListeners.add(shiftsRegistration)

            // 4. TRANSACTIONS LISTENER
            val transactionsRegistration = db.collection("transactions")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) return@addSnapshotListener
                    if (snapshot != null && !snapshot.isEmpty) {
                        scope.launch {
                            isSuppressingRemoteSync = true
                            for (doc in snapshot.documents) {
                                val id = doc.getLong("id") ?: doc.id.toLongOrNull() ?: continue
                                val shiftId = doc.getLong("shiftId") ?: 0L
                                val type = doc.getString("type") ?: "SALE"
                                val paymentMethod = doc.getString("paymentMethod") ?: "CASH"
                                val totalAmount = doc.getDouble("totalAmount") ?: 0.0
                                val totalCostPrice = doc.getDouble("totalCostPrice") ?: 0.0
                                val discountAmount = doc.getDouble("discountAmount") ?: 0.0
                                val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                                val relatedTransactionId = doc.getLong("relatedTransactionId")
                                val customerId = doc.getLong("customerId")
                                val customerName = doc.getString("customerName")
                                val isPaid = doc.getBoolean("isPaid") ?: true

                                val transaction = TransactionEntity(
                                    id = id,
                                    shiftId = shiftId,
                                    type = type,
                                    paymentMethod = paymentMethod,
                                    totalAmount = totalAmount,
                                    totalCostPrice = totalCostPrice,
                                    discountAmount = discountAmount,
                                    timestamp = timestamp,
                                    relatedTransactionId = relatedTransactionId,
                                    customerId = customerId,
                                    customerName = customerName,
                                    isPaid = isPaid
                                )
                                transactionDao.insertTransaction(transaction)
                            }
                            isSuppressingRemoteSync = false
                        }
                    }
                }
            activeListeners.add(transactionsRegistration)

        } catch (e: Exception) {
            Log.e(TAG, "Error starting realtime listeners: ${e.message}")
        }
    }

    // --- PUSH OPERATIONS LOCAL ROOM -> FIRESTORE ---

    fun pushProduct(product: ProductEntity) {
        if (isSuppressingRemoteSync) return
        val db = firestore ?: return
        _syncState.value = SyncState.SYNCING

        val data = hashMapOf(
            "id" to product.id,
            "name" to product.name,
            "sku" to product.sku,
            "category" to product.category,
            "costPrice" to product.costPrice,
            "sellingPrice" to product.sellingPrice,
            "currentStock" to product.currentStock,
            "unit" to product.unit,
            "createdAt" to product.createdAt,
            "updatedAt" to System.currentTimeMillis()
        )

        db.collection("products")
            .document(product.id.toString())
            .set(data, SetOptions.merge())
            .addOnSuccessListener {
                _syncState.value = SyncState.SYNCED
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to push product: ${e.message}")
                _syncState.value = SyncState.ERROR
            }
    }

    fun deleteProduct(productId: Long) {
        val db = firestore ?: return
        db.collection("products").document(productId.toString()).delete()
    }

    fun pushCustomer(customer: CustomerEntity) {
        if (isSuppressingRemoteSync) return
        val db = firestore ?: return
        _syncState.value = SyncState.SYNCING

        val data = hashMapOf(
            "id" to customer.id,
            "name" to customer.name,
            "phone" to customer.phone,
            "note" to customer.note,
            "createdAt" to customer.createdAt,
            "updatedAt" to System.currentTimeMillis()
        )

        db.collection("customers")
            .document(customer.id.toString())
            .set(data, SetOptions.merge())
            .addOnSuccessListener { _syncState.value = SyncState.SYNCED }
    }

    fun deleteCustomer(customerId: Long) {
        val db = firestore ?: return
        db.collection("customers").document(customerId.toString()).delete()
    }

    fun pushShift(shift: ShiftEntity) {
        if (isSuppressingRemoteSync) return
        val db = firestore ?: return
        _syncState.value = SyncState.SYNCING

        val data = hashMapOf(
            "id" to shift.id,
            "shiftNumber" to shift.shiftNumber,
            "openedAt" to shift.openedAt,
            "closedAt" to shift.closedAt,
            "initialCash" to shift.initialCash,
            "closingCash" to shift.closingCash,
            "cashRevenue" to shift.cashRevenue,
            "cardRevenue" to shift.cardRevenue,
            "totalReturns" to shift.totalReturns,
            "totalProfit" to shift.totalProfit,
            "status" to shift.status
        )

        db.collection("shifts")
            .document(shift.id.toString())
            .set(data, SetOptions.merge())
            .addOnSuccessListener { _syncState.value = SyncState.SYNCED }
    }

    fun pushSupply(supply: SupplyEntity) {
        if (isSuppressingRemoteSync) return
        val db = firestore ?: return
        _syncState.value = SyncState.SYNCING

        val data = hashMapOf(
            "id" to supply.id,
            "productId" to supply.productId,
            "productName" to supply.productName,
            "stockBefore" to supply.stockBefore,
            "quantityAdded" to supply.quantityAdded,
            "stockAfter" to supply.stockAfter,
            "costPriceAtSupply" to supply.costPriceAtSupply,
            "supplierNote" to supply.supplierNote,
            "timestamp" to supply.timestamp
        )

        db.collection("supplies")
            .document(supply.id.toString())
            .set(data, SetOptions.merge())
            .addOnSuccessListener { _syncState.value = SyncState.SYNCED }
    }

    fun pushTransaction(transaction: TransactionEntity, items: List<TransactionItemEntity>) {
        if (isSuppressingRemoteSync) return
        val db = firestore ?: return
        _syncState.value = SyncState.SYNCING

        val data = hashMapOf(
            "id" to transaction.id,
            "shiftId" to transaction.shiftId,
            "type" to transaction.type,
            "paymentMethod" to transaction.paymentMethod,
            "totalAmount" to transaction.totalAmount,
            "totalCostPrice" to transaction.totalCostPrice,
            "discountAmount" to transaction.discountAmount,
            "timestamp" to transaction.timestamp,
            "relatedTransactionId" to transaction.relatedTransactionId,
            "customerId" to transaction.customerId,
            "customerName" to transaction.customerName,
            "isPaid" to transaction.isPaid,
            "items" to items.map { item ->
                hashMapOf(
                    "id" to item.id,
                    "transactionId" to item.transactionId,
                    "productId" to item.productId,
                    "productName" to item.productName,
                    "quantity" to item.quantity,
                    "unitPrice" to item.unitPrice,
                    "costPrice" to item.costPrice,
                    "unit" to item.unit
                )
            }
        )

        db.collection("transactions")
            .document(transaction.id.toString())
            .set(data, SetOptions.merge())
            .addOnSuccessListener { _syncState.value = SyncState.SYNCED }
    }

    // --- FORCE SYNC FULL LOCAL DATABASE TO FIRESTORE ---
    suspend fun syncAllLocalToCloud() = withContext(Dispatchers.IO) {
        val db = firestore ?: return@withContext
        _syncState.value = SyncState.SYNCING

        try {
            // Upload Products
            val products = productDao.getAllProductsList()
            for (p in products) {
                pushProduct(p)
            }

            // Upload Customers
            val customers = customerDao.getAllCustomersList()
            for (c in customers) {
                pushCustomer(c)
            }

            // Upload Shifts
            val shifts = shiftDao.getAllShiftsList()
            for (s in shifts) {
                pushShift(s)
            }

            // Upload Supplies
            val supplies = supplyDao.getAllSuppliesList()
            for (sup in supplies) {
                pushSupply(sup)
            }

            _syncState.value = SyncState.SYNCED
        } catch (e: Exception) {
            Log.e(TAG, "Full sync error: ${e.message}")
            _syncState.value = SyncState.ERROR
        }
    }

    fun stopListeners() {
        activeListeners.forEach { it.remove() }
        activeListeners.clear()
    }
}
