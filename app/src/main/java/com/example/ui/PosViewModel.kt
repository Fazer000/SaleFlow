package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.entity.CustomerEntity
import com.example.data.entity.ProductEntity
import com.example.data.entity.ShiftEntity
import com.example.data.entity.SupplyBatchEntity
import com.example.data.entity.SupplyEntity
import com.example.data.entity.TransactionEntity
import com.example.data.entity.TransactionItemEntity
import com.example.data.repository.CartItem
import com.example.data.repository.PosRepository
import com.example.data.repository.SupplyBatchItemDraft
import com.example.data.repository.ShiftReportSummary
import com.example.updater.UpdateCheckResult
import com.example.updater.UpdateManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import android.content.Context
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PosViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    private val _isDarkTheme = MutableStateFlow(prefs.getBoolean("dark_theme", false))
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    fun setDarkTheme(enabled: Boolean) {
        _isDarkTheme.value = enabled
        prefs.edit().putBoolean("dark_theme", enabled).apply()
    }

    private val db = AppDatabase.getDatabase(application)
    private val syncManager = com.example.data.sync.FirebaseSyncManager(
        context = application,
        productDao = db.productDao(),
        customerDao = db.customerDao(),
        shiftDao = db.shiftDao(),
        supplyDao = db.supplyDao(),
        transactionDao = db.transactionDao()
    )
    private val repository = PosRepository(
        productDao = db.productDao(),
        supplyDao = db.supplyDao(),
        supplyBatchDao = db.supplyBatchDao(),
        shiftDao = db.shiftDao(),
        transactionDao = db.transactionDao(),
        customerDao = db.customerDao(),
        syncManager = syncManager
    )

    val syncState: StateFlow<com.example.data.sync.SyncState> = syncManager.syncState

    fun triggerFullCloudSync() {
        viewModelScope.launch {
            syncManager.syncAllLocalToCloud()
            _messageEvent.value = "Запущена полная синхронизация с облаком Firebase"
        }
    }

    private val updateManager = UpdateManager()

    // Base flows
    val allProducts: StateFlow<List<ProductEntity>> = repository.allProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSupplies: StateFlow<List<SupplyEntity>> = repository.allSupplies
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSupplyBatches: StateFlow<List<SupplyBatchEntity>> = repository.allSupplyBatches
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allShifts: StateFlow<List<ShiftEntity>> = repository.allShifts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentShift: StateFlow<ShiftEntity?> = repository.currentOpenShift
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val allTransactions: StateFlow<List<TransactionEntity>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCustomers: StateFlow<List<CustomerEntity>> = repository.allCustomers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    data class CustomerWithStats(
        val customer: CustomerEntity,
        val totalPurchased: Double,
        val totalDebt: Double,
        val totalPaid: Double,
        val transactionCount: Int,
        val unpaidCount: Int
    )

    val customerStats: StateFlow<List<CustomerWithStats>> = combine(
        allCustomers,
        allTransactions
    ) { customers, transactions ->
        customers.map { customer ->
            val customerTxs = transactions.filter { it.customerId == customer.id }
            val sales = customerTxs.filter { it.type == "SALE" }
            val returns = customerTxs.filter { it.type == "RETURN" }
            val totalSales = sales.sumOf { it.totalAmount }
            val totalReturns = returns.sumOf { it.totalAmount }
            val totalPurchased = (totalSales - totalReturns).coerceAtLeast(0.0)

            val unpaidSales = sales.filter { !it.isPaid }
            val totalDebt = unpaidSales.sumOf { it.totalAmount }
            val totalPaid = (totalPurchased - totalDebt).coerceAtLeast(0.0)

            CustomerWithStats(
                customer = customer,
                totalPurchased = totalPurchased,
                totalDebt = totalDebt,
                totalPaid = totalPaid,
                transactionCount = sales.size,
                unpaidCount = unpaidSales.size
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedCheckoutCustomer = MutableStateFlow<CustomerEntity?>(null)
    val selectedCheckoutCustomer: StateFlow<CustomerEntity?> = _selectedCheckoutCustomer.asStateFlow()

    private val _checkoutIsPaid = MutableStateFlow(true)
    val checkoutIsPaid: StateFlow<Boolean> = _checkoutIsPaid.asStateFlow()

    // UI state
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("Все")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _cart = MutableStateFlow<List<CartItem>>(emptyList())
    val cart: StateFlow<List<CartItem>> = _cart.asStateFlow()

    private val _cartDiscount = MutableStateFlow(0.0)
    val cartDiscount: StateFlow<Double> = _cartDiscount.asStateFlow()

    private val _githubRepoSlug = MutableStateFlow("Fazer000/SaleFlow")
    val githubRepoSlug: StateFlow<String> = _githubRepoSlug.asStateFlow()

    private val _updateState = MutableStateFlow<UpdateCheckResult>(UpdateCheckResult.Idle)
    val updateState: StateFlow<UpdateCheckResult> = _updateState.asStateFlow()

    private val _currentShiftReport = MutableStateFlow<ShiftReportSummary?>(null)
    val currentShiftReport: StateFlow<ShiftReportSummary?> = _currentShiftReport.asStateFlow()

    private val _lastSaleReceipt = MutableStateFlow<TransactionEntity?>(null)
    val lastSaleReceipt: StateFlow<TransactionEntity?> = _lastSaleReceipt.asStateFlow()

    private val _lastSaleItems = MutableStateFlow<List<TransactionItemEntity>>(emptyList())
    val lastSaleItems: StateFlow<List<TransactionItemEntity>> = _lastSaleItems.asStateFlow()

    private val _messageEvent = MutableStateFlow<String?>(null)
    val messageEvent: StateFlow<String?> = _messageEvent.asStateFlow()

    // Filtered Products
    val filteredProducts: StateFlow<List<ProductEntity>> = combine(
        allProducts,
        _searchQuery,
        _selectedCategory
    ) { products, query, cat ->
        products.filter { p ->
            val matchesQuery = query.isBlank() ||
                    p.name.contains(query, ignoreCase = true) ||
                    p.sku.contains(query, ignoreCase = true)
            val matchesCat = cat == "Все" || p.category.equals(cat, ignoreCase = true)
            matchesQuery && matchesCat
        }.sortedWith(
            compareByDescending<ProductEntity> { it.currentStock > 0 }
                .thenBy { it.name }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val availableCategories: StateFlow<List<String>> = allProducts.map { products ->
        val cats = products.map { it.category }.filter { it.isNotBlank() }.distinct().sorted()
        listOf("Все") + cats
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf("Все"))

    init {
        // Auto check if shift report needs recalculation when shift changes
        viewModelScope.launch {
            currentShift.collect { shift ->
                if (shift != null) {
                    loadShiftReport(shift.id)
                } else {
                    _currentShiftReport.value = null
                }
            }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedCategory(cat: String) {
        _selectedCategory.value = cat
    }

    fun setGithubRepoSlug(slug: String) {
        _githubRepoSlug.value = slug
    }

    fun clearMessage() {
        _messageEvent.value = null
    }

    fun clearReceipt() {
        _lastSaleReceipt.value = null
        _lastSaleItems.value = emptyList()
    }

    // --- CART ACTIONS ---
    fun addToCart(product: ProductEntity) {
        if (product.currentStock <= 0) {
            _messageEvent.value = "Товара '${product.name}' нет в наличии!"
            return
        }
        val currentList = _cart.value.toMutableList()
        val index = currentList.indexOfFirst { it.product.id == product.id }
        if (index >= 0) {
            val existing = currentList[index]
            if (existing.quantity + 1 > product.currentStock) {
                _messageEvent.value = "Максимальный остаток (${product.currentStock} ${product.unit})"
                return
            }
            currentList[index] = existing.copy(quantity = existing.quantity + 1)
        } else {
            currentList.add(CartItem(product = product, quantity = 1.0))
        }
        _cart.value = currentList
    }

    fun updateCartQuantity(productId: Long, newQuantity: Double) {
        val currentList = _cart.value.toMutableList()
        val index = currentList.indexOfFirst { it.product.id == productId }
        if (index >= 0) {
            if (newQuantity <= 0) {
                currentList.removeAt(index)
            } else {
                val item = currentList[index]
                if (newQuantity > item.product.currentStock) {
                    _messageEvent.value = "Доступно только ${item.product.currentStock} ${item.product.unit}"
                    currentList[index] = item.copy(quantity = item.product.currentStock)
                } else {
                    currentList[index] = item.copy(quantity = newQuantity)
                }
            }
            _cart.value = currentList
        }
    }

    fun setSelectedCheckoutCustomer(customer: CustomerEntity?) {
        _selectedCheckoutCustomer.value = customer
    }

    fun setCheckoutIsPaid(isPaid: Boolean) {
        _checkoutIsPaid.value = isPaid
    }

    fun addCustomer(name: String, phone: String = "", note: String = "") {
        if (name.isBlank()) {
            _messageEvent.value = "Введите имя покупателя"
            return
        }
        viewModelScope.launch {
            repository.insertCustomer(CustomerEntity(name = name.trim(), phone = phone.trim(), note = note.trim()))
            _messageEvent.value = "Покупатель добавлен"
        }
    }

    fun deleteCustomer(id: Long) {
        viewModelScope.launch {
            repository.deleteCustomer(id)
            if (_selectedCheckoutCustomer.value?.id == id) {
                _selectedCheckoutCustomer.value = null
            }
            _messageEvent.value = "Покупатель удален"
        }
    }

    fun toggleTransactionPaidStatus(transactionId: Long, isPaid: Boolean) {
        viewModelScope.launch {
            repository.updateTransactionPaidStatus(transactionId, isPaid)
            val shift = currentShift.value
            if (shift != null) {
                loadShiftReport(shift.id)
            }
            _messageEvent.value = if (isPaid) "Чек отмечен как оплаченный" else "Чек отмечен как неоплаченный (в долг)"
        }
    }

    fun setCartDiscount(discount: Double) {
        _cartDiscount.value = discount.coerceAtLeast(0.0)
    }

    fun clearCart() {
        _cart.value = emptyList()
        _cartDiscount.value = 0.0
    }

    fun processCheckout(
        paymentMethod: String,
        customer: CustomerEntity? = _selectedCheckoutCustomer.value,
        isPaid: Boolean = _checkoutIsPaid.value
    ) {
        val items = _cart.value
        if (items.isEmpty()) {
            _messageEvent.value = "Корзина пуста!"
            return
        }

        viewModelScope.launch {
            try {
                val activeShiftId = currentShift.value?.id ?: repository.openShift(0.0)
                val txId = repository.processSale(
                    items = items,
                    paymentMethod = paymentMethod,
                    discountAmount = _cartDiscount.value,
                    shiftId = activeShiftId,
                    customerId = customer?.id,
                    customerName = customer?.name,
                    isPaid = isPaid
                )
                val tx = db.transactionDao().getTransactionById(txId)
                val txItems = db.transactionDao().getItemsForTransactionSync(txId)

                _lastSaleReceipt.value = tx
                _lastSaleItems.value = txItems
                clearCart()
                _selectedCheckoutCustomer.value = null
                _checkoutIsPaid.value = true

                loadShiftReport(activeShiftId)
                _messageEvent.value = if (isPaid) "Продажа успешно проведена!" else "Продажа оформлена в долг!"
            } catch (e: Exception) {
                _messageEvent.value = "Ошибка продажи: ${e.localizedMessage}"
            }
        }
    }

    // --- STOCK INTAKE ---
    fun recordStockIntake(
        productId: Long,
        quantityAdded: Double,
        newCostPrice: Double? = null,
        newSellingPrice: Double? = null,
        note: String? = null
    ) {
        if (quantityAdded <= 0) {
            _messageEvent.value = "Количество должно быть больше 0"
            return
        }
        viewModelScope.launch {
            val success = repository.recordStockSupply(
                productId = productId,
                quantityAdded = quantityAdded,
                newCostPrice = newCostPrice,
                newSellingPrice = newSellingPrice,
                note = note
            )
            if (success) {
                _messageEvent.value = "Поступление товара успешно зарегистрировано!"
            } else {
                _messageEvent.value = "Ошибка записи поступления!"
            }
        }
    }

    fun recordGroupedSupplyBatch(
        supplierName: String,
        invoiceNumber: String,
        note: String,
        items: List<SupplyBatchItemDraft>
    ) {
        if (items.isEmpty()) {
            _messageEvent.value = "Поставка не содержит товаров!"
            return
        }
        viewModelScope.launch {
            val success = repository.recordGroupedSupplyBatch(
                supplierName = supplierName,
                invoiceNumber = invoiceNumber,
                note = note,
                items = items
            )
            if (success) {
                _messageEvent.value = "Поставка успешно сформирована и проведена!"
            } else {
                _messageEvent.value = "Ошибка при проведении поставки!"
            }
        }
    }

    suspend fun getSuppliesForBatch(batchId: Long): List<SupplyEntity> {
        return repository.getSuppliesForBatch(batchId)
    }

    // --- SHIFTS ---
    fun openShift(initialCash: Double) {
        viewModelScope.launch {
            val shiftId = repository.openShift(initialCash)
            loadShiftReport(shiftId)
            _messageEvent.value = "Смена успешно открыта!"
        }
    }

    fun closeShift(closingCash: Double) {
        viewModelScope.launch {
            val success = repository.closeShift(closingCash)
            if (success) {
                _messageEvent.value = "Смена успешно закрыта! Отчет сформирован."
            } else {
                _messageEvent.value = "Ошибка при закрытии смены!"
            }
        }
    }

    fun loadShiftReport(shiftId: Long) {
        viewModelScope.launch {
            _currentShiftReport.value = repository.generateShiftReport(shiftId)
        }
    }

    // --- RETURNS ---
    fun processReturn(
        originalTransaction: TransactionEntity,
        itemsToReturn: List<TransactionItemEntity>
    ) {
        if (itemsToReturn.isEmpty()) {
            _messageEvent.value = "Выберите товары для возврата!"
            return
        }

        viewModelScope.launch {
            try {
                val activeShiftId = currentShift.value?.id ?: repository.openShift(0.0)
                repository.processReturn(
                    originalTransactionId = originalTransaction.id,
                    returnedItems = itemsToReturn,
                    paymentMethod = originalTransaction.paymentMethod,
                    shiftId = activeShiftId
                )
                loadShiftReport(activeShiftId)
                _messageEvent.value = "Возврат успешно оформлен, остатки обновлены!"
            } catch (e: Exception) {
                _messageEvent.value = "Ошибка при возврате: ${e.localizedMessage}"
            }
        }
    }

    suspend fun getTransactionItems(txId: Long): List<TransactionItemEntity> {
        return repository.getItemsForTransactionSync(txId)
    }

    // --- PRODUCT MANAGEMENT ---
    fun saveProduct(product: ProductEntity) {
        if (product.name.isBlank()) {
            _messageEvent.value = "Введите название товара!"
            return
        }
        viewModelScope.launch {
            repository.saveProduct(product)
            _messageEvent.value = "Товар '${product.name}' сохранён!"
        }
    }

    fun deleteProduct(product: ProductEntity) {
        viewModelScope.launch {
            repository.deleteProduct(product)
            _messageEvent.value = "Товар удалён!"
        }
    }

    // --- UPDATER ---
    fun checkForUpdates(currentAppVersionName: String = "1.0.0") {
        viewModelScope.launch {
            _updateState.value = UpdateCheckResult.Checking
            val result = updateManager.checkForUpdates(_githubRepoSlug.value, currentAppVersionName)
            _updateState.value = result
        }
    }

    fun downloadAndInstallApk(context: android.content.Context, apkUrl: String) {
        viewModelScope.launch {
            try {
                _updateState.value = UpdateCheckResult.Downloading(0)
                val apkFile = updateManager.downloadApk(context, apkUrl) { progress ->
                    _updateState.value = UpdateCheckResult.Downloading(progress)
                }
                _updateState.value = UpdateCheckResult.ReadyToInstall(apkFile)
                updateManager.installApk(context, apkFile)
            } catch (e: Exception) {
                _updateState.value = UpdateCheckResult.Error("Ошибка скачивания: ${e.localizedMessage}")
            }
        }
    }

    // --- DEMO DATA ---
    fun seedDemoData() {
        viewModelScope.launch {
            repository.seedDemoData()
            _messageEvent.value = "Демонстрационные товары и начальная смена загружены!"
        }
    }
}
