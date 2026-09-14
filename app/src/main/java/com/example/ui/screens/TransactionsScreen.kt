package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AssignmentReturn
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Money
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.example.data.entity.TransactionEntity
import com.example.data.entity.TransactionItemEntity
import com.example.ui.PosViewModel
import com.example.ui.theme.PosSuccess
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    viewModel: PosViewModel,
    transactions: List<TransactionEntity>
) {
    var selectedFilter by remember { mutableStateOf("ALL") } // ALL, SALE, RETURN
    var selectedTxForReturn by remember { mutableStateOf<TransactionEntity?>(null) }
    var txItemsToReturn by remember { mutableStateOf<List<TransactionItemEntity>>(emptyList()) }
    var txAlreadyReturnedMap by remember { mutableStateOf<Map<Long, Double>>(emptyMap()) }

    val filteredTransactions = remember(transactions, selectedFilter) {
        val sales = transactions.filter { it.type == "SALE" }
        val orphanReturns = transactions.filter { it.type == "RETURN" && sales.none { s -> s.id == it.relatedTransactionId } }
        when (selectedFilter) {
            "SALE" -> sales
            "RETURN" -> {
                val salesWithReturns = sales.filter { tx -> transactions.any { ret -> ret.relatedTransactionId == tx.id && ret.type == "RETURN" } }
                salesWithReturns + orphanReturns
            }
            else -> sales + orphanReturns
        }
    }

    val salesCount = remember(transactions) { transactions.count { it.type == "SALE" } }
    val returnsCount = remember(transactions) {
        transactions.count { tx ->
            tx.type == "SALE" && transactions.any { ret -> ret.relatedTransactionId == tx.id && ret.type == "RETURN" }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp)) {
                    Text(
                        text = "История чеков и продаж",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Всего продаж: $salesCount",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Scrollable Filter Chips - Edge-to-Edge Carousel
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        FilterChip(
                            selected = selectedFilter == "ALL",
                            onClick = { selectedFilter = "ALL" },
                            label = { Text("Все ($salesCount)") }
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedFilter == "SALE",
                            onClick = { selectedFilter = "SALE" },
                            label = { Text("Продажи ($salesCount)") }
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedFilter == "RETURN",
                            onClick = { selectedFilter = "RETURN" },
                            label = { Text("Возвраты ($returnsCount)") }
                        )
                    }
                }
            }

            if (filteredTransactions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.ReceiptLong,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Транзакции не найдены",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = "Проведите первую продажу в разделе 'Касса'",
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
                        items(filteredTransactions, key = { it.id }) { tx ->
                            TransactionCard(
                                transaction = tx,
                                allTransactions = transactions,
                                viewModel = viewModel,
                                onInitiateReturn = { transaction, items, alreadyReturnedMap ->
                                    selectedTxForReturn = transaction
                                    txItemsToReturn = items
                                    txAlreadyReturnedMap = alreadyReturnedMap
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
                                        androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.12f),
                                        androidx.compose.ui.graphics.Color.Transparent
                                    )
                                )
                            )
                    )
                }
            }
        }

        // Return Modal Dialog
        if (selectedTxForReturn != null && txItemsToReturn.isNotEmpty()) {
            ProcessReturnDialog(
                transaction = selectedTxForReturn!!,
                items = txItemsToReturn,
                alreadyReturnedMap = txAlreadyReturnedMap,
                onDismiss = {
                    selectedTxForReturn = null
                    txItemsToReturn = emptyList()
                    txAlreadyReturnedMap = emptyMap()
                },
                onConfirmReturn = { selectedItems ->
                    viewModel.processReturn(selectedTxForReturn!!, selectedItems)
                    selectedTxForReturn = null
                    txItemsToReturn = emptyList()
                    txAlreadyReturnedMap = emptyMap()
                }
            )
        }
    }
}

@Composable
fun TransactionCard(
    transaction: TransactionEntity,
    allTransactions: List<TransactionEntity>,
    viewModel: PosViewModel,
    onInitiateReturn: (TransactionEntity, List<TransactionItemEntity>, Map<Long, Double>) -> Unit
) {
    var items by remember { mutableStateOf<List<TransactionItemEntity>>(emptyList()) }
    var isExpanded by remember { mutableStateOf(false) }

    val dateFormat = SimpleDateFormat("dd.MM.yy HH:mm", Locale.getDefault())
    val isSale = transaction.type == "SALE"

    val relatedReturns = remember(allTransactions, transaction.id) {
        allTransactions.filter { it.type == "RETURN" && it.relatedTransactionId == transaction.id }
    }

    var returnInfoMap by remember { mutableStateOf<Map<Long, List<Pair<Long, Double>>>>(emptyMap()) }
    var totalReturnedAmount by remember { mutableStateOf(0.0) }

    LaunchedEffect(transaction.id, relatedReturns) {
        val saleItems = viewModel.getTransactionItems(transaction.id)
        items = saleItems

        if (relatedReturns.isNotEmpty()) {
            val map = mutableMapOf<Long, MutableList<Pair<Long, Double>>>()
            var sumReturn = 0.0
            for (retTx in relatedReturns) {
                sumReturn += retTx.totalAmount
                val retItems = viewModel.getTransactionItems(retTx.id)
                for (rItem in retItems) {
                    val list = map.getOrPut(rItem.productId) { mutableListOf() }
                    list.add(Pair(retTx.id, rItem.quantity))
                }
            }
            returnInfoMap = map
            totalReturnedAmount = sumReturn
        } else {
            returnInfoMap = emptyMap()
            totalReturnedAmount = 0.0
        }
    }

    val allReturnTxIdsStr = remember(relatedReturns) {
        relatedReturns.map { "№${it.id}" }.joinToString(", ")
    }

    val isFullyReturned = remember(totalReturnedAmount, transaction.totalAmount, items, returnInfoMap) {
        if (totalReturnedAmount <= 0.0) false
        else if (totalReturnedAmount >= transaction.totalAmount) true
        else items.isNotEmpty() && items.all {
            val retQty = returnInfoMap[it.productId]?.sumOf { pair -> pair.second } ?: 0.0
            retQty >= it.quantity
        }
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("tx_card_${transaction.id}")
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Top Row: Type Badge + Shift + Date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    val badgeColor = when {
                        !isSale -> MaterialTheme.colorScheme.errorContainer
                        isFullyReturned -> MaterialTheme.colorScheme.errorContainer
                        relatedReturns.isNotEmpty() -> Color(0xFFFFF3E0)
                        else -> PosSuccess.copy(alpha = 0.15f)
                    }

                    val textColor = when {
                        !isSale -> MaterialTheme.colorScheme.error
                        isFullyReturned -> MaterialTheme.colorScheme.error
                        relatedReturns.isNotEmpty() -> Color(0xFFE65100)
                        else -> PosSuccess
                    }

                    val badgeText = when {
                        !isSale -> "ВОЗВРАТ №${transaction.id}"
                        isFullyReturned -> "ВОЗВРАТ (Чек $allReturnTxIdsStr)"
                        relatedReturns.isNotEmpty() -> "ЧАСТИЧНЫЙ ВОЗВРАТ (Чек $allReturnTxIdsStr)"
                        else -> "ПРОДАЖА №${transaction.id}"
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = badgeColor
                    ) {
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = textColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        text = "Смена №${transaction.shiftId}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                Text(
                    text = dateFormat.format(Date(transaction.timestamp)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Customer & Paid Status Row
            if (transaction.customerName != null || isSale) {
                Spacer(modifier = Modifier.height(6.dp))
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
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = transaction.customerName ?: "Частный клиент",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (isSale) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (transaction.isPaid) "Оплачен" else "В долг",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (transaction.isPaid) PosSuccess else MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(end = 4.dp)
                            )
                            Switch(
                                checked = transaction.isPaid,
                                onCheckedChange = { isChecked ->
                                    viewModel.toggleTransactionPaidStatus(transaction.id, isChecked)
                                },
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

            Spacer(modifier = Modifier.height(8.dp))
            Divider()
            Spacer(modifier = Modifier.height(8.dp))

            // Items preview with Return indicators
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items.take(if (isExpanded) items.size else 2).forEach { item ->
                    val returnedQty = returnInfoMap[item.productId]?.sumOf { it.second } ?: 0.0
                    val itemReturnTxIds = returnInfoMap[item.productId]?.map { "№${it.first}" }?.distinct()?.joinToString(", ") ?: ""
                    val isItemFullyReturned = returnedQty >= item.quantity

                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${item.productName} × ${item.quantity.toInt()} ${item.unit}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                textDecoration = if (isItemFullyReturned) TextDecoration.LineThrough else TextDecoration.None,
                                color = if (isItemFullyReturned) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${(item.quantity * item.unitPrice).toInt()} ₽",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                textDecoration = if (isItemFullyReturned) TextDecoration.LineThrough else TextDecoration.None,
                                color = if (isItemFullyReturned) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        if (returnedQty > 0) {
                            Text(
                                text = "↳ Возвращено ${returnedQty.toInt()} ${item.unit} (Возврат по Чеку $itemReturnTxIds)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(start = 8.dp, top = 1.dp)
                            )
                        }
                    }
                }
                if (items.size > 2 && !isExpanded) {
                    TextButton(
                        onClick = { isExpanded = true },
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text("Показать все (${items.size} поз.)", fontSize = 11.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Divider()
            Spacer(modifier = Modifier.height(8.dp))

            // Footer Row: Payment method + Total
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val payIcon = when (transaction.paymentMethod) {
                        "CARD" -> Icons.Default.CreditCard
                        "QR" -> Icons.Default.QrCode
                        else -> Icons.Default.Money
                    }
                    Icon(payIcon, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.outline)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = when(transaction.paymentMethod) {
                            "CARD" -> "Карта"
                            "QR" -> "СБП / QR"
                            else -> "Наличные"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "ИТОГО: ",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                    if (totalReturnedAmount > 0) {
                        Text(
                            text = "${transaction.totalAmount.toInt()} ₽ ",
                            style = MaterialTheme.typography.bodySmall,
                            textDecoration = TextDecoration.LineThrough,
                            color = MaterialTheme.colorScheme.outline
                        )
                        val netAmount = (transaction.totalAmount - totalReturnedAmount).coerceAtLeast(0.0)
                        Text(
                            text = "${netAmount.toInt()} ₽",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isFullyReturned) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Text(
                            text = "${if (isSale) "" else "-"}${transaction.totalAmount.toInt()} ₽",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isSale) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            // Action: Return for Sale
            if (isSale) {
                Spacer(modifier = Modifier.height(8.dp))
                if (isFullyReturned) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.AssignmentReturn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Товар полностью возвращен (Чек $allReturnTxIdsStr)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                } else {
                    OutlinedButton(
                        onClick = {
                            val alreadyReturnedMap = returnInfoMap.mapValues { entry -> entry.value.sumOf { it.second } }
                            onInitiateReturn(transaction, items, alreadyReturnedMap)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp)
                            .testTag("return_button_${transaction.id}"),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.AssignmentReturn, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (totalReturnedAmount > 0) "Оформить доп. возврат" else "Оформить возврат",
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ProcessReturnDialog(
    transaction: TransactionEntity,
    items: List<TransactionItemEntity>,
    alreadyReturnedMap: Map<Long, Double>,
    onDismiss: () -> Unit,
    onConfirmReturn: (List<TransactionItemEntity>) -> Unit
) {
    // Map of productId -> Pair(isSelected: Boolean, returnQuantity: Double)
    val returnStateMap = remember {
        mutableStateMapOf<Long, Pair<Boolean, Double>>().apply {
            items.forEach { item ->
                val alreadyReturned = alreadyReturnedMap[item.productId] ?: 0.0
                val maxReturnable = (item.quantity - alreadyReturned).coerceAtLeast(0.0)
                if (maxReturnable > 0) {
                    this[item.productId] = Pair(true, maxReturnable)
                } else {
                    this[item.productId] = Pair(false, 0.0)
                }
            }
        }
    }

    val selectedItemsToReturn = items.mapNotNull { item ->
        val (isSelected, qty) = returnStateMap[item.productId] ?: Pair(false, 0.0)
        if (isSelected && qty > 0) {
            item.copy(quantity = qty)
        } else {
            null
        }
    }

    val refundTotal = selectedItemsToReturn.sumOf { it.quantity * it.unitPrice }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth(0.95f)
            .padding(vertical = 12.dp),
        title = {
            Column {
                Text("Оформление возврата", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    text = "Чек №${transaction.id}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Укажите количество товаров для возврата на склад:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(modifier = Modifier.height(200.dp)) {
                    items(items) { item ->
                        val alreadyReturned = alreadyReturnedMap[item.productId] ?: 0.0
                        val maxReturnable = (item.quantity - alreadyReturned).coerceAtLeast(0.0)
                        val (isSelected, currentQty) = returnStateMap[item.productId] ?: Pair(false, 0.0)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { checked ->
                                    if (maxReturnable > 0) {
                                        returnStateMap[item.productId] = Pair(checked, if (checked) (if (currentQty > 0) currentQty else maxReturnable) else 0.0)
                                    }
                                },
                                enabled = maxReturnable > 0
                            )

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.productName,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = if (maxReturnable <= 0) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface
                                )
                                if (maxReturnable <= 0) {
                                    Text(
                                        text = "Товар полностью возвращен",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error,
                                        fontSize = 11.sp
                                    )
                                } else {
                                    Text(
                                        text = "Куплено: ${item.quantity.toInt()} ${item.unit}" +
                                                if (alreadyReturned > 0) " (возвр: ${alreadyReturned.toInt()})" else "",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }

                            if (maxReturnable > 0 && isSelected) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (maxReturnable > 1) {
                                        IconButton(
                                            onClick = {
                                                val newQ = (currentQty - 1).coerceAtLeast(1.0)
                                                returnStateMap[item.productId] = Pair(true, newQ)
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(14.dp))
                                        }
                                        Text(
                                            text = "${currentQty.toInt()} ${item.unit}",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 2.dp)
                                        )
                                        IconButton(
                                            onClick = {
                                                val newQ = (currentQty + 1).coerceAtMost(maxReturnable)
                                                returnStateMap[item.productId] = Pair(true, newQ)
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                        }
                                    } else {
                                        Text(
                                            text = "${currentQty.toInt()} ${item.unit}",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "${(currentQty * item.unitPrice).toInt()} ₽",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }

                Divider()
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("К возврату:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "-${refundTotal.toInt()} ₽",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirmReturn(selectedItemsToReturn) },
                enabled = selectedItemsToReturn.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.testTag("confirm_return_button")
            ) {
                Text("Подтвердить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}
