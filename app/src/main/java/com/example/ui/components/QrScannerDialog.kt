package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.example.data.model.ProductEntity
import com.example.ui.theme.PosSecondary
import com.example.ui.theme.PosSuccess

@Composable
fun QrScannerDialog(
    products: List<ProductEntity>,
    onBarcodeScanned: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            hasCameraPermission = granted
        }
    )

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            launcher.launch(Manifest.permission.CAMERA)
        }
    }

    var manualBarcode by remember { mutableStateOf("") }
    var isFlashOn by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "laser_line")
    val laserYRatio by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_y"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.95f)),
            color = Color.Black.copy(alpha = 0.95f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_scanner_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar Escáner",
                            tint = Color.White
                        )
                    }

                    Text(
                        text = "Escáner QR & Código de Barras",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    )

                    IconButton(
                        onClick = { isFlashOn = !isFlashOn },
                        modifier = Modifier.testTag("scanner_flashlight_button")
                    ) {
                        Icon(
                            imageVector = if (isFlashOn) Icons.Default.FlashOn else Icons.Default.FlashlightOff,
                            contentDescription = "Linterna",
                            tint = if (isFlashOn) Color.Yellow else Color.White
                        )
                    }
                }

                // Center Viewfinder
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(260.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF1E293B).copy(alpha = 0.8f))
                            .border(2.dp, PosSecondary, RoundedCornerShape(16.dp))
                            .testTag("scanner_viewfinder"),
                        contentAlignment = Alignment.Center
                    ) {
                        // Corner brackets & Animated laser
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height
                            val stroke = 6.dp.toPx()
                            val cornerLen = 36.dp.toPx()

                            // Top-Left
                            drawLine(PosSecondary, Offset(0f, 0f), Offset(cornerLen, 0f), stroke)
                            drawLine(PosSecondary, Offset(0f, 0f), Offset(0f, cornerLen), stroke)

                            // Top-Right
                            drawLine(PosSecondary, Offset(w, 0f), Offset(w - cornerLen, 0f), stroke)
                            drawLine(PosSecondary, Offset(w, 0f), Offset(w, cornerLen), stroke)

                            // Bottom-Left
                            drawLine(PosSecondary, Offset(0f, h), Offset(cornerLen, h), stroke)
                            drawLine(PosSecondary, Offset(0f, h), Offset(0f, h - cornerLen), stroke)

                            // Bottom-Right
                            drawLine(PosSecondary, Offset(w, h), Offset(w - cornerLen, h), stroke)
                            drawLine(PosSecondary, Offset(w, h), Offset(w, h - cornerLen), stroke)

                            // Scanning laser line
                            val laserY = h * laserYRatio
                            drawLine(
                                color = Color.Red,
                                start = Offset(16.dp.toPx(), laserY),
                                end = Offset(w - 16.dp.toPx(), laserY),
                                strokeWidth = 3.dp.toPx()
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.25f),
                            modifier = Modifier.size(100.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Apunta la cámara al código QR o de barras del producto",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color.LightGray),
                        fontSize = 12.sp
                    )
                }

                // Bottom Section: Quick simulation scan chips & manual entry
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Simulación rápida de escaneo:",
                        style = MaterialTheme.typography.labelMedium.copy(color = Color.White),
                        modifier = Modifier
                            .align(Alignment.Start)
                            .padding(bottom = 6.dp)
                    )

                    // Quick scan chips from existing products
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(products.take(6)) { product ->
                            Card(
                                onClick = { onBarcodeScanned(product.barcode) },
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("scan_preset_${product.barcode}")
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                                    Text(
                                        text = product.name,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        maxLines = 1
                                    )
                                    Text(
                                        text = "Cod: ${product.barcode}",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = PosSuccess,
                                            fontSize = 10.sp
                                        )
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Manual code entry
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = manualBarcode,
                            onValueChange = { manualBarcode = it },
                            placeholder = { Text("Ingresar código manual...", color = Color.Gray) },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("manual_barcode_input"),
                            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = PosSecondary,
                                unfocusedBorderColor = Color.Gray
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (manualBarcode.isNotBlank()) {
                                    onBarcodeScanned(manualBarcode)
                                }
                            },
                            enabled = manualBarcode.isNotBlank(),
                            modifier = Modifier.testTag("manual_barcode_submit")
                        ) {
                            Icon(imageVector = Icons.Default.Search, contentDescription = "Buscar")
                        }
                    }
                }
            }
        }
    }
}
