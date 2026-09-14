package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val barcode: String,
    val name: String,
    val category: String,
    val costPrice: Double,
    val salePrice: Double,
    val stock: Int,
    val minStock: Int = 5,
    val taxRate: Double = 0.16, // Default IVA
    val imageUrl: String? = null,
    val qrCode: String? = null,
    val isSynced: Boolean = true,
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun effectiveQrCode(): String = qrCode?.ifBlank { null } ?: barcode.ifBlank { "PROD-$id" }
}

@Entity(tableName = "sales")
data class SaleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val folio: String,
    val timestamp: Long = System.currentTimeMillis(),
    val customerName: String = "Público en General",
    val customerTaxId: String = "XAXX010101000", // Generic RFC/NIT
    val customerEmail: String = "",
    val isElectronicInvoice: Boolean = false,
    val invoiceUuid: String? = null,
    val subtotal: Double,
    val taxTotal: Double,
    val discount: Double = 0.0,
    val total: Double,
    val paymentMethod: String, // CASH, CARD, QR_TRANSFER
    val amountPaid: Double,
    val changeGiven: Double,
    val cashierName: String = "Cajero 1",
    val isSynced: Boolean = false,
    val isCancelled: Boolean = false
) {
    fun formattedDate(): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}

@Entity(tableName = "sale_items")
data class SaleItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val saleId: Long,
    val productId: Long,
    val productName: String,
    val quantity: Int,
    val unitPrice: Double,
    val subtotal: Double,
    val taxRate: Double
)

@Entity(tableName = "cash_shifts")
data class CashShiftEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val cashierName: String,
    val openingTime: Long = System.currentTimeMillis(),
    val closingTime: Long? = null,
    val initialCash: Double,
    val cashSales: Double = 0.0,
    val cardSales: Double = 0.0,
    val qrSales: Double = 0.0,
    val expectedCash: Double = initialCash,
    val actualCash: Double? = null,
    val difference: Double? = null,
    val isOpen: Boolean = true
)

@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey
    val id: Int = 1,
    val businessName: String = "Comercial & Retail Bolivia S.R.L.",
    val businessTaxId: String = "1023456789", // NIT Bolivia
    val businessAddress: String = "Av. 6 de Agosto #2450, Sopocachi, La Paz",
    val businessPhone: String = "+591 2 2441234",
    val businessEmail: String = "contacto@comerciobolivia.bo",
    val currencySymbol: String = "Bs.",
    val currencyCode: String = "BOB",
    val defaultTaxRate: Double = 0.13, // 13% IVA Bolivia
    val invoicePrefix: String = "FAC-",
    val nextFolioNumber: Int = 1001,
    val isOfflineSimulated: Boolean = false,
    val lastSyncTimestamp: Long = System.currentTimeMillis(),
    val thermalPaperWidthMm: Int = 80 // 58 or 80mm
)

// UI Helper Models
data class CartItem(
    val product: ProductEntity,
    var quantity: Int = 1,
    var discountPercent: Double = 0.0
) {
    val unitPriceAfterDiscount: Double
        get() = product.salePrice * (1.0 - (discountPercent / 100.0))

    val subtotal: Double
        get() = unitPriceAfterDiscount * quantity

    val taxAmount: Double
        get() = subtotal * product.taxRate

    val total: Double
        get() = subtotal + taxAmount
}

enum class PaymentType(val displayName: String) {
    CASH("Efectivo"),
    CARD("Tarjeta Débito/Crédito"),
    QR_TRANSFER("Transferencia QR")
}

data class ElectronicInvoiceData(
    val uuid: String,
    val folio: String,
    val issuerName: String,
    val issuerTaxId: String,
    val receiverName: String,
    val receiverTaxId: String,
    val receiverEmail: String,
    val subtotal: Double,
    val taxTotal: Double,
    val total: Double,
    val digitalStamp: String,
    val satStamp: String,
    val certificationDate: String,
    val qrFiscalData: String
)
