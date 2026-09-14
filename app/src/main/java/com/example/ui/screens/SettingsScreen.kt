package com.example.ui.screens

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.LatAmCountryConfig
import com.example.data.model.LatAmPresets
import com.example.ui.PosViewModel
import com.example.ui.theme.PosPrimary
import com.example.ui.theme.PosSuccess
import com.example.ui.theme.PosWarning
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class CurrencyOption(val code: String, val symbol: String, val name: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: PosViewModel,
    onNavigateBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.settings.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val unsyncedCount by viewModel.unsyncedCount.collectAsState()

    var businessName by remember(settings) { mutableStateOf(settings.businessName) }
    var taxId by remember(settings) { mutableStateOf(settings.businessTaxId) }
    var address by remember(settings) { mutableStateOf(settings.businessAddress) }
    var phone by remember(settings) { mutableStateOf(settings.businessPhone) }

    val currencies = remember {
        listOf(
            CurrencyOption("BOB", "Bs.", "🇧🇴 BOB - Boliviano (Bs.)"),
            CurrencyOption("USD", "$", "🇺🇸 USD - Dólar Estadounidense ($)"),
            CurrencyOption("EUR", "€", "🇪🇺 EUR - Euro (€)"),
            CurrencyOption("MXN", "$", "🇲🇽 MXN - Peso Mexicano ($)"),
            CurrencyOption("COP", "$", "🇨🇴 COP - Peso Colombiano ($)"),
            CurrencyOption("ARS", "$", "🇦🇷 ARS - Peso Argentino ($)"),
            CurrencyOption("PEN", "S/.", "🇵🇪 PEN - Sol Peruano (S/.)"),
            CurrencyOption("CLP", "$", "🇨🇱 CLP - Peso Chileno ($)"),
            CurrencyOption("BRL", "R$", "🇧🇷 BRL - Real Brasileño (R$)"),
            CurrencyOption("PYG", "₲", "🇵🇾 PYG - Guaraní Paraguayo (₲)"),
            CurrencyOption("UYU", "\$U", "🇺🇾 UYU - Peso Uruguayo (\$U)"),
            CurrencyOption("GTQ", "Q", "🇬🇹 GTQ - Quetzal Guatemalteco (Q)"),
            CurrencyOption("CRC", "₡", "🇨🇷 CRC - Colón Costarricense (₡)"),
            CurrencyOption("DOP", "RD$", "🇩🇴 DOP - Peso Dominicano (RD$)"),
            CurrencyOption("PAB", "B/.", "🇵🇦 PAB - Balboa Panameño (B/.)"),
            CurrencyOption("HNL", "L", "🇭🇳 HNL - Lempira Hondureña (L)"),
            CurrencyOption("NIO", "C$", "🇳🇮 NIO - Córdoba Nicaragüense (C$)"),
            CurrencyOption("VES", "Bs.", "🇻🇪 VES - Bolívar Venezolano (Bs.)")
        )
    }

    var selectedCurrency by remember(settings) {
        mutableStateOf(currencies.find { it.code == settings.currencyCode } ?: currencies.first())
    }
    var currencyDropdownExpanded by remember { mutableStateOf(false) }

    var selectedCountry by remember(settings) {
        mutableStateOf(LatAmPresets.findByCurrencyCode(settings.currencyCode))
    }
    var countryDropdownExpanded by remember { mutableStateOf(false) }

    val taxRates = listOf(0.13, 0.16, 0.18, 0.19, 0.21, 0.15, 0.12, 0.08, 0.00)
    var selectedTaxRate by remember(settings) { mutableStateOf(settings.defaultTaxRate) }

    var paperWidth by remember(settings) { mutableStateOf(settings.thermalPaperWidthMm) }

    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())

    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (onNavigateBack != null) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .padding(end = 6.dp)
                            .testTag("settings_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                Column {
                    Text(
                        text = "Configuración",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 18.sp,
                            letterSpacing = (-0.3).sp
                        )
                    )
                    Text(
                        text = "Fiscal, moneda, hardware y sincronización",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            fontSize = 12.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Latin American Regional Presets Card
            Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                border = BorderStroke(0.6.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Public,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "REGIÓN LATINOAMERICANA",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "${selectedCountry.flag} ${selectedCountry.countryName}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Bolivia Quick Action Callout
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (selectedCountry.currencyCode == "BOB") MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(
                            1.dp,
                            if (selectedCountry.currencyCode == "BOB") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("🇧🇴", fontSize = 18.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Bolivia (Bolivianos - Bs.)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                                Text(
                                    "IVA 13% • NIT • SIN SIAT • QR Simple BCB",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Button(
                                onClick = {
                                    val bolivia = LatAmPresets.BOLIVIA
                                    selectedCountry = bolivia
                                    selectedCurrency = currencies.find { it.code == "BOB" } ?: currencies.first()
                                    selectedTaxRate = bolivia.defaultTaxRate
                                    businessName = bolivia.defaultBusinessName
                                    taxId = bolivia.sampleTaxId
                                    address = bolivia.defaultAddress
                                    phone = bolivia.defaultPhone
                                    viewModel.applyLatAmPreset(bolivia)
                                },
                                shape = RoundedCornerShape(6.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (selectedCountry.currencyCode == "BOB") PosSuccess else MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier
                                    .height(34.dp)
                                    .testTag("activate_bolivia_button")
                            ) {
                                if (selectedCountry.currencyCode == "BOB") {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Activo", fontSize = 11.sp)
                                } else {
                                    Text("Activar Bolivia", fontSize = 11.sp)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text("Seleccionar otro país de Latinoamérica:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(6.dp))

                    // Country Dropdown
                    ExposedDropdownMenuBox(
                        expanded = countryDropdownExpanded,
                        onExpandedChange = { countryDropdownExpanded = !countryDropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = "${selectedCountry.flag} ${selectedCountry.countryName} (${selectedCountry.currencyCode} ${selectedCountry.currencySymbol})",
                            onValueChange = {},
                            readOnly = true,
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
                            ),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = countryDropdownExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .menuAnchor()
                                .testTag("country_preset_selector")
                        )
                        ExposedDropdownMenu(
                            expanded = countryDropdownExpanded,
                            onDismissRequest = { countryDropdownExpanded = false }
                        ) {
                            LatAmPresets.ALL_COUNTRIES.forEach { country ->
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(country.flag, fontSize = 16.sp)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(
                                                    text = "${country.countryName} (${country.currencyCode} ${country.currencySymbol})",
                                                    fontSize = 12.sp,
                                                    fontWeight = if (country.currencyCode == selectedCountry.currencyCode) FontWeight.Bold else FontWeight.Normal
                                                )
                                                Text(
                                                    text = "${country.taxName} ${(country.defaultTaxRate * 100).toInt()}% • ${country.taxIdLabel} • ${country.taxAuthority}",
                                                    fontSize = 10.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    },
                                    onClick = {
                                        selectedCountry = country
                                        selectedCurrency = currencies.find { it.code == country.currencyCode }
                                            ?: CurrencyOption(country.currencyCode, country.currencySymbol, "${country.flag} ${country.currencyCode} - ${country.countryName} (${country.currencySymbol})")
                                        selectedTaxRate = country.defaultTaxRate
                                        businessName = country.defaultBusinessName
                                        taxId = country.sampleTaxId
                                        address = country.defaultAddress
                                        phone = country.defaultPhone
                                        countryDropdownExpanded = false
                                        viewModel.applyLatAmPreset(country)
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Regional Parameters Specs Summary
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(
                                text = "• Impuesto: ${selectedCountry.taxName} (${(selectedCountry.defaultTaxRate * 100).toInt()}%)",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "• Identificador: ${selectedCountry.taxIdLabel} | Entidad: ${selectedCountry.taxAuthority}",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "• Sistema Fiscal: ${selectedCountry.invoiceSystemName}",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "• Pagos QR: ${selectedCountry.qrPaymentName}",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Multi-currency Section
            Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                border = BorderStroke(0.6.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("SÍMBOLO Y CÓDIGO MONETARIO", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                    Spacer(modifier = Modifier.height(8.dp))

                    ExposedDropdownMenuBox(
                        expanded = currencyDropdownExpanded,
                        onExpandedChange = { currencyDropdownExpanded = !currencyDropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedCurrency.name,
                            onValueChange = {},
                            readOnly = true,
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
                            ),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = currencyDropdownExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .menuAnchor()
                                .testTag("currency_selector")
                        )
                        ExposedDropdownMenu(
                            expanded = currencyDropdownExpanded,
                            onDismissRequest = { currencyDropdownExpanded = false }
                        ) {
                            currencies.forEach { currency ->
                                DropdownMenuItem(
                                    text = { Text(currency.name, fontSize = 13.sp) },
                                    onClick = {
                                        selectedCurrency = currency
                                        currencyDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Fiscal & Business Info Section
            Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                border = BorderStroke(0.6.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("DATOS FISCALES DEL EMISOR", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                    Spacer(modifier = Modifier.height(10.dp))

                    MinimalTextField(
                        value = businessName,
                        onValueChange = { businessName = it },
                        label = "Nombre o Razón Social",
                        tag = "business_name_input"
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    MinimalTextField(
                        value = taxId,
                        onValueChange = { taxId = it.uppercase() },
                        label = "${selectedCountry.taxIdLabel} (${selectedCountry.countryName})",
                        tag = "business_tax_id_input"
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    MinimalTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = "Dirección",
                        tag = "business_address_input"
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    MinimalTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = "Teléfono",
                        tag = "business_phone_input"
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Default Tax Rate Selector
                    Text("Tasa ${selectedCountry.taxName} por defecto:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        taxRates.forEach { rate ->
                            val isSelected = kotlin.math.abs(rate - selectedTaxRate) < 0.005
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                border = if (isSelected) null else BorderStroke(0.6.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { selectedTaxRate = rate }
                            ) {
                                Text(
                                    text = "%.0f%%".format(rate * 100),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Thermal Printer Hardware
            Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                border = BorderStroke(0.6.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("IMPRESORA TÉRMICA ESC/POS", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (paperWidth == 80) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                            border = if (paperWidth == 80) null else BorderStroke(0.6.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { paperWidth = 80 }
                        ) {
                            Text(
                                text = "80 mm (Estándar)",
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                fontSize = 11.sp,
                                fontWeight = if (paperWidth == 80) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (paperWidth == 80) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (paperWidth == 58) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                            border = if (paperWidth == 58) null else BorderStroke(0.6.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { paperWidth = 58 }
                        ) {
                            Text(
                                text = "58 mm (Portátil)",
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                fontSize = 11.sp,
                                fontWeight = if (paperWidth == 58) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (paperWidth == 58) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = { viewModel.printThermalTicket() },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().testTag("test_print_button")
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Imprimir Ticket de Prueba", fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Cloud & Offline Engine Card
            Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                border = BorderStroke(0.6.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("NUBE Y SINCRONIZACIÓN", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))

                        if (isSyncing) {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 1.8.dp)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Última sincronización: ${sdf.format(Date(settings.lastSyncTimestamp))}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                    Text(
                        text = "Ventas pendientes: $unsyncedCount",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (unsyncedCount > 0) PosWarning else PosSuccess
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.toggleOfflineSimulation() },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).testTag("offline_toggle_settings")
                        ) {
                            Text(if (settings.isOfflineSimulated) "Activar Nube" else "Modo Offline", fontSize = 11.sp)
                        }

                        Button(
                            onClick = { viewModel.syncWithCloud() },
                            enabled = !isSyncing,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.weight(1.2f).testTag("sync_now_settings_button")
                        ) {
                            Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Sincronizar", fontSize = 11.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Save All Changes Button
            Button(
                onClick = {
                    viewModel.updateBusinessSettings(
                        businessName = businessName,
                        taxId = taxId,
                        address = address,
                        phone = phone,
                        currencySymbol = selectedCurrency.symbol,
                        currencyCode = selectedCurrency.code,
                        taxRate = selectedTaxRate,
                        paperWidth = paperWidth
                    )
                },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .testTag("save_all_settings_button")
            ) {
                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Guardar Configuración", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun MinimalTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    tag: String
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 11.sp) },
        singleLine = true,
        shape = RoundedCornerShape(8.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(tag)
    )
}
