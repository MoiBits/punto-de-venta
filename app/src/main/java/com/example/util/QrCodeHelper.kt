package com.example.util

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.io.ByteArrayOutputStream
import android.util.Base64

object QrCodeHelper {

    fun generateQrBitmap(
        content: String,
        sizePx: Int = 512,
        foregroundColor: Int = AndroidColor.BLACK,
        backgroundColor: Int = AndroidColor.WHITE
    ): Bitmap {
        val safeContent = content.ifBlank { "N/A" }
        return try {
            val hints = mapOf(
                EncodeHintType.CHARACTER_SET to "UTF-8",
                EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
                EncodeHintType.MARGIN to 1
            )
            val matrix = QRCodeWriter().encode(
                safeContent,
                BarcodeFormat.QR_CODE,
                sizePx,
                sizePx,
                hints
            )
            val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
            for (x in 0 until sizePx) {
                for (y in 0 until sizePx) {
                    bitmap.setPixel(x, y, if (matrix[x, y]) foregroundColor else backgroundColor)
                }
            }
            bitmap
        } catch (e: Exception) {
            val fallback = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
            fallback.eraseColor(backgroundColor)
            fallback
        }
    }

    fun generateQrBase64Png(content: String, sizePx: Int = 300): String {
        val bitmap = generateQrBitmap(content, sizePx)
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        val byteArray = outputStream.toByteArray()
        return "data:image/png;base64," + Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }
}

@Composable
fun QrCodeView(
    content: String,
    modifier: Modifier = Modifier,
    size: Dp = 160.dp,
    padding: Dp = 8.dp
) {
    val bitmap = remember(content) {
        QrCodeHelper.generateQrBitmap(content, sizePx = 400)
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .padding(padding),
        contentAlignment = Alignment.Center
    ) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "Código QR: $content",
            modifier = Modifier.fillMaxSize()
        )
    }
}
