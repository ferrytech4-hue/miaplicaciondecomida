package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CompanyEntity
import com.example.data.model.DeliveryType
import com.example.data.model.PaymentMethod
import com.example.data.repository.OrderWithDetails
import com.example.ui.components.AlcoholWarningBadge
import com.example.ui.components.ChileanPriceText
import com.example.ui.components.WhatsAppOrderButton
import com.example.ui.theme.WhatsAppGreen
import com.example.ui.viewmodel.MainViewModel
import com.example.util.ChileanUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartCheckoutModal(
    viewModel: MainViewModel,
    company: CompanyEntity,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val cartItems by viewModel.cartItems.collectAsState()
    val orderError by viewModel.orderError.collectAsState()

    var customerName by remember { mutableStateOf("") }
    var customerPhone by remember { mutableStateOf("+56 9 ") }
    var customerAddress by remember { mutableStateOf("") }
    var deliveryNotes by remember { mutableStateOf("") }
    var deliveryType by remember { mutableStateOf(DeliveryType.DELIVERY) }
    var paymentMethod by remember { mutableStateOf(PaymentMethod.EFECTIVO) }
    var isAdultConfirmed by remember { mutableStateOf(false) }

    var placedOrderResult by remember { mutableStateOf<OrderWithDetails?>(null) }
    var validationError by remember { mutableStateOf<String?>(null) }

    val hasAlcoholInCart = cartItems.any { it.product.isAlcohol }
    val subtotal = cartItems.sumOf { it.subtotal }
    val deliveryFee = if (deliveryType == DeliveryType.DELIVERY) company.deliveryFeeBase else 0
    val total = subtotal + deliveryFee

    // Confirmation Success Dialog
    if (placedOrderResult != null) {
        val order = placedOrderResult!!.order
        val items = placedOrderResult!!.items
        val waMessage = ChileanUtils.buildWhatsAppOrderMessage(company.commercialName, order, items)

        AlertDialog(
            onDismissRequest = {
                placedOrderResult = null
                onDismiss()
            },
            icon = {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = WhatsAppGreen,
                    modifier = Modifier.size(48.dp)
                )
            },
            title = {
                Text(
                    text = "¡Pedido #${order.orderNumber} Registrado!",
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp
                )
            },
            text = {
                Column {
                    Text(
                        text = "Tu comanda ya ingresó al sistema de ${company.commercialName} por un total de ${ChileanUtils.formatClp(order.totalClp)}.",
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        color = Color(0xFFE8F5E9),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "ℹ️ Al tocar el botón abajo se abrirá WhatsApp con el mensaje estructurado para que lo envíes al local. Abrir WhatsApp no significa que el mensaje ya fue enviado.",
                            fontSize = 12.sp,
                            color = Color(0xFF1B5E20),
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        ChileanUtils.openWhatsApp(context, company.whatsappNumber, waMessage)
                        placedOrderResult = null
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen),
                    modifier = Modifier.testTag("open_whatsapp_dialog_button")
                ) {
                    Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Abrir WhatsApp Ahora", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Button(
                    onClick = {
                        placedOrderResult = null
                        onDismiss()
                    },
                    colors = ButtonDefaults.outlinedButtonColors()
                ) {
                    Text("Cerrar")
                }
            }
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
                .testTag("cart_checkout_modal")
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ShoppingBag, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Tu Carrito (${cartItems.size} ítems)",
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Cerrar")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Cart Items List
            if (cartItems.isEmpty()) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp)
                ) {
                    Text("El carrito está vacío.", color = Color.Gray)
                }
            } else {
                cartItems.forEachIndexed { index, item ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.product.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                if (item.modifiersDescription.isNotBlank()) {
                                    Text(
                                        text = item.modifiersDescription,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                if (!item.notes.isNullOrBlank()) {
                                    Text(
                                        text = "Obs: \"${item.notes}\"",
                                        fontSize = 11.sp,
                                        color = Color.Gray
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                ChileanPriceText(
                                    amount = item.subtotal,
                                    fontSize = 13,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Stepper
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { viewModel.updateCartItemQuantity(index, item.quantity - 1) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        if (item.quantity == 1) Icons.Default.Delete else Icons.Default.Remove,
                                        contentDescription = "Menos",
                                        modifier = Modifier.size(16.dp),
                                        tint = if (item.quantity == 1) Color.Red else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Text(
                                    text = "${item.quantity}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp)
                                )
                                IconButton(
                                    onClick = { viewModel.updateCartItemQuantity(index, item.quantity + 1) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Más", modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Delivery Type Selector
            Text("Modalidad de Entrega", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = deliveryType == DeliveryType.DELIVERY,
                    onClick = { deliveryType = DeliveryType.DELIVERY },
                    label = { Text("Despacho a Domicilio (${ChileanUtils.formatClp(company.deliveryFeeBase)})") }
                )
                FilterChip(
                    selected = deliveryType == DeliveryType.PICKUP,
                    onClick = { deliveryType = DeliveryType.PICKUP },
                    label = { Text("Retiro en Local (Gratis)") }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Guest Customer Form
            Text("Datos para el Pedido (Sin cuenta obligatoria)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = customerName,
                onValueChange = { customerName = it },
                label = { Text("Nombre y Apellido *") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("checkout_name_input")
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = customerPhone,
                onValueChange = { customerPhone = it },
                label = { Text("Teléfono de Contacto (WhatsApp) *") },
                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("checkout_phone_input")
            )

            if (deliveryType == DeliveryType.DELIVERY) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = customerAddress,
                    onValueChange = { customerAddress = it },
                    label = { Text("Dirección de Entrega (Calle, Número, Depto) *") },
                    leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("checkout_address_input")
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = deliveryNotes,
                onValueChange = { deliveryNotes = it },
                label = { Text("Instrucciones o Referencia (Opcional)") },
                maxLines = 2,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Payment Methods
            Text("Medio de Pago", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(6.dp))
            PaymentMethod.entries.forEach { method ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                ) {
                    RadioButton(
                        selected = paymentMethod == method,
                        onClick = { paymentMethod = method }
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = method.label, fontSize = 13.sp)
                }
            }

            // Legal Alcohol Notice & Age Verification (Ley N° 19.925)
            if (hasAlcoholInCart) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    color = Color(0xFFFFF3E0),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFB74D)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        AlcoholWarningBadge()
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = isAdultConfirmed,
                                onCheckedChange = { isAdultConfirmed = it },
                                modifier = Modifier.testTag("alcohol_confirm_checkbox")
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Confirmo que soy mayor de 18 años y presentaré mi Cédula de Identidad chilena vigente al recibir el pedido.",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFBF360C)
                            )
                        }
                    }
                }
            }

            // Error Message
            if (validationError != null || orderError != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    color = Color(0xFFFFEBEE),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = validationError ?: orderError ?: "",
                        color = Color(0xFFC62828),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Totals Breakdown
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Subtotal", fontSize = 13.sp)
                        ChileanPriceText(amount = subtotal, fontSize = 13, fontWeight = FontWeight.Normal)
                    }
                    if (deliveryType == DeliveryType.DELIVERY) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Despacho", fontSize = 13.sp)
                            ChileanPriceText(amount = deliveryFee, fontSize = 13, fontWeight = FontWeight.Normal)
                        }
                    }
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("TOTAL A PAGAR", fontWeight = FontWeight.Black, fontSize = 16.sp)
                        ChileanPriceText(amount = total, fontSize = 18, fontWeight = FontWeight.Black)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Order Action Button
            WhatsAppOrderButton(
                onClick = {
                    validationError = null
                    if (customerName.isBlank()) {
                        validationError = "Por favor ingresa tu nombre."
                        return@WhatsAppOrderButton
                    }
                    if (customerPhone.filter { it.isDigit() }.length < 8) {
                        validationError = "Ingresa un número de teléfono válido (ej: +56 9 8765 4321)."
                        return@WhatsAppOrderButton
                    }
                    if (deliveryType == DeliveryType.DELIVERY && customerAddress.isBlank()) {
                        validationError = "Ingresa tu dirección para el despacho."
                        return@WhatsAppOrderButton
                    }
                    if (hasAlcoholInCart && !isAdultConfirmed) {
                        validationError = "Debes confirmar que eres mayor de 18 años según la Ley N° 19.925."
                        return@WhatsAppOrderButton
                    }

                    viewModel.submitOrder(
                        customerName = customerName.trim(),
                        customerPhone = customerPhone.trim(),
                        customerAddress = if (deliveryType == DeliveryType.DELIVERY) customerAddress.trim() else null,
                        deliveryNotes = deliveryNotes.ifBlank { null },
                        deliveryType = deliveryType,
                        paymentMethod = paymentMethod,
                        isAdultConfirmed = isAdultConfirmed,
                        onSuccess = { createdOrder ->
                            placedOrderResult = createdOrder
                        }
                    )
                },
                text = "Confirmar y Enviar Pedido (${ChileanUtils.formatClp(total)})"
            )

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}
