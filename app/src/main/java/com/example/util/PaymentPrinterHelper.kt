package com.example.util

import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import com.example.data.model.AppSettingsEntity
import com.example.data.model.ProductEntity
import com.example.data.model.SaleEntity
import com.example.data.model.SaleItemEntity

object PaymentPrinterHelper {

    fun printPaymentReceipt(
        context: Context,
        sale: SaleEntity,
        items: List<SaleItemEntity>,
        settings: AppSettingsEntity,
        onComplete: (() -> Unit)? = null
    ) {
        val qrData = sale.invoiceUuid ?: "FOLIO:${sale.folio}|TOTAL:${sale.total}|FECHA:${sale.formattedDate()}"
        val qrBase64 = QrCodeHelper.generateQrBase64Png(qrData, sizePx = 240)

        val htmlContent = buildReceiptHtml(sale, items, settings, qrBase64)

        val webView = WebView(context)
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
                if (printManager != null) {
                    val jobName = "Comprobante_Pago_${sale.folio}"
                    val printAdapter = webView.createPrintDocumentAdapter(jobName)
                    val attributes = PrintAttributes.Builder()
                        .setMediaSize(PrintAttributes.MediaSize.ISO_A7)
                        .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                        .build()
                    printManager.print(jobName, printAdapter, attributes)
                    onComplete?.invoke()
                }
            }
        }
        webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
    }

    fun printProductLabel(
        context: Context,
        product: ProductEntity,
        settings: AppSettingsEntity
    ) {
        val qrContent = product.effectiveQrCode()
        val qrBase64 = QrCodeHelper.generateQrBase64Png(qrContent, sizePx = 280)

        val htmlContent = buildProductLabelHtml(product, settings, qrBase64)

        val webView = WebView(context)
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
                if (printManager != null) {
                    val jobName = "Etiqueta_QR_${product.barcode}"
                    val printAdapter = webView.createPrintDocumentAdapter(jobName)
                    val attributes = PrintAttributes.Builder()
                        .setMediaSize(PrintAttributes.MediaSize.ISO_A7)
                        .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                        .build()
                    printManager.print(jobName, printAdapter, attributes)
                }
            }
        }
        webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
    }

    private fun buildReceiptHtml(
        sale: SaleEntity,
        items: List<SaleItemEntity>,
        settings: AppSettingsEntity,
        qrBase64: String
    ): String {
        val itemsRows = items.joinToString("") { item ->
            """
            <tr>
                <td style="padding: 3px 0;">${item.productName}<br><span style="font-size: 10px; color: #555;">${item.quantity} x ${settings.currencySymbol} ${"%.2f".format(item.unitPrice)}</span></td>
                <td style="text-align: right; vertical-align: top; padding: 3px 0; font-weight: bold;">${settings.currencySymbol} ${"%.2f".format(item.subtotal)}</td>
            </tr>
            """.trimIndent()
        }

        val paymentLabel = when (sale.paymentMethod) {
            "CASH" -> "EFECTIVO"
            "CARD" -> "TARJETA BANCARIA"
            "QR_TRANSFER" -> "PAGO QR / TRANSFERENCIA"
            else -> sale.paymentMethod
        }

        return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8">
            <style>
                body {
                    font-family: 'Courier New', Courier, monospace, sans-serif;
                    width: 78mm;
                    margin: 0 auto;
                    padding: 8px;
                    color: #000;
                    background: #fff;
                    font-size: 11px;
                    line-height: 1.25;
                }
                .text-center { text-align: center; }
                .text-right { text-align: right; }
                .bold { font-weight: bold; }
                .divider { border-bottom: 1px dashed #000; margin: 6px 0; }
                .double-divider { border-bottom: 2px solid #000; margin: 6px 0; }
                table { width: 100%; border-collapse: collapse; font-size: 11px; }
                .qr-container { text-align: center; margin: 8px 0; }
                .qr-container img { width: 110px; height: 110px; }
                .header-title { font-size: 15px; font-weight: bold; letter-spacing: 0.5px; }
                .fiscal-box { font-size: 10px; color: #333; margin-top: 4px; }
            </style>
        </head>
        <body>
            <div class="text-center">
                <div class="header-title">${settings.businessName.uppercase()}</div>
                <div class="fiscal-box">
                    NIT / RFC: ${settings.businessTaxId}<br>
                    ${settings.businessAddress}<br>
                    Tel: ${settings.businessPhone}
                </div>
            </div>

            <div class="divider"></div>

            <div style="font-size: 10px;">
                <div><span class="bold">FOLIO:</span> ${sale.folio}</div>
                <div><span class="bold">FECHA:</span> ${sale.formattedDate()}</div>
                <div><span class="bold">CAJERO:</span> ${sale.cashierName}</div>
                <div><span class="bold">CLIENTE:</span> ${sale.customerName}</div>
                <div><span class="bold">NIT/CI:</span> ${sale.customerTaxId}</div>
            </div>

            <div class="divider"></div>

            <table>
                <thead>
                    <tr style="border-bottom: 1px solid #000;">
                        <th style="text-align: left; padding-bottom: 2px;">CANT / DESCRIPCIÓN</th>
                        <th style="text-align: right; padding-bottom: 2px;">TOTAL</th>
                    </tr>
                </thead>
                <tbody>
                    $itemsRows
                </tbody>
            </table>

            <div class="divider"></div>

            <table>
                <tr>
                    <td>SUBTOTAL:</td>
                    <td class="text-right">${settings.currencySymbol} ${"%.2f".format(sale.subtotal)}</td>
                </tr>
                <tr>
                    <td>IVA / IMPUESTO (${(settings.defaultTaxRate * 100).toInt()}%):</td>
                    <td class="text-right">${settings.currencySymbol} ${"%.2f".format(sale.taxTotal)}</td>
                </tr>
                <tr class="bold" style="font-size: 14px;">
                    <td style="padding-top: 4px;">TOTAL:</td>
                    <td class="text-right" style="padding-top: 4px;">${settings.currencySymbol} ${"%.2f".format(sale.total)} ${settings.currencyCode}</td>
                </tr>
            </table>

            <div class="divider"></div>

            <table>
                <tr>
                    <td>FORMA DE PAGO:</td>
                    <td class="text-right bold">$paymentLabel</td>
                </tr>
                <tr>
                    <td>IMPORTE PAGADO:</td>
                    <td class="text-right">${settings.currencySymbol} ${"%.2f".format(sale.amountPaid)}</td>
                </tr>
                <tr class="bold">
                    <td>CAMBIO / VUELTO:</td>
                    <td class="text-right">${settings.currencySymbol} ${"%.2f".format(sale.changeGiven)}</td>
                </tr>
            </table>

            <div class="qr-container">
                <img src="$qrBase64" alt="QR Comprobante" />
                <div style="font-size: 9px; letter-spacing: 1px;">VERIFICACIÓN FISCAL Y QR</div>
            </div>

            <div class="double-divider"></div>

            <div class="text-center" style="font-size: 10px;">
                ¡GRACIAS POR SU PREFERENCIA!<br>
                ESTE DOCUMENTO ES UN COMPROBANTE DE PAGO VÁLIDO
            </div>
        </body>
        </html>
        """.trimIndent()
    }

    private fun buildProductLabelHtml(
        product: ProductEntity,
        settings: AppSettingsEntity,
        qrBase64: String
    ): String {
        return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8">
            <style>
                body {
                    font-family: Arial, sans-serif;
                    width: 60mm;
                    margin: 0 auto;
                    padding: 8px;
                    text-align: center;
                    color: #000;
                }
                .business { font-size: 9px; text-transform: uppercase; color: #555; }
                .title { font-size: 13px; font-weight: bold; margin: 4px 0; }
                .category { font-size: 10px; background: #eee; padding: 2px 6px; border-radius: 4px; display: inline-block; }
                .qr-box { margin: 8px 0; }
                .qr-box img { width: 120px; height: 120px; }
                .price { font-size: 20px; font-weight: 900; margin-top: 4px; }
                .code { font-size: 11px; font-family: monospace; letter-spacing: 1px; color: #333; }
            </style>
        </head>
        <body>
            <div class="business">${settings.businessName}</div>
            <div class="title">${product.name}</div>
            <div class="category">${product.category.uppercase()}</div>
            
            <div class="qr-box">
                <img src="$qrBase64" alt="Código QR Producto">
            </div>

            <div class="code">SKU: ${product.barcode}</div>
            <div class="price">${settings.currencySymbol} ${"%.2f".format(product.salePrice)} ${settings.currencyCode}</div>
        </body>
        </html>
        """.trimIndent()
    }

    fun formatThermalEscPosText(
        sale: SaleEntity,
        items: List<SaleItemEntity>,
        settings: AppSettingsEntity
    ): String {
        val sb = StringBuilder()
        val width = if (settings.thermalPaperWidthMm <= 58) 32 else 42

        fun center(text: String): String {
            if (text.length >= width) return text
            val padding = (width - text.length) / 2
            return " ".repeat(padding) + text
        }

        fun row(left: String, right: String): String {
            val space = width - left.length - right.length
            return if (space > 0) left + " ".repeat(space) + right else "$left $right"
        }

        sb.appendLine(center(settings.businessName))
        sb.appendLine(center("NIT: ${settings.businessTaxId}"))
        sb.appendLine(center(settings.businessAddress))
        sb.appendLine(center("Tel: ${settings.businessPhone}"))
        sb.appendLine("=".repeat(width))
        sb.appendLine(row("FOLIO: ${sale.folio}", sale.formattedDate()))
        sb.appendLine(row("CLIENTE: ${sale.customerName}", "NIT: ${sale.customerTaxId}"))
        sb.appendLine(row("CAJERO: ${sale.cashierName}", ""))
        sb.appendLine("-".repeat(width))
        sb.appendLine(row("CANT DESCRIPCIÓN", "TOTAL"))
        sb.appendLine("-".repeat(width))

        items.forEach { item ->
            sb.appendLine("${item.quantity}x ${item.productName}")
            sb.appendLine(row("  @ ${settings.currencySymbol} ${"%.2f".format(item.unitPrice)}", "${settings.currencySymbol} ${"%.2f".format(item.subtotal)}"))
        }

        sb.appendLine("-".repeat(width))
        sb.appendLine(row("SUBTOTAL:", "${settings.currencySymbol} ${"%.2f".format(sale.subtotal)}"))
        sb.appendLine(row("IVA (${(settings.defaultTaxRate * 100).toInt()}%):", "${settings.currencySymbol} ${"%.2f".format(sale.taxTotal)}"))
        sb.appendLine(row("TOTAL:", "${settings.currencySymbol} ${"%.2f".format(sale.total)} ${settings.currencyCode}"))
        sb.appendLine("-".repeat(width))
        sb.appendLine(row("PAGO CON:", sale.paymentMethod))
        sb.appendLine(row("RECIBIDO:", "${settings.currencySymbol} ${"%.2f".format(sale.amountPaid)}"))
        sb.appendLine(row("CAMBIO:", "${settings.currencySymbol} ${"%.2f".format(sale.changeGiven)}"))
        sb.appendLine("=".repeat(width))
        sb.appendLine(center("¡GRACIAS POR SU COMPRA!"))
        sb.appendLine("\n\n")

        return sb.toString()
    }
}
