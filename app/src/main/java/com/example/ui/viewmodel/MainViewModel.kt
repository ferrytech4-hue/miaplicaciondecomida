package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.CashMovementEntity
import com.example.data.model.CashShiftEntity
import com.example.data.model.CategoryEntity
import com.example.data.model.CompanyEntity
import com.example.data.model.DeliveryType
import com.example.data.model.ModifierOptionEntity
import com.example.data.model.MovementType
import com.example.data.model.OrderEntity
import com.example.data.model.OrderItemEntity
import com.example.data.model.OrderStatus
import com.example.data.model.PaymentMethod
import com.example.data.model.ProductEntity
import com.example.data.repository.CartItem
import com.example.data.repository.MiPedidoRepository
import com.example.data.repository.OrderWithDetails
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppMode(val label: String) {
    MARKETPLACE("Catálogo"),
    STOREFRONT("Tienda Digital"),
    KDS("Cocina KDS"),
    POS_CASH("Caja & POS"),
    ADMIN_MENU("Menú"),
    ADMIN_COMPANIES("Locales")
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application, viewModelScope)
    val repository = MiPedidoRepository(
        database.companyDao(),
        database.catalogDao(),
        database.orderDao(),
        database.posDao(),
        database.reviewDao(),
        database.notificationDao(),
        application
    )

    // Push Notifications History
    val allNotifications = repository.getAllNotifications()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unreadNotificationsCount = repository.getUnreadNotificationsCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun markAllNotificationsAsRead() {
        viewModelScope.launch {
            repository.markAllNotificationsAsRead()
        }
    }

    // Customer Reviews & Ratings
    fun submitCompanyReview(
        companyId: Long,
        orderId: Long?,
        customerName: String,
        rating: Int,
        comment: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            repository.createCompanyReview(companyId, orderId, customerName, rating, comment)
            onSuccess()
        }
    }

    fun replyToReview(reviewId: Long, replyText: String) {
        viewModelScope.launch {
            repository.replyToReview(reviewId, replyText)
        }
    }

    fun submitProductReview(
        productId: Long,
        companyId: Long,
        orderId: Long?,
        customerName: String,
        rating: Int,
        comment: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            repository.createProductReview(productId, companyId, orderId, customerName, rating, comment)
            onSuccess()
        }
    }

    // Navigation and Mode State
    private val _currentMode = MutableStateFlow(AppMode.MARKETPLACE)
    val currentMode: StateFlow<AppMode> = _currentMode.asStateFlow()

    fun setMode(mode: AppMode) {
        _currentMode.value = mode
    }

    // Companies & Current Tenant (Multi-tenancy)
    val allCompanies: StateFlow<List<CompanyEntity>> = repository.getAllCompanies()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val publishedCompanies: StateFlow<List<CompanyEntity>> = repository.getPublishedCompanies()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedCompany = MutableStateFlow<CompanyEntity?>(null)
    val selectedCompany: StateFlow<CompanyEntity?> = _selectedCompany.asStateFlow()

    fun selectCompany(company: CompanyEntity) {
        _selectedCompany.value = company
    }

    fun selectCompanyBySlug(slug: String) {
        viewModelScope.launch {
            allCompanies.value.find { it.slug == slug }?.let {
                _selectedCompany.value = it
            }
        }
    }

    // Shopping Cart (Scoped to selected company)
    private val _cartItems = MutableStateFlow<List<CartItem>>(emptyList())
    val cartItems: StateFlow<List<CartItem>> = _cartItems.asStateFlow()

    fun addToCart(
        product: ProductEntity,
        quantity: Int,
        selectedOptions: List<ModifierOptionEntity> = emptyList(),
        notes: String? = null
    ) {
        val existingIndex = _cartItems.value.indexOfFirst {
            it.product.id == product.id &&
                    it.selectedOptions == selectedOptions &&
                    it.notes == notes
        }
        if (existingIndex >= 0) {
            val updated = _cartItems.value.toMutableList()
            val current = updated[existingIndex]
            updated[existingIndex] = current.copy(quantity = current.quantity + quantity)
            _cartItems.value = updated
        } else {
            _cartItems.value = _cartItems.value + CartItem(product, quantity, selectedOptions, notes)
        }
    }

    fun updateCartItemQuantity(index: Int, newQuantity: Int) {
        val updated = _cartItems.value.toMutableList()
        if (index in updated.indices) {
            if (newQuantity <= 0) {
                updated.removeAt(index)
            } else {
                updated[index] = updated[index].copy(quantity = newQuantity)
            }
            _cartItems.value = updated
        }
    }

    fun clearCart() {
        _cartItems.value = emptyList()
    }

    // Checkout & Order Placement
    private val _lastPlacedOrder = MutableStateFlow<OrderWithDetails?>(null)
    val lastPlacedOrder: StateFlow<OrderWithDetails?> = _lastPlacedOrder.asStateFlow()

    private val _orderError = MutableStateFlow<String?>(null)
    val orderError: StateFlow<String?> = _orderError.asStateFlow()

    fun clearOrderError() {
        _orderError.value = null
    }

    fun clearLastPlacedOrder() {
        _lastPlacedOrder.value = null
    }

    fun submitOrder(
        customerName: String,
        customerPhone: String,
        customerAddress: String?,
        deliveryNotes: String?,
        deliveryType: DeliveryType,
        paymentMethod: PaymentMethod,
        isAdultConfirmed: Boolean,
        onSuccess: (OrderWithDetails) -> Unit
    ) {
        val company = _selectedCompany.value ?: return
        viewModelScope.launch {
            try {
                val orderWithDetails = repository.placeOrder(
                    company = company,
                    cartItems = _cartItems.value,
                    customerName = customerName,
                    customerPhone = customerPhone,
                    customerAddress = customerAddress,
                    deliveryNotes = deliveryNotes,
                    deliveryType = deliveryType,
                    paymentMethod = paymentMethod,
                    isAdultConfirmed = isAdultConfirmed
                )
                _lastPlacedOrder.value = orderWithDetails
                clearCart()
                onSuccess(orderWithDetails)
            } catch (e: Exception) {
                _orderError.value = e.message ?: "Error al procesar el pedido."
            }
        }
    }

    // KDS - Cocina Rápida
    fun updateOrderStatus(orderId: Long, newStatus: OrderStatus, role: String, comment: String? = null) {
        viewModelScope.launch {
            repository.updateOrderStatus(orderId, newStatus, role, comment)
        }
    }

    // POS - Caja
    fun markOrderPaid(orderId: Long, isPaid: Boolean, shiftId: Long?) {
        viewModelScope.launch {
            repository.updateOrderPayment(orderId, isPaid, shiftId)
        }
    }

    fun openCashShift(companyId: Long, cashierName: String, initialBalance: Int) {
        viewModelScope.launch {
            repository.openShift(companyId, cashierName, initialBalance)
        }
    }

    fun closeCashShift(shiftId: Long, declaredCash: Int, notes: String?) {
        viewModelScope.launch {
            repository.closeShift(shiftId, declaredCash, notes)
        }
    }

    fun addCashMovement(shiftId: Long, type: MovementType, amount: Int, reason: String) {
        viewModelScope.launch {
            repository.registerCashMovement(shiftId, type, amount, reason)
        }
    }

    // Menu Management
    fun toggleProductAvailability(product: ProductEntity) {
        viewModelScope.launch {
            repository.toggleProductAvailability(product)
        }
    }

    fun createProduct(product: ProductEntity) {
        viewModelScope.launch {
            repository.createProduct(product)
        }
    }

    fun createCategory(category: CategoryEntity) {
        viewModelScope.launch {
            repository.createCategory(category)
        }
    }

    // Company Registration
    fun registerCompany(company: CompanyEntity, onSuccess: (CompanyEntity) -> Unit) {
        viewModelScope.launch {
            val id = repository.createCompany(company)
            val created = company.copy(id = id)
            _selectedCompany.value = created
            onSuccess(created)
        }
    }

    fun updateCompany(company: CompanyEntity) {
        viewModelScope.launch {
            repository.updateCompany(company)
            if (_selectedCompany.value?.id == company.id) {
                _selectedCompany.value = company
            }
        }
    }

    init {
        // Default to first company if available
        viewModelScope.launch {
            allCompanies.collect { list ->
                if (_selectedCompany.value == null && list.isNotEmpty()) {
                    _selectedCompany.value = list.first()
                }
            }
        }
    }
}
