package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class PlanType {
    INICIAL,
    PROFESIONAL,
    AVANZADO
}

enum class DeliveryType(val label: String) {
    DELIVERY("Envío a Domicilio"),
    PICKUP("Retiro en Local"),
    DINE_IN("Consumo en Mesa")
}

enum class PaymentMethod(val label: String) {
    EFECTIVO("Efectivo al recibir"),
    TRANSBANK_DEBITO("Débito / Redcompra"),
    TRANSBANK_CREDITO("Tarjeta de Crédito"),
    TRANSFERENCIA("Transferencia Bancaria"),
    PENDIENTE_AL_ENTREGAR("Pago contra Entrega")
}

enum class OrderStatus(val label: String) {
    PENDING("Pendiente"),
    CONFIRMED("Confirmado"),
    IN_PREPARATION("En Preparación"),
    READY("Listo para Retiro/Envío"),
    IN_DELIVERY("En Reparto"),
    DELIVERED("Entregado"),
    CANCELLED("Anulado")
}

enum class UserRole(val label: String) {
    SUPERADMIN("Superadministrador"),
    OWNER("Dueño / Administrador"),
    CASHIER("Cajero (POS)"),
    COOK("Cocinero (KDS)"),
    DELIVERY("Repartidor")
}

enum class CashShiftStatus {
    OPEN,
    CLOSED
}

enum class MovementType(val label: String) {
    INGRESO("Ingreso de Efectivo"),
    EGRESO("Egreso de Efectivo"),
    VENTA("Venta Registrada"),
    ANULACION("Anulación de Venta")
}

@Entity(
    tableName = "companies",
    indices = [Index(value = ["slug"], unique = true)]
)
data class CompanyEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val slug: String,
    val legalName: String,
    val rut: String,
    val commercialName: String,
    val contactEmail: String,
    val contactPhone: String,
    val whatsappNumber: String,
    val logoUrl: String,
    val bannerUrl: String,
    val primaryColorHex: String = "#D32F2F",
    val secondaryColorHex: String = "#FF9800",
    val category: String = "Comida Rápida",
    val isPublished: Boolean = true,
    val isOpenManual: Boolean = true,
    val pickupEnabled: Boolean = true,
    val deliveryEnabled: Boolean = true,
    val deliveryFeeBase: Int = 1500,
    val minOrderAmount: Int = 5000,
    val estimatedTimeMin: Int = 30,
    val subscriptionPlan: PlanType = PlanType.PROFESIONAL,
    val wabaPhoneNumberId: String? = null,
    val wabaAccessToken: String? = null
)

@Entity(
    tableName = "categories",
    foreignKeys = [
        ForeignKey(
            entity = CompanyEntity::class,
            parentColumns = ["id"],
            childColumns = ["companyId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("companyId")]
)
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val companyId: Long,
    val name: String,
    val sortOrder: Int = 0,
    val isActive: Boolean = true
)

@Entity(
    tableName = "products",
    foreignKeys = [
        ForeignKey(
            entity = CompanyEntity::class,
            parentColumns = ["id"],
            childColumns = ["companyId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("companyId"), Index("categoryId")]
)
data class ProductEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val companyId: Long,
    val categoryId: Long,
    val name: String,
    val description: String,
    val basePrice: Int,
    val imageUrl: String,
    val isAvailable: Boolean = true,
    val isAlcohol: Boolean = false,
    val sku: String? = null
)

@Entity(
    tableName = "product_modifier_groups",
    foreignKeys = [
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["id"],
            childColumns = ["productId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("productId")]
)
data class ModifierGroupEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val productId: Long,
    val name: String,
    val isRequired: Boolean = false,
    val minSelection: Int = 0,
    val maxSelection: Int = 1
)

@Entity(
    tableName = "product_modifier_options",
    foreignKeys = [
        ForeignKey(
            entity = ModifierGroupEntity::class,
            parentColumns = ["id"],
            childColumns = ["groupId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("groupId")]
)
data class ModifierOptionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val groupId: Long,
    val name: String,
    val priceDelta: Int = 0,
    val isAvailable: Boolean = true
)

@Entity(
    tableName = "orders",
    foreignKeys = [
        ForeignKey(
            entity = CompanyEntity::class,
            parentColumns = ["id"],
            childColumns = ["companyId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("companyId"), Index("status")]
)
data class OrderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val companyId: Long,
    val orderNumber: Int,
    val customerName: String,
    val customerPhone: String,
    val customerAddress: String?,
    val deliveryNotes: String?,
    val hasAlcohol: Boolean = false,
    val isAdultConfirmed: Boolean = false,
    val deliveryType: DeliveryType,
    val subtotalClp: Int,
    val deliveryFeeClp: Int = 0,
    val discountClp: Int = 0,
    val totalClp: Int,
    val status: OrderStatus = OrderStatus.PENDING,
    val paymentMethod: PaymentMethod,
    val isPaid: Boolean = false,
    val deliveryUserId: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "order_items",
    foreignKeys = [
        ForeignKey(
            entity = OrderEntity::class,
            parentColumns = ["id"],
            childColumns = ["orderId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("orderId")]
)
data class OrderItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val orderId: Long,
    val productId: Long?,
    val productName: String,
    val unitPrice: Int,
    val quantity: Int,
    val subtotal: Int,
    val notes: String? = null,
    val modifiersDescription: String = ""
)

@Entity(
    tableName = "order_status_history",
    foreignKeys = [
        ForeignKey(
            entity = OrderEntity::class,
            parentColumns = ["id"],
            childColumns = ["orderId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("orderId")]
)
data class OrderStatusHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val orderId: Long,
    val previousStatus: String?,
    val newStatus: String,
    val changedByRole: String,
    val comment: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "cash_shifts",
    foreignKeys = [
        ForeignKey(
            entity = CompanyEntity::class,
            parentColumns = ["id"],
            childColumns = ["companyId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("companyId")]
)
data class CashShiftEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val companyId: Long,
    val cashierName: String,
    val openedAt: Long = System.currentTimeMillis(),
    val closedAt: Long? = null,
    val initialBalance: Int = 50000,
    val declaredCashAtClose: Int? = null,
    val systemCashAtClose: Int? = null,
    val cashDifference: Int? = null,
    val status: CashShiftStatus = CashShiftStatus.OPEN,
    val closingNotes: String? = null
)

@Entity(
    tableName = "cash_movements",
    foreignKeys = [
        ForeignKey(
            entity = CashShiftEntity::class,
            parentColumns = ["id"],
            childColumns = ["shiftId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("shiftId")]
)
data class CashMovementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val shiftId: Long,
    val orderId: Long? = null,
    val movementType: MovementType,
    val amount: Int,
    val reason: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "company_reviews",
    foreignKeys = [
        ForeignKey(
            entity = CompanyEntity::class,
            parentColumns = ["id"],
            childColumns = ["companyId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("companyId")]
)
data class ReviewEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val companyId: Long,
    val orderId: Long? = null,
    val customerName: String,
    val rating: Int, // 1 to 5
    val comment: String,
    val ownerReply: String? = null,
    val ownerReplyAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "product_reviews",
    foreignKeys = [
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["id"],
            childColumns = ["productId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("productId"), Index("companyId")]
)
data class ProductReviewEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val productId: Long,
    val companyId: Long,
    val orderId: Long? = null,
    val customerName: String,
    val rating: Int, // 1 to 5
    val comment: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "app_notifications",
    indices = [Index("companyId"), Index("orderId")]
)
data class AppNotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val companyId: Long? = null,
    val orderId: Long? = null,
    val orderNumber: Int? = null,
    val title: String,
    val message: String,
    val type: String = "ORDER_STATUS", // ORDER_STATUS, REVIEW_REQUEST, PROMO
    val isRead: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

