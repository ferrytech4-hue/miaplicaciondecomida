package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.CashMovementEntity
import com.example.data.model.CashShiftEntity
import com.example.data.model.CategoryEntity
import com.example.data.model.CompanyEntity
import com.example.data.model.ModifierGroupEntity
import com.example.data.model.ModifierOptionEntity
import com.example.data.model.OrderEntity
import com.example.data.model.OrderItemEntity
import com.example.data.model.OrderStatus
import com.example.data.model.OrderStatusHistoryEntity
import com.example.data.model.PaymentMethod
import com.example.data.model.PlanType
import com.example.data.model.AppNotificationEntity
import com.example.data.model.ProductEntity
import com.example.data.model.ProductReviewEntity
import com.example.data.model.ReviewEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        CompanyEntity::class,
        CategoryEntity::class,
        ProductEntity::class,
        ModifierGroupEntity::class,
        ModifierOptionEntity::class,
        OrderEntity::class,
        OrderItemEntity::class,
        OrderStatusHistoryEntity::class,
        CashShiftEntity::class,
        CashMovementEntity::class,
        ReviewEntity::class,
        ProductReviewEntity::class,
        AppNotificationEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun companyDao(): CompanyDao
    abstract fun catalogDao(): CatalogDao
    abstract fun orderDao(): OrderDao
    abstract fun posDao(): PosDao
    abstract fun reviewDao(): ReviewDao
    abstract fun notificationDao(): NotificationDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "mipedido_express.db"
                )
                    .addCallback(AppDatabaseCallback(scope))
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class AppDatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateInitialData(database)
                }
            }
        }

        private suspend fun populateInitialData(db: AppDatabase) {
            val companyDao = db.companyDao()
            val catalogDao = db.catalogDao()
            val orderDao = db.orderDao()
            val posDao = db.posDao()

            // 1. Empresa 1: Donde El Guatón
            val company1Id = companyDao.insertCompany(
                CompanyEntity(
                    slug = "donde-el-guaton",
                    legalName = "Gastronomía Chilena Tradicional SpA",
                    rut = "76.842.110-3",
                    commercialName = "Donde El Guatón — Sanguchería",
                    contactEmail = "pedidos@dondeelguaton.cl",
                    contactPhone = "+56987654321",
                    whatsappNumber = "+56987654321",
                    logoUrl = "https://images.unsplash.com/photo-1568901346375-23c9450c58cd?w=400",
                    bannerUrl = "https://images.unsplash.com/photo-1555396273-367ea4eb4db5?w=1200",
                    primaryColorHex = "#D32F2F",
                    secondaryColorHex = "#FF9800",
                    category = "Completos & Sanguchería",
                    isPublished = true,
                    isOpenManual = true,
                    pickupEnabled = true,
                    deliveryEnabled = true,
                    deliveryFeeBase = 1500,
                    minOrderAmount = 4000,
                    estimatedTimeMin = 25,
                    subscriptionPlan = PlanType.PROFESIONAL
                )
            )

            // Categorías Empresa 1
            val cat1Completos = catalogDao.insertCategory(
                CategoryEntity(companyId = company1Id, name = "Completos & Ases", sortOrder = 1)
            )
            val cat1Sanguches = catalogDao.insertCategory(
                CategoryEntity(companyId = company1Id, name = "Sándwiches Tradicionales", sortOrder = 2)
            )
            val cat1Acomp = catalogDao.insertCategory(
                CategoryEntity(companyId = company1Id, name = "Papas & Acompañamientos", sortOrder = 3)
            )
            val cat1Bebidas = catalogDao.insertCategory(
                CategoryEntity(companyId = company1Id, name = "Bebidas & Cervezas", sortOrder = 4)
            )

            // Productos Empresa 1
            val prodCompleto = catalogDao.insertProduct(
                ProductEntity(
                    companyId = company1Id,
                    categoryId = cat1Completos,
                    name = "Completo Italiano Gigante",
                    description = "Vienesa premium en pan crujiente con tomate en cubos, abundante palta hass molida y mayonesa casera.",
                    basePrice = 3500,
                    imageUrl = "https://images.unsplash.com/photo-1619740455993-9e612b1af08a?w=600",
                    isAvailable = true,
                    isAlcohol = false
                )
            )

            val groupPan = catalogDao.insertModifierGroup(
                ModifierGroupEntity(productId = prodCompleto, name = "Tipo de Pan", isRequired = true, minSelection = 1, maxSelection = 1)
            )
            catalogDao.insertModifierOption(ModifierOptionEntity(groupId = groupPan, name = "Pan Lengua Tradicional", priceDelta = 0))
            catalogDao.insertModifierOption(ModifierOptionEntity(groupId = groupPan, name = "Marraqueta Tostada", priceDelta = 300))

            val groupAgregados = catalogDao.insertModifierGroup(
                ModifierGroupEntity(productId = prodCompleto, name = "Agregados", isRequired = false, minSelection = 0, maxSelection = 3)
            )
            catalogDao.insertModifierOption(ModifierOptionEntity(groupId = groupAgregados, name = "Extra Palta Hass", priceDelta = 1000))
            catalogDao.insertModifierOption(ModifierOptionEntity(groupId = groupAgregados, name = "Ají Verde Picado", priceDelta = 400))
            catalogDao.insertModifierOption(ModifierOptionEntity(groupId = groupAgregados, name = "Chucrut Alemán", priceDelta = 500))

            val prodChurrasco = catalogDao.insertProduct(
                ProductEntity(
                    companyId = company1Id,
                    categoryId = cat1Sanguches,
                    name = "Churrasco Chacarero en Pan Frica",
                    description = "Finas láminas de posta rosada a la plancha, porotos verdes frescos, tomate maduro y ají verde en pan frica de la casa.",
                    basePrice = 6900,
                    imageUrl = "https://images.unsplash.com/photo-1528735602780-2552fd46c7af?w=600",
                    isAvailable = true,
                    isAlcohol = false
                )
            )

            val prodPapas = catalogDao.insertProduct(
                ProductEntity(
                    companyId = company1Id,
                    categoryId = cat1Acomp,
                    name = "Papas Fritas Rústicas Supremas",
                    description = "Papas fritas con piel, salsa cheddar cremosa y crocantes trozos de tocino ahumado.",
                    basePrice = 4500,
                    imageUrl = "https://images.unsplash.com/photo-1573080496219-bb080dd4f877?w=600",
                    isAvailable = true,
                    isAlcohol = false
                )
            )

            val prodCerveza = catalogDao.insertProduct(
                ProductEntity(
                    companyId = company1Id,
                    categoryId = cat1Bebidas,
                    name = "Cerveza Kunstmann Torobayo 500cc",
                    description = "Cerveza Amber Ale valdiviana. Venta exclusiva a mayores de 18 años bajo Ley N° 19.925.",
                    basePrice = 3800,
                    imageUrl = "https://images.unsplash.com/photo-1608270119853-41a4a496c1df?w=600",
                    isAvailable = true,
                    isAlcohol = true
                )
            )

            // 2. Empresa 2: Bella Napoli & Tragos (Providencia)
            val company2Id = companyDao.insertCompany(
                CompanyEntity(
                    slug = "bella-napoli",
                    legalName = "Pizzería y Cócteles Bella Napoli Ltda",
                    rut = "77.219.540-8",
                    commercialName = "Pizzería Bella Napoli & Tragos",
                    contactEmail = "contacto@bellanapoli.cl",
                    contactPhone = "+56976543210",
                    whatsappNumber = "+56976543210",
                    logoUrl = "https://images.unsplash.com/photo-1513104890138-7c749659a591?w=400",
                    bannerUrl = "https://images.unsplash.com/photo-1579751626657-72bc17010498?w=1200",
                    primaryColorHex = "#1E88E5",
                    secondaryColorHex = "#43A047",
                    category = "Pizzas & Tragos",
                    isPublished = true,
                    isOpenManual = true,
                    pickupEnabled = true,
                    deliveryEnabled = true,
                    deliveryFeeBase = 2000,
                    minOrderAmount = 8000,
                    estimatedTimeMin = 35,
                    subscriptionPlan = PlanType.AVANZADO
                )
            )

            val cat2Pizzas = catalogDao.insertCategory(
                CategoryEntity(companyId = company2Id, name = "Pizzas Artesanales", sortOrder = 1)
            )
            val cat2Tragos = catalogDao.insertCategory(
                CategoryEntity(companyId = company2Id, name = "Cócteles & Tragos Chilenos", sortOrder = 2)
            )

            catalogDao.insertProduct(
                ProductEntity(
                    companyId = company2Id,
                    categoryId = cat2Pizzas,
                    name = "Pizza Napolitana Fior di Latte Familiar",
                    description = "Masa madre 48 hrs de fermentación, salsa pomodoro San Marzano, mozzarella fresca y albahaca.",
                    basePrice = 11900,
                    imageUrl = "https://images.unsplash.com/photo-1534308983496-4fabb1a015ee?w=600",
                    isAvailable = true,
                    isAlcohol = false
                )
            )

            catalogDao.insertProduct(
                ProductEntity(
                    companyId = company2Id,
                    categoryId = cat2Tragos,
                    name = "Pisco Sour Peruano/Chileno Gran Reserva 40°",
                    description = "Pisco añejado con jugo de limón de pica fresco, goma y amargo de angostura. Sujeto a Ley 19.925 (+18).",
                    basePrice = 4900,
                    imageUrl = "https://images.unsplash.com/photo-1551024709-8f23befc6f87?w=600",
                    isAvailable = true,
                    isAlcohol = true
                )
            )

            // Seed Caja y Turno para Empresa 1
            val shift1Id = posDao.insertShift(
                CashShiftEntity(
                    companyId = company1Id,
                    cashierName = "Claudio Valenzuela (Cajero Turno Tarde)",
                    initialBalance = 50000,
                    status = com.example.data.model.CashShiftStatus.OPEN
                )
            )
            posDao.insertMovement(
                CashMovementEntity(
                    shiftId = shift1Id,
                    movementType = com.example.data.model.MovementType.INGRESO,
                    amount = 50000,
                    reason = "Apertura de caja - Fondo de sencillo inicial"
                )
            )

            // Seed Pedido inicial para KDS y visualización
            val sampleOrder1Id = orderDao.insertOrder(
                OrderEntity(
                    companyId = company1Id,
                    orderNumber = 101,
                    customerName = "Camila Morales",
                    customerPhone = "+56991234567",
                    customerAddress = "Av. Providencia 1450, Depto 402, Santiago",
                    deliveryNotes = "Tocar citófono 402, dejar en conserjería si no respondo",
                    hasAlcohol = true,
                    isAdultConfirmed = true,
                    deliveryType = com.example.data.model.DeliveryType.DELIVERY,
                    subtotalClp = 10400,
                    deliveryFeeClp = 1500,
                    totalClp = 11900,
                    status = OrderStatus.IN_PREPARATION,
                    paymentMethod = PaymentMethod.TRANSBANK_DEBITO,
                    isPaid = true
                )
            )

            orderDao.insertOrderItem(
                OrderItemEntity(
                    orderId = sampleOrder1Id,
                    productId = prodCompleto,
                    productName = "Completo Italiano Gigante",
                    unitPrice = 3500,
                    quantity = 2,
                    subtotal = 7000,
                    notes = "Sin mayonesa en uno de los completos",
                    modifiersDescription = "Pan Lengua Tradicional, Extra Palta Hass"
                )
            )

            orderDao.insertOrderItem(
                OrderItemEntity(
                    orderId = sampleOrder1Id,
                    productId = prodCerveza,
                    productName = "Cerveza Kunstmann Torobayo 500cc",
                    unitPrice = 3400,
                    quantity = 1,
                    subtotal = 3400,
                    notes = null,
                    modifiersDescription = "+18 Exige Cédula"
                )
            )

            orderDao.insertOrderStatusHistory(
                OrderStatusHistoryEntity(
                    orderId = sampleOrder1Id,
                    previousStatus = "PENDING",
                    newStatus = "IN_PREPARATION",
                    changedByRole = "Cocinero KDS",
                    comment = "Comanda recibida y en plancha"
                )
            )

            // Seed Reseñas de Clientes (Reviews)
            val reviewDao = db.reviewDao()
            reviewDao.insertReview(
                ReviewEntity(
                    companyId = company1Id,
                    orderId = sampleOrder1Id,
                    customerName = "Camila Morales",
                    rating = 5,
                    comment = "¡Los mejores completos italianos de Santiago! La palta fresquísima y abundante, pan crujiente y llegó caliente.",
                    ownerReply = "¡Muchas gracias Camila! Es un placer atenderte siempre. ¡Te esperamos pronto!",
                    ownerReplyAt = System.currentTimeMillis() - 86400000L
                )
            )

            reviewDao.insertReview(
                ReviewEntity(
                    companyId = company1Id,
                    orderId = null,
                    customerName = "Rodrigo Fuentes",
                    rating = 5,
                    comment = "El chacarero en frica es insuperable. Carne blanda y los porotos verdes tiernos. Despacho rápido en 20 minutos.",
                    ownerReply = null
                )
            )

            reviewDao.insertReview(
                ReviewEntity(
                    companyId = company2Id,
                    orderId = null,
                    customerName = "Francisca Soto",
                    rating = 5,
                    comment = "La pizza Napolitana fior di latte con masa madre es exquisita y el Pisco Sour llegó bien frío y sellado.",
                    ownerReply = "¡Gracias Francisca! Cuidamos cada detalle del sellado para tragos y pizzas."
                )
            )

            // Seed Notificaciones para el cliente
            val notifDao = db.notificationDao()
            notifDao.insertNotification(
                AppNotificationEntity(
                    companyId = company1Id,
                    orderId = sampleOrder1Id,
                    orderNumber = 101,
                    title = "👨‍🍳 Comanda en Cocina",
                    message = "Donde El Guatón comenzó la preparación de tu pedido #101 en plancha.",
                    type = "ORDER_STATUS",
                    isRead = false
                )
            )
            notifDao.insertNotification(
                AppNotificationEntity(
                    companyId = company1Id,
                    orderId = null,
                    orderNumber = null,
                    title = "🎉 ¡Bienvenido a MiPedido Express!",
                    message = "Descubre locales gastronómicos, comida rápida y bebidas con despacho o retiro en Chile.",
                    type = "PROMO",
                    isRead = true
                )
            )
        }
    }
}
