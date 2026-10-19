package com.example

import com.example.data.model.DeliveryType
import com.example.data.model.ModifierOptionEntity
import com.example.data.model.OrderEntity
import com.example.data.model.OrderItemEntity
import com.example.data.model.PaymentMethod
import com.example.data.model.ProductEntity
import com.example.data.repository.CartItem
import com.example.util.ChileanUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testClpCurrencyFormatting() {
        val formatted = ChileanUtils.formatClp(3500)
        assertTrue("Debe incluir formato con punto chileno: $formatted", formatted.contains("3.500"))
        assertTrue("Debe incluir símbolo peso: $formatted", formatted.contains("$"))
    }

    @Test
    fun testChileanPhoneSanitization() {
        val sanitized1 = ChileanUtils.sanitizeChileanPhone("+56 9 8765 4321")
        assertEquals("56987654321", sanitized1)

        val sanitized2 = ChileanUtils.sanitizeChileanPhone("98765432")
        assertEquals("56998765432", sanitized2)
    }

    @Test
    fun testCartItemCalculationWithModifiers() {
        val product = ProductEntity(
            companyId = 1,
            categoryId = 1,
            name = "Completo Italiano",
            description = "Palta, tomate, mayo",
            basePrice = 3500,
            imageUrl = ""
        )
        val mod1 = ModifierOptionEntity(groupId = 1, name = "Extra Palta", priceDelta = 1000)
        val mod2 = ModifierOptionEntity(groupId = 1, name = "Ají Verde", priceDelta = 400)

        val cartItem = CartItem(
            product = product,
            quantity = 2,
            selectedOptions = listOf(mod1, mod2)
        )

        assertEquals(4900, cartItem.unitPrice)
        assertEquals(9800, cartItem.subtotal)
    }

    @Test
    fun testWhatsAppMessageContainsChileanLaw19925() {
        val order = OrderEntity(
            id = 1,
            companyId = 1,
            orderNumber = 105,
            customerName = "Ignacio Silva",
            customerPhone = "+56912345678",
            customerAddress = "Santa Isabel 320",
            deliveryNotes = "Timbre 4",
            hasAlcohol = true,
            isAdultConfirmed = true,
            deliveryType = DeliveryType.DELIVERY,
            subtotalClp = 10000,
            deliveryFeeClp = 1500,
            totalClp = 11500,
            paymentMethod = PaymentMethod.EFECTIVO
        )

        val items = listOf(
            OrderItemEntity(
                orderId = 1,
                productId = 10,
                productName = "Cerveza Torobayo 500cc",
                unitPrice = 3800,
                quantity = 2,
                subtotal = 7600
            )
        )

        val message = ChileanUtils.buildWhatsAppOrderMessage("Donde El Guatón", order, items)
        assertTrue("Debe mencionar N° de pedido", message.contains("#105"))
        assertTrue("Debe indicar total en CLP", message.contains("11.500"))
        assertTrue("Debe incluir mención obligatoria a Ley N° 19.925", message.contains("Ley N° 19.925"))
    }
}
