package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.entity.ProductEntity
import com.example.data.entity.ShiftEntity
import com.example.data.entity.SupplyEntity
import com.example.data.entity.TransactionEntity
import com.example.data.entity.TransactionItemEntity
import com.example.data.repository.CartItem
import com.example.data.repository.PosRepository
import com.example.data.repository.ShiftReportSummary
import com.example.updater.UpdateCheckResult
import com.example.updater.UpdateManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PosViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = PosRepository(
        productDao = db.productDao(),
        supplyDao = db.supplyDao(),
        shiftDao = db.shiftDao(),
        transactionDao = db.transactionDao()
    )

    private val updateManager = UpdateManager()

    // Base flows
    val allProducts: StateFlow<List<ProductEntity>> = repository.allProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSupplies: StateFlow<List<SupplyEntity>> = repository.allSupplies
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allShifts: StateFlow<List<ShiftEntity>> = repository.allShifts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentShift: StateFlow<ShiftEntity?> = repository.currentOpenShift
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val allTransactions: StateFlow<List<TransactionEntity>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI state
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("Все")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _cart = MutableStateFlow<List<CartItem>>(emptyList())
    val cart: StateFlow<List<CartItem>> = _cart.asStateFlow()

    private val _cartDiscount = MutableStateFlow(0.0)
    val cartDiscount: StateFlow<Double> = _cartDiscount.asStateFlow()

    private val _githubRepoSlug = MutableStateFlow("terminal-pos/android-app")
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
        }
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

    fun setCartDiscount(discount: Double) {
        _cartDiscount.value = discount.coerceAtLeast(0.0)
    }

    fun clearCart() {
        _cart.value = emptyList()
        _cartDiscount.value = 0.0
    }

    fun processCheckout(paymentMethod: String) {
        val activeShift = currentShift.value
        if (activeShift == null) {
            _messageEvent.value = "Откройте смену перед проведением продажи!"
            return
        }
        val items = _cart.value
        if (items.isEmpty()) {
            _messageEvent.value = "Корзина пуста!"
            return
        }

        viewModelScope.launch {
            try {
                val txId = repository.processSale(
                    items = items,
                    paymentMethod = paymentMethod,
                    discountAmount = _cartDiscount.value,
                    shiftId = activeShift.id
                )
                val tx = db.transactionDao().getTransactionById(txId)
                val txItems = db.transactionDao().getItemsForTransactionSync(txId)

                _lastSaleReceipt.value = tx
                _lastSaleItems.value = txItems
                clearCart()

                loadShiftReport(activeShift.id)
                _messageEvent.value = "Продажа успешно проведена!"
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
        val activeShift = currentShift.value
        if (activeShift == null) {
            _messageEvent.value = "Откройте смену для проведения возврата!"
            return
        }
        if (itemsToReturn.isEmpty()) {
            _messageEvent.value = "Выберите товары для возврата!"
            return
        }

        viewModelScope.launch {
            try {
                repository.processReturn(
                    originalTransactionId = originalTransaction.id,
                    returnedItems = itemsToReturn,
                    paymentMethod = originalTransaction.paymentMethod,
                    shiftId = activeShift.id
                )
                loadShiftReport(activeShift.id)
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

    // --- DEMO DATA ---
    fun seedDemoData() {
        viewModelScope.launch {
            repository.seedDemoData()
            _messageEvent.value = "Демонстрационные товары и начальная смена загружены!"
        }
    }
}
