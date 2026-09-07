package com.example.data.repository

import com.example.data.db.ProductDao
import com.example.data.db.ShiftDao
import com.example.data.db.SupplyDao
import com.example.data.db.TransactionDao
import com.example.data.entity.ProductEntity
import com.example.data.entity.ShiftEntity
import com.example.data.entity.SupplyEntity
import com.example.data.entity.TransactionEntity
import com.example.data.entity.TransactionItemEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

data class CartItem(
    val product: ProductEntity,
    val quantity: Double,
    val overridePrice: Double? = null
) {
    val unitPrice: Double get() = overridePrice ?: product.sellingPrice
    val totalPrice: Double get() = unitPrice * quantity
    val totalCostPrice: Double get() = product.costPrice * quantity
}

data class ShiftReportSummary(
    val shift: ShiftEntity,
    val totalSalesCount: Int,
    val totalReturnsCount: Int,
    val grossSalesAmount: Double,
    val totalDiscountAmount: Double,
    val netSalesAmount: Double,
    val totalCostPriceAmount: Double,
    val totalProfitAmount: Double,
    val cashRevenue: Double,
    val cardRevenue: Double,
    val qrRevenue: Double,
    val topSellingItems: List<TopSoldItem>
)

data class TopSoldItem(
    val productName: String,
    val totalQuantity: Double,
    val totalRevenue: Double,
    val unit: String
)

class PosRepository(
    private val productDao: ProductDao,
    private val supplyDao: SupplyDao,
    private val shiftDao: ShiftDao,
    private val transactionDao: TransactionDao
) {
    val allProducts: Flow<List<ProductEntity>> = productDao.getAllProducts()
    val allSupplies: Flow<List<SupplyEntity>> = supplyDao.getAllSupplies()
    val allShifts: Flow<List<ShiftEntity>> = shiftDao.getAllShifts()
    val currentOpenShift: Flow<ShiftEntity?> = shiftDao.getCurrentOpenShift()
    val allTransactions: Flow<List<TransactionEntity>> = transactionDao.getAllTransactions()

    suspend fun getProductById(id: Long): ProductEntity? = withContext(Dispatchers.IO) {
        productDao.getProductById(id)
    }

    suspend fun getProductBySku(sku: String): ProductEntity? = withContext(Dispatchers.IO) {
        productDao.getProductBySku(sku)
    }

    suspend fun saveProduct(product: ProductEntity): Long = withContext(Dispatchers.IO) {
        if (product.id == 0L) {
            productDao.insertProduct(product)
        } else {
            productDao.updateProduct(product)
            product.id
        }
    }

    suspend fun deleteProduct(product: ProductEntity) = withContext(Dispatchers.IO) {
        productDao.deleteProduct(product)
    }

    // --- STOCK INTAKE / ПОСТУПЛЕНИЕ ТОВАРА ---
    suspend fun recordStockSupply(
        productId: Long,
        quantityAdded: Double,
        newCostPrice: Double? = null,
        newSellingPrice: Double? = null,
        note: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        val product = productDao.getProductById(productId) ?: return@withContext false
        val stockBefore = product.currentStock
        val stockAfter = stockBefore + quantityAdded
        val costPriceToUse = newCostPrice ?: product.costPrice
        val sellingPriceToUse = newSellingPrice ?: product.sellingPrice

        // Update product stock and pricing
        val updatedProduct = product.copy(
            currentStock = stockAfter,
            costPrice = costPriceToUse,
            sellingPrice = sellingPriceToUse
        )
        productDao.updateProduct(updatedProduct)

        // Log supply movement record
        supplyDao.insertSupply(
            SupplyEntity(
                productId = productId,
                productName = product.name,
                stockBefore = stockBefore,
                quantityAdded = quantityAdded,
                stockAfter = stockAfter,
                costPriceAtSupply = costPriceToUse,
                supplierNote = note
            )
        )
        true
    }

    // --- SHIFTS / СМЕНЫ ---
    suspend fun openShift(initialCash: Double): Long = withContext(Dispatchers.IO) {
        val currentOpen = shiftDao.getCurrentOpenShiftSync()
        if (currentOpen != null) return@withContext currentOpen.id

        val lastNumber = shiftDao.getLatestShiftNumber() ?: 0
        val newShiftNumber = lastNumber + 1

        shiftDao.insertShift(
            ShiftEntity(
                shiftNumber = newShiftNumber,
                initialCash = initialCash,
                openedAt = System.currentTimeMillis(),
                status = "OPEN"
            )
        )
    }

    suspend fun closeShift(closingCash: Double): Boolean = withContext(Dispatchers.IO) {
        val currentOpen = shiftDao.getCurrentOpenShiftSync() ?: return@withContext false
        val summary = generateShiftReport(currentOpen.id) ?: return@withContext false

        val updatedShift = currentOpen.copy(
            closedAt = System.currentTimeMillis(),
            closingCash = closingCash,
            cashRevenue = summary.cashRevenue,
            cardRevenue = summary.cardRevenue,
            totalReturns = summary.totalReturnsCount.toDouble(),
            totalProfit = summary.totalProfitAmount,
            status = "CLOSED"
        )
        shiftDao.updateShift(updatedShift)
        true
    }

    suspend fun generateShiftReport(shiftId: Long): ShiftReportSummary? = withContext(Dispatchers.IO) {
        val shift = shiftDao.getShiftById(shiftId) ?: return@withContext null
        val transactions = transactionDao.getTransactionsForShiftSync(shiftId)

        var totalSalesCount = 0
        var totalReturnsCount = 0
        var grossSales = 0.0
        var totalDiscounts = 0.0
        var totalCost = 0.0
        var cashRev = 0.0
        var cardRev = 0.0
        var qrRev = 0.0

        val itemSalesMap = mutableMapOf<String, Pair<Double, Double>>() // Name -> (Quantity, Revenue)
        val itemUnitMap = mutableMapOf<String, String>()

        for (tx in transactions) {
            val items = transactionDao.getItemsForTransactionSync(tx.id)
            if (tx.type == "SALE") {
                totalSalesCount++
                grossSales += tx.totalAmount
                totalDiscounts += tx.discountAmount
                totalCost += tx.totalCostPrice

                when (tx.paymentMethod) {
                    "CASH" -> cashRev += tx.totalAmount
                    "CARD" -> cardRev += tx.totalAmount
                    "QR" -> qrRev += tx.totalAmount
                }

                for (item in items) {
                    val prev = itemSalesMap[item.productName] ?: Pair(0.0, 0.0)
                    itemSalesMap[item.productName] = Pair(
                        prev.first + item.quantity,
                        prev.second + (item.quantity * item.unitPrice)
                    )
                    itemUnitMap[item.productName] = item.unit
                }
            } else if (tx.type == "RETURN") {
                totalReturnsCount++
                grossSales -= tx.totalAmount
                totalCost -= tx.totalCostPrice

                when (tx.paymentMethod) {
                    "CASH" -> cashRev -= tx.totalAmount
                    "CARD" -> cardRev -= tx.totalAmount
                    "QR" -> qrRev -= tx.totalAmount
                }

                for (item in items) {
                    val prev = itemSalesMap[item.productName] ?: Pair(0.0, 0.0)
                    itemSalesMap[item.productName] = Pair(
                        prev.first - item.quantity,
                        prev.second - (item.quantity * item.unitPrice)
                    )
                }
            }
        }

        val netSales = grossSales
        val profit = netSales - totalCost

        val topSellingItems = itemSalesMap.map { (name, pair) ->
            TopSoldItem(
                productName = name,
                totalQuantity = pair.first,
                totalRevenue = pair.second,
                unit = itemUnitMap[name] ?: "шт"
            )
        }.sortedByDescending { it.totalRevenue }.take(5)

        ShiftReportSummary(
            shift = shift,
            totalSalesCount = totalSalesCount,
            totalReturnsCount = totalReturnsCount,
            grossSalesAmount = grossSales + totalDiscounts,
            totalDiscountAmount = totalDiscounts,
            netSalesAmount = netSales,
            totalCostPriceAmount = totalCost,
            totalProfitAmount = profit,
            cashRevenue = cashRev,
            cardRevenue = cardRev,
            qrRevenue = qrRev,
            topSellingItems = topSellingItems
        )
    }

    // --- SALES & RETURNS / ПРОДАЖА И ВОЗВРАТ ---
    suspend fun processSale(
        items: List<CartItem>,
        paymentMethod: String,
        discountAmount: Double,
        shiftId: Long
    ): Long = withContext(Dispatchers.IO) {
        val grossTotal = items.sumOf { it.totalPrice }
        val netTotal = (grossTotal - discountAmount).coerceAtLeast(0.0)
        val totalCost = items.sumOf { it.totalCostPrice }

        val saleTx = TransactionEntity(
            shiftId = shiftId,
            type = "SALE",
            paymentMethod = paymentMethod,
            totalAmount = netTotal,
            totalCostPrice = totalCost,
            discountAmount = discountAmount,
            timestamp = System.currentTimeMillis()
        )
        val txId = transactionDao.insertTransaction(saleTx)

        val itemEntities = items.map { cartItem ->
            // Deduct stock
            val currentProduct = productDao.getProductById(cartItem.product.id)
            if (currentProduct != null) {
                val newStock = (currentProduct.currentStock - cartItem.quantity).coerceAtLeast(0.0)
                productDao.updateStock(currentProduct.id, newStock)
            }

            TransactionItemEntity(
                transactionId = txId,
                productId = cartItem.product.id,
                productName = cartItem.product.name,
                quantity = cartItem.quantity,
                unitPrice = cartItem.unitPrice,
                costPrice = cartItem.product.costPrice,
                unit = cartItem.product.unit
            )
        }
        transactionDao.insertTransactionItems(itemEntities)

        txId
    }

    suspend fun processReturn(
        originalTransactionId: Long,
        returnedItems: List<TransactionItemEntity>,
        paymentMethod: String,
        shiftId: Long
    ): Long = withContext(Dispatchers.IO) {
        val totalReturnAmount = returnedItems.sumOf { it.quantity * it.unitPrice }
        val totalReturnCost = returnedItems.sumOf { it.quantity * it.costPrice }

        val returnTx = TransactionEntity(
            shiftId = shiftId,
            type = "RETURN",
            paymentMethod = paymentMethod,
            totalAmount = totalReturnAmount,
            totalCostPrice = totalReturnCost,
            discountAmount = 0.0,
            timestamp = System.currentTimeMillis(),
            relatedTransactionId = originalTransactionId
        )
        val returnTxId = transactionDao.insertTransaction(returnTx)

        val itemEntities = returnedItems.map { item ->
            // Restore product stock
            val currentProduct = productDao.getProductById(item.productId)
            if (currentProduct != null) {
                val newStock = currentProduct.currentStock + item.quantity
                productDao.updateStock(currentProduct.id, newStock)
            }

            TransactionItemEntity(
                transactionId = returnTxId,
                productId = item.productId,
                productName = item.productName,
                quantity = item.quantity,
                unitPrice = item.unitPrice,
                costPrice = item.costPrice,
                unit = item.unit
            )
        }
        transactionDao.insertTransactionItems(itemEntities)

        returnTxId
    }

    fun getItemsForTransaction(transactionId: Long): Flow<List<TransactionItemEntity>> {
        return transactionDao.getItemsForTransaction(transactionId)
    }

    suspend fun getItemsForTransactionSync(transactionId: Long): List<TransactionItemEntity> = withContext(Dispatchers.IO) {
        transactionDao.getItemsForTransactionSync(transactionId)
    }

    // --- DEMO DATA INITIALIZATION ---
    suspend fun seedDemoData() = withContext(Dispatchers.IO) {
        val existing = productDao.getAllProducts()
        // If empty, insert demo products and open shift
        val p1 = ProductEntity(name = "Кофе Латте 0.4л", sku = "46000001", category = "Напитки", costPrice = 45.0, sellingPrice = 180.0, currentStock = 50.0, unit = "шт")
        val p2 = ProductEntity(name = "Капучино 0.3л", sku = "46000002", category = "Напитки", costPrice = 40.0, sellingPrice = 160.0, currentStock = 60.0, unit = "шт")
        val p3 = ProductEntity(name = "Сэндвич с курицей", sku = "46000003", category = "Еда", costPrice = 90.0, sellingPrice = 240.0, currentStock = 25.0, unit = "шт")
        val p4 = ProductEntity(name = "Круассан с шоколадом", sku = "46000004", category = "Выпечка", costPrice = 35.0, sellingPrice = 120.0, currentStock = 30.0, unit = "шт")
        val p5 = ProductEntity(name = "Сок яблочный 0.5л", sku = "46000005", category = "Напитки", costPrice = 50.0, sellingPrice = 110.0, currentStock = 40.0, unit = "шт")
        val p6 = ProductEntity(name = "Шоколад Алёнка 90г", sku = "46000006", category = "Сладости", costPrice = 65.0, sellingPrice = 130.0, currentStock = 35.0, unit = "шт")

        val id1 = productDao.insertProduct(p1)
        val id2 = productDao.insertProduct(p2)
        val id3 = productDao.insertProduct(p3)
        productDao.insertProduct(p4)
        productDao.insertProduct(p5)
        productDao.insertProduct(p6)

        // Add initial supply intake logs
        supplyDao.insertSupply(SupplyEntity(productId = id1, productName = p1.name, stockBefore = 0.0, quantityAdded = 50.0, stockAfter = 50.0, costPriceAtSupply = 45.0, supplierNote = "Первичный завоз"))
        supplyDao.insertSupply(SupplyEntity(productId = id2, productName = p2.name, stockBefore = 0.0, quantityAdded = 60.0, stockAfter = 60.0, costPriceAtSupply = 40.0, supplierNote = "Первичный завоз"))
        supplyDao.insertSupply(SupplyEntity(productId = id3, productName = p3.name, stockBefore = 0.0, quantityAdded = 25.0, stockAfter = 25.0, costPriceAtSupply = 90.0, supplierNote = "Свежая выпечка"))

        // Open shift #1 automatically if none is open
        openShift(initialCash = 3000.0)
    }
}
