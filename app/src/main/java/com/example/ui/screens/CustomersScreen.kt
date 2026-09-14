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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
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
fun CustomersScreen(
    viewModel: PosViewModel
) {
    val customerStats by viewModel.customerStats.collectAsState()
    val allTransactions by viewModel.allTransactions.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }

    var nameInput by remember { mutableStateOf("") }
    var phoneInput by remember { mutableStateOf("") }
    var noteInput by remember { mutableStateOf("") }

    var selectedCustomerForDetails by remember { mutableStateOf<PosViewModel.CustomerWithStats?>(null) }
    var customerToDelete by remember { mutableStateOf<Long?>(null) }

    val filteredCustomers = remember(customerStats, searchQuery) {
        if (searchQuery.isBlank()) {
            customerStats
        } else {
            customerStats.filter {
                it.customer.name.contains(searchQuery, ignoreCase = true) ||
                        it.customer.phone.contains(searchQuery, ignoreCase = true) ||
                        it.customer.note.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    val totalDebtAll = remember(customerStats) {
        customerStats.sumOf { it.totalDebt }
    }
    val totalPurchasedAll = remember(customerStats) {
        customerStats.sumOf { it.totalPurchased }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header stats & search row
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                // Summary Cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Всего клиентов", fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                            Text(
                                "${customerStats.size}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = if (totalDebtAll > 0) MaterialTheme.colorScheme.errorContainer else PosSuccess.copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Общий долг", fontSize = 11.sp, color = if (totalDebtAll > 0) MaterialTheme.colorScheme.onErrorContainer else PosSuccess)
                            Text(
                                "${totalDebtAll.toInt()} ₽",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (totalDebtAll > 0) MaterialTheme.colorScheme.error else PosSuccess
                            )
                        }
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Выручка", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                "${totalPurchasedAll.toInt()} ₽",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Search + Add Button Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CustomSearchInput(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholderText = "Поиск покупателя...",
                        modifier = Modifier
                            .weight(1f)
                            .testTag("customer_search_input")
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    Button(
                        onClick = {
                            nameInput = ""
                            phoneInput = ""
                            noteInput = ""
                            showAddDialog = true
                        },
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("add_customer_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Клиент", fontSize = 13.sp)
                    }
                }
            }

            // Seamless Edge-to-Edge List
            if (filteredCustomers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (searchQuery.isNotEmpty()) "Клиенты не найдены" else "Список клиентов пуст",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 6.dp, bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredCustomers, key = { it.customer.id }) { item ->
                            CustomerCard(
                                item = item,
                                onSelect = { selectedCustomerForDetails = item },
                                onDelete = { customerToDelete = item.customer.id }
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
    }

    // Add Customer Dialog
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(vertical = 12.dp),
            title = { Text("Новый покупатель") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("ФИО / Имя *") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("customer_name_field")
                    )
                    OutlinedTextField(
                        value = phoneInput,
                        onValueChange = { phoneInput = it },
                        label = { Text("Телефон") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = noteInput,
                        onValueChange = { noteInput = it },
                        label = { Text("Заметка (адрес, скидка и т.д.)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.addCustomer(nameInput, phoneInput, noteInput)
                        showAddDialog = false
                    },
                    modifier = Modifier.testTag("save_customer_button")
                ) {
                    Text("Сохранить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    // Delete Confirmation Dialog
    customerToDelete?.let { id ->
        AlertDialog(
            onDismissRequest = { customerToDelete = null },
            title = { Text("Удалить покупателя?") },
            text = { Text("Вы уверены, что хотите удалить этого клиента из базы?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteCustomer(id)
                        customerToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Удалить")
                }
            },
            dismissButton = {
                TextButton(onClick = { customerToDelete = null }) {
                    Text("Отмена")
                }
            }
        )
    }

    // Customer History & Debt Details Modal
    selectedCustomerForDetails?.let { item ->
        val customerTxs = remember(allTransactions, item.customer.id) {
            allTransactions.filter { it.customerId == item.customer.id }
        }
        val dateFormat = remember { SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()) }

        Dialog(
            onDismissRequest = { selectedCustomerForDetails = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                shadowElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .padding(vertical = 16.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Header (No close button)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(item.customer.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            if (item.customer.phone.isNotEmpty()) {
                                Text(item.customer.phone, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                            }
                        }
                    }

                    // Summary Banner
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = if (item.totalDebt > 0) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f) else PosSuccess.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Всего покупок", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${item.totalPurchased.toInt()} ₽", fontWeight = FontWeight.Bold)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Задолженность", fontSize = 11.sp, color = if (item.totalDebt > 0) MaterialTheme.colorScheme.error else PosSuccess)
                                Text(
                                    "${item.totalDebt.toInt()} ₽",
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (item.totalDebt > 0) MaterialTheme.colorScheme.error else PosSuccess
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        "История покупок (${customerTxs.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    if (customerTxs.isEmpty()) {
                        Text(
                            "У покупателя пока нет чеков",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                        )
                    } else {
                        // Scrollable List going to the very bottom without bottom padding & with Top Shadow
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 280.dp)
                        ) {
                            LazyColumn(
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 0.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(customerTxs, key = { it.id }) { tx ->
                                    CustomerTransactionCard(
                                        tx = tx,
                                        viewModel = viewModel,
                                        dateFormat = dateFormat
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
            }
        }
    }
}

@Composable
fun CustomerCard(
    item: PosViewModel.CustomerWithStats,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    val hasDebt = item.totalDebt > 0

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (hasDebt) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            tint = if (hasDebt) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        item.customer.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (item.customer.phone.isNotEmpty()) {
                            Text(
                                item.customer.phone,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text(
                            "Покупок: ${item.transactionCount} (${item.totalPurchased.toInt()} ₽)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline,
                            fontSize = 11.sp
                        )
                    }
                    if (item.customer.note.isNotEmpty()) {
                        Text(
                            item.customer.note,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline,
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (hasDebt) MaterialTheme.colorScheme.errorContainer else PosSuccess.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = if (hasDebt) "Долг: ${item.totalDebt.toInt()} ₽" else "Долгов нет",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (hasDebt) MaterialTheme.colorScheme.error else PosSuccess,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }

                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Удалить",
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun CustomerTransactionCard(
    tx: TransactionEntity,
    viewModel: PosViewModel,
    dateFormat: SimpleDateFormat
) {
    var isExpanded by remember { mutableStateOf(false) }
    var items by remember { mutableStateOf<List<TransactionItemEntity>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }

    LaunchedEffect(isExpanded) {
        if (isExpanded && items.isEmpty()) {
            isLoading = true
            items = viewModel.getTransactionItems(tx.id)
            isLoading = false
        }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Чек №${tx.id}  •  ${if (tx.type == "SALE") "Продажа" else "Возврат"}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (isExpanded) "Свернуть" else "Развернуть",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        "${dateFormat.format(Date(tx.timestamp))}  (${tx.paymentMethod})",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        "${tx.totalAmount.toInt()} ₽",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 13.sp
                    )
                }

                // Switch Оплачено
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (tx.isPaid) "Оплачен" else "В долг",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (tx.isPaid) PosSuccess else MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    Switch(
                        checked = tx.isPaid,
                        onCheckedChange = { isChecked ->
                            viewModel.toggleTransactionPaidStatus(tx.id, isChecked)
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

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(6.dp))

                    if (isLoading) {
                        Text(
                            "Загрузка товаров...",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    } else if (items.isEmpty()) {
                        Text(
                            "Список товаров пуст",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    } else {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(bottom = 4.dp)
                        ) {
                            items.forEach { item ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        item.productName,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.weight(1f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    val qtyStr = if (item.quantity % 1.0 == 0.0) item.quantity.toInt().toString() else item.quantity.toString()
                                    val itemTotal = (item.quantity * item.unitPrice).toInt()
                                    Text(
                                        "$qtyStr ${item.unit} x ${item.unitPrice.toInt()} ₽ = $itemTotal ₽",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
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
