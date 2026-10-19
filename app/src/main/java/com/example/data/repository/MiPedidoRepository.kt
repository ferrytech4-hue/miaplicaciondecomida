package com.example.data.repository

import android.content.Context
import com.example.data.local.CatalogDao
import com.example.data.local.CompanyDao
import com.example.data.local.NotificationDao
import com.example.data.local.OrderDao
import com.example.data.local.PosDao
import com.example.data.local.ReviewDao
import com.example.data.model.AppNotificationEntity
import com.example.data.model.CashMovementEntity
import com.example.data.model.CashShiftEntity
import com.example.data.model.CashShiftStatus
import com.example.data.model.CategoryEntity
import com.example.data.model.CompanyEntity
import com.example.data.model.DeliveryType
import com.example.data.model.ModifierGroupEntity
import com.example.data.model.ModifierOptionEntity
import com.example.data.model.MovementType
import com.example.data.model.OrderEntity
import com.example.data.model.OrderItemEntity
import com.example.data.model.OrderStatus
import com.example.data.model.OrderStatusHistoryEntity
import com.example.data.model.PaymentMethod
import com.example.data.model.ProductEntity
import com.example.data.model.ProductReviewEntity
import com.example.data.model.ReviewEntity
import com.example.services.NotificationService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

data class CartItem(
    val product: ProductEntity,
    val quantity: Int,
    val selectedOptions: List<ModifierOptionEntity> = emptyList(),
    val notes: String? = null
) {
    val unitPrice: Int get() = product.basePrice + selectedOptions.sumOf { it.priceDelta }
    val subtotal: Int get() = unitPrice * quantity
    val modifiersDescription: String
        get() = selectedOptions.joinToString(", ") {
            if (it.priceDelta > 0) "${it.name} (+${it.priceDelta})" else it.name
        }
}

data class OrderWithDetails(
    val order: OrderEntity,
    val items: List<OrderItemEntity>,
    val history: List<OrderStatusHistoryEntity> = emptyList()
)

class MiPedidoRepository(
    private val companyDao: CompanyDao,
    private val catalogDao: CatalogDao,
    private val orderDao: OrderDao,
    private val posDao: PosDao,
    private val reviewDao: ReviewDao,
    private val notificationDao: NotificationDao,
    private val context: Context? = null
) {
    // Companies
    fun getPublishedCompanies(): Flow<List<CompanyEntity>> = companyDao.getPublishedCompanies()
    fun getAllCompanies(): Flow<List<CompanyEntity>> = companyDao.getAllCompanies()
    fun getCompanyBySlug(slug: String): Flow<CompanyEntity?> = companyDao.getCompanyBySlug(slug)
    fun getCompanyById(id: Long): Flow<CompanyEntity?> = companyDao.getCompanyById(id)
    suspend fun getCompanyByIdSync(id: Long): CompanyEntity? = companyDao.getCompanyByIdSync(id)

    suspend fun createCompany(company: CompanyEntity): Long = companyDao.insertCompany(company)
    suspend fun updateCompany(company: CompanyEntity) = companyDao.updateCompany(company)

    // Catalog
    fun getCategoriesByCompany(companyId: Long): Flow<List<CategoryEntity>> =
        catalogDao.getCategoriesByCompany(companyId)

    fun getProductsByCompany(companyId: Long): Flow<List<ProductEntity>> =
        catalogDao.getProductsByCompany(companyId)

    suspend fun getProductModifiers(productId: Long): Pair<List<ModifierGroupEntity>, Map<Long, List<ModifierOptionEntity>>> {
        val groups = catalogDao.getModifierGroups(productId)
        val optionsMap = mutableMapOf<Long, List<ModifierOptionEntity>>()
        for (g in groups) {
            optionsMap[g.id] = catalogDao.getModifierOptions(g.id)
        }
        return Pair(groups, optionsMap)
    }

    suspend fun toggleProductAvailability(product: ProductEntity) {
        catalogDao.updateProduct(product.copy(isAvailable = !product.isAvailable))
    }

    suspend fun createProduct(product: ProductEntity): Long = catalogDao.insertProduct(product)
    suspend fun createCategory(category: CategoryEntity): Long = catalogDao.insertCategory(category)

    // Orders & Placement
    fun getOrdersByCompany(companyId: Long): Flow<List<OrderEntity>> =
        orderDao.getOrdersByCompany(companyId)

    fun getActiveOrdersForKds(companyId: Long): Flow<List<OrderEntity>> =
        orderDao.getActiveOrdersForKds(companyId)

    suspend fun getOrderWithDetails(orderId: Long): OrderWithDetails? {
        val order = orderDao.getOrderById(orderId) ?: return null
        val items = orderDao.getOrderItems(orderId)
        val history = orderDao.getOrderStatusHistory(orderId)
        return OrderWithDetails(order, items, history)
    }

    suspend fun getItemsForOrders(orderIds: List<Long>): Map<Long, List<OrderItemEntity>> {
        if (orderIds.isEmpty()) return emptyMap()
        val allItems = orderDao.getOrderItemsForOrders(orderIds)
        return allItems.groupBy { it.orderId }
    }

    suspend fun placeOrder(
        company: CompanyEntity,
        cartItems: List<CartItem>,
        customerName: String,
        customerPhone: String,
        customerAddress: String?,
        deliveryNotes: String?,
        deliveryType: DeliveryType,
        paymentMethod: PaymentMethod,
        isAdultConfirmed: Boolean
    ): OrderWithDetails {
        require(cartItems.isNotEmpty()) { "El carrito no puede estar vacío." }

        var subtotal = 0
        var hasAlcohol = false

        for (item in cartItems) {
            subtotal += item.subtotal
            if (item.product.isAlcohol) {
                hasAlcohol = true
            }
        }

        if (hasAlcohol && !isAdultConfirmed) {
            throw IllegalArgumentException("El pedido contiene bebidas alcohólicas. Bajo la Ley N° 19.925 se exige confirmación de mayoría de edad.")
        }

        val deliveryFee = if (deliveryType == DeliveryType.DELIVERY) company.deliveryFeeBase else 0
        val total = subtotal + deliveryFee

        val maxNum = orderDao.getMaxOrderNumber(company.id) ?: 100
        val nextOrderNumber = maxNum + 1

        val orderEntity = OrderEntity(
            companyId = company.id,
            orderNumber = nextOrderNumber,
            customerName = customerName,
            customerPhone = customerPhone,
            customerAddress = customerAddress,
            deliveryNotes = deliveryNotes,
            hasAlcohol = hasAlcohol,
            isAdultConfirmed = isAdultConfirmed,
            deliveryType = deliveryType,
            subtotalClp = subtotal,
            deliveryFeeClp = deliveryFee,
            totalClp = total,
            status = OrderStatus.PENDING,
            paymentMethod = paymentMethod,
            isPaid = false
        )

        val orderId = orderDao.insertOrder(orderEntity)

        val createdItems = mutableListOf<OrderItemEntity>()
        for (item in cartItems) {
            val orderItem = OrderItemEntity(
                orderId = orderId,
                productId = item.product.id,
                productName = item.product.name,
                unitPrice = item.unitPrice,
                quantity = item.quantity,
                subtotal = item.subtotal,
                notes = item.notes,
                modifiersDescription = item.modifiersDescription
            )
            val itemId = orderDao.insertOrderItem(orderItem)
            createdItems.add(orderItem.copy(id = itemId))
        }

        orderDao.insertOrderStatusHistory(
            OrderStatusHistoryEntity(
                orderId = orderId,
                previousStatus = null,
                newStatus = OrderStatus.PENDING.name,
                changedByRole = "Cliente (App)",
                comment = "Pedido ingresado por checkout digital"
            )
        )

        val savedOrder = orderEntity.copy(id = orderId)
        return OrderWithDetails(savedOrder, createdItems)
    }

    suspend fun updateOrderStatus(
        orderId: Long,
        newStatus: OrderStatus,
        changedByRole: String,
        comment: String? = null
    ) {
        val current = orderDao.getOrderById(orderId) ?: return
        orderDao.updateOrderStatus(orderId, newStatus)
        orderDao.insertOrderStatusHistory(
            OrderStatusHistoryEntity(
                orderId = orderId,
                previousStatus = current.status.name,
                newStatus = newStatus.name,
                changedByRole = changedByRole,
                comment = comment ?: "Estado actualizado a ${newStatus.label}"
            )
        )

        val company = companyDao.getCompanyByIdSync(current.companyId)
        val companyName = company?.commercialName ?: "Tu Local"

        // Backend push notification trigger logic
        val notifTitle = when (newStatus) {
            OrderStatus.IN_PREPARATION -> "👨‍🍳 ¡En Cocina! Pedido #${current.orderNumber}"
            OrderStatus.READY -> "🥡 ¡Listo! Pedido #${current.orderNumber}"
            OrderStatus.IN_DELIVERY -> "🛵 ¡En Reparto! Pedido #${current.orderNumber}"
            OrderStatus.DELIVERED -> "⭐ ¡Entregado! Pedido #${current.orderNumber}"
            OrderStatus.CANCELLED -> "❌ Pedido #${current.orderNumber} Anulado"
            OrderStatus.CONFIRMED -> "✅ Pedido #${current.orderNumber} Confirmado"
            OrderStatus.PENDING -> "🛒 Pedido #${current.orderNumber} Recibido"
        }

        val notifMessage = when (newStatus) {
            OrderStatus.IN_PREPARATION -> "$companyName comenzó la preparación en plancha de tu pedido #${current.orderNumber}."
            OrderStatus.READY -> "Tu pedido #${current.orderNumber} está listo para ser retirado o despachado."
            OrderStatus.IN_DELIVERY -> "El repartidor va en camino a tu domicilio."
            OrderStatus.DELIVERED -> "Tu pedido #${current.orderNumber} fue entregado exitosamente. ¿Cómo estuvo todo? Toca para calificar con estrellas."
            OrderStatus.CANCELLED -> "Tu pedido #${current.orderNumber} fue cancelado por el restaurante."
            OrderStatus.CONFIRMED -> "$companyName confirmó tu pedido."
            OrderStatus.PENDING -> "Tu comanda ingresó al sistema del local."
        }

        notificationDao.insertNotification(
            AppNotificationEntity(
                companyId = current.companyId,
                orderId = current.id,
                orderNumber = current.orderNumber,
                title = notifTitle,
                message = notifMessage,
                type = if (newStatus == OrderStatus.DELIVERED) "REVIEW_REQUEST" else "ORDER_STATUS"
            )
        )

        // Show Android system push notification
        if (context != null) {
            NotificationService.showOrderStatusNotification(
                context = context,
                orderNumber = current.orderNumber,
                companyName = companyName,
                newStatus = newStatus
            )
        }
    }

    suspend fun updateOrderPayment(orderId: Long, isPaid: Boolean, shiftId: Long? = null) {
        orderDao.updateOrderPayment(orderId, isPaid)
        if (isPaid && shiftId != null) {
            val order = orderDao.getOrderById(orderId)
            if (order != null) {
                posDao.insertMovement(
                    CashMovementEntity(
                        shiftId = shiftId,
                        orderId = orderId,
                        movementType = MovementType.VENTA,
                        amount = order.totalClp,
                        reason = "Cobro de Pedido #${order.orderNumber} (${order.paymentMethod.label})"
                    )
                )
            }
        }
    }

    // POS & Cash Management
    fun getActiveShift(companyId: Long): Flow<CashShiftEntity?> = posDao.getActiveShift(companyId)
    fun getShiftsByCompany(companyId: Long): Flow<List<CashShiftEntity>> = posDao.getShiftsByCompany(companyId)
    fun getMovementsByShift(shiftId: Long): Flow<List<CashMovementEntity>> = posDao.getMovementsByShift(shiftId)

    suspend fun openShift(companyId: Long, cashierName: String, initialBalance: Int): Long {
        val shift = CashShiftEntity(
            companyId = companyId,
            cashierName = cashierName,
            initialBalance = initialBalance,
            status = CashShiftStatus.OPEN
        )
        val shiftId = posDao.insertShift(shift)
        posDao.insertMovement(
            CashMovementEntity(
                shiftId = shiftId,
                movementType = MovementType.INGRESO,
                amount = initialBalance,
                reason = "Apertura de turno - Fondo inicial de caja"
            )
        )
        return shiftId
    }

    suspend fun registerCashMovement(shiftId: Long, type: MovementType, amount: Int, reason: String) {
        posDao.insertMovement(
            CashMovementEntity(
                shiftId = shiftId,
                movementType = type,
                amount = if (type == MovementType.EGRESO || type == MovementType.ANULACION) -kotlin.math.abs(amount) else kotlin.math.abs(amount),
                reason = reason
            )
        )
    }

    suspend fun closeShift(shiftId: Long, declaredCash: Int, notes: String?) {
        val shift = posDao.getShiftsByCompany(1).firstOrNull()?.find { it.id == shiftId } ?: return
        val movements = posDao.getMovementsByShift(shiftId).firstOrNull() ?: emptyList()
        val systemCash = movements.sumOf { it.amount }
        val difference = declaredCash - systemCash

        val updated = shift.copy(
            closedAt = System.currentTimeMillis(),
            declaredCashAtClose = declaredCash,
            systemCashAtClose = systemCash,
            cashDifference = difference,
            status = CashShiftStatus.CLOSED,
            closingNotes = notes
        )
        posDao.updateShift(updated)
    }

    // Reviews & Ratings (1 to 5 Stars)
    fun getReviewsByCompany(companyId: Long): Flow<List<ReviewEntity>> =
        reviewDao.getReviewsByCompany(companyId)

    fun getAverageRatingForCompany(companyId: Long): Flow<Float?> =
        reviewDao.getAverageRatingForCompany(companyId)

    fun getReviewCountForCompany(companyId: Long): Flow<Int> =
        reviewDao.getReviewCountForCompany(companyId)

    suspend fun createCompanyReview(
        companyId: Long,
        orderId: Long?,
        customerName: String,
        rating: Int,
        comment: String
    ): Long {
        return reviewDao.insertReview(
            ReviewEntity(
                companyId = companyId,
                orderId = orderId,
                customerName = customerName,
                rating = rating.coerceIn(1, 5),
                comment = comment
            )
        )
    }

    suspend fun replyToReview(reviewId: Long, reply: String) {
        reviewDao.updateOwnerReply(reviewId, reply, System.currentTimeMillis())
    }

    fun getProductReviews(productId: Long): Flow<List<ProductReviewEntity>> =
        reviewDao.getProductReviews(productId)

    fun getAverageRatingForProduct(productId: Long): Flow<Float?> =
        reviewDao.getAverageRatingForProduct(productId)

    suspend fun createProductReview(
        productId: Long,
        companyId: Long,
        orderId: Long?,
        customerName: String,
        rating: Int,
        comment: String
    ): Long {
        return reviewDao.insertProductReview(
            ProductReviewEntity(
                productId = productId,
                companyId = companyId,
                orderId = orderId,
                customerName = customerName,
                rating = rating.coerceIn(1, 5),
                comment = comment
            )
        )
    }

    // Push Notification History Center
    fun getAllNotifications(): Flow<List<AppNotificationEntity>> =
        notificationDao.getAllNotifications()

    fun getUnreadNotificationsCount(): Flow<Int> =
        notificationDao.getUnreadCount()

    suspend fun markAllNotificationsAsRead() {
        notificationDao.markAllAsRead()
    }

    suspend fun markNotificationAsRead(id: Long) {
        notificationDao.markAsRead(id)
    }
}
