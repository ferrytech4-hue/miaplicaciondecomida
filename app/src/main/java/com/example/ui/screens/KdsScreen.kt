package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CompanyEntity
import com.example.data.model.OrderEntity
import com.example.data.model.OrderItemEntity
import com.example.data.model.OrderStatus
import com.example.ui.components.AlcoholWarningBadge
import com.example.ui.components.ChileanPriceText
import com.example.ui.components.StatusChip
import com.example.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KdsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val allCompanies by viewModel.allCompanies.collectAsState()
    val selectedCompany by viewModel.selectedCompany.collectAsState()

    var activeOrders by remember { mutableStateOf<List<OrderEntity>>(emptyList()) }
    var orderItemsMap by remember { mutableStateOf<Map<Long, List<OrderItemEntity>>>(emptyMap()) }
    var companyDropdownExpanded by remember { mutableStateOf(false) }

    val currentCompany = selectedCompany ?: allCompanies.firstOrNull()

    // Real-time flow of orders for active company
    if (currentCompany != null) {
        val ordersFlow = remember(currentCompany.id) {
            viewModel.repository.getActiveOrdersForKds(currentCompany.id)
        }
        val liveOrders by ordersFlow.collectAsState(initial = emptyList())
        activeOrders = liveOrders

        LaunchedEffect(liveOrders) {
            val orderIds = liveOrders.map { it.id }
            orderItemsMap = viewModel.repository.getItemsForOrders(orderIds)
        }
    }

    val pendingCount = activeOrders.count { it.status == OrderStatus.PENDING }
    val inPrepCount = activeOrders.count { it.status == OrderStatus.IN_PREPARATION }
    val readyCount = activeOrders.count { it.status == OrderStatus.READY }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .padding(16.dp)
            .testTag("kds_screen")
    ) {
        // KDS Header Bar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.LocalFireDepartment,
                        contentDescription = null,
                        tint = Color(0xFFFF9800),
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "COCINA RÁPIDA (KDS)",
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp,
                        color = Color.White
                    )
                }
                Text(
                    text = "Línea de comandas en vivo • Tiempo real",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }

            // Tenant Switcher Dropdown (Multi-tenancy isolation verification)
            ExposedDropdownMenuBox(
                expanded = companyDropdownExpanded,
                onExpandedChange = { companyDropdownExpanded = !companyDropdownExpanded }
            ) {
                Surface(
                    color = Color(0xFF242424),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF444444)),
                    modifier = Modifier.menuAnchor()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = currentCompany?.commercialName ?: "Seleccionar Local",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = companyDropdownExpanded)
                    }
                }

                ExposedDropdownMenu(
                    expanded = companyDropdownExpanded,
                    onDismissRequest = { companyDropdownExpanded = false }
                ) {
                    allCompanies.forEach { comp ->
                        DropdownMenuItem(
                            text = { Text(comp.commercialName, fontSize = 13.sp) },
                            onClick = {
                                viewModel.selectCompany(comp)
                                companyDropdownExpanded = false
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Status Metrics Banner
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            MetricCard(label = "En Cola", count = pendingCount, color = Color(0xFFFFF176), modifier = Modifier.weight(1f))
            MetricCard(label = "En Plancha", count = inPrepCount, color = Color(0xFFFFB74D), modifier = Modifier.weight(1f))
            MetricCard(label = "Listos", count = readyCount, color = Color(0xFF81C784), modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Comandas List
        if (activeOrders.isEmpty()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Restaurant,
                        contentDescription = null,
                        tint = Color.DarkGray,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Sin comandas pendientes en cocina",
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "Los nuevos pedidos recibidos por WhatsApp o Checkout aparecerán aquí automáticamente.",
                        color = Color.DarkGray,
                        fontSize = 12.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(activeOrders) { order ->
                    val items = orderItemsMap[order.id] ?: emptyList()
                    KdsTicketCard(
                        order = order,
                        items = items,
                        onMoveToPrep = {
                            viewModel.updateOrderStatus(order.id, OrderStatus.IN_PREPARATION, "Cocinero KDS")
                        },
                        onMarkReady = {
                            viewModel.updateOrderStatus(order.id, OrderStatus.READY, "Cocinero KDS")
                        },
                        onDeliver = {
                            viewModel.updateOrderStatus(order.id, OrderStatus.DELIVERED, "Cocinero / Despacho")
                        },
                        onCancel = {
                            viewModel.updateOrderStatus(order.id, OrderStatus.CANCELLED, "Cocinero KDS", "Anulado por cocina")
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun MetricCard(label: String, count: Int, color: Color, modifier: Modifier = Modifier) {
    Surface(
        color = Color(0xFF1E1E1E),
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.4f)),
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 10.dp)
        ) {
            Text(text = "$count", fontSize = 20.sp, fontWeight = FontWeight.Black, color = color)
            Text(text = label, fontSize = 11.sp, color = Color.LightGray)
        }
    }
}

@Composable
fun KdsTicketCard(
    order: OrderEntity,
    items: List<OrderItemEntity>,
    onMoveToPrep: () -> Unit,
    onMarkReady: () -> Unit,
    onDeliver: () -> Unit,
    onCancel: () -> Unit
) {
    val timeFormat = SimpleDateFormat("HH:mm", Locale("es", "CL"))
    val formattedTime = timeFormat.format(Date(order.createdAt))

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
        border = androidx.compose.foundation.BorderStroke(
            1.5.dp,
            when (order.status) {
                OrderStatus.PENDING -> Color(0xFFFFF176)
                OrderStatus.IN_PREPARATION -> Color(0xFFFF9800)
                OrderStatus.READY -> Color(0xFF4CAF50)
                else -> Color.Gray
            }
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("kds_ticket_${order.orderNumber}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Ticket Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "#${order.orderNumber}",
                        fontWeight = FontWeight.Black,
                        fontSize = 22.sp,
                        color = Color(0xFFFF9800)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Surface(
                        color = Color(0xFF2C2C2C),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = order.deliveryType.label,
                            color = Color.LightGray,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = formattedTime, color = Color.Gray, fontSize = 12.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    StatusChip(status = order.status)
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Cliente: ${order.customerName} • ${order.customerPhone}",
                fontSize = 12.sp,
                color = Color.LightGray
            )

            if (!order.deliveryNotes.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    color = Color(0xFF332000),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "⚠️ Nota: ${order.deliveryNotes}",
                        color = Color(0xFFFFD54F),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(6.dp)
                    )
                }
            }

            if (order.hasAlcohol) {
                Spacer(modifier = Modifier.height(4.dp))
                AlcoholWarningBadge(compact = true)
            }

            Divider(color = Color(0xFF333333), modifier = Modifier.padding(vertical = 8.dp))

            // Items List
            items.forEach { item ->
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "${item.quantity}x ${item.productName}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color.White
                        )
                    }
                    if (item.modifiersDescription.isNotBlank()) {
                        Text(
                            text = "   └ ${item.modifiersDescription}",
                            fontSize = 12.sp,
                            color = Color(0xFFFFB74D)
                        )
                    }
                    if (!item.notes.isNullOrBlank()) {
                        Text(
                            text = "   └ 💬 \"${item.notes}\"",
                            fontSize = 12.sp,
                            color = Color(0xFFFF8A80),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Divider(color = Color(0xFF333333), modifier = Modifier.padding(vertical = 8.dp))

            // Action Buttons by State
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                when (order.status) {
                    OrderStatus.PENDING -> {
                        Button(
                            onClick = onMoveToPrep,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                        ) {
                            Text("Iniciar Preparación", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                    OrderStatus.IN_PREPARATION -> {
                        Button(
                            onClick = onMarkReady,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Marcar Listo ✓", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                    OrderStatus.READY -> {
                        Button(
                            onClick = onDeliver,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                        ) {
                            Icon(Icons.Default.LocalShipping, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Entregar / Despachar", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                    else -> {}
                }

                OutlinedButton(
                    onClick = onCancel,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF5252)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(42.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Anular", modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}
