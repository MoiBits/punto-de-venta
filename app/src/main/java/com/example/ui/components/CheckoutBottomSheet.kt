package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppSettingsEntity
import com.example.data.model.CartItem
import com.example.data.model.LatAmPresets
import com.example.data.model.PaymentType
import com.example.ui.theme.PosPrimary
import com.example.ui.theme.PosSecondary
import com.example.ui.theme.PosSuccess
import com.example.util.QrCodeView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutBottomSheet(
    cartItems: List<CartItem>,
    subtotal: Double,
    taxTotal: Double,
    total: Double,
    settings: AppSettingsEntity,
    onCompleteSale: (
        customerName: String,
        customerTaxId: String,
        customerEmail: String,
        isElectronicInvoice: Boolean,
        paymentType: PaymentType,
        amountPaid: Double
    ) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var selectedPaymentType by remember { mutableStateOf(PaymentType.CASH) }
    var cashPaidInput by remember { mutableStateOf(String.format("%.2f", total)) }
    var isElectronicInvoice by remember { mutableStateOf(false) }

    var customerName by remember { mutableStateOf("") }
    var customerTaxId by remember { mutableStateOf("") }
    var customerEmail by remember { mutableStateOf("") }

    val cashPaid = cashPaidInput.toDoubleOrNull() ?: total
    val change = (cashPaid - total).coerceAtLeast(0.0)

    val countryConfig = LatAmPresets.findByCurrencyCode(settings.currencyCode)

    val quickAmounts = (
        listOf(total) +
        countryConfig.quickCashAmounts.filter { it >= total } +
        listOf(
            kotlin.math.ceil(total / 10.0) * 10.0,
            kotlin.math.ceil(total / 50.0) * 50.0,
            kotlin.math.ceil(total / 100.0) * 100.0
        )
    ).distinct().sorted().filter { it >= total }.take(5)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header Total Box
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(0.6.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "TOTAL A COBRAR",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                letterSpacing = 0.5.sp,
                                fontSize = 10.sp
                            )
                        )
                        Text(
                            text = "${settings.currencySymbol} %.2f".format(total),
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 24.sp
                            )
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Subtotal: ${settings.currencySymbol} %.2f".format(subtotal),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "IVA: ${settings.currencySymbol} %.2f".format(taxTotal),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${cartItems.sumOf { it.quantity }} artículos",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Payment Methods Tabs
            Text(
                text = "MÉTODO DE PAGO",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.5.sp,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PaymentMethodCard(
                    title = "Efectivo",
                    icon = Icons.Default.Payments,
                    isSelected = selectedPaymentType == PaymentType.CASH,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("pay_method_cash"),
                    onClick = { selectedPaymentType = PaymentType.CASH }
                )
                PaymentMethodCard(
                    title = "Tarjeta",
                    icon = Icons.Default.CreditCard,
                    isSelected = selectedPaymentType == PaymentType.CARD,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("pay_method_card"),
                    onClick = { selectedPaymentType = PaymentType.CARD }
                )
                PaymentMethodCard(
                    title = "QR Móvil",
                    icon = Icons.Default.QrCode,
                    isSelected = selectedPaymentType == PaymentType.QR_TRANSFER,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("pay_method_qr"),
                    onClick = { selectedPaymentType = PaymentType.QR_TRANSFER }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Payment Specific Controls
            when (selectedPaymentType) {
                PaymentType.CASH -> {
                    Column {
                        Text("Monto Recibido en Efectivo:", style = MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.height(6.dp))

                        OutlinedTextField(
                            value = cashPaidInput,
                            onValueChange = { cashPaidInput = it },
                            prefix = { Text(settings.currencySymbol) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("cash_amount_input")
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Quick cash chips
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(quickAmounts) { amount ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { cashPaidInput = String.format("%.2f", amount) }
                                        .testTag("quick_cash_${amount.toInt()}")
                                ) {
                                    Text(
                                        text = "${settings.currencySymbol} %.2f".format(amount),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Change calculation
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = PosSuccess.copy(alpha = 0.12f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Cambio a devolver al cliente:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    text = "${settings.currencySymbol} %.2f".format(change),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = PosSuccess
                                )
                            }
                        }
                    }
                }

                PaymentType.CARD -> {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.CreditCard,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Pase o inserte la tarjeta en la terminal",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }
                    }
                }

                PaymentType.QR_TRANSFER -> {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.QrCode,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = countryConfig.qrPaymentName,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                    Text(
                                        text = countryConfig.qrPaymentSubtitle,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Dynamic Payment QR Code with exact amount
                            val paymentPayload = "00020101021226580014${settings.businessTaxId}520400005303${settings.currencyCode}5406${"%.2f".format(total)}5802BO5913${settings.businessName.take(13)}6304"
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color.White,
                                shadowElevation = 2.dp
                            ) {
                                QrCodeView(
                                    content = paymentPayload,
                                    size = 145.dp,
                                    padding = 8.dp
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Monto a Pagar: ${settings.currencySymbol} ${"%.2f".format(total)} ${settings.currencyCode}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "El cliente escanea este QR desde su app bancaria para abonar.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 14.dp))

            // Electronic Invoicing Fiscal Option Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ReceiptLong,
                        contentDescription = null,
                        tint = if (isElectronicInvoice) PosSuccess else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Facturación Fiscal Oficial",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "${countryConfig.invoiceSystemName} (${countryConfig.taxAuthority})",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Switch(
                    checked = isElectronicInvoice,
                    onCheckedChange = { isElectronicInvoice = it },
                    modifier = Modifier.testTag("toggle_fiscal_invoice")
                )
            }

            AnimatedVisibility(visible = isElectronicInvoice) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = customerName,
                        onValueChange = { customerName = it },
                        label = { Text("Nombre o Razón Social del Cliente") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("invoice_customer_name")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = customerTaxId,
                            onValueChange = { customerTaxId = it.uppercase() },
                            label = { Text("${countryConfig.taxIdLabel} del Cliente") },
                            placeholder = { Text(countryConfig.sampleTaxId) },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("invoice_customer_tax_id")
                        )

                        OutlinedTextField(
                            value = customerEmail,
                            onValueChange = { customerEmail = it },
                            label = { Text("Correo Electrónico") },
                            placeholder = { Text("cliente@email.com") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1.2f)
                                .testTag("invoice_customer_email")
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Action Button
            Button(
                onClick = {
                    onCompleteSale(
                        customerName,
                        customerTaxId,
                        customerEmail,
                        isElectronicInvoice,
                        selectedPaymentType,
                        if (selectedPaymentType == PaymentType.CASH) cashPaid else total
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .testTag("confirm_checkout_button"),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = if (isElectronicInvoice) Icons.Default.ReceiptLong else Icons.Default.Done,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isElectronicInvoice) "Cobrar y Timbrar (${settings.currencySymbol} %.2f)".format(total)
                    else "Cobrar ${settings.currencySymbol} %.2f".format(total),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun PaymentMethodCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(
            if (isSelected) 1.dp else 0.6.dp,
            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
        ),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
