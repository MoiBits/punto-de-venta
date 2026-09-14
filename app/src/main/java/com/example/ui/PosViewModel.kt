package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.PosDatabase
import com.example.data.model.AppSettingsEntity
import com.example.data.model.CartItem
import com.example.data.model.CashShiftEntity
import com.example.data.model.LatAmCountryConfig
import com.example.data.model.LatAmPresets
import com.example.data.model.PaymentType
import com.example.data.model.ProductEntity
import com.example.data.model.SaleEntity
import com.example.data.model.SaleItemEntity
import com.example.data.repository.PosRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

class PosViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: PosRepository
    init {
        val database = PosDatabase.getDatabase(application, viewModelScope)
        repository = PosRepository(database.posDao())

        // Ensure default settings are updated to Bolivianos (BOB / Bs. / 13%) if still on initial USD placeholder
        viewModelScope.launch {
            repository.settings.collect { currentSettings ->
                if (currentSettings != null &&
                    (currentSettings.currencyCode == "USD" && (currentSettings.businessTaxId == "CRE920815AA1" || currentSettings.businessTaxId == "TSR890312XYZ"))
                ) {
                    val bolivia = LatAmPresets.BOLIVIA
                    repository.updateSettings(
                        currentSettings.copy(
                            businessName = bolivia.defaultBusinessName,
                            businessTaxId = bolivia.sampleTaxId,
                            businessAddress = bolivia.defaultAddress,
                            businessPhone = bolivia.defaultPhone,
                            businessEmail = "contacto@comerciobolivia.bo",
                            currencySymbol = bolivia.currencySymbol,
                            currencyCode = bolivia.currencyCode,
                            defaultTaxRate = bolivia.defaultTaxRate
                        )
                    )
                }
            }
        }
    }

    // --- Products & Filtering ---
    val allProducts: StateFlow<List<ProductEntity>> = repository.allProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("Todos")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    val filteredProducts: StateFlow<List<ProductEntity>> = combine(
        allProducts,
        searchQuery,
        selectedCategory
    ) { products, query, category ->
        products.filter { product ->
            val matchesQuery = query.isBlank() ||
                    product.name.contains(query, ignoreCase = true) ||
                    product.barcode.contains(query, ignoreCase = true)
            val matchesCategory = category == "Todos" || product.category.equals(category, ignoreCase = true)
            matchesQuery && matchesCategory
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<String>> = allProducts.combine(_searchQuery) { products, _ ->
        val list = mutableListOf("Todos")
        list.addAll(products.map { it.category }.distinct().sorted())
        list
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf("Todos"))

    // --- Cart ---
    private val _cartItems = MutableStateFlow<List<CartItem>>(emptyList())
    val cartItems: StateFlow<List<CartItem>> = _cartItems.asStateFlow()

    val cartSubtotal: StateFlow<Double> = _cartItems.combine(allProducts) { items, _ ->
        items.sumOf { it.subtotal }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val cartTaxTotal: StateFlow<Double> = _cartItems.combine(allProducts) { items, _ ->
        items.sumOf { it.taxAmount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val cartTotal: StateFlow<Double> = _cartItems.combine(allProducts) { items, _ ->
        items.sumOf { it.total }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val cartItemCount: StateFlow<Int> = _cartItems.combine(allProducts) { items, _ ->
        items.sumOf { it.quantity }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // --- Settings & Shifts ---
    val settings: StateFlow<AppSettingsEntity> = repository.settings
        .combine(_cartItems) { s, _ -> s ?: AppSettingsEntity() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettingsEntity())

    val activeShift: StateFlow<CashShiftEntity?> = repository.activeShift
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val allSales: StateFlow<List<SaleEntity>> = repository.allSales
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSaleItems: StateFlow<List<SaleItemEntity>> = repository.allSaleItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Sync & Cloud State ---
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _unsyncedCount = MutableStateFlow(0)
    val unsyncedCount: StateFlow<Int> = _unsyncedCount.asStateFlow()

    private val _toastMessage = MutableSharedFlow<String>()
    val toastMessage = _toastMessage.asSharedFlow()

    // --- Dialogs & Active Actions ---
    private val _showCheckoutSheet = MutableStateFlow(false)
    val showCheckoutSheet: StateFlow<Boolean> = _showCheckoutSheet.asStateFlow()

    private val _showScannerDialog = MutableStateFlow(false)
    val showScannerDialog: StateFlow<Boolean> = _showScannerDialog.asStateFlow()

    private val _showReceiptDialog = MutableStateFlow(false)
    val showReceiptDialog: StateFlow<Boolean> = _showReceiptDialog.asStateFlow()

    private val _showInvoiceDialog = MutableStateFlow(false)
    val showInvoiceDialog: StateFlow<Boolean> = _showInvoiceDialog.asStateFlow()

    private val _showShiftDialog = MutableStateFlow(false)
    val showShiftDialog: StateFlow<Boolean> = _showShiftDialog.asStateFlow()

    private val _editingProduct = MutableStateFlow<ProductEntity?>(null)
    val editingProduct: StateFlow<ProductEntity?> = _editingProduct.asStateFlow()
    val showProductDialog = MutableStateFlow(false)

    // QR Code Dialog for product
    private val _productForQr = MutableStateFlow<ProductEntity?>(null)
    val productForQr: StateFlow<ProductEntity?> = _productForQr.asStateFlow()

    // Completed Sale Cache for Receipt/Invoice
    private val _currentViewingSale = MutableStateFlow<SaleEntity?>(null)
    val currentViewingSale: StateFlow<SaleEntity?> = _currentViewingSale.asStateFlow()

    private val _currentViewingItems = MutableStateFlow<List<SaleItemEntity>>(emptyList())
    val currentViewingItems: StateFlow<List<SaleItemEntity>> = _currentViewingItems.asStateFlow()

    // Printing simulation state
    private val _isPrinting = MutableStateFlow(false)
    val isPrinting: StateFlow<Boolean> = _isPrinting.asStateFlow()

    init {
        refreshUnsyncedCount()
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedCategory(category: String) {
        _selectedCategory.value = category
    }

    // --- Cart Actions ---
    fun addToCart(product: ProductEntity) {
        if (product.stock <= 0) {
            viewModelScope.launch {
                _toastMessage.emit("¡Agotado! No hay stock disponible de ${product.name}")
            }
            return
        }
        val current = _cartItems.value.toMutableList()
        val index = current.indexOfFirst { it.product.id == product.id }
        if (index >= 0) {
            val item = current[index]
            if (item.quantity + 1 > product.stock) {
                viewModelScope.launch {
                    _toastMessage.emit("Stock máximo alcanzado (${product.stock}) para ${product.name}")
                }
                return
            }
            current[index] = item.copy(quantity = item.quantity + 1)
        } else {
            current.add(CartItem(product = product, quantity = 1))
        }
        _cartItems.value = current
    }

    fun updateCartItemQuantity(productId: Long, delta: Int) {
        val current = _cartItems.value.toMutableList()
        val index = current.indexOfFirst { it.product.id == productId }
        if (index >= 0) {
            val item = current[index]
            val newQty = item.quantity + delta
            if (newQty <= 0) {
                current.removeAt(index)
            } else if (newQty > item.product.stock) {
                viewModelScope.launch {
                    _toastMessage.emit("Stock insuficiente. Disponible: ${item.product.stock}")
                }
            } else {
                current[index] = item.copy(quantity = newQty)
            }
            _cartItems.value = current
        }
    }

    fun removeFromCart(productId: Long) {
        _cartItems.value = _cartItems.value.filter { it.product.id != productId }
    }

    fun clearCart() {
        _cartItems.value = emptyList()
    }

    // --- Barcode / QR Scan Handler ---
    fun onBarcodeScanned(barcode: String) {
        viewModelScope.launch {
            val product = repository.getProductByBarcode(barcode.trim())
            if (product != null) {
                addToCart(product)
                _toastMessage.emit("Producto agregado: ${product.name}")
                _showScannerDialog.value = false
            } else {
                _toastMessage.emit("No se encontró producto con código: $barcode")
            }
        }
    }

    // --- Modals Controls ---
    fun openCheckout() {
        if (_cartItems.value.isEmpty()) {
            viewModelScope.launch {
                _toastMessage.emit("El carrito está vacío")
            }
            return
        }
        _showCheckoutSheet.value = true
    }

    fun closeCheckout() {
        _showCheckoutSheet.value = false
    }

    fun openScanner() {
        _showScannerDialog.value = true
    }

    fun closeScanner() {
        _showScannerDialog.value = false
    }

    fun openShiftDialog() {
        _showShiftDialog.value = true
    }

    fun closeShiftDialog() {
        _showShiftDialog.value = false
    }

    fun openNewProductDialog() {
        _editingProduct.value = null
        showProductDialog.value = true
    }

    fun openEditProductDialog(product: ProductEntity) {
        _editingProduct.value = product
        showProductDialog.value = true
    }

    fun closeProductDialog() {
        showProductDialog.value = false
        _editingProduct.value = null
    }

    // --- Complete Checkout ---
    fun completeSale(
        customerName: String,
        customerTaxId: String,
        customerEmail: String,
        isElectronicInvoice: Boolean,
        paymentType: PaymentType,
        amountPaid: Double
    ) {
        val items = _cartItems.value
        if (items.isEmpty()) return

        viewModelScope.launch {
            try {
                val cashier = activeShift.value?.cashierName ?: "Cajero Principal"
                val sale = repository.checkout(
                    cartItems = items,
                    customerName = customerName,
                    customerTaxId = customerTaxId,
                    customerEmail = customerEmail,
                    isElectronicInvoice = isElectronicInvoice,
                    paymentType = paymentType,
                    amountPaid = amountPaid,
                    cashierName = cashier
                )

                // Load items for receipt
                val saleItems = repository.getSaleItems(sale.id)
                _currentViewingSale.value = sale
                _currentViewingItems.value = saleItems

                // Clear cart & close checkout
                _cartItems.value = emptyList()
                _showCheckoutSheet.value = false

                // If electronic invoice requested, open invoice dialog, else thermal receipt
                if (isElectronicInvoice) {
                    _showInvoiceDialog.value = true
                } else {
                    _showReceiptDialog.value = true
                }

                refreshUnsyncedCount()
                _toastMessage.emit("¡Venta completada con éxito! Folio: ${sale.folio}")
            } catch (e: Exception) {
                _toastMessage.emit("Error al procesar venta: ${e.message}")
            }
        }
    }

    fun viewReceiptForSale(sale: SaleEntity) {
        viewModelScope.launch {
            val items = repository.getSaleItems(sale.id)
            _currentViewingSale.value = sale
            _currentViewingItems.value = items
            if (sale.isElectronicInvoice) {
                _showInvoiceDialog.value = true
            } else {
                _showReceiptDialog.value = true
            }
        }
    }

    fun closeReceiptDialog() {
        _showReceiptDialog.value = false
    }

    fun closeInvoiceDialog() {
        _showInvoiceDialog.value = false
    }

    // --- Print Simulation ---
    fun printThermalTicket() {
        viewModelScope.launch {
            _isPrinting.value = true
            delay(1200) // Simulates ESC/POS thermal printer head action
            _isPrinting.value = false
            _toastMessage.emit("Ticket impreso correctamente en impresora térmica")
        }
    }

    // --- Inventory Management ---
    fun openProductQr(product: ProductEntity) {
        _productForQr.value = product
    }

    fun closeProductQr() {
        _productForQr.value = null
    }

    fun saveProduct(
        id: Long,
        barcode: String,
        name: String,
        category: String,
        costPrice: Double,
        salePrice: Double,
        stock: Int,
        minStock: Int,
        taxRate: Double,
        imageUrl: String? = null,
        qrCode: String? = null
    ) {
        viewModelScope.launch {
            val product = ProductEntity(
                id = id,
                barcode = barcode.trim(),
                name = name.trim(),
                category = category.trim(),
                costPrice = costPrice,
                salePrice = salePrice,
                stock = stock,
                minStock = minStock,
                taxRate = taxRate,
                imageUrl = imageUrl?.ifBlank { null },
                qrCode = qrCode?.ifBlank { null }
            )
            repository.saveProduct(product)
            closeProductDialog()
            _toastMessage.emit("Producto guardado correctamente")
        }
    }

    fun adjustStock(product: ProductEntity, newStock: Int) {
        viewModelScope.launch {
            repository.updateStock(product.id, newStock)
            _toastMessage.emit("Stock actualizado a $newStock para ${product.name}")
        }
    }

    fun deleteProduct(product: ProductEntity) {
        viewModelScope.launch {
            repository.deleteProduct(product)
            _toastMessage.emit("Producto eliminado")
        }
    }

    // --- Cash Shift Management ---
    fun openNewShift(cashierName: String, initialCash: Double) {
        viewModelScope.launch {
            repository.openShift(cashierName, initialCash)
            closeShiftDialog()
            _toastMessage.emit("Turno de caja abierto correctamente con $initialCash")
        }
    }

    fun closeShift(actualCash: Double) {
        val shift = activeShift.value ?: return
        viewModelScope.launch {
            repository.closeShift(shift.id, actualCash)
            closeShiftDialog()
            _toastMessage.emit("Turno cerrado con éxito. Corte de caja Z registrado.")
        }
    }

    // --- Cloud Sync & Offline Simulation ---
    fun toggleOfflineSimulation() {
        viewModelScope.launch {
            val current = settings.value
            val newOfflineState = !current.isOfflineSimulated
            repository.updateSettings(current.copy(isOfflineSimulated = newOfflineState))
            if (newOfflineState) {
                _toastMessage.emit("Modo Offline Activado: Las ventas se guardarán localmente")
            } else {
                _toastMessage.emit("Modo Online Activado: Iniciando sincronización...")
                syncWithCloud()
            }
        }
    }

    fun syncWithCloud() {
        viewModelScope.launch {
            _isSyncing.value = true
            delay(1500) // Realistic cloud synchronization latency
            val synced = repository.syncPendingSales()
            _isSyncing.value = false
            refreshUnsyncedCount()
            _toastMessage.emit(
                if (synced > 0) "¡$synced ventas sincronizadas con la nube con éxito!"
                else "La base de datos está al día con la nube"
            )
        }
    }

    private fun refreshUnsyncedCount() {
        viewModelScope.launch {
            _unsyncedCount.value = repository.getUnsyncedCount()
        }
    }

    // --- Settings & Multi-currency ---
    fun updateBusinessSettings(
        businessName: String,
        taxId: String,
        address: String,
        phone: String,
        currencySymbol: String,
        currencyCode: String,
        taxRate: Double,
        paperWidth: Int
    ) {
        viewModelScope.launch {
            val updated = settings.value.copy(
                businessName = businessName,
                businessTaxId = taxId,
                businessAddress = address,
                businessPhone = phone,
                currencySymbol = currencySymbol,
                currencyCode = currencyCode,
                defaultTaxRate = taxRate,
                thermalPaperWidthMm = paperWidth
            )
            repository.updateSettings(updated)
            _toastMessage.emit("Configuración y moneda actualizadas")
        }
    }

    fun applyLatAmPreset(config: LatAmCountryConfig) {
        viewModelScope.launch {
            val current = settings.value
            val isOldPlaceholder = current.businessTaxId.isBlank() ||
                    current.businessTaxId == "CRE920815AA1" ||
                    current.businessTaxId == "TSR890312XYZ" ||
                    current.businessTaxId == "XAXX010101000" ||
                    current.businessTaxId == "1023456789"

            val isOldBusiness = current.businessName.isBlank() ||
                    current.businessName.contains("TechStore", ignoreCase = true) ||
                    current.businessName == "Comercial & Retail Express"

            val updated = current.copy(
                currencySymbol = config.currencySymbol,
                currencyCode = config.currencyCode,
                defaultTaxRate = config.defaultTaxRate,
                businessTaxId = if (isOldPlaceholder) config.sampleTaxId else current.businessTaxId,
                businessName = if (isOldBusiness) config.defaultBusinessName else current.businessName,
                businessAddress = if (current.businessAddress.contains("Reforma") || current.businessAddress.contains("Comercial #450")) config.defaultAddress else current.businessAddress,
                businessPhone = if (current.businessPhone.contains("(800)") || current.businessPhone.contains("(555)")) config.defaultPhone else current.businessPhone
            )
            repository.updateSettings(updated)
            _toastMessage.emit("Configuración aplicada: ${config.flag} ${config.countryName} (${config.currencySymbol} - ${config.currencyCode})")
        }
    }

    // Helper currency formatting
    fun formatCurrency(amount: Double): String {
        val symbol = settings.value.currencySymbol
        val code = settings.value.currencyCode
        return String.format(Locale.getDefault(), "%s %.2f %s", symbol, amount, code)
    }
}
