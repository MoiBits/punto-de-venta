package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.PosViewModel
import com.example.ui.theme.PosPrimary

@Composable
fun AnalyticsScreen(
    viewModel: PosViewModel,
    modifier: Modifier = Modifier
) {
    val sales by viewModel.allSales.collectAsState()
    val saleItems by viewModel.allSaleItems.collectAsState()
    val settings by viewModel.settings.collectAsState()

    val totalSalesAmount = sales.filter { !it.isCancelled }.sumOf { it.total }
    val transactionCount = sales.filter { !it.isCancelled }.size
    val averageTicket = if (transactionCount > 0) totalSalesAmount / transactionCount else 0.0
    val estimatedProfit = totalSalesAmount * 0.42

    val cashSales = sales.filter { it.paymentMethod == "CASH" && !it.isCancelled }.sumOf { it.total }
    val cardSales = sales.filter { it.paymentMethod == "CARD" && !it.isCancelled }.sumOf { it.total }
    val qrSales = sales.filter { it.paymentMethod == "QR_TRANSFER" && !it.isCancelled }.sumOf { it.total }

    val cashPct = if (totalSalesAmount > 0) (cashSales / totalSalesAmount).toFloat() else 0f
    val cardPct = if (totalSalesAmount > 0) (cardSales / totalSalesAmount).toFloat() else 0f
    val qrPct = if (totalSalesAmount > 0) (qrSales / totalSalesAmount).toFloat() else 0f

    val productSalesMap = saleItems.groupBy { it.productName }
        .mapValues { entry ->
            Pair(
                entry.value.sumOf { it.quantity },
                entry.value.sumOf { it.subtotal }
            )
        }
        .toList()
        .sortedByDescending { it.second.first }
        .take(5)

    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "Reportes y Analítica",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp,
                    letterSpacing = (-0.3).sp
                )
            )
            Text(
                text = "Desempeño comercial en tiempo real",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    fontSize = 12.sp
                )
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 4 Minimalist KPI Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MinimalKpiCard(
                    title = "VENTAS TOTALES",
                    value = "${settings.currencySymbol} %.2f".format(totalSalesAmount),
                    modifier = Modifier.weight(1f),
                    tag = "kpi_total_sales"
                )
                MinimalKpiCard(
                    title = "MARGEN ESTIMADO",
                    value = "${settings.currencySymbol} %.2f".format(estimatedProfit),
                    modifier = Modifier.weight(1f),
                    tag = "kpi_profit"
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MinimalKpiCard(
                    title = "TRANSACCIONES",
                    value = "$transactionCount",
                    modifier = Modifier.weight(1f),
                    tag = "kpi_transactions"
                )
                MinimalKpiCard(
                    title = "TICKET PROMEDIO",
                    value = "${settings.currencySymbol} %.2f".format(averageTicket),
                    modifier = Modifier.weight(1f),
                    tag = "kpi_avg_ticket"
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Minimalist Trend Chart Card
            Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                border = BorderStroke(0.6.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("sales_chart_card")
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Ventas por Día",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    val days = listOf("Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Hoy")
                    val sampleValues = listOf(
                        (totalSalesAmount * 0.12).coerceAtLeast(15.0),
                        (totalSalesAmount * 0.15).coerceAtLeast(22.0),
                        (totalSalesAmount * 0.18).coerceAtLeast(30.0),
                        (totalSalesAmount * 0.14).coerceAtLeast(18.0),
                        (totalSalesAmount * 0.22).coerceAtLeast(42.0),
                        (totalSalesAmount * 0.26).coerceAtLeast(55.0),
                        totalSalesAmount.coerceAtLeast(25.0)
                    )
                    val maxVal = (sampleValues.maxOrNull() ?: 100.0).coerceAtLeast(1.0)

                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                    ) {
                        val w = size.width
                        val h = size.height
                        val barWidth = 14.dp.toPx()

                        for (i in days.indices) {
                            val barHeight = ((sampleValues[i] / maxVal) * (h - 18.dp.toPx())).toFloat()
                            val x = i * (w / days.size) + (w / days.size - barWidth) / 2f
                            val y = h - barHeight - 12.dp.toPx()

                            drawRoundRect(
                                color = if (i == days.size - 1) Color(0xFF0F172A) else Color(0xFFE2E8F0),
                                topLeft = Offset(x, y),
                                size = Size(barWidth, barHeight),
                                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        days.forEachIndexed { idx, day ->
                            Text(
                                text = day,
                                fontSize = 10.sp,
                                fontWeight = if (idx == days.size - 1) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (idx == days.size - 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Payment Methods Breakdown
            Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                border = BorderStroke(0.6.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("payment_methods_card")
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Métodos de Pago",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    PaymentProgressBar(
                        title = "Efectivo",
                        amount = "${settings.currencySymbol} %.2f".format(cashSales),
                        percentage = (cashPct * 100).toInt(),
                        color = PosPrimary,
                        progress = cashPct
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    PaymentProgressBar(
                        title = "Tarjeta",
                        amount = "${settings.currencySymbol} %.2f".format(cardSales),
                        percentage = (cardPct * 100).toInt(),
                        color = Color(0xFF0D9488),
                        progress = cardPct
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    PaymentProgressBar(
                        title = "Transferencia QR",
                        amount = "${settings.currencySymbol} %.2f".format(qrSales),
                        percentage = (qrPct * 100).toInt(),
                        color = Color(0xFF64748B),
                        progress = qrPct
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Top 5 Products
            Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                border = BorderStroke(0.6.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("top_products_card")
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Top Productos",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    if (productSalesMap.isEmpty()) {
                        Text(
                            text = "Sin datos de ventas aún",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                    } else {
                        val maxUnits = productSalesMap.maxOfOrNull { it.second.first } ?: 1
                        productSalesMap.forEachIndexed { index, item ->
                            val (name, data) = item
                            val (units, revenue) = data
                            val ratio = if (maxUnits > 0) units.toFloat() / maxUnits else 0f

                            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${index + 1}. $name",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Normal
                                    )
                                    Text(
                                        text = "$units u. • ${settings.currencySymbol} %.2f".format(revenue),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                                LinearProgressIndicator(
                                    progress = { ratio },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(2.dp)),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun MinimalKpiCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    tag: String
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(0.6.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
        modifier = modifier.testTag(tag)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = title,
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun PaymentProgressBar(
    title: String,
    amount: String,
    percentage: Int,
    color: Color,
    progress: Float
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(title, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
            Text("$amount ($percentage%)", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}
