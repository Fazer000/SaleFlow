package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.PosViewModel
import com.example.ui.screens.ProductsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.ShiftReportsScreen
import com.example.ui.screens.TerminalScreen
import com.example.ui.screens.TransactionsScreen
import com.example.ui.theme.PosTerminalTheme

enum class PosTab(val title: String, val icon: ImageVector) {
    TERMINAL("Касса", Icons.Default.PointOfSale),
    PRODUCTS("Товары", Icons.Default.Inventory),
    TRANSACTIONS("Чеки", Icons.Default.ReceiptLong),
    REPORTS("Отчеты", Icons.Default.Assessment),
    SETTINGS("Настройки", Icons.Default.Settings)
}

class MainActivity : ComponentActivity() {

    private val viewModel: PosViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            PosTerminalTheme(darkTheme = false) { // Enforce Light M3 theme requested by user
                MainPosApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainPosApp(viewModel: PosViewModel) {
    var currentTab by remember { mutableStateOf(PosTab.TERMINAL) }
    val snackbarHostState = remember { SnackbarHostState() }

    // State bindings
    val products by viewModel.filteredProducts.collectAsStateWithLifecycle()
    val allProducts by viewModel.allProducts.collectAsStateWithLifecycle()
    val categories by viewModel.availableCategories.collectAsStateWithLifecycle()
    val cart by viewModel.cart.collectAsStateWithLifecycle()
    val cartDiscount by viewModel.cartDiscount.collectAsStateWithLifecycle()
    val currentShift by viewModel.currentShift.collectAsStateWithLifecycle()
    val allShifts by viewModel.allShifts.collectAsStateWithLifecycle()
    val shiftReport by viewModel.currentShiftReport.collectAsStateWithLifecycle()
    val transactions by viewModel.allTransactions.collectAsStateWithLifecycle()
    val supplies by viewModel.allSupplies.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val lastReceipt by viewModel.lastSaleReceipt.collectAsStateWithLifecycle()
    val lastReceiptItems by viewModel.lastSaleItems.collectAsStateWithLifecycle()
    val updateState by viewModel.updateState.collectAsStateWithLifecycle()
    val repoSlug by viewModel.githubRepoSlug.collectAsStateWithLifecycle()
    val messageEvent by viewModel.messageEvent.collectAsStateWithLifecycle()

    // Handle snackbar notifications
    LaunchedEffect(messageEvent) {
        val msg = messageEvent
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .testTag("pos_navigation_bar")
            ) {
                PosTab.values().forEach { tab ->
                    NavigationBarItem(
                        selected = currentTab == tab,
                        onClick = { currentTab = tab },
                        icon = {
                            if (tab == PosTab.TERMINAL && cart.isNotEmpty()) {
                                BadgedBox(
                                    badge = { Badge { Text("${cart.sumOf { it.quantity.toInt() }}") } }
                                ) {
                                    Icon(tab.icon, contentDescription = tab.title)
                                }
                            } else {
                                Icon(tab.icon, contentDescription = tab.title)
                            }
                        },
                        label = { Text(tab.title) },
                        modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                PosTab.TERMINAL -> {
                    TerminalScreen(
                        viewModel = viewModel,
                        products = products,
                        categories = categories,
                        cart = cart,
                        cartDiscount = cartDiscount,
                        currentShift = currentShift,
                        lastReceipt = lastReceipt,
                        lastReceiptItems = lastReceiptItems,
                        searchQuery = searchQuery,
                        selectedCategory = selectedCategory
                    )
                }

                PosTab.PRODUCTS -> {
                    ProductsScreen(
                        viewModel = viewModel,
                        products = allProducts,
                        supplies = supplies,
                        searchQuery = searchQuery
                    )
                }

                PosTab.TRANSACTIONS -> {
                    TransactionsScreen(
                        viewModel = viewModel,
                        transactions = transactions
                    )
                }

                PosTab.REPORTS -> {
                    ShiftReportsScreen(
                        viewModel = viewModel,
                        currentShift = currentShift,
                        shiftReport = shiftReport,
                        allShifts = allShifts
                    )
                }

                PosTab.SETTINGS -> {
                    SettingsScreen(
                        viewModel = viewModel,
                        updateState = updateState,
                        repoSlug = repoSlug
                    )
                }
            }
        }
    }
}
