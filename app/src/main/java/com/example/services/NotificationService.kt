package com.example.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R
import com.example.data.model.OrderStatus

object NotificationService {
    const val CHANNEL_ID = "mipedido_orders_channel"
    const val CHANNEL_NAME = "Estados de Pedidos"
    const val CHANNEL_DESC = "Notificaciones sobre el avance de pedidos, cocina y entrega"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESC
                enableVibration(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showOrderStatusNotification(
        context: Context,
        orderNumber: Int,
        companyName: String,
        newStatus: OrderStatus
    ) {
        createNotificationChannel(context)

        val (title, body) = when (newStatus) {
            OrderStatus.PENDING -> Pair(
                "🛒 Pedido #$orderNumber Recibido",
                "$companyName ha recibido tu pedido y está verificando la disponibilidad."
            )
            OrderStatus.CONFIRMED -> Pair(
                "✅ Pedido #$orderNumber Confirmado",
                "$companyName confirmó tu pedido. Entrará a cocina en breve."
            )
            OrderStatus.IN_PREPARATION -> Pair(
                "👨‍🍳 ¡En Cocina! Pedido #$orderNumber",
                "Tu comida está siendo preparada en la plancha de $companyName."
            )
            OrderStatus.READY -> Pair(
                "🥡 ¡Listo! Pedido #$orderNumber",
                "Tu pedido ya está listo para ser retirado o despachado."
            )
            OrderStatus.IN_DELIVERY -> Pair(
                "🛵 ¡En Reparto! Pedido #$orderNumber",
                "El repartidor va en camino a tu domicilio."
            )
            OrderStatus.DELIVERED -> Pair(
                "⭐ ¡Entregado! Pedido #$orderNumber",
                "¿Cómo estuvo tu experiencia? Toca para calificar con estrellas y dejar tu reseña."
            )
            OrderStatus.CANCELLED -> Pair(
                "❌ Pedido #$orderNumber Anulado",
                "Tu comanda ha sido cancelada por el local."
            )
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            orderNumber,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify(orderNumber, builder.build())
        } catch (e: SecurityException) {
            // Permission POST_NOTIFICATIONS might be denied on Android 13+
        } catch (e: Exception) {
            // Fallback
        }
    }
}
