package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.data.model.DeliveryType
import com.example.data.model.OrderEntity
import com.example.data.model.OrderItemEntity
import java.net.URLEncoder
import java.text.NumberFormat
import java.util.Locale

object ChileanUtils {

    /**
     * Formats integer amount to Chilean Peso (CLP) with thousand separators.
     * Example: 3500 -> "$3.500 CLP"
     */
    fun formatClp(amount: Int): String {
        val formatter = NumberFormat.getNumberInstance(Locale("es", "CL"))
        return "$${formatter.format(amount)} CLP"
    }

    /**
     * Formats integer amount to short CLP string (e.g. "$3.500")
     */
    fun formatClpShort(amount: Int): String {
        val formatter = NumberFormat.getNumberInstance(Locale("es", "CL"))
        return "$${formatter.format(amount)}"
    }

    /**
     * Sanitizes Chilean phone number to international E.164 without plus or spaces for WhatsApp.
     * Example: "+56 9 8765 4321" -> "56987654321"
     */
    fun sanitizeChileanPhone(phone: String): String {
        val digits = phone.filter { it.isDigit() }
        return if (digits.startsWith("56")) {
            digits
        } else if (digits.length == 9) {
            "56$digits"
        } else if (digits.length == 8) {
            "569$digits"
        } else {
            digits
        }
    }

    /**
     * Validates and formats Chilean RUT (e.g. 12345678-K)
     */
    fun formatRut(raw: String): String {
        val clean = raw.replace(".", "").replace("-", "").trim().uppercase()
        if (clean.length < 2) return clean
        val body = clean.substring(0, clean.length - 1)
        val dv = clean.substring(clean.length - 1)
        return "$body-$dv"
    }

    /**
     * Builds structured Chilean WhatsApp Click-to-Chat message.
     */
    fun buildWhatsAppOrderMessage(
        companyName: String,
        order: OrderEntity,
        items: List<OrderItemEntity>
    ): String {
        val sb = StringBuilder()
        sb.append("🛒 *NUEVO PEDIDO #${order.orderNumber}*\n")
        sb.append("🍽️ *Local:* $companyName\n")
        sb.append("━━━━━━━━━━━━━━━━━━━━\n")
        sb.append("👤 *Cliente:* ${order.customerName}\n")
        sb.append("📞 *Teléfono:* ${order.customerPhone}\n")
        sb.append("🛵 *Modalidad:* ${order.deliveryType.label}\n")
        if (order.deliveryType == DeliveryType.DELIVERY && !order.customerAddress.isNullOrBlank()) {
            sb.append("📍 *Dirección:* ${order.customerAddress}\n")
        }
        if (!order.deliveryNotes.isNullOrBlank()) {
            sb.append("📝 *Observaciones:* ${order.deliveryNotes}\n")
        }
        sb.append("━━━━━━━━━━━━━━━━━━━━\n")
        sb.append("*Detalle del Pedido:*\n")

        items.forEach { item ->
            sb.append("▪ *${item.quantity}x ${item.productName}* (${formatClpShort(item.subtotal)})\n")
            if (item.modifiersDescription.isNotBlank()) {
                sb.append("   └ _${item.modifiersDescription}_\n")
            }
            if (!item.notes.isNullOrBlank()) {
                sb.append("   └ 💬 _Nota: ${item.notes}_\n")
            }
        }

        sb.append("━━━━━━━━━━━━━━━━━━━━\n")
        sb.append("Subtotal: ${formatClp(order.subtotalClp)}\n")
        if (order.deliveryFeeClp > 0) {
            sb.append("Despacho: ${formatClp(order.deliveryFeeClp)}\n")
        }
        if (order.discountClp > 0) {
            sb.append("Descuento: -${formatClp(order.discountClp)}\n")
        }
        sb.append("💰 *TOTAL A PAGAR:* *${formatClp(order.totalClp)}*\n")
        sb.append("💳 *Medio de Pago:* ${order.paymentMethod.label}\n")

        if (order.hasAlcohol) {
            sb.append("\n⚠️ *CONTIENE BEBIDAS CON ALCOHOL (Ley N° 19.925)*\n")
            sb.append("Se exigirá Cédula de Identidad física a mayor de 18 años al momento de la entrega.\n")
        }

        sb.append("━━━━━━━━━━━━━━━━━━━━\n")
        sb.append("Generado vía *MiPedido Express Chile* 🇨🇱\n")
        sb.append("Consulte el estado de su pedido con este mensaje.")
        return sb.toString()
    }

    /**
     * Launches WhatsApp intent or opens web browser with wa.me link.
     */
    fun openWhatsApp(context: Context, phone: String, message: String) {
        val cleanPhone = sanitizeChileanPhone(phone)
        val encodedMessage = try {
            URLEncoder.encode(message, "UTF-8")
        } catch (e: Exception) {
            Uri.encode(message)
        }
        val uri = Uri.parse("https://wa.me/$cleanPhone?text=$encodedMessage")
        val intent = Intent(Intent.ACTION_VIEW, uri)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback to browser
            val browserIntent = Intent(Intent.ACTION_VIEW, uri)
            browserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(browserIntent)
        }
    }
}
