package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AssignmentReturn
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Money
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
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

    val filteredTransactions = remember(transactions, selectedFilter) {
        when (selectedFilter) {
            "SALE" -> transactions.filter { it.type == "SALE" }
            "RETURN" -> transactions.filter { it.type == "RETURN" }
            else -> transactions
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
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "История чеков и продаж",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Всего операций: ${transactions.size}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Scrollable Filter Chips for Mobile Screens
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        FilterChip(
                            selected = selectedFilter == "ALL",
                            onClick = { selectedFilter = "ALL" },
                            label = { Text("Все (${transactions.size})") }
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedFilter == "SALE",
                            onClick = { selectedFilter = "SALE" },
                            label = { Text("Продажи (${transactions.count { it.type == "SALE" }})") }
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedFilter == "RETURN",
                            onClick = { selectedFilter = "RETURN" },
                            label = { Text("Возвраты (${transactions.count { it.type == "RETURN" }})") }
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
                                viewModel = viewModel,
                                onInitiateReturn = { transaction, items ->
                                    selectedTxForReturn = transaction
                                    txItemsToReturn = items
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
                onDismiss = {
                    selectedTxForReturn = null
                    txItemsToReturn = emptyList()
                },
                onConfirmReturn = { selectedItems ->
                    viewModel.processReturn(selectedTxForReturn!!, selectedItems)
                    selectedTxForReturn = null
                    txItemsToReturn = emptyList()
                }
            )
        }
    }
}

@Composable
fun TransactionCard(
    transaction: TransactionEntity,
    viewModel: PosViewModel,
    onInitiateReturn: (TransactionEntity, List<TransactionItemEntity>) -> Unit
) {
    var items by remember { mutableStateOf<List<TransactionItemEntity>>(emptyList()) }
    var isExpanded by remember { mutableStateOf(false) }

    val dateFormat = SimpleDateFormat("dd.MM.yy HH:mm", Locale.getDefault())
    val isSale = transaction.type == "SALE"

    LaunchedEffect(transaction.id) {
        items = viewModel.getTransactionItems(transaction.id)
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
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isSale) PosSuccess.copy(alpha = 0.15f) else MaterialTheme.colorScheme.errorContainer
                    ) {
                        Text(
                            text = if (isSale) "ПРОДАЖА №${transaction.id}" else "ВОЗВРАТ №${transaction.id}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isSale) PosSuccess else MaterialTheme.colorScheme.error,
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

            // Items preview
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items.take(if (isExpanded) items.size else 2).forEach { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${item.productName} × ${item.quantity.toInt()} ${item.unit}",
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${(item.quantity * item.unitPrice).toInt()} ₽",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
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
                    Text(
                        text = "${if (isSale) "" else "-"}${transaction.totalAmount.toInt()} ₽",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isSale) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                }
            }

            // Action: Return for Sale
            if (isSale) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { onInitiateReturn(transaction, items) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .testTag("return_button_${transaction.id}"),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.AssignmentReturn, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Оформить возврат", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun ProcessReturnDialog(
    transaction: TransactionEntity,
    items: List<TransactionItemEntity>,
    onDismiss: () -> Unit,
    onConfirmReturn: (List<TransactionItemEntity>) -> Unit
) {
    val selectedMap = remember {
        mutableStateMapOf<Long, Boolean>().apply {
            items.forEach { this[it.id] = true } // default select all
        }
    }

    val selectedItems = items.filter { selectedMap[it.id] == true }
    val refundTotal = selectedItems.sumOf { it.quantity * it.unitPrice }

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
                    text = "Выберите товары для возврата на склад:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(modifier = Modifier.height(180.dp)) {
                    items(items) { item ->
                        val isChecked = selectedMap[item.id] ?: false
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { selectedMap[item.id] = it }
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    item.productName,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    "${item.quantity.toInt()} ${item.unit} × ${item.unitPrice.toInt()} ₽",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "${(item.quantity * item.unitPrice).toInt()} ₽",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
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
                onClick = { onConfirmReturn(selectedItems) },
                enabled = selectedItems.isNotEmpty(),
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
