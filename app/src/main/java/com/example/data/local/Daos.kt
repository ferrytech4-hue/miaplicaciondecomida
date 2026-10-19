package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
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
import com.example.data.model.AppNotificationEntity
import com.example.data.model.ProductEntity
import com.example.data.model.ProductReviewEntity
import com.example.data.model.ReviewEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CompanyDao {
    @Query("SELECT * FROM companies WHERE isPublished = 1 AND isPublished = 1 ORDER BY commercialName ASC")
    fun getPublishedCompanies(): Flow<List<CompanyEntity>>

    @Query("SELECT * FROM companies ORDER BY id ASC")
    fun getAllCompanies(): Flow<List<CompanyEntity>>

    @Query("SELECT * FROM companies WHERE slug = :slug LIMIT 1")
    fun getCompanyBySlug(slug: String): Flow<CompanyEntity?>

    @Query("SELECT * FROM companies WHERE id = :id LIMIT 1")
    fun getCompanyById(id: Long): Flow<CompanyEntity?>

    @Query("SELECT * FROM companies WHERE id = :id LIMIT 1")
    suspend fun getCompanyByIdSync(id: Long): CompanyEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCompany(company: CompanyEntity): Long

    @Update
    suspend fun updateCompany(company: CompanyEntity)
}

@Dao
interface CatalogDao {
    @Query("SELECT * FROM categories WHERE companyId = :companyId AND isActive = 1 ORDER BY sortOrder ASC")
    fun getCategoriesByCompany(companyId: Long): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM products WHERE companyId = :companyId ORDER BY id ASC")
    fun getProductsByCompany(companyId: Long): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE id = :id LIMIT 1")
    suspend fun getProductById(id: Long): ProductEntity?

    @Query("SELECT * FROM product_modifier_groups WHERE productId = :productId")
    suspend fun getModifierGroups(productId: Long): List<ModifierGroupEntity>

    @Query("SELECT * FROM product_modifier_options WHERE groupId = :groupId AND isAvailable = 1")
    suspend fun getModifierOptions(groupId: Long): List<ModifierOptionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ProductEntity): Long

    @Update
    suspend fun updateProduct(product: ProductEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertModifierGroup(group: ModifierGroupEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertModifierOption(option: ModifierOptionEntity): Long
}

@Dao
interface OrderDao {
    @Query("SELECT * FROM orders WHERE companyId = :companyId ORDER BY id DESC")
    fun getOrdersByCompany(companyId: Long): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE companyId = :companyId AND status NOT IN ('DELIVERED', 'CANCELLED') ORDER BY id ASC")
    fun getActiveOrdersForKds(companyId: Long): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE id = :orderId LIMIT 1")
    suspend fun getOrderById(orderId: Long): OrderEntity?

    @Query("SELECT * FROM order_items WHERE orderId = :orderId")
    suspend fun getOrderItems(orderId: Long): List<OrderItemEntity>

    @Query("SELECT * FROM order_items WHERE orderId IN (:orderIds)")
    suspend fun getOrderItemsForOrders(orderIds: List<Long>): List<OrderItemEntity>

    @Query("SELECT * FROM order_status_history WHERE orderId = :orderId ORDER BY createdAt DESC")
    suspend fun getOrderStatusHistory(orderId: Long): List<OrderStatusHistoryEntity>

    @Query("SELECT MAX(orderNumber) FROM orders WHERE companyId = :companyId")
    suspend fun getMaxOrderNumber(companyId: Long): Int?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: OrderEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrderItem(item: OrderItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrderStatusHistory(history: OrderStatusHistoryEntity): Long

    @Query("UPDATE orders SET status = :status WHERE id = :orderId")
    suspend fun updateOrderStatus(orderId: Long, status: OrderStatus)

    @Query("UPDATE orders SET isPaid = :isPaid WHERE id = :orderId")
    suspend fun updateOrderPayment(orderId: Long, isPaid: Boolean)
}

@Dao
interface PosDao {
    @Query("SELECT * FROM cash_shifts WHERE companyId = :companyId AND status = 'OPEN' ORDER BY id DESC LIMIT 1")
    fun getActiveShift(companyId: Long): Flow<CashShiftEntity?>

    @Query("SELECT * FROM cash_shifts WHERE companyId = :companyId ORDER BY id DESC")
    fun getShiftsByCompany(companyId: Long): Flow<List<CashShiftEntity>>

    @Query("SELECT * FROM cash_movements WHERE shiftId = :shiftId ORDER BY createdAt DESC")
    fun getMovementsByShift(shiftId: Long): Flow<List<CashMovementEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShift(shift: CashShiftEntity): Long

    @Update
    suspend fun updateShift(shift: CashShiftEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMovement(movement: CashMovementEntity): Long
}

@Dao
interface ReviewDao {
    @Query("SELECT * FROM company_reviews WHERE companyId = :companyId ORDER BY createdAt DESC")
    fun getReviewsByCompany(companyId: Long): Flow<List<ReviewEntity>>

    @Query("SELECT AVG(rating) FROM company_reviews WHERE companyId = :companyId")
    fun getAverageRatingForCompany(companyId: Long): Flow<Float?>

    @Query("SELECT COUNT(*) FROM company_reviews WHERE companyId = :companyId")
    fun getReviewCountForCompany(companyId: Long): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReview(review: ReviewEntity): Long

    @Query("UPDATE company_reviews SET ownerReply = :reply, ownerReplyAt = :replyAt WHERE id = :reviewId")
    suspend fun updateOwnerReply(reviewId: Long, reply: String, replyAt: Long)

    @Query("SELECT * FROM product_reviews WHERE productId = :productId ORDER BY createdAt DESC")
    fun getProductReviews(productId: Long): Flow<List<ProductReviewEntity>>

    @Query("SELECT AVG(rating) FROM product_reviews WHERE productId = :productId")
    fun getAverageRatingForProduct(productId: Long): Flow<Float?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProductReview(review: ProductReviewEntity): Long
}

@Dao
interface NotificationDao {
    @Query("SELECT * FROM app_notifications ORDER BY createdAt DESC")
    fun getAllNotifications(): Flow<List<AppNotificationEntity>>

    @Query("SELECT COUNT(*) FROM app_notifications WHERE isRead = 0")
    fun getUnreadCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: AppNotificationEntity): Long

    @Query("UPDATE app_notifications SET isRead = 1 WHERE isRead = 0")
    suspend fun markAllAsRead()

    @Query("UPDATE app_notifications SET isRead = 1 WHERE id = :id")
    suspend fun markAsRead(id: Long)
}

