package com.example.ui.components

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.AppSettingsEntity
import com.example.data.model.LatAmPresets
import com.example.data.model.SaleEntity
import com.example.data.model.SaleItemEntity
import com.example.ui.theme.PosPrimary
import com.example.ui.theme.PosReceiptBg
import com.example.ui.theme.PosSuccess
import com.example.util.PaymentPrinterHelper
import com.example.util.QrCodeView

@Composable
fun ThermalReceiptDialog(
    sale: SaleEntity?,
    items: List<SaleItemEntity>,
    settings: AppSettingsEntity,
    isPrinting: Boolean,
    onPrintTicket: () -> Unit,
    onDismiss: () -> Unit
) {
    if (sale == null) return
    val context = LocalContext.current
    val countryConfig = LatAmPresets.findByCurrencyCode(settings.currencyCode)
    val qrVerificationData = sale.invoiceUuid ?: "FOLIO:${sale.folio}|NIT:${settings.businessTaxId}|TOTAL:${sale.total}|FECHA:${sale.formattedDate()}"

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Vista Previa de Ticket Térmico",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_receipt_dialog")) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Cerrar")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Thermal Paper Container (Styled like real POS paper)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .clip(RoundedCornerShape(8.dp))
                        .background(PosReceiptBg)
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                        .testTag("thermal_receipt_paper")
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Business Header
                        Text(
                            text = settings.businessName.uppercase(),
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "${countryConfig.taxIdLabel}: ${settings.businessTaxId}",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = settings.businessAddress,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Tel: ${settings.businessPhone}",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center
                        )

                        DashedDivider()

                        // Ticket Meta
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("FOLIO: ${sale.folio}", fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text(sale.formattedDate(), fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("CAJERO: ${sale.cashierName}", fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                            Text("CLIENTE: ${sale.customerName}", fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                        }

                        DashedDivider()

                        // Items Table Header
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("CANT / DESCRIPCIÓN", fontFamily = FontFamily.Monospace, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text("TOTAL", fontFamily = FontFamily.Monospace, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Items List
                        items.forEach { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.productName,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "${item.quantity} x ${settings.currencySymbol} %.2f".format(item.unitPrice),
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp,
                                        color = Color.DarkGray
                                    )
                                }
                                Text(
                                    text = "${settings.currencySymbol} %.2f".format(item.subtotal),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        DashedDivider()

                        // Totals
                        val taxRatePct = (settings.defaultTaxRate * 100).toInt()
                        SummaryRow("SUBTOTAL:", "${settings.currencySymbol} %.2f".format(sale.subtotal))
                        SummaryRow("${countryConfig.taxName} ($taxRatePct%):", "${settings.currencySymbol} %.2f".format(sale.taxTotal))
                        SummaryRow("TOTAL:", "${settings.currencySymbol} %.2f".format(sale.total), isBold = true, fontSize = 14)

                        DashedDivider()

                        // Payment Details
                        SummaryRow("MÉTODO:", sale.paymentMethod)
                        if (sale.paymentMethod == "CASH") {
                            SummaryRow("RECIBIDO:", "${settings.currencySymbol} %.2f".format(sale.amountPaid))
                            SummaryRow("CAMBIO:", "${settings.currencySymbol} %.2f".format(sale.changeGiven))
                        } else if (sale.paymentMethod == "CARD") {
                            SummaryRow("AUTORIZACIÓN:", "AUT-994821")
                            SummaryRow("TERMINAL ID:", "POS-EMV-01")
                        } else {
                            SummaryRow("REF. ${countryConfig.qrPaymentName.take(14).uppercase()}:", "QR-${sale.id * 8371}")
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Real Scannable QR Code on Receipt
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, Color(0xFFDDDDDD)),
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            QrCodeView(
                                content = qrVerificationData,
                                size = 112.dp,
                                padding = 8.dp
                            )
                        }
                        Text(
                            text = "COMPROBANTE ELECTRÓNICO CON QR",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 8.5.sp,
                            letterSpacing = 0.5.sp,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "* ${sale.folio} *",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp,
                            color = Color.DarkGray
                        )

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "¡GRACIAS POR SU PREFERENCIA!",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center
                        )
                        if (countryConfig.invoiceLegend.isNotBlank()) {
                            Text(
                                text = countryConfig.invoiceLegend,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 8.sp,
                                color = Color.DarkGray,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                        Text(
                            text = "Impresora Térmica ${settings.thermalPaperWidthMm}mm ESC/POS",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Actions: Print & Share
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    "Ticket de Compra ${settings.businessName}\nFolio: ${sale.folio}\nTotal: ${settings.currencySymbol} ${sale.total}\nFecha: ${sale.formattedDate()}"
                                )
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Compartir ticket"))
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("share_receipt_button")
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Compartir", fontSize = 13.sp)
                    }

                    Button(
                        onClick = {
                            PaymentPrinterHelper.printPaymentReceipt(context, sale, items, settings)
                            onPrintTicket()
                        },
                        enabled = !isPrinting,
                        colors = ButtonDefaults.buttonColors(containerColor = PosPrimary),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("print_thermal_ticket_button")
                    ) {
                        if (isPrinting) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(imageVector = Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isPrinting) "Imprimiendo..." else "Imprimir Pago", fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String, isBold: Boolean = false, fontSize: Int = 11) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontFamily = FontFamily.Monospace,
            fontSize = fontSize.sp,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal
        )
        Text(
            text = value,
            fontFamily = FontFamily.Monospace,
            fontSize = fontSize.sp,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun DashedDivider() {
    Text(
        text = "------------------------------------------",
        fontFamily = FontFamily.Monospace,
        fontSize = 10.sp,
        color = Color.Gray,
        maxLines = 1,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}
