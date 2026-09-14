package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.PosViewModel
import com.example.ui.components.CashShiftDialog
import com.example.ui.components.CheckoutBottomSheet
import com.example.ui.components.ElectronicInvoiceDialog
import com.example.ui.components.PosTopBar
import com.example.ui.components.ProductEditDialog
import com.example.ui.components.ProductQrDialog
import com.example.ui.components.QrScannerDialog
import com.example.ui.components.ThermalReceiptDialog
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.InventoryScreen
import com.example.ui.screens.RegisterScreen
import com.example.ui.screens.SalesHistoryScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MyApplicationTheme

sealed class PosDestination(val route: String, val label: String, val icon: ImageVector) {
    object Register : PosDestination("register", "Lector QR", Icons.Default.QrCodeScanner)
    object Inventory : PosDestination("inventory", "Inventario", Icons.Default.Inventory2)
    object Sales : PosDestination("sales", "Ventas", Icons.Default.ReceiptLong)
    object Analytics : PosDestination("analytics", "Analíticas", Icons.Default.Analytics)
    object Settings : PosDestination("settings", "Ajustes", Icons.Default.Settings)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                PosApp()
            }
        }
    }
}

@Composable
fun PosApp(viewModel: PosViewModel = viewModel()) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: PosDestination.Register.route

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.toastMessage.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    val settings by viewModel.settings.collectAsState()
    val activeShift by viewModel.activeShift.collectAsState()
    val unsyncedCount by viewModel.unsyncedCount.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()

    // Dialog state collectors
    val showScanner by viewModel.showScannerDialog.collectAsState()
    val showCheckout by viewModel.showCheckoutSheet.collectAsState()
    val showShiftDialog by viewModel.showShiftDialog.collectAsState()
    val showProductDialog by viewModel.showProductDialog.collectAsState()
    val editingProduct by viewModel.editingProduct.collectAsState()
    val productForQr by viewModel.productForQr.collectAsState()
    val showReceiptDialog by viewModel.showReceiptDialog.collectAsState()
    val showInvoiceDialog by viewModel.showInvoiceDialog.collectAsState()
    val currentViewingSale by viewModel.currentViewingSale.collectAsState()
    val currentViewingItems by viewModel.currentViewingItems.collectAsState()
    val isPrinting by viewModel.isPrinting.collectAsState()

    val cartItems by viewModel.cartItems.collectAsState()
    val cartSubtotal by viewModel.cartSubtotal.collectAsState()
    val cartTaxTotal by viewModel.cartTaxTotal.collectAsState()
    val cartTotal by viewModel.cartTotal.collectAsState()
    val allProducts by viewModel.allProducts.collectAsState()

    val destinations = listOf(
        PosDestination.Register,
        PosDestination.Inventory,
        PosDestination.Sales,
        PosDestination.Settings
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            PosTopBar(
                settings = settings,
                activeShift = activeShift,
                unsyncedCount = unsyncedCount,
                isSyncing = isSyncing,
                onToggleOffline = { viewModel.toggleOfflineSimulation() },
                onTriggerSync = { viewModel.syncWithCloud() },
                onOpenShift = { viewModel.openShiftDialog() },
                onOpenSettings = { navController.navigate(PosDestination.Settings.route) }
            )
        },
        bottomBar = {
            Column(modifier = Modifier.fillMaxWidth().navigationBarsPadding()) {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    thickness = 0.5.dp
                )
                // 3 Segmented Bottom Bar matching wireframe sketch
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pos_bottom_navigation")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        destinations.forEachIndexed { index, dest ->
                            val isSelected = currentRoute == dest.route
                            if (index > 0) {
                                VerticalDivider(
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                    modifier = Modifier
                                        .height(40.dp)
                                        .padding(vertical = 4.dp),
                                    thickness = 1.dp
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clickable {
                                        if (currentRoute != dest.route) {
                                            navController.navigate(dest.route) {
                                                popUpTo(navController.graph.findStartDestination().id) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    }
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                        else Color.Transparent
                                    )
                                    .testTag("nav_tab_${dest.route}"),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = dest.icon,
                                        contentDescription = dest.label,
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = dest.label,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = PosDestination.Register.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            composable(PosDestination.Register.route) {
                RegisterScreen(
                    viewModel = viewModel,
                    onNavigateToSettings = { navController.navigate(PosDestination.Settings.route) }
                )
            }
            composable(PosDestination.Inventory.route) {
                InventoryScreen(viewModel = viewModel)
            }
            composable(PosDestination.Sales.route) {
                SalesHistoryScreen(viewModel = viewModel)
            }
            composable(PosDestination.Analytics.route) {
                AnalyticsScreen(viewModel = viewModel)
            }
            composable(PosDestination.Settings.route) {
                SettingsScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }

        // Global Modals & Dialogs
        if (showScanner) {
            QrScannerDialog(
                products = allProducts,
                onBarcodeScanned = { barcode ->
                    viewModel.onBarcodeScanned(barcode)
                },
                onDismiss = { viewModel.closeScanner() }
            )
        }

        if (showCheckout) {
            CheckoutBottomSheet(
                cartItems = cartItems,
                subtotal = cartSubtotal,
                taxTotal = cartTaxTotal,
                total = cartTotal,
                settings = settings,
                onCompleteSale = { name, taxId, email, isFiscal, payType, amountPaid ->
                    viewModel.completeSale(
                        customerName = name,
                        customerTaxId = taxId,
                        customerEmail = email,
                        isElectronicInvoice = isFiscal,
                        paymentType = payType,
                        amountPaid = amountPaid
                    )
                },
                onDismiss = { viewModel.closeCheckout() }
            )
        }

        if (showReceiptDialog && currentViewingSale != null) {
            ThermalReceiptDialog(
                sale = currentViewingSale,
                items = currentViewingItems,
                settings = settings,
                isPrinting = isPrinting,
                onPrintTicket = { viewModel.printThermalTicket() },
                onDismiss = { viewModel.closeReceiptDialog() }
            )
        }

        if (showInvoiceDialog && currentViewingSale != null) {
            ElectronicInvoiceDialog(
                sale = currentViewingSale,
                items = currentViewingItems,
                settings = settings,
                onDismiss = { viewModel.closeInvoiceDialog() }
            )
        }

        if (showShiftDialog) {
            CashShiftDialog(
                activeShift = activeShift,
                settings = settings,
                onOpenShift = { cashier, initialCash ->
                    viewModel.openNewShift(cashier, initialCash)
                },
                onCloseShift = { actualCash ->
                    viewModel.closeShift(actualCash)
                },
                onDismiss = { viewModel.closeShiftDialog() }
            )
        }

        if (showProductDialog) {
            ProductEditDialog(
                product = editingProduct,
                settings = settings,
                onSave = { id, barcode, name, category, cost, price, stock, minStock, taxRate, imageUrl, qrCode ->
                    viewModel.saveProduct(id, barcode, name, category, cost, price, stock, minStock, taxRate, imageUrl, qrCode)
                },
                onDelete = if (editingProduct != null) {
                    { prod -> viewModel.deleteProduct(prod) }
                } else null,
                onDismiss = { viewModel.closeProductDialog() }
            )
        }

        if (productForQr != null) {
            ProductQrDialog(
                product = productForQr!!,
                settings = settings,
                onDismiss = { viewModel.closeProductQr() }
            )
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "Hello $name!", modifier = modifier)
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MyApplicationTheme { Greeting("Android") }
}
