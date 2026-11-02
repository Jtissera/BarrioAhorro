package com.barrioahorro.app.ui.screens.client.coupons

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.barrioahorro.app.data.explore.RedemptionDto
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class RedemptionStatus(val label: String, val container: Color, val content: Color) {
    PENDING("Pendiente", Color(0xFFFBE7B0), Color(0xFF7A5B00)),
    VALIDATED("Validado", Color(0xFFDDF0E3), Color(0xFF2E7D4F)),
    CANCELLED("Cancelado", Color(0xFFE6DDD0), Color(0xFF6F6559)),
    EXPIRED("Vencido", Color(0xFFE6DDD0), Color(0xFF6F6559)),
    UNKNOWN("—", Color(0xFFE6DDD0), Color(0xFF6F6559)),
}

data class RedemptionUi(
    val id: Long,
    val receiptCode: String,
    /** Contenido del QR; solo existe mientras el canje está pendiente. */
    val code: String?,
    val status: RedemptionStatus,
    val productName: String,
    val photoUrl: String?,
    val businessName: String,
    val pointsCost: Int,
    val validUntil: String,
    val redeemedAt: String,
    val validatedAt: String?,
)

fun RedemptionDto.toUi() = RedemptionUi(
    id = id,
    receiptCode = receiptCode,
    code = code,
    status = when (status) {
        "PENDIENTE_VALIDACION" -> RedemptionStatus.PENDING
        "VALIDADO" -> RedemptionStatus.VALIDATED
        "CANCELADO" -> RedemptionStatus.CANCELLED
        "VENCIDO" -> RedemptionStatus.EXPIRED
        else -> RedemptionStatus.UNKNOWN
    },
    productName = productName,
    photoUrl = photoUrl,
    businessName = businessName ?: "Comercio del barrio",
    pointsCost = pointsCost,
    validUntil = formatDay(validUntil),
    redeemedAt = formatDateTime(redeemedAt),
    validatedAt = validatedAt?.let(::formatDateTime),
)

private val AR_LOCALE = Locale("es", "AR")
private val AR_ZONE = ZoneId.of("America/Argentina/Buenos_Aires")

private fun formatDay(raw: String): String = runCatching {
    LocalDate.parse(raw).format(DateTimeFormatter.ofPattern("d 'de' MMMM", AR_LOCALE))
}.getOrDefault(raw)

private fun formatDateTime(raw: String): String = runCatching {
    OffsetDateTime.parse(raw).atZoneSameInstant(AR_ZONE)
        .format(DateTimeFormatter.ofPattern("d 'de' MMMM, HH:mm", AR_LOCALE))
}.getOrDefault(raw)

@Composable
fun StatusChip(status: RedemptionStatus, modifier: Modifier = Modifier) {
    Surface(shape = RoundedCornerShape(50), color = status.container, modifier = modifier) {
        Text(
            status.label,
            color = status.content,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

@Composable
fun QrCode(content: String, modifier: Modifier = Modifier) {
    val bitmap = remember(content) { buildQr(content) }
    Image(bitmap = bitmap, contentDescription = "Código QR del canje", modifier = modifier)
}

private fun buildQr(content: String, size: Int = 640): ImageBitmap {
    val matrix = QRCodeWriter().encode(
        content,
        BarcodeFormat.QR_CODE,
        size,
        size,
        mapOf(EncodeHintType.MARGIN to 1),
    )
    val pixels = IntArray(size * size)
    for (y in 0 until size) {
        for (x in 0 until size) {
            pixels[y * size + x] = if (matrix.get(x, y)) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
        }
    }
    return Bitmap.createBitmap(pixels, size, size, Bitmap.Config.ARGB_8888).asImageBitmap()
}