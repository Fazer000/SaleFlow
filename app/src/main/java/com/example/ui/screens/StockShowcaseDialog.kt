package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.entity.ProductEntity
import com.example.util.CardStyle
import com.example.util.StockCardBitmapGenerator
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockShowcaseDialog(
    products: List<ProductEntity>,
    initialProduct: ProductEntity? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val inStockProducts = remember(products) {
        products.filter { it.currentStock > 0 }.ifEmpty { products }
    }

    var isSingleMode by remember { mutableStateOf(initialProduct != null || inStockProducts.size == 1) }
    var selectedProduct by remember { mutableStateOf(initialProduct ?: inStockProducts.firstOrNull()) }

    var shopName by remember { mutableStateOf("Наш Магазин") }
    var contactInfo by remember { mutableStateOf("+7 (999) 000-00-00") }
    var customNote by remember { mutableStateOf("Товар в наличии! Всегда свежий завоз") }
    var selectedStyle by remember { mutableStateOf(CardStyle.OCEAN) }

    var productDropdownExpanded by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
            shadowElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.90f)
                .padding(vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header: Title & Close Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 12.dp, top = 14.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Image,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            "Карточка товара в наличии",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Закрыть",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                // Scrollable Body
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    // Mode Selector Tabs
                    TabRow(
                        selectedTabIndex = if (isSingleMode) 0 else 1,
                        containerColor = Color.Transparent,
                        divider = {},
                        indicator = { tabPositions ->
                            val selectedIndex = if (isSingleMode) 0 else 1
                            if (selectedIndex < tabPositions.size) {
                                val currentTab = tabPositions[selectedIndex]
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
                            selected = isSingleMode,
                            onClick = { isSingleMode = true },
                            text = {
                                Text(
                                    "Один товар",
                                    fontSize = 13.sp,
                                    fontWeight = if (isSingleMode) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSingleMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        )
                        Tab(
                            selected = !isSingleMode,
                            onClick = { isSingleMode = false },
                            text = {
                                Text(
                                    "Витрина каталога",
                                    fontSize = 13.sp,
                                    fontWeight = if (!isSingleMode) FontWeight.Bold else FontWeight.Medium,
                                    color = if (!isSingleMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Product Selector (If single mode)
                    if (isSingleMode) {
                        Text("Выберите товар:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))

                        ExposedDropdownMenuBox(
                            expanded = productDropdownExpanded,
                            onExpandedChange = { productDropdownExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = selectedProduct?.name ?: "Не выбран",
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = productDropdownExpanded) },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp)
                            )

                            ExposedDropdownMenu(
                                expanded = productDropdownExpanded,
                                onDismissRequest = { productDropdownExpanded = false }
                            ) {
                                inStockProducts.forEach { prod ->
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(prod.name, fontWeight = FontWeight.SemiBold)
                                                Text(
                                                    "${formatCurrency(prod.sellingPrice)} ₽",
                                                    color = MaterialTheme.colorScheme.primary,
                                                    fontWeight = FontWeight.Bold
                                                )
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
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    // Store Customization Fields
                    OutlinedTextField(
                        value = shopName,
                        onValueChange = { shopName = it },
                        label = { Text("Название магазина / бренда") },
                        leadingIcon = { Icon(Icons.Default.Store, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = contactInfo,
                        onValueChange = { contactInfo = it },
                        label = { Text("Контакты / Телефон / Telegram") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    if (isSingleMode) {
                        OutlinedTextField(
                            value = customNote,
                            onValueChange = { customNote = it },
                            label = { Text("Заметка для клиента") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    // Color Style Selector
                    Text("Стиль оформления:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CardStyle.values().forEach { style ->
                            val isSelected = style == selectedStyle
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(style.bgStartColor),
                                border = if (isSelected) BorderStroke(3.dp, MaterialTheme.colorScheme.primary) else null,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .clickable { selectedStyle = style }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        style.title.take(6) + "..",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(style.primaryTextColor)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // LIVE PREVIEW CARD
                    Text("Превью скриншота:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(selectedStyle.bgStartColor),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color(selectedStyle.cardBgColor),
                                shadowElevation = 4.dp,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    val cleanShop = shopName.trim('.', ',', ' ', '\n', '\t')
                                    val cleanContact = contactInfo.trim('.', ',', ' ', '\n', '\t')

                                    if (cleanShop.isNotBlank()) {
                                        Text(
                                            cleanShop,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(selectedStyle.accentColor),
                                            fontSize = 16.sp
                                        )
                                    }
                                    if (cleanContact.isNotBlank()) {
                                        Text(
                                            cleanContact,
                                            fontSize = 11.sp,
                                            color = Color(selectedStyle.primaryTextColor).copy(alpha = 0.7f)
                                        )
                                    }

                                    if (cleanShop.isNotBlank() || cleanContact.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                    }

                                    Surface(
                                        color = Color(0xFF2E7D32),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            "В НАЛИЧИИ",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))
                                    Divider(color = Color(selectedStyle.primaryTextColor).copy(alpha = 0.2f))
                                    Spacer(modifier = Modifier.height(8.dp))

                                    if (isSingleMode && selectedProduct != null) {
                                        val prod = selectedProduct!!
                                        Text(
                                            prod.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 18.sp,
                                            color = Color(selectedStyle.primaryTextColor)
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))

                                        Surface(
                                            color = Color(selectedStyle.bgStartColor),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp)) {
                                                Text("Цена:", fontSize = 10.sp, color = Color(selectedStyle.primaryTextColor).copy(alpha = 0.7f))
                                                Text(
                                                    "${formatCurrency(prod.sellingPrice)} ₽ / ${prod.unit}",
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 20.sp,
                                                    color = Color(selectedStyle.priceTextColor)
                                                )
                                            }
                                        }

                                        if (customNote.isNotBlank()) {
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                "💬 $customNote",
                                                fontSize = 11.sp,
                                                color = Color(selectedStyle.primaryTextColor).copy(alpha = 0.8f)
                                            )
                                        }
                                    } else {
                                        Text(
                                            "Список товаров (${inStockProducts.size} шт.):",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = Color(selectedStyle.primaryTextColor)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))

                                        inStockProducts.forEach { item ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    item.name,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.weight(1f),
                                                    color = Color(selectedStyle.primaryTextColor)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    "${formatCurrency(item.sellingPrice)} ₽",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(selectedStyle.priceTextColor)
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(2.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                // Action Buttons Footer
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Main Share Button
                    Button(
                        onClick = {
                            val bitmap = if (isSingleMode && selectedProduct != null) {
                                StockCardBitmapGenerator.generateSingleProductCard(
                                    product = selectedProduct!!,
                                    shopName = shopName,
                                    contactInfo = contactInfo,
                                    customNote = customNote,
                                    style = selectedStyle
                                )
                            } else {
                                StockCardBitmapGenerator.generateCatalogShowcaseCard(
                                    products = inStockProducts,
                                    shopName = shopName,
                                    contactInfo = contactInfo,
                                    style = selectedStyle
                                )
                            }

                            StockCardBitmapGenerator.shareBitmap(
                                context = context,
                                bitmap = bitmap,
                                caption = if (isSingleMode) "${selectedProduct?.name}: ${selectedProduct?.sellingPrice} ₽ в наличии!" else "Товары в наличии: $shopName"
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("share_stock_card_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Поделиться скриншотом в мессенджер")
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Save to Gallery
                        OutlinedButton(
                            onClick = {
                                val bitmap = if (isSingleMode && selectedProduct != null) {
                                    StockCardBitmapGenerator.generateSingleProductCard(
                                        product = selectedProduct!!,
                                        shopName = shopName,
                                        contactInfo = contactInfo,
                                        customNote = customNote,
                                        style = selectedStyle
                                    )
                                } else {
                                    StockCardBitmapGenerator.generateCatalogShowcaseCard(
                                        products = inStockProducts,
                                        shopName = shopName,
                                        contactInfo = contactInfo,
                                        style = selectedStyle
                                    )
                                }

                                val success = StockCardBitmapGenerator.saveBitmapToGallery(
                                    context = context,
                                    bitmap = bitmap,
                                    title = "StockCard_${System.currentTimeMillis()}"
                                )

                                if (success) {
                                    Toast.makeText(context, "Изображение сохранено в Галерею!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Не удалось сохранить файл", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("В галерею", fontSize = 12.sp)
                        }

                        // Copy Text Price
                        OutlinedButton(
                            onClick = {
                                val textToCopy = if (isSingleMode && selectedProduct != null) {
                                    "🛒 ${selectedProduct!!.name}\n💰 Цена: ${formatCurrency(selectedProduct!!.sellingPrice)} ₽ / ${selectedProduct!!.unit}\n📦 В наличии: ${selectedProduct!!.currentStock.toInt()} ${selectedProduct!!.unit}\n📍 $shopName $contactInfo"
                                } else {
                                    "🛒 ТОВАРЫ В НАЛИЧИИ ($shopName):\n" + inStockProducts.joinToString("\n") { "• ${it.name}: ${formatCurrency(it.sellingPrice)} ₽" } + "\n📍 Контакты: $contactInfo"
                                }

                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Прайс", textToCopy)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Текст скопирован в буфер!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Текст прайса", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

private fun formatCurrency(amount: Double): String {
    val format = NumberFormat.getNumberInstance(Locale("ru", "RU"))
    format.minimumFractionDigits = 0
    format.maximumFractionDigits = 2
    return format.format(amount)
}
