package com.example.ui.components

import android.content.Intent
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
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.AppSettingsEntity
import com.example.data.model.LatAmPresets
import com.example.data.model.SaleEntity
import com.example.data.model.SaleItemEntity
import com.example.data.repository.PosRepository
import com.example.ui.theme.PosPrimary
import com.example.ui.theme.PosSecondary
import com.example.ui.theme.PosSuccess

@Composable
fun ElectronicInvoiceDialog(
    sale: SaleEntity?,
    items: List<SaleItemEntity>,
    settings: AppSettingsEntity,
    onDismiss: () -> Unit
) {
    if (sale == null) return
    val context = LocalContext.current
    val countryConfig = LatAmPresets.findByCurrencyCode(settings.currencyCode)

    val uuid = sale.invoiceUuid ?: "E82A9C14-3DF9-4B52-87F1-92D014F98EAA"
    val originalChain = "||1.1|$uuid|${sale.formattedDate()}|${settings.businessTaxId}|${sale.total}|${countryConfig.taxName} ${(settings.defaultTaxRate * 100).toInt()}%||"
    val digitalStamp = PosRepository.generateFiscalHash(originalChain)
    val satStamp = PosRepository.generateFiscalHash(digitalStamp + "${countryConfig.taxAuthority}_PAC")

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = PosSuccess,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Factura Electrónica Timbrada",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Comprobante Fiscal Digital Oficial",
                                style = MaterialTheme.typography.bodySmall.copy(color = PosSuccess, fontSize = 11.sp)
                            )
                        }
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_invoice_dialog")) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Cerrar")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Scrollable Invoice Body
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(14.dp)
                ) {
                    // Fiscal Certificate Stamp Banner
                    Surface(
                        color = PosSuccess.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = PosSuccess, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "${countryConfig.invoiceStampLabel.uppercase()}:",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = uuid,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Issuer & Receiver Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Issuer
                        Column(modifier = Modifier.weight(1f)) {
                            Text("DATOS DEL EMISOR", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = PosSecondary)
                            Text(settings.businessName, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                            Text("${countryConfig.taxIdLabel}: ${settings.businessTaxId}", fontSize = 11.sp)
                            Text("Régimen: General Fiscal (${countryConfig.countryName})", fontSize = 10.sp, color = Color.Gray)
                            Text("Entidad: ${countryConfig.taxAuthority}", fontSize = 10.sp, color = Color.Gray, maxLines = 1)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        // Receiver
                        Column(modifier = Modifier.weight(1f)) {
                            Text("DATOS DEL RECEPTOR", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = PosSecondary)
                            Text(sale.customerName, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                            Text("${countryConfig.taxIdLabel}: ${sale.customerTaxId.ifBlank { "SIN DATO" }}", fontSize = 11.sp)
                            Text("Sistema: ${countryConfig.invoiceSystemName}", fontSize = 10.sp, color = Color.Gray, maxLines = 1)
                            if (sale.customerEmail.isNotBlank()) {
                                Text("Email: ${sale.customerEmail}", fontSize = 10.sp, color = Color.Gray, maxLines = 1)
                            }
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                    // Concept / Products Table
                    Text("CONCEPTOS FACTURADOS", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(6.dp))

                    items.forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.productName, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                Text(
                                    "ClaveSAT: 50201708 | ${item.quantity} pza x ${settings.currencySymbol} %.2f".format(item.unitPrice),
                                    fontSize = 10.sp,
                                    color = Color.Gray
                                )
                            }
                            Text(
                                "${settings.currencySymbol} %.2f".format(item.subtotal),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    // Tax Breakdown
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Subtotal:", fontSize = 12.sp)
                        Text("${settings.currencySymbol} %.2f".format(sale.subtotal), fontSize = 12.sp)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Impuestos Trasladados (IVA 16%):", fontSize = 12.sp)
                        Text("${settings.currencySymbol} %.2f".format(sale.taxTotal), fontSize = 12.sp)
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("TOTAL FACTURA:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(
                            "${settings.currencySymbol} %.2f ${settings.currencyCode}".format(sale.total),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = PosSuccess
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // SAT Stamps & QR Fiscal
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // QR Fiscal Canvas
                        Canvas(modifier = Modifier.size(72.dp)) {
                            val qrSize = size.width
                            val matrix = 9
                            val cellSize = qrSize / matrix
                            drawRect(Color.White)

                            for (r in 0 until matrix) {
                                for (c in 0 until matrix) {
                                    val isCorner = (r < 3 && c < 3) || (r < 3 && c > 5) || (r > 5 && c < 3)
                                    val isCenter = (r == 4 && c == 4) || (r in 3..5 && c in 2..6 && (r + c) % 2 == 0)
                                    if (isCorner || isCenter || (r * 3 + c * 7) % 5 == 0) {
                                        drawRect(
                                            color = Color.Black,
                                            topLeft = Offset(c * cellSize, r * cellSize),
                                            size = Size(cellSize, cellSize)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text("SELLO DIGITAL DEL EMISOR", fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            Text(digitalStamp.take(32) + "...", fontSize = 8.sp, fontFamily = FontFamily.Monospace, maxLines = 1)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("AUTORIZACIÓN / CERTIFICACIÓN", fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            Text(satStamp.take(32) + "...", fontSize = 8.sp, fontFamily = FontFamily.Monospace, maxLines = 1)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("Entidad: ${countryConfig.taxAuthority}", fontSize = 8.sp, color = Color.Gray, maxLines = 1)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Actions: Share / Email & Close
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
                                    "Factura Electrónica ${countryConfig.countryName}\n${countryConfig.invoiceStampLabel}: $uuid\nEmisor: ${settings.businessName} (${countryConfig.taxIdLabel}: ${settings.businessTaxId})\nReceptor: ${sale.customerName} (${sale.customerTaxId})\nTotal: ${settings.currencySymbol} ${sale.total}\nVerificación: ${countryConfig.verificationUrlPrefix}$uuid"
                                )
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Enviar Factura Electrónica"))
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("share_invoice_button")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Compartir", fontSize = 13.sp)
                    }

                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = PosSuccess),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("done_invoice_button")
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Aceptar", fontSize = 13.sp)
                    }
                }
            }
        }
    }
}
