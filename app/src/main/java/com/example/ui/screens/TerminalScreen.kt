package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Discount
import androidx.compose.material.icons.filled.Money
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.CustomerEntity
import com.example.data.entity.ProductEntity
import com.example.data.entity.ShiftEntity
import com.example.data.entity.TransactionEntity
import com.example.data.entity.TransactionItemEntity
import com.example.data.repository.CartItem
import com.example.ui.PosViewModel
import com.example.ui.theme.PosSuccess
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminalScreen(
    viewModel: PosViewModel,
    products: List<ProductEntity>,
    categories: List<String>,
    cart: List<CartItem>,
    cartDiscount: Double,
    currentShift: ShiftEntity?,
    lastReceipt: TransactionEntity?,
    lastReceiptItems: List<TransactionItemEntity>,
    searchQuery: String,
    selectedCategory: String
) {
    var selectedPaymentMethod by remember { mutableStateOf("CASH") }
    var showDiscountModal by remember { mutableStateOf(false) }
    var discountInput by remember { mutableStateOf(if (cartDiscount > 0) cartDiscount.toInt().toString() else "") }
    var showCheckoutSheet by remember { mutableStateOf(false) }

    val grossTotal = cart.sumOf { it.totalPrice }
    val netTotal = (grossTotal - cartDiscount).coerceAtLeast(0.0)
    val totalCartCount = cart.sumOf { it.quantity.toInt() }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 600.dp

        if (isWideScreen) {
            // Wide Screen Layout (Tablets / Landscape): Side-by-side
            Row(modifier = Modifier.fillMaxSize()) {
                // Left Column: Catalog
                Column(
                    modifier = Modifier
                        .weight(1.3f)
                        .fillMaxHeight()
                        .padding(12.dp)
                ) {
                    CatalogSection(
                        products = products,
                        categories = categories,
                        searchQuery = searchQuery,
                        selectedCategory = selectedCategory,
                        viewModel = viewModel
                    )
                }

                // Right Column: Cart Pane
                Surface(
                    modifier = Modifier
                        .weight(1.0f)
                        .fillMaxHeight(),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    tonalElevation = 2.dp
                ) {
                    CartCheckoutPane(
                        cart = cart,
                        grossTotal = grossTotal,
                        cartDiscount = cartDiscount,
                        netTotal = netTotal,
                        selectedPaymentMethod = selectedPaymentMethod,
                        onPaymentMethodSelect = { selectedPaymentMethod = it },
                        onOpenDiscountModal = {
                            discountInput = if (cartDiscount > 0) cartDiscount.toInt().toString() else ""
                            showDiscountModal = true
                        },
                        onCheckout = { viewModel.processCheckout(selectedPaymentMethod) },
                        onClearCart = { viewModel.clearCart() },
                        viewModel = viewModel
                    )
                }
            }
        } else {
            // Mobile Portrait Layout: Full-width catalog + Floating Cart Bar + Bottom Sheet Checkout
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    CatalogSection(
                        products = products,
                        categories = categories,
                        searchQuery = searchQuery,
                        selectedCategory = selectedCategory,
                        viewModel = viewModel,
                        bottomPadding = if (cart.isNotEmpty()) 80.dp else 0.dp
                    )
                }

                // Floating Cart Bar (Shown when cart has items)
                AnimatedVisibility(
                    visible = cart.isNotEmpty(),
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(12.dp)
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showCheckoutSheet = true }
                            .testTag("floating_cart_bar")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                BadgedBox(
                                    badge = {
                                        Badge(containerColor = MaterialTheme.colorScheme.primary) {
                                            Text("$totalCartCount")
                                        }
                                    }
                                ) {
                                    Icon(
                                        Icons.Default.ShoppingCart,
                                        contentDescription = "Cart",
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Чек: $totalCartCount поз.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                    )
                                    Text(
                                        text = "${netTotal.toInt()} ₽",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }

                            Button(
                                onClick = { showCheckoutSheet = true },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("open_checkout_sheet_button")
                            ) {
                                Text("Оплатить", fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }

        // Mobile Checkout Bottom Sheet
        if (showCheckoutSheet && !isWideScreen) {
            ModalBottomSheet(
                onDismissRequest = { showCheckoutSheet = false },
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                modifier = Modifier.testTag("checkout_bottom_sheet")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Оформление чека ($totalCartCount поз.)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Row {
                            TextButton(onClick = { viewModel.clearCart() }) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Очистить", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Divider()

                    // Cart Items List (Scrollable)
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(cart, key = { it.product.id }) { item ->
                            MobileCartItemCard(
                                item = item,
                                onIncrease = { viewModel.updateCartQuantity(item.product.id, item.quantity + 1) },
                                onDecrease = { viewModel.updateCartQuantity(item.product.id, item.quantity - 1) },
                                onRemove = { viewModel.updateCartQuantity(item.product.id, 0.0) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Divider()
                    Spacer(modifier = Modifier.height(8.dp))

                    // Discount & Total
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Итоговая сумма:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                            if (cartDiscount > 0) {
                                Text("Скидка: -${cartDiscount.toInt()} ₽", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = {
                                discountInput = if (cartDiscount > 0) cartDiscount.toInt().toString() else ""
                                showDiscountModal = true
                            }) {
                                Icon(Icons.Default.Discount, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (cartDiscount > 0) "-${cartDiscount.toInt()} ₽" else "+ Скидка", fontSize = 12.sp)
                            }

                            Text(
                                text = "${netTotal.toInt()} ₽",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    CustomerAndPaidSelector(viewModel = viewModel)

                    Spacer(modifier = Modifier.height(10.dp))

                    // Payment Method Selectors
                    Text("Выберите способ оплаты:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FullPaymentOptionCard(
                            title = "Наличные",
                            icon = Icons.Default.Money,
                            isSelected = selectedPaymentMethod == "CASH",
                            modifier = Modifier.weight(1f),
                            onClick = { selectedPaymentMethod = "CASH" }
                        )
                        FullPaymentOptionCard(
                            title = "Перевод",
                            icon = Icons.Default.CreditCard,
                            isSelected = selectedPaymentMethod == "CARD",
                            modifier = Modifier.weight(1f),
                            onClick = { selectedPaymentMethod = "CARD" }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Big Checkout Button
                    Button(
                        onClick = {
                            viewModel.processCheckout(selectedPaymentMethod)
                            showCheckoutSheet = false
                        },
                        enabled = cart.isNotEmpty(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("sheet_checkout_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ПРОБИТЬ ЧЕК (${netTotal.toInt()} ₽)",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }

        // Receipt Success Modal
        if (lastReceipt != null) {
            ReceiptSuccessDialog(
                receipt = lastReceipt,
                items = lastReceiptItems,
                onDismiss = { viewModel.clearReceipt() }
            )
        }

        // Discount Modal
        if (showDiscountModal) {
            AlertDialog(
                onDismissRequest = { showDiscountModal = false },
                title = { Text("Укажите скидку на чек (₽)") },
                text = {
                    Column {
                        OutlinedTextField(
                            value = discountInput,
                            onValueChange = { discountInput = it },
                            label = { Text("Сумма скидки в рублях") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        val disc = discountInput.toDoubleOrNull() ?: 0.0
                        viewModel.setCartDiscount(disc)
                        showDiscountModal = false
                    }) {
                        Text("Применить")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDiscountModal = false }) {
                        Text("Отмена")
                    }
                }
            )
        }
    }
}

@Composable
fun CatalogSection(
    products: List<ProductEntity>,
    categories: List<String>,
    searchQuery: String,
    selectedCategory: String,
    viewModel: PosViewModel,
    bottomPadding: androidx.compose.ui.unit.Dp = 0.dp
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Search Field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.setSearchQuery(it) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("terminal_search_input"),
            placeholder = { Text("Поиск товара или штрихкода...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.setSearchQuery("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Categories Pills
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(categories) { cat ->
                val isSelected = cat == selectedCategory
                FilterChip(
                    selected = isSelected,
                    onClick = { viewModel.setSelectedCategory(cat) },
                    label = { Text(cat, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Products Grid
        if (products.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Store,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Товары не найдены",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = "Добавьте товары в разделе 'Товары' или загрузите демо-данные в 'Настройках'",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 140.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(top = 4.dp, bottom = bottomPadding + 24.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(products, key = { it.id }) { product ->
                    SpaciousProductCard(
                        product = product,
                        onAddToCart = { viewModel.addToCart(product) }
                    )
                }
            }
        }
    }
}

@Composable
fun SpaciousProductCard(
    product: ProductEntity,
    onAddToCart: () -> Unit
) {
    val inStock = product.currentStock > 0

    Card(
        onClick = onAddToCart,
        enabled = inStock,
        colors = CardDefaults.cardColors(
            containerColor = if (inStock) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("product_card_${product.id}")
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = product.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "${product.sellingPrice.toInt()} ₽",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = if (inStock) "${product.currentStock.toInt()} ${product.unit}" else "Нет в наличии",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (inStock) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.error,
                        fontSize = 10.sp
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (inStock) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Add",
                            tint = if (inStock) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Чек",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (inStock) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(start = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MobileCartItemCard(
    item: CartItem,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    onRemove: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.product.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${item.unitPrice.toInt()} ₽ × ${item.quantity.toInt()} = ${(item.totalPrice).toInt()} ₽",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(
                    onClick = onDecrease,
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "-", modifier = Modifier.size(16.dp))
                }

                Text(
                    text = "${item.quantity.toInt()}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Button(
                    onClick = onIncrease,
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "+", modifier = Modifier.size(16.dp))
                }

                IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
fun FullPaymentOptionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        modifier = modifier.clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                title,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun CartCheckoutPane(
    cart: List<CartItem>,
    grossTotal: Double,
    cartDiscount: Double,
    netTotal: Double,
    selectedPaymentMethod: String,
    onPaymentMethodSelect: (String) -> Unit,
    onOpenDiscountModal: () -> Unit,
    onCheckout: () -> Unit,
    onClearCart: () -> Unit,
    viewModel: PosViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp)
    ) {
        // Cart Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.ShoppingCart,
                    contentDescription = "Cart",
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Чек заказа",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            if (cart.isNotEmpty()) {
                TextButton(onClick = onClearCart) {
                    Text("Очистить", color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
        Divider()
        Spacer(modifier = Modifier.height(6.dp))

        // Cart Items List
        if (cart.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Корзина пуста",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Нажмите на товар слева",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(cart, key = { it.product.id }) { item ->
                    MobileCartItemCard(
                        item = item,
                        onIncrease = { viewModel.updateCartQuantity(item.product.id, item.quantity + 1) },
                        onDecrease = { viewModel.updateCartQuantity(item.product.id, item.quantity - 1) },
                        onRemove = { viewModel.updateCartQuantity(item.product.id, 0.0) }
                    )
                }
            }
        }

        Divider()
        Spacer(modifier = Modifier.height(8.dp))

        // Totals & Checkout
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Сумма:", style = MaterialTheme.typography.bodyMedium)
                Text("${grossTotal.toInt()} ₽", style = MaterialTheme.typography.bodyMedium)
            }

            if (cartDiscount > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Скидка:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    Text("-${cartDiscount.toInt()} ₽", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("ИТОГО К ОПЛАТЕ:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "${netTotal.toInt()} ₽",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            TextButton(
                onClick = onOpenDiscountModal,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(if (cartDiscount > 0) "Изменить скидку" else "+ Добавить скидку")
            }

            Spacer(modifier = Modifier.height(4.dp))

            CustomerAndPaidSelector(viewModel = viewModel)

            Spacer(modifier = Modifier.height(6.dp))

            Text("Способ оплаты:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                FullPaymentOptionCard(
                    title = "Наличные",
                    icon = Icons.Default.Money,
                    isSelected = selectedPaymentMethod == "CASH",
                    modifier = Modifier.weight(1f),
                    onClick = { onPaymentMethodSelect("CASH") }
                )
                FullPaymentOptionCard(
                    title = "Перевод",
                    icon = Icons.Default.CreditCard,
                    isSelected = selectedPaymentMethod == "CARD",
                    modifier = Modifier.weight(1f),
                    onClick = { onPaymentMethodSelect("CARD") }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = onCheckout,
                enabled = cart.isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("checkout_button"),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("ПРОБИТЬ ЧЕК (${netTotal.toInt()} ₽)", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ReceiptSuccessDialog(
    receipt: TransactionEntity,
    items: List<TransactionItemEntity>,
    onDismiss: () -> Unit
) {
    val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(PosSuccess.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = PosSuccess, modifier = Modifier.size(32.dp))
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("ЧЕК ОПЛАЧЕН", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Транзакция №${receipt.id}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Дата: ${dateFormat.format(Date(receipt.timestamp))}", style = MaterialTheme.typography.bodySmall)
                Text("Способ оплаты: ${
                    when(receipt.paymentMethod) {
                        "CARD" -> "Банковская карта"
                        "QR" -> "СБП / QR-код"
                        else -> "Наличные"
                    }
                }", style = MaterialTheme.typography.bodySmall)

                Spacer(modifier = Modifier.height(8.dp))
                Divider()
                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(modifier = Modifier.height(130.dp)) {
                    items(items) { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("${item.productName} x${item.quantity.toInt()}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                            Text("${(item.quantity * item.unitPrice).toInt()} ₽", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Divider()
                Spacer(modifier = Modifier.height(8.dp))

                if (receipt.discountAmount > 0) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Скидка:", style = MaterialTheme.typography.bodySmall)
                        Text("-${receipt.discountAmount.toInt()} ₽", style = MaterialTheme.typography.bodySmall)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("ИТОГО:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text("${receipt.totalAmount.toInt()} ₽", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("ГОТОВО / СЛЕДУЮЩИЙ ЧЕК")
            }
        }
    )
}

@Composable
fun CustomerAndPaidSelector(viewModel: PosViewModel) {
    val customers by viewModel.allCustomers.collectAsState()
    val selectedCustomer by viewModel.selectedCheckoutCustomer.collectAsState()
    val isPaid by viewModel.checkoutIsPaid.collectAsState()

    var expanded by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            // Customer Selector Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text("Покупатель:", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                        Text(
                            text = selectedCustomer?.name ?: "Частный клиент",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Box {
                    TextButton(onClick = { expanded = true }) {
                        Text("Выбрать", fontSize = 11.sp)
                    }

                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Частный клиент (без имени)", fontWeight = FontWeight.Bold) },
                            onClick = {
                                viewModel.setSelectedCheckoutCustomer(null)
                                expanded = false
                            }
                        )
                        Divider()
                        customers.forEach { customer: CustomerEntity ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(customer.name, fontWeight = FontWeight.SemiBold)
                                        if (customer.phone.isNotEmpty()) {
                                            Text(customer.phone, fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                                        }
                                    }
                                },
                                onClick = {
                                    viewModel.setSelectedCheckoutCustomer(customer)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Divider()
            Spacer(modifier = Modifier.height(4.dp))

            // Switch Оплачено
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (isPaid) PosSuccess else MaterialTheme.colorScheme.error)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isPaid) "Оплачено" else "Оформление в долг",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isPaid) PosSuccess else MaterialTheme.colorScheme.error
                    )
                }

                Switch(
                    checked = isPaid,
                    onCheckedChange = { viewModel.setCheckoutIsPaid(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = PosSuccess,
                        checkedTrackColor = PosSuccess.copy(alpha = 0.3f),
                        uncheckedThumbColor = MaterialTheme.colorScheme.error,
                        uncheckedTrackColor = MaterialTheme.colorScheme.errorContainer
                    )
                )
            }
        }
    }
}
