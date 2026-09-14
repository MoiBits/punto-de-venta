package com.example.data.repository

import com.example.data.local.PosDao
import com.example.data.model.AppSettingsEntity
import com.example.data.model.CartItem
import com.example.data.model.CashShiftEntity
import com.example.data.model.PaymentType
import com.example.data.model.ProductEntity
import com.example.data.model.SaleEntity
import com.example.data.model.SaleItemEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class PosRepository(private val posDao: PosDao) {

    // --- Products ---
    val allProducts: Flow<List<ProductEntity>> = posDao.getAllProducts()

    suspend fun getProductByBarcode(barcode: String): ProductEntity? = withContext(Dispatchers.IO) {
        posDao.getProductByBarcode(barcode)
    }

    suspend fun getProductById(id: Long): ProductEntity? = withContext(Dispatchers.IO) {
        posDao.getProductById(id)
    }

    suspend fun saveProduct(product: ProductEntity): Long = withContext(Dispatchers.IO) {
        if (product.id == 0L) {
            posDao.insertProduct(product)
        } else {
            posDao.updateProduct(product)
            product.id
        }
    }

    suspend fun deleteProduct(product: ProductEntity) = withContext(Dispatchers.IO) {
        posDao.deleteProduct(product)
    }

    suspend fun updateStock(productId: Long, newStock: Int) = withContext(Dispatchers.IO) {
        posDao.updateStock(productId, newStock)
    }

    // --- Sales ---
    val allSales: Flow<List<SaleEntity>> = posDao.getAllSales()
    val allSaleItems: Flow<List<SaleItemEntity>> = posDao.getAllSaleItems()

    suspend fun getSaleById(saleId: Long): SaleEntity? = withContext(Dispatchers.IO) {
        posDao.getSaleById(saleId)
    }

    suspend fun getSaleItems(saleId: Long): List<SaleItemEntity> = withContext(Dispatchers.IO) {
        posDao.getSaleItemsSync(saleId)
    }

    suspend fun cancelSale(saleId: Long) = withContext(Dispatchers.IO) {
        posDao.cancelSale(saleId)
    }

    // --- Settings & Shifts ---
    val settings: Flow<AppSettingsEntity?> = posDao.getSettings()
    val activeShift: Flow<CashShiftEntity?> = posDao.getActiveShift()
    val allShifts: Flow<List<CashShiftEntity>> = posDao.getAllShifts()

    suspend fun updateSettings(settings: AppSettingsEntity) = withContext(Dispatchers.IO) {
        posDao.saveSettings(settings)
    }

    suspend fun openShift(cashierName: String, initialCash: Double): Long = withContext(Dispatchers.IO) {
        val shift = CashShiftEntity(
            cashierName = cashierName,
            openingTime = System.currentTimeMillis(),
            initialCash = initialCash,
            expectedCash = initialCash,
            isOpen = true
        )
        posDao.insertShift(shift)
    }

    suspend fun closeShift(shiftId: Long, actualCash: Double) = withContext(Dispatchers.IO) {
        val currentShift = posDao.getActiveShiftSync() ?: return@withContext
        val diff = actualCash - currentShift.expectedCash
        val updated = currentShift.copy(
            closingTime = System.currentTimeMillis(),
            actualCash = actualCash,
            difference = diff,
            isOpen = false
        )
        posDao.updateShift(updated)
    }

    // --- Process Sale & Electronic Invoicing ---
    suspend fun checkout(
        cartItems: List<CartItem>,
        customerName: String,
        customerTaxId: String,
        customerEmail: String,
        isElectronicInvoice: Boolean,
        paymentType: PaymentType,
        amountPaid: Double,
        cashierName: String
    ): SaleEntity = withContext(Dispatchers.IO) {
        val currentSettings = posDao.getSettingsSync() ?: AppSettingsEntity()
        val folioNumber = currentSettings.nextFolioNumber
        val prefix = if (isElectronicInvoice) currentSettings.invoicePrefix else "TKT-"
        val folio = "$prefix$folioNumber"

        val subtotal = cartItems.sumOf { it.subtotal }
        val taxTotal = cartItems.sumOf { it.taxAmount }
        val total = cartItems.sumOf { it.total }
        val changeGiven = (amountPaid - total).coerceAtLeast(0.0)

        // Electronic fiscal invoice stamping simulation (CFDI / DIAN standard format)
        val invoiceUuid = if (isElectronicInvoice) {
            UUID.randomUUID().toString().uppercase()
        } else null

        val isOffline = currentSettings.isOfflineSimulated

        val sale = SaleEntity(
            folio = folio,
            timestamp = System.currentTimeMillis(),
            customerName = customerName.ifBlank { "Público en General" },
            customerTaxId = customerTaxId.ifBlank { "XAXX010101000" },
            customerEmail = customerEmail,
            isElectronicInvoice = isElectronicInvoice,
            invoiceUuid = invoiceUuid,
            subtotal = subtotal,
            taxTotal = taxTotal,
            discount = 0.0,
            total = total,
            paymentMethod = paymentType.name,
            amountPaid = amountPaid,
            changeGiven = changeGiven,
            cashierName = cashierName,
            isSynced = !isOffline, // If offline, remains unsynced
            isCancelled = false
        )

        val saleId = posDao.insertSale(sale)

        // Insert items
        val itemEntities = cartItems.map { item ->
            SaleItemEntity(
                saleId = saleId,
                productId = item.product.id,
                productName = item.product.name,
                quantity = item.quantity,
                unitPrice = item.unitPriceAfterDiscount,
                subtotal = item.subtotal,
                taxRate = item.product.taxRate
            )
        }
        posDao.insertSaleItems(itemEntities)

        // Deduct inventory
        for (item in cartItems) {
            posDao.deductStock(item.product.id, item.quantity)
        }

        // Update active shift balances
        val activeShift = posDao.getActiveShiftSync()
        if (activeShift != null) {
            val updatedShift = when (paymentType) {
                PaymentType.CASH -> activeShift.copy(
                    cashSales = activeShift.cashSales + total,
                    expectedCash = activeShift.expectedCash + total
                )
                PaymentType.CARD -> activeShift.copy(
                    cardSales = activeShift.cardSales + total
                )
                PaymentType.QR_TRANSFER -> activeShift.copy(
                    qrSales = activeShift.qrSales + total
                )
            }
            posDao.updateShift(updatedShift)
        }

        // Advance next folio
        posDao.saveSettings(currentSettings.copy(nextFolioNumber = folioNumber + 1))

        sale.copy(id = saleId)
    }

    // --- Cloud Sync ---
    suspend fun syncPendingSales(): Int = withContext(Dispatchers.IO) {
        val unsynced = posDao.getUnsyncedSales()
        if (unsynced.isNotEmpty()) {
            // Mark all as synced to cloud database
            posDao.markSalesAsSynced(unsynced.map { it.id })
            val settings = posDao.getSettingsSync() ?: AppSettingsEntity()
            posDao.saveSettings(settings.copy(lastSyncTimestamp = System.currentTimeMillis()))
        }
        unsynced.size
    }

    suspend fun getUnsyncedCount(): Int = withContext(Dispatchers.IO) {
        posDao.getUnsyncedSales().size
    }

    companion object {
        fun generateFiscalHash(seed: String): String {
            val bytes = MessageDigest.getInstance("SHA-256").digest(seed.toByteArray())
            return bytes.joinToString("") { "%02x".format(it) }.take(40).uppercase()
        }
    }
}
