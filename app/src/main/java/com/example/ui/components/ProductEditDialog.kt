package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Save
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.model.AppSettingsEntity
import com.example.data.model.ProductEntity
import com.example.ui.theme.PosError
import com.example.ui.theme.PosPrimary
import com.example.util.QrCodeView
import kotlin.random.Random

data class ProductImagePreset(val name: String, val url: String, val iconLabel: String)

val PRESET_IMAGES = listOf(
    ProductImagePreset(
        "Café",
        "https://images.unsplash.com/photo-1509042239860-f550ce710b93?w=300&q=80",
        "☕"
    ),
    ProductImagePreset(
        "Capuchino",
        "https://images.unsplash.com/photo-1572442388796-11668ba67e53?w=300&q=80",
        "🥛"
    ),
    ProductImagePreset(
        "Agua / Bebida",
        "https://images.unsplash.com/photo-1548839140-29a749e1bc4e?w=300&q=80",
        "🥤"
    ),
    ProductImagePreset(
        "Sándwich",
        "https://images.unsplash.com/photo-1528735602780-2552fd46c7af?w=300&q=80",
        "🥪"
    ),
    ProductImagePreset(
        "Croissant",
        "https://images.unsplash.com/photo-1555507036-ab1f4038808a?w=300&q=80",
        "🥐"
    ),
    ProductImagePreset(
        "Snacks",
        "https://images.unsplash.com/photo-1566478989037-eec170784d0b?w=300&q=80",
        "🍟"
    ),
    ProductImagePreset(
        "Barra Cereal",
        "https://images.unsplash.com/photo-1622484214777-a8775f0a8c2d?w=300&q=80",
        "🍫"
    ),
    ProductImagePreset(
        "Cable / Tech",
        "https://images.unsplash.com/photo-1588508065123-287b28e013da?w=300&q=80",
        "🔌"
    ),
    ProductImagePreset(
        "Cargador",
        "https://images.unsplash.com/photo-1583863788434-e58a36330cf0?w=300&q=80",
        "🔋"
    ),
    ProductImagePreset(
        "Higiene",
        "https://images.unsplash.com/photo-1584308666744-24d5c474f2ae?w=300&q=80",
        "🧴"
    ),
    ProductImagePreset(
        "Cuaderno",
        "https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?w=300&q=80",
        "📓"
    )
)

@Composable
fun ProductEditDialog(
    product: ProductEntity?,
    settings: AppSettingsEntity,
    onSave: (
        id: Long,
        barcode: String,
        name: String,
        category: String,
        costPrice: Double,
        salePrice: Double,
        stock: Int,
        minStock: Int,
        taxRate: Double,
        imageUrl: String?,
        qrCode: String?
    ) -> Unit,
    onDelete: ((ProductEntity) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    var name by remember(product) { mutableStateOf(product?.name ?: "") }
    var barcode by remember(product) {
        mutableStateOf(product?.barcode ?: (750000000000L + Random.nextLong(10000000, 99999999)).toString())
    }
    var category by remember(product) { mutableStateOf(product?.category ?: "General") }
    var costPrice by remember(product) { mutableStateOf(product?.costPrice?.toString() ?: "1.00") }
    var salePrice by remember(product) { mutableStateOf(product?.salePrice?.toString() ?: "2.50") }
    var stock by remember(product) { mutableStateOf(product?.stock?.toString() ?: "20") }
    var minStock by remember(product) { mutableStateOf(product?.minStock?.toString() ?: "5") }
    var imageUrl by remember(product) { mutableStateOf(product?.imageUrl ?: "") }
    var showUrlField by remember { mutableStateOf(false) }

    // Modern zero-permission Android Photo Picker
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            imageUrl = uri.toString()
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(6.dp)
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
                    Text(
                        text = if (product == null) "Nuevo Producto" else "Editar Producto",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_product_dialog")) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Cerrar")
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // ==========================================
                // SECTION: IMAGEN DEL PRODUCTO
                // ==========================================
                Text(
                    text = "Foto o Imagen del Producto",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Image Preview Box
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier
                            .size(76.dp)
                            .clip(RoundedCornerShape(12.dp))
                    ) {
                        if (imageUrl.isNotBlank()) {
                            AsyncImage(
                                model = imageUrl,
                                contentDescription = "Foto de $name",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Icon(
                                    imageVector = Icons.Default.Image,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    }

                    // Photo Action Buttons
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.fillMaxWidth().testTag("pick_product_photo_button")
                        ) {
                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Elegir de Galería", fontSize = 12.sp)
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (imageUrl.isNotBlank()) {
                                OutlinedButton(
                                    onClick = { imageUrl = "" },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = PosError),
                                    modifier = Modifier.weight(1f).testTag("remove_product_photo_button")
                                ) {
                                    Text("Quitar", fontSize = 11.sp)
                                }
                            }
                            OutlinedButton(
                                onClick = { showUrlField = !showUrlField },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(if (showUrlField) "Ocultar URL" else "URL", fontSize = 11.sp)
                            }
                        }
                    }
                }

                if (showUrlField) {
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = imageUrl,
                        onValueChange = { imageUrl = it },
                        label = { Text("URL de la imagen (http... o content://)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("product_image_url_input")
                    )
                }

                // Fast Image Presets
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "O elige una imagen predeterminada:",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(PRESET_IMAGES) { preset ->
                        val isSelected = imageUrl == preset.url
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(
                                if (isSelected) 1.5.dp else 0.5.dp,
                                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                            ),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { imageUrl = preset.url }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(preset.iconLabel, fontSize = 13.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    preset.name,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                // Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre del Producto") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("product_name_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Barcode + Auto-generate button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = barcode,
                        onValueChange = { barcode = it },
                        label = { Text("Código de Barras / SKU") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("product_barcode_input")
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(
                        onClick = {
                            barcode = (750000000000L + Random.nextLong(10000000, 99999999)).toString()
                        },
                        modifier = Modifier.testTag("generate_barcode_button")
                    ) {
                        Icon(Icons.Default.Autorenew, contentDescription = "Generar código aleatorio")
                    }
                }

                // ==========================================
                // SECTION: CÓDIGO QR DEL PRODUCTO
                // ==========================================
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    border = BorderStroke(0.6.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        QrCodeView(
                            content = barcode.ifBlank { "PROD" },
                            size = 56.dp,
                            padding = 4.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.QrCode,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Código QR activo", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Text(
                                text = "El POS puede escanear este QR con la cámara directamente.",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Contenido: $barcode",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Category
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Categoría (ej: Bebidas, Alimentos, Snacks)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("product_category_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Prices
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = costPrice,
                        onValueChange = { costPrice = it },
                        label = { Text("Precio Costo") },
                        prefix = { Text(settings.currencySymbol) },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("product_cost_input")
                    )
                    OutlinedTextField(
                        value = salePrice,
                        onValueChange = { salePrice = it },
                        label = { Text("Precio Venta") },
                        prefix = { Text(settings.currencySymbol) },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("product_price_input")
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Stocks
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = stock,
                        onValueChange = { stock = it },
                        label = { Text("Stock Actual") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("product_stock_input")
                    )
                    OutlinedTextField(
                        value = minStock,
                        onValueChange = { minStock = it },
                        label = { Text("Stock Mínimo") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("product_min_stock_input")
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (product != null && onDelete != null) {
                        OutlinedButton(
                            onClick = { onDelete(product) },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = PosError),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("delete_product_button")
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Eliminar", fontSize = 12.sp)
                        }
                    }

                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                onSave(
                                    product?.id ?: 0L,
                                    barcode,
                                    name,
                                    category,
                                    costPrice.toDoubleOrNull() ?: 0.0,
                                    salePrice.toDoubleOrNull() ?: 0.0,
                                    stock.toIntOrNull() ?: 0,
                                    minStock.toIntOrNull() ?: 5,
                                    settings.defaultTaxRate,
                                    imageUrl.ifBlank { null },
                                    barcode
                                )
                            }
                        },
                        enabled = name.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = PosPrimary),
                        modifier = Modifier
                            .weight(if (product != null && onDelete != null) 1.5f else 1f)
                            .testTag("save_product_button")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Guardar Producto", fontSize = 13.sp)
                    }
                }
            }
        }
    }
}
