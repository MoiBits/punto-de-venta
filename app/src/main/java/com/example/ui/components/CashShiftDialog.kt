package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.AppSettingsEntity
import com.example.data.model.CashShiftEntity
import com.example.ui.theme.PosError
import com.example.ui.theme.PosPrimary
import com.example.ui.theme.PosSuccess
import com.example.ui.theme.PosWarning
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CashShiftDialog(
    activeShift: CashShiftEntity?,
    settings: AppSettingsEntity,
    onOpenShift: (cashierName: String, initialCash: Double) -> Unit,
    onCloseShift: (actualCash: Double) -> Unit,
    onDismiss: () -> Unit
) {
    var newCashierName by remember { mutableStateOf("Carlos Méndez (Cajero 1)") }
    var newInitialCashInput by remember { mutableStateOf("200.00") }

    var actualCashInput by remember(activeShift) {
        mutableStateOf(String.format("%.2f", activeShift?.expectedCash ?: 0.0))
    }

    val actualCash = actualCashInput.toDoubleOrNull() ?: 0.0
    val expectedCash = activeShift?.expectedCash ?: 0.0
    val difference = actualCash - expectedCash

    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

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
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.PointOfSale,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (activeShift?.isOpen == true) "Corte de Caja (Turno Activo)" else "Apertura de Turno",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_shift_dialog")) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Cerrar")
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                if (activeShift?.isOpen == true) {
                    // Active shift overview (Corte X / Z)
                    Text("INFORMACIÓN DEL TURNO", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(4.dp))

                    DetailRow("Cajero en Turno:", activeShift.cashierName)
                    DetailRow("Hora de Apertura:", sdf.format(Date(activeShift.openingTime)))
                    DetailRow("Fondo Inicial:", "${settings.currencySymbol} %.2f".format(activeShift.initialCash))

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    Text("VENTAS POR MÉTODO DE PAGO", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(4.dp))

                    DetailRow("Ventas en Efectivo (+):", "${settings.currencySymbol} %.2f".format(activeShift.cashSales))
                    DetailRow("Ventas con Tarjeta (POS):", "${settings.currencySymbol} %.2f".format(activeShift.cardSales))
                    DetailRow("Ventas con Transferencia QR:", "${settings.currencySymbol} %.2f".format(activeShift.qrSales))

                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Efectivo Esperado en Caja:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text(
                                text = "${settings.currencySymbol} %.2f".format(expectedCash),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Arqueo Input
                    Text("ARQUEO DE CAJA (EFECTIVO FÍSICO CONTADO)", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(4.dp))

                    OutlinedTextField(
                        value = actualCashInput,
                        onValueChange = { actualCashInput = it },
                        prefix = { Text(settings.currencySymbol) },
                        label = { Text("Efectivo Contado en Gaveta") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("actual_cash_input")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Difference pill
                    val diffColor = when {
                        difference > 0.01 -> PosSuccess
                        difference < -0.01 -> PosError
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    val diffLabel = when {
                        difference > 0.01 -> "Sobrante en caja: +${settings.currencySymbol} %.2f".format(difference)
                        difference < -0.01 -> "Faltante en caja: -${settings.currencySymbol} %.2f".format(kotlin.math.abs(difference))
                        else -> "Cuadre exacto: Sin diferencia"
                    }

                    Surface(
                        color = diffColor.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = diffLabel,
                            fontWeight = FontWeight.Bold,
                            color = diffColor,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Close Shift Action
                    Button(
                        onClick = { onCloseShift(actualCash) },
                        colors = ButtonDefaults.buttonColors(containerColor = PosError),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("close_shift_button")
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Cerrar Turno (Corte Z Definitivo)", fontWeight = FontWeight.Bold)
                    }
                } else {
                    // Open New Shift form
                    Text("APERTURA DE NUEVO TURNO", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = newCashierName,
                        onValueChange = { newCashierName = it },
                        label = { Text("Nombre del Cajero / Encargado") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("new_cashier_name_input")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = newInitialCashInput,
                        onValueChange = { newInitialCashInput = it },
                        label = { Text("Fondo Inicial de Caja") },
                        prefix = { Text(settings.currencySymbol) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("new_initial_cash_input")
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            val initial = newInitialCashInput.toDoubleOrNull() ?: 200.0
                            onOpenShift(newCashierName, initial)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PosSuccess),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("open_new_shift_submit")
                    ) {
                        Icon(Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Iniciar Turno de Caja", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}
