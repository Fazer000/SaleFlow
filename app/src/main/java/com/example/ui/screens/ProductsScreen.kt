package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.example.data.entity.ProductEntity
import com.example.data.entity.SupplyBatchEntity
import com.example.data.entity.SupplyEntity
import com.example.data.repository.SupplyBatchItemDraft
import com.example.ui.PosViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductsScreen(
    viewModel: PosViewModel,
    products: List<ProductEntity>,
    supplies: List<SupplyEntity>,
    searchQuery: String
) {
    var selectedTab by remember { mutableStateOf(0) } // 0 = Catalog, 1 = Batches
    var showAddEditModal by remember { mutableStateOf(false) }
    var editingProduct by remember { mutableStateOf<ProductEntity?>(null) }

    var showCreateSupplyModal by remember { mutableStateOf(false) }
    var preselectedProductForSupply by remember { mutableStateOf<ProductEntity?>(null) }

    var showShowcaseModal by remember { mutableStateOf(false) }
    var showcaseProduct by remember { mutableStateOf<ProductEntity?>(null) }

    val supplyBatches by viewModel.allSupplyBatches.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Tabs
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.background,
                divider = {},
                indicator = { tabPositions ->
                    if (selectedTab < tabPositions.size) {
                        val currentTab = tabPositions[selectedTab]
                        val currentTabWidth = currentTab.contentWidth
                        val indicatorOffset = currentTab.left + (currentTab.width - currentTabWidth) / 2

                        Box(
                            Modifier
                                .fillMaxWidth()
                                .wrapContentSize(Alignment.BottomStart)
                                .offset(x = indicatorOffset)
                                .width(currentTabWidth)
                                .height(3.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)
                                )
                        )
                    }
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Inventory, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Каталог товаров (${products.size})", fontWeight = FontWeight.SemiBold)
                        }
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocalShipping, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Поставки (${supplyBatches.size})", fontWeight = FontWeight.SemiBold)
                        }
                    }
                )
            }

            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            if (selectedTab == 0) {
                // CATALOG TAB
                var selectedCategory by remember { mutableStateOf<String?>(null) }
                val categories = remember(products) {
                    products.map { it.category }.distinct().filter { it.isNotBlank() }
                }

                val filteredProducts = remember(products, searchQuery, selectedCategory) {
                    products.filter { p ->
                        val matchesSearch = searchQuery.isBlank() ||
                                p.name.contains(searchQuery, ignoreCase = true) ||
                                p.sku.contains(searchQuery, ignoreCase = true) ||
                                p.category.contains(searchQuery, ignoreCase = true)
                        val matchesCategory = selectedCategory == null || p.category.equals(selectedCategory, ignoreCase = true)
                        matchesSearch && matchesCategory
                    }
                }

                // Category Chips and Action Row
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            item {
                                FilterChip(
                                    selected = selectedCategory == null,
                                    onClick = { selectedCategory = null },
                                    label = { Text("Все") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                )
                            }
                            items(categories) { cat ->
                                FilterChip(
                                    selected = selectedCategory == cat,
                                    onClick = { selectedCategory = if (selectedCategory == cat) null else cat },
                                    label = { Text(cat) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        Button(
                            onClick = {
                                editingProduct = null
                                showAddEditModal = true
                            },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("add_product_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Товар", style = MaterialTheme.typography.labelLarge)
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        OutlinedButton(
                            onClick = {
                                preselectedProductForSupply = null
                                showCreateSupplyModal = true
                            },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.LocalShipping, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Поставка", style = MaterialTheme.typography.labelLarge)
                        }
                    }

                    if (filteredProducts.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.Inventory,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.outline
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Список товаров пуст",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.outline
                                )
                                Text(
                                    "Нажмите '+ Товар' или оформите закупку через 'Поставка'",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    } else {
                        Box(modifier = Modifier.fillMaxSize()) {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 6.dp, bottom = 24.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(filteredProducts, key = { it.id }) { product ->
                                    ProductDetailRow(
                                        product = product,
                                        onEdit = {
                                            editingProduct = product
                                            showAddEditModal = true
                                        },
                                        onStockIntake = {
                                            preselectedProductForSupply = product
                                            showCreateSupplyModal = true
                                        },
                                        onShareCard = {
                                            showcaseProduct = product
                                            showShowcaseModal = true
                                        },
                                        onDelete = {
                                            viewModel.deleteProduct(product)
                                        }
                                    )
                                }
                            }

                            // Top Shadow Overlay
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(10.dp)
                                    .align(Alignment.TopCenter)
                                    .background(
                                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                            colors = listOf(
                                                Color.Black.copy(alpha = 0.12f),
                                                Color.Transparent
                                            )
                                        )
                                    )
                            )
                        }
                    }
                }
            } else {
                // GROUPED SUPPLY BATCHES TAB
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp)
                ) {
                    // Header Banner with Analytics
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Поставки & Приход товара",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Групповой учёт поступлений и аналитика партий",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Button(
                                    onClick = {
                                        preselectedProductForSupply = null
                                        showCreateSupplyModal = true
                                    },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                    modifier = Modifier.testTag("new_supply_button")
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Новая поставка")
                                }
                            }

                            if (supplyBatches.isNotEmpty()) {
                                val totalCostAll = supplyBatches.sumOf { it.totalCostPrice }
                                val totalSellingAll = supplyBatches.sumOf { it.totalSellingPrice }
                                val totalProfitAll = totalSellingAll - totalCostAll
                                val markupAll = if (totalCostAll > 0) (totalProfitAll / totalCostAll * 100) else 0.0

                                Spacer(modifier = Modifier.height(10.dp))
                                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Всего закупка:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                        Text("${totalCostAll.toInt()} ₽", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Ожид. выручка:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                        Text("${totalSellingAll.toInt()} ₽", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                    }
                                    Column(modifier = Modifier.weight(1.2f)) {
                                        Text("Маржа / Наценка:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                        Text(
                                            "+${totalProfitAll.toInt()} ₽ (${markupAll.toInt()}%)",
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.primary,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (supplyBatches.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.LocalShipping,
                                    contentDescription = null,
                                    modifier = Modifier.size(56.dp),
                                    tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    "Поставок пока не зафиксировано",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.outline
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "Нажмите '+ Новая поставка', чтобы оприходовать группу товаров",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 24.dp)
                        ) {
                            items(supplyBatches, key = { it.id }) { batch ->
                                SupplyBatchCard(batch = batch, viewModel = viewModel)
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal: Create or Edit Product
    if (showAddEditModal) {
        AddEditProductModal(
            productToEdit = editingProduct,
            existingCategories = products.map { it.category }.distinct().filter { it.isNotBlank() },
            onDismiss = { showAddEditModal = false },
            onSave = { product ->
                viewModel.saveProduct(product)
                showAddEditModal = false
            }
        )
    }

    // Master Modal: Create Supply Batch
    if (showCreateSupplyModal) {
        CreateSupplyBatchDialog(
            products = products,
            initialSelectedProduct = preselectedProductForSupply,
            onDismiss = { showCreateSupplyModal = false },
            onConfirmBatch = { supplierName, invoiceNumber, note, items ->
                viewModel.recordGroupedSupplyBatch(
                    supplierName = supplierName,
                    invoiceNumber = invoiceNumber,
                    note = note,
                    items = items
                )
                showCreateSupplyModal = false
            },
            onRequestCreateProduct = {
                editingProduct = null
                showAddEditModal = true
            }
        )
    }

    // Modal: Showcase product
    if (showShowcaseModal && showcaseProduct != null) {
        ProductShowcaseModal(
            product = showcaseProduct!!,
            onDismiss = { showShowcaseModal = false }
        )
    }
}

@Composable
fun SupplyBatchCard(
    batch: SupplyBatchEntity,
    viewModel: PosViewModel
) {
    var isExpanded by remember { mutableStateOf(false) }
    var suppliesList by remember { mutableStateOf<List<SupplyEntity>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }

    val dateFormat = remember { SimpleDateFormat("dd.MM.yy HH:mm", Locale.getDefault()) }
    val scope = rememberCoroutineScope()

    val title = if (batch.supplierName.isNotBlank()) batch.supplierName else "Поставка №${batch.id}"
    val profit = batch.totalSellingPrice - batch.totalCostPrice
    val markupPercent = if (batch.totalCostPrice > 0) (profit / batch.totalCostPrice * 100) else 0.0

    LaunchedEffect(isExpanded) {
        if (isExpanded && suppliesList.isEmpty()) {
            isLoading = true
            suppliesList = viewModel.getSuppliesForBatch(batch.id)
            isLoading = false
        }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.LocalShipping,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (batch.invoiceNumber.isNotBlank()) {
                                Text(
                                    text = "Накл: ${batch.invoiceNumber}  •  ",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = dateFormat.format(Date(batch.timestamp)),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }

                IconButton(
                    onClick = { isExpanded = !isExpanded },
                    modifier = Modifier.testTag("expand_batch_${batch.id}")
                ) {
                    Icon(
                        if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Развернуть поставку"
                    )
                }
            }

            if (batch.note.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "💬 ${batch.note}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(10.dp))

            // Batch Analytics Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("💰 Закупка", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    Text("${batch.totalCostPrice.toInt()} ₽", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("📈 Выручка", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    Text("${batch.totalSellingPrice.toInt()} ₽", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                }
                Column(modifier = Modifier.weight(1.2f)) {
                    Text("💵 Прибыль (Маржа)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    Text(
                        "+${profit.toInt()} ₽ (${markupPercent.toInt()}%)",
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📦 Содержит: ${batch.itemCount} наим. (${batch.totalQuantity.toInt()} шт)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                TextButton(
                    onClick = { isExpanded = !isExpanded },
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        if (isExpanded) "Свернуть товары" else "Посмотреть товары",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }

            // Expandable Items Table
            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Товары этой поставки:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    if (isLoading) {
                        Text(
                            "Загрузка позиций...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    } else if (suppliesList.isEmpty()) {
                        Text(
                            "Нет детальных записей по товарам",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            suppliesList.forEach { supply ->
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = supply.productName,
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.bodySmall,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Text(
                                                text = "+${supply.quantityAdded.toInt()} шт",
                                                fontWeight = FontWeight.ExtraBold,
                                                color = MaterialTheme.colorScheme.primary,
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(2.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "Было: ${supply.stockBefore.toInt()} → Стало: ${supply.stockAfter.toInt()}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.outline
                                            )
                                            Text(
                                                text = "Закупка: ${supply.costPriceAtSupply.toInt()}₽ | Итого: ${(supply.costPriceAtSupply * supply.quantityAdded).toInt()}₽",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateSupplyBatchDialog(
    products: List<ProductEntity>,
    initialSelectedProduct: ProductEntity? = null,
    onDismiss: () -> Unit,
    onConfirmBatch: (supplierName: String, invoiceNumber: String, note: String, items: List<SupplyBatchItemDraft>) -> Unit,
    onRequestCreateProduct: () -> Unit
) {
    var supplierName by remember { mutableStateOf("") }
    var invoiceNumber by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    val draftItems = remember { mutableStateListOf<SupplyBatchItemDraft>() }

    // Selected product adding form state
    var selectedProduct by remember { mutableStateOf<ProductEntity?>(initialSelectedProduct ?: products.firstOrNull()) }
    var productDropdownExpanded by remember { mutableStateOf(false) }

    var quantityInput by remember { mutableStateOf("1") }
    var costPriceInput by remember { mutableStateOf(selectedProduct?.costPrice?.toInt()?.toString() ?: "0") }
    var sellingPriceInput by remember { mutableStateOf(selectedProduct?.sellingPrice?.toInt()?.toString() ?: "0") }
    var itemNoteInput by remember { mutableStateOf("") }

    // When selected product changes, reset prices to its default
    LaunchedEffect(selectedProduct) {
        selectedProduct?.let { p ->
            costPriceInput = p.costPrice.toInt().toString()
            sellingPriceInput = p.sellingPrice.toInt().toString()
        }
    }

    // Calculations for draft batch
    val totalDraftCost = draftItems.sumOf { it.quantityAdded * it.costPrice }
    val totalDraftRevenue = draftItems.sumOf { it.quantityAdded * it.sellingPrice }
    val totalDraftProfit = totalDraftRevenue - totalDraftCost
    val draftMarkupPercent = if (totalDraftCost > 0) (totalDraftProfit / totalDraftCost * 100) else 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth(0.96f)
            .padding(vertical = 12.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.LocalShipping,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Новая поставка товаров",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Clear, contentDescription = "Закрыть")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Section 1: Batch Info
                Text(
                    "1. Параметры поставки",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = supplierName,
                        onValueChange = { supplierName = it },
                        label = { Text("Поставщик / Компания") },
                        placeholder = { Text("ООО Вектор") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1.2f)
                            .testTag("supply_supplier_input")
                    )
                    OutlinedTextField(
                        value = invoiceNumber,
                        onValueChange = { invoiceNumber = it },
                        label = { Text("№ Накладной") },
                        placeholder = { Text("ТН-104") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(0.8f)
                            .testTag("supply_invoice_input")
                    )
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Заметка к поставке (необязательно)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(4.dp))
                Divider()

                // Section 2: Real-time Draft Summary Banner
                Text(
                    "2. Аналитика создаваемой поставки",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Итого закупка:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                Text("${totalDraftCost.toInt()} ₽", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Ожид. выручка:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                Text("${totalDraftRevenue.toInt()} ₽", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            }
                            Column(modifier = Modifier.weight(1.2f)) {
                                Text("Прибыль (Маржа):", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                Text(
                                    "+${totalDraftProfit.toInt()} ₽ (${draftMarkupPercent.toInt()}%)",
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Позиций: ${draftItems.size} наим. (${draftItems.sumOf { it.quantityAdded }.toInt()} шт)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Section 3: Added Items in Draft
                if (draftItems.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Товары в поставку пока не добавлены.\nВыберите товар ниже и нажмите '+ В поставку'.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                } else {
                    Text(
                        "Товары в этой поставке (${draftItems.size}):",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        draftItems.forEachIndexed { index, draft ->
                            val matchedProduct = products.find { it.id == draft.productId }
                            val prodName = matchedProduct?.name ?: "Товар #${draft.productId}"
                            val subCost = draft.quantityAdded * draft.costPrice
                            val subRev = draft.quantityAdded * draft.sellingPrice

                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = prodName,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${draft.quantityAdded.toInt()} шт x ${draft.costPrice.toInt()}₽ (Закуп: ${subCost.toInt()}₽) | Продажа: ${draft.sellingPrice.toInt()}₽ (${subRev.toInt()}₽)",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    IconButton(
                                        onClick = { draftItems.removeAt(index) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Удалить из поставки",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Divider()

                // Section 4: Add Product Form
                Text(
                    "3. Добавить товар в поставку",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                if (products.isEmpty()) {
                    Column {
                        Text(
                            "В каталоге пока нет товаров.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Button(
                            onClick = onRequestCreateProduct,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Создать первый товар")
                        }
                    }
                } else {
                    // Product Selection Dropdown
                    ExposedDropdownMenuBox(
                        expanded = productDropdownExpanded,
                        onExpandedChange = { productDropdownExpanded = !productDropdownExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = selectedProduct?.name ?: "Выберите товар из каталога",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Выберите товар *") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = productDropdownExpanded) },
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                                .testTag("supply_product_selector")
                        )

                        ExposedDropdownMenu(
                            expanded = productDropdownExpanded,
                            onDismissRequest = { productDropdownExpanded = false }
                        ) {
                            products.forEach { prod ->
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(prod.name, fontWeight = FontWeight.SemiBold)
                                            Text("Остаток: ${prod.currentStock.toInt()} ${prod.unit}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                                        }
                                    },
                                    onClick = {
                                        selectedProduct = prod
                                        productDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    selectedProduct?.let { prod ->
                        Text(
                            text = "Текущий остаток на складе: ${prod.currentStock.toInt()} ${prod.unit}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(
                            value = quantityInput,
                            onValueChange = { quantityInput = it },
                            label = { Text("Кол-во *") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier
                                .weight(0.9f)
                                .testTag("supply_add_qty")
                        )
                        OutlinedTextField(
                            value = costPriceInput,
                            onValueChange = { costPriceInput = it },
                            label = { Text("Закупка (₽)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("supply_add_cost")
                        )
                        OutlinedTextField(
                            value = sellingPriceInput,
                            onValueChange = { sellingPriceInput = it },
                            label = { Text("Продажа (₽)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("supply_add_selling")
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onRequestCreateProduct,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Новый товар")
                        }

                        Button(
                            onClick = {
                                val prod = selectedProduct ?: return@Button
                                val q = quantityInput.toDoubleOrNull() ?: 0.0
                                val c = costPriceInput.toDoubleOrNull() ?: prod.costPrice
                                val s = sellingPriceInput.toDoubleOrNull() ?: prod.sellingPrice

                                if (q > 0) {
                                    draftItems.add(
                                        SupplyBatchItemDraft(
                                            productId = prod.id,
                                            quantityAdded = q,
                                            costPrice = c,
                                            sellingPrice = s,
                                            note = itemNoteInput.ifBlank { null }
                                        )
                                    )
                                    // Reset input quantity for next item
                                    quantityInput = "1"
                                }
                            },
                            enabled = selectedProduct != null && (quantityInput.toDoubleOrNull() ?: 0.0) > 0,
                            modifier = Modifier
                                .weight(1.3f)
                                .testTag("add_item_to_batch_button")
                        ) {
                            Icon(Icons.Default.AddShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("+ В поставку")
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalSupplier = supplierName.ifBlank { "Поставка №" + System.currentTimeMillis().toString().takeLast(4) }
                    onConfirmBatch(finalSupplier, invoiceNumber, note, draftItems.toList())
                },
                enabled = draftItems.isNotEmpty(),
                modifier = Modifier.testTag("confirm_supply_batch")
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Провести поставку (${draftItems.size})")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditProductModal(
    productToEdit: ProductEntity?,
    existingCategories: List<String>,
    onDismiss: () -> Unit,
    onSave: (ProductEntity) -> Unit
) {
    var name by remember { mutableStateOf(productToEdit?.name ?: "") }
    var category by remember { mutableStateOf(productToEdit?.category ?: "Общее") }
    var costPriceStr by remember { mutableStateOf(productToEdit?.costPrice?.toInt()?.toString() ?: "0") }
    var sellingPriceStr by remember { mutableStateOf(productToEdit?.sellingPrice?.toInt()?.toString() ?: "0") }
    var currentStockStr by remember { mutableStateOf(productToEdit?.currentStock?.toInt()?.toString() ?: "0") }
    var unit by remember { mutableStateOf(productToEdit?.unit ?: "шт") }

    var categoryExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth(0.95f)
            .padding(vertical = 12.dp),
        title = {
            Text(
                text = if (productToEdit == null) "Создать новый товар" else "Редактировать товар",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Название товара *") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("product_name_input")
                )

                // Category Selection
                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = !categoryExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = {
                            category = it
                            categoryExpanded = true
                        },
                        label = { Text("Категория (выберите или введите)") },
                        placeholder = { Text("Общее") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        singleLine = true,
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )

                    val categoriesToShow = remember(existingCategories, category) {
                        if (category.isBlank()) {
                            existingCategories
                        } else {
                            val filtered = existingCategories.filter { it.contains(category, ignoreCase = true) }
                            if (filtered.isNotEmpty()) filtered else existingCategories
                        }
                    }

                    if (existingCategories.isNotEmpty()) {
                        ExposedDropdownMenu(
                            expanded = categoryExpanded,
                            onDismissRequest = { categoryExpanded = false }
                        ) {
                            categoriesToShow.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat) },
                                    onClick = {
                                        category = cat
                                        categoryExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = costPriceStr,
                        onValueChange = { costPriceStr = it },
                        label = { Text("Себест. (₽)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("product_cost_input")
                    )
                    OutlinedTextField(
                        value = sellingPriceStr,
                        onValueChange = { sellingPriceStr = it },
                        label = { Text("Продажа (₽) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("product_price_input")
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = currentStockStr,
                        onValueChange = { currentStockStr = it },
                        label = { Text("Нач. остаток") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text("Ед. изм.") },
                        singleLine = true,
                        modifier = Modifier.weight(0.8f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cost = costPriceStr.toDoubleOrNull() ?: 0.0
                    val selling = sellingPriceStr.toDoubleOrNull() ?: 0.0
                    val stock = currentStockStr.toDoubleOrNull() ?: 0.0

                    val product = productToEdit?.copy(
                        name = name,
                        sku = "",
                        category = category.ifBlank { "Общее" },
                        costPrice = cost,
                        sellingPrice = selling,
                        currentStock = stock,
                        unit = unit
                    ) ?: ProductEntity(
                        name = name,
                        sku = "",
                        category = category.ifBlank { "Общее" },
                        costPrice = cost,
                        sellingPrice = selling,
                        currentStock = stock,
                        unit = unit
                    )
                    onSave(product)
                },
                enabled = name.isNotBlank(),
                modifier = Modifier.testTag("save_product_confirm")
            ) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

@Composable
fun ProductDetailRow(
    product: ProductEntity,
    onEdit: () -> Unit,
    onStockIntake: () -> Unit,
    onShareCard: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Категория: ${product.category}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = "${product.sellingPrice.toInt()} ₽",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column {
                        Text("Остаток", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        Text(
                            text = "${product.currentStock.toInt()} ${product.unit}",
                            fontWeight = FontWeight.Bold,
                            color = if (product.currentStock <= 5) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Column {
                        Text("Себестоимость", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        Text(
                            text = "${product.costPrice.toInt()} ₽",
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Column {
                        Text("Наценка", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        val margin = product.sellingPrice - product.costPrice
                        Text(
                            text = "+${margin.toInt()} ₽",
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    IconButton(onClick = onStockIntake, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Default.LocalShipping,
                            contentDescription = "Оформить поставку",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(onClick = onShareCard, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = "Витрина",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Редактировать",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Удалить",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ProductShowcaseModal(
    product: ProductEntity,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Карточка товара для витрины", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        },
        text = {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Inventory,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = product.category,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "${product.sellingPrice.toInt()} ₽",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "В наличии: ${product.currentStock.toInt()} ${product.unit}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Закрыть")
            }
        }
    )
}
