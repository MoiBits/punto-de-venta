package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.model.CartItem
import com.example.data.model.ProductEntity
import com.example.ui.PosViewModel
import com.example.ui.theme.PosError
import com.example.ui.theme.PosSecondary
import com.example.ui.theme.PosSuccess
import com.example.ui.theme.PosWarning

@Composable
fun RegisterScreen(
    viewModel: PosViewModel,
    onNavigateToSettings: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val allProducts by viewModel.allProducts.collectAsState()
    val cartItems by viewModel.cartItems.collectAsState()
    val cartTotal by viewModel.cartTotal.collectAsState()
    val cartCount by viewModel.cartItemCount.collectAsState()
    val settings by viewModel.settings.collectAsState()

    // State for currently active/scanned product in the 4 pill rows
    var selectedProduct by remember { mutableStateOf<ProductEntity?>(null) }
    var enteredBarcode by remember { mutableStateOf("") }
    var quantity by remember { mutableIntStateOf(1) }
    var showProductPicker by remember { mutableStateOf(false) }
    var showCartBreakdown by remember { mutableStateOf(false) }

    // Camera & Scanner State
    var isCameraEnabled by remember { mutableStateOf(true) }
    var isFlashOn by remember { mutableStateOf(false) }
    var lensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }
    var isSimulatingLaser by remember { mutableStateOf(true) }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> hasCameraPermission = granted }
    )

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Auto-select first product if none selected yet
    LaunchedEffect(allProducts) {
        if (selectedProduct == null && allProducts.isNotEmpty()) {
            selectedProduct = allProducts.first()
            enteredBarcode = allProducts.first().barcode
        }
    }

    // Function to handle newly scanned or chosen barcode
    fun handleCodeSelect(barcode: String) {
        val trimmed = barcode.trim()
        enteredBarcode = trimmed
        val found = allProducts.find { it.barcode.equals(trimmed, ignoreCase = true) }
        if (found != null) {
            selectedProduct = found
            quantity = 1
        }
    }

    val scrollState = rememberScrollState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ==========================================
            // TOP SECTION: "Lector qr" Card
            // ==========================================
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFF0F172A), // Sleek deep slate
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(290.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .testTag("scanner_viewfinder_container")
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // Camera Live Preview (CameraX) or Visual Laser Guide
                    if (isCameraEnabled && hasCameraPermission) {
                        CameraXLivePreview(
                            isFlashOn = isFlashOn,
                            lensFacing = lensFacing,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        // Camera Off Placeholder State
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFF090D16)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VideocamOff,
                                    contentDescription = "Cámara Desactivada",
                                    tint = Color.White.copy(alpha = 0.4f),
                                    modifier = Modifier.size(48.dp)
                                )
                                Text(
                                    text = "Cámara apagada",
                                    color = Color.White.copy(alpha = 0.65f),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { isCameraEnabled = true }
                                ) {
                                    Text(
                                        text = "Encender Cámara",
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Viewfinder overlay graphics: scanning laser line and corner brackets (when camera active)
                    if (isCameraEnabled) {
                        ScannerOverlayLaser(modifier = Modifier.fillMaxSize())
                    }

                    // Top-Left Header: "Lector qr"
                    Column(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(top = 16.dp, start = 18.dp)
                    ) {
                        Text(
                            text = "Lector qr",
                            style = MaterialTheme.typography.titleLarge.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                letterSpacing = (-0.5).sp
                            )
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(if (isCameraEnabled) PosSuccess else PosError)
                            )
                            Text(
                                text = if (isCameraEnabled) "Listo para escanear" else "Cámara en pausa",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.75f)
                            )
                        }
                    }

                    // Top/Mid-Right Controls: Power (on/off), Flash, Switch lens, and Settings
                    Column(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 14.dp, end = 14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // 1. Settings / Ajustes Button (direct shortcut to settings)
                        if (onNavigateToSettings != null) {
                            Surface(
                                shape = CircleShape,
                                color = Color.White.copy(alpha = 0.9f),
                                shadowElevation = 3.dp,
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .clickable { onNavigateToSettings() }
                                    .testTag("scanner_settings_shortcut_button")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = "Ajustes",
                                        tint = Color(0xFF0F172A),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        // 2. Camera On / Off Toggle Button
                        Surface(
                            shape = CircleShape,
                            color = if (isCameraEnabled) Color.White.copy(alpha = 0.95f) else Color(0xFFEF4444),
                            shadowElevation = 3.dp,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .clickable {
                                    isCameraEnabled = !isCameraEnabled
                                    if (!isCameraEnabled) isFlashOn = false
                                }
                                .testTag("camera_power_toggle_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isCameraEnabled) Icons.Default.Videocam else Icons.Default.VideocamOff,
                                    contentDescription = if (isCameraEnabled) "Apagar Cámara" else "Encender Cámara",
                                    tint = if (isCameraEnabled) Color(0xFF0F172A) else Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // 3. Circular Lens Switch (Front / Back camera)
                        if (isCameraEnabled) {
                            Surface(
                                shape = CircleShape,
                                color = Color.White.copy(alpha = 0.9f),
                                shadowElevation = 3.dp,
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .clickable {
                                        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                                            CameraSelector.LENS_FACING_FRONT
                                        } else {
                                            CameraSelector.LENS_FACING_BACK
                                        }
                                    }
                                    .testTag("camera_switch_circular_button")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.FlipCameraAndroid,
                                        contentDescription = "Cambiar Cámara",
                                        tint = Color(0xFF0F172A),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            // 4. Lightning Bolt Icon Button (Flash / Linterna Toggle)
                            Surface(
                                shape = CircleShape,
                                color = if (isFlashOn) Color(0xFFFDE047) else Color.White.copy(alpha = 0.9f),
                                shadowElevation = 3.dp,
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .clickable { isFlashOn = !isFlashOn }
                                    .testTag("flashlight_lightning_button")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (isFlashOn) Icons.Default.FlashOn else Icons.Default.FlashlightOff,
                                        contentDescription = "Linterna",
                                        tint = if (isFlashOn) Color(0xFF854D0E) else Color(0xFF1E293B),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Bottom strip inside Lector QR: Quick sample chips for instant tap-to-scan in emulator
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.45f))
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "Códigos de prueba (toca para simular escaneo):",
                            fontSize = 10.sp,
                            color = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(allProducts.take(8)) { prod ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (selectedProduct?.id == prod.id) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.18f),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { handleCodeSelect(prod.barcode) }
                                        .testTag("scan_chip_${prod.barcode}")
                                ) {
                                    Text(
                                        text = "${prod.name} (${prod.barcode})",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ==========================================
            // 4 ROUNDED PILL BARS (Stacked vertically)
            // ==========================================

            // BAR 1: Código (Scanned Code Input/Display)
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                shadowElevation = 1.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(CircleShape)
                    .testTag("pill_barcode")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = "Código",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )

                    BasicTextField(
                        value = enteredBarcode,
                        onValueChange = {
                            enteredBarcode = it
                            val match = allProducts.find { p -> p.barcode.equals(it.trim(), ignoreCase = true) }
                            if (match != null) {
                                selectedProduct = match
                            }
                        },
                        textStyle = TextStyle(
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        singleLine = true,
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("barcode_input_field"),
                        decorationBox = { innerTextField ->
                            if (enteredBarcode.isEmpty()) {
                                Text(
                                    text = "Código de barras o QR...",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                            }
                            innerTextField()
                        }
                    )

                    if (enteredBarcode.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                enteredBarcode = ""
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Borrar",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // BAR 2: Producto (Selected Product Name & Stock)
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                shadowElevation = 1.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(CircleShape)
                    .clickable { showProductPicker = true }
                    .testTag("pill_product")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShoppingBag,
                            contentDescription = "Producto",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = selectedProduct?.name ?: "Seleccionar producto...",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (selectedProduct != null) {
                                Text(
                                    text = "Categoría: ${selectedProduct?.category}",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    // Stock pill & Dropdown arrow
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        selectedProduct?.let { prod ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (prod.stock > 0) PosSuccess.copy(alpha = 0.15f) else PosError.copy(alpha = 0.15f),
                                modifier = Modifier.padding(end = 6.dp)
                            ) {
                                Text(
                                    text = if (prod.stock > 0) "Stock: ${prod.stock}" else "Agotado",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (prod.stock > 0) PosSuccess else PosError,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Ver catálogo",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // BAR 3: Cantidad y Precio (Qty Stepper & Subtotal)
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                shadowElevation = 1.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(CircleShape)
                    .testTag("pill_quantity_price")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Unit Price
                    val unitPrice = selectedProduct?.salePrice ?: 0.0
                    Text(
                        text = "${settings.currencySymbol} %.2f c/u".format(unitPrice),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Stepper: [-] [ quantity ] [+]
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .clickable {
                                    if (quantity > 1) quantity--
                                }
                                .testTag("stepper_decrease")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Remove,
                                    contentDescription = "Disminuir",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Text(
                            text = "$quantity",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 6.dp)
                        )

                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .clickable {
                                    val maxStock = selectedProduct?.stock ?: 999
                                    if (quantity < maxStock) quantity++
                                }
                                .testTag("stepper_increase")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Aumentar",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    // Line Subtotal
                    val lineTotal = unitPrice * quantity
                    Text(
                        text = "${settings.currencySymbol} %.2f".format(lineTotal),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // BAR 4: Botón de Acción Principal (Cobrar)
            val currentLineTotal = (selectedProduct?.salePrice ?: 0.0) * quantity
            val effectiveTotal = if (cartItems.isNotEmpty()) cartTotal else currentLineTotal

            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                shadowElevation = 3.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .clip(CircleShape)
                    .clickable {
                        // If cart is empty, add the currently selected product with its quantity
                        selectedProduct?.let { prod ->
                            if (prod.stock > 0) {
                                repeat(quantity) {
                                    viewModel.addToCart(prod)
                                }
                            }
                        }
                        viewModel.openCheckout()
                    }
                    .testTag("pill_checkout_button")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Payment,
                            contentDescription = "Cobrar",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = if (cartItems.size > 1) {
                                "Cobrar Carrito (${cartCount} unids)"
                            } else {
                                "Cobrar Ahora"
                            },
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        )
                    }

                    Text(
                        text = "${settings.currencySymbol} %.2f".format(effectiveTotal),
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 17.sp
                        )
                    )
                }
            }

            // Optional Cart Summary Strip (when cart has items)
            if (cartItems.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showCartBreakdown = !showCartBreakdown }
                        .padding(top = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShoppingCart,
                                contentDescription = "Carrito",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "${cartItems.size} tipos de producto en cola (${cartCount} unids)",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Text(
                            text = if (showCartBreakdown) "Ocultar" else "Ver detalle",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                if (showCartBreakdown) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.surface,
                                RoundedCornerShape(16.dp)
                            )
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                RoundedCornerShape(16.dp)
                            )
                            .padding(12.dp)
                    ) {
                        cartItems.forEach { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${item.quantity}x ${item.product.name}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "${settings.currencySymbol} %.2f".format(item.subtotal),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 6.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Text(
                                text = "Vaciar Carrito",
                                fontSize = 12.sp,
                                color = PosError,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { viewModel.clearCart() }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // ==========================================
        // PRODUCT SELECTOR MODAL (when tapping Bar 2)
        // ==========================================
        if (showProductPicker) {
            ProductPickerModal(
                products = allProducts,
                currencySymbol = settings.currencySymbol,
                onSelect = { prod ->
                    selectedProduct = prod
                    enteredBarcode = prod.barcode
                    quantity = 1
                    showProductPicker = false
                },
                onDismiss = { showProductPicker = false }
            )
        }
    }
}

// --------------------------------------------------------------------------
// CameraX Live Preview with Flashlight & Lens Controls
// --------------------------------------------------------------------------
@Composable
private fun CameraXLivePreview(
    isFlashOn: Boolean,
    lensFacing: Int,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                try {
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                    val cameraSelector = CameraSelector.Builder()
                        .requireLensFacing(lensFacing)
                        .build()

                    cameraProvider.unbindAll()
                    val camera: Camera = cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview
                    )
                    if (camera.cameraInfo.hasFlashUnit()) {
                        camera.cameraControl.enableTorch(isFlashOn)
                    }
                } catch (e: Exception) {
                    // Graceful fallback for emulators without hardware camera
                }
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        },
        update = { previewView ->
            try {
                val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                val cameraProvider = cameraProviderFuture.get()
                val cameraSelector = CameraSelector.Builder()
                    .requireLensFacing(lensFacing)
                    .build()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                cameraProvider.unbindAll()
                val camera: Camera = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview
                )
                if (camera.cameraInfo.hasFlashUnit()) {
                    camera.cameraControl.enableTorch(isFlashOn)
                }
            } catch (e: Exception) {
                // Keep smooth
            }
        },
        modifier = modifier
    )
}

// --------------------------------------------------------------------------
// Animated Scanner Laser Overlay with Corner Brackets
// --------------------------------------------------------------------------
@Composable
private fun ScannerOverlayLaser(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "scanner_laser")
    val laserYRatio by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_y"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Scanner bounding box
        val boxWidth = w * 0.68f
        val boxHeight = h * 0.65f
        val left = (w - boxWidth) / 2f
        val top = (h - boxHeight) / 2f
        val right = left + boxWidth
        val bottom = top + boxHeight

        val cornerLength = 28.dp.toPx()
        val strokeWidth = 3.5.dp.toPx()
        val cornerColor = Color(0xFF38BDF8) // Bright Cyan / PosSecondary

        // Top-Left Corner
        drawLine(cornerColor, Offset(left, top), Offset(left + cornerLength, top), strokeWidth)
        drawLine(cornerColor, Offset(left, top), Offset(left, top + cornerLength), strokeWidth)

        // Top-Right Corner
        drawLine(cornerColor, Offset(right, top), Offset(right - cornerLength, top), strokeWidth)
        drawLine(cornerColor, Offset(right, top), Offset(right, top + cornerLength), strokeWidth)

        // Bottom-Left Corner
        drawLine(cornerColor, Offset(left, bottom), Offset(left + cornerLength, bottom), strokeWidth)
        drawLine(cornerColor, Offset(left, bottom), Offset(left, bottom - cornerLength), strokeWidth)

        // Bottom-Right Corner
        drawLine(cornerColor, Offset(right, bottom), Offset(right - cornerLength, bottom), strokeWidth)
        drawLine(cornerColor, Offset(right, bottom), Offset(right, bottom - cornerLength), strokeWidth)

        // Animated Laser Beam
        val laserY = top + (boxHeight * laserYRatio)
        drawLine(
            color = Color(0xFFEF4444), // High-visibility Laser Red
            start = Offset(left + 8.dp.toPx(), laserY),
            end = Offset(right - 8.dp.toPx(), laserY),
            strokeWidth = 2.5.dp.toPx()
        )
    }
}

// --------------------------------------------------------------------------
// Product Picker Dialog for Pill 2
// --------------------------------------------------------------------------
@Composable
private fun ProductPickerModal(
    products: List<ProductEntity>,
    currencySymbol: String,
    onSelect: (ProductEntity) -> Unit,
    onDismiss: () -> Unit
) {
    var search by remember { mutableStateOf("") }
    val filtered = products.filter {
        search.isBlank() ||
                it.name.contains(search, ignoreCase = true) ||
                it.barcode.contains(search, ignoreCase = true) ||
                it.category.contains(search, ignoreCase = true)
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .height(480.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Seleccionar Producto",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Clear, contentDescription = "Cerrar")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    placeholder = { Text("Buscar por nombre, código o categoría...", fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(filtered) { product ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onSelect(product) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = product.name,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "Cod: ${product.barcode} • Stock: ${product.stock}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = "$currencySymbol %.2f".format(product.salePrice),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
