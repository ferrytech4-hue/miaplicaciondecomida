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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import com.example.data.model.CashMovementEntity
import com.example.data.model.CashShiftEntity
import com.example.data.model.CashShiftStatus
import com.example.data.model.CompanyEntity
import com.example.data.model.MovementType
import com.example.data.model.OrderEntity
import com.example.ui.components.ChileanPriceText
import com.example.ui.components.StatusChip
import com.example.ui.viewmodel.MainViewModel
import com.example.util.ChileanUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PosCashScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val selectedCompany by viewModel.selectedCompany.collectAsState()
    val allCompanies by viewModel.allCompanies.collectAsState()
    val company = selectedCompany ?: allCompanies.firstOrNull()

    if (company == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No hay negocio seleccionado.")
        }
        return
    }

    val activeShift by viewModel.repository.getActiveShift(company.id).collectAsState(initial = null)
    val movements by (activeShift?.let { viewModel.repository.getMovementsByShift(it.id) }
        ?: kotlinx.coroutines.flow.flowOf(emptyList())).collectAsState(initial = emptyList())
    val companyOrders by viewModel.repository.getOrdersByCompany(company.id).collectAsState(initial = emptyList())

    var showOpenShiftDialog by remember { mutableStateOf(false) }
    var showCloseShiftDialog by remember { mutableStateOf(false) }
    var showMovementDialog by remember { mutableStateOf(false) }

    // Dialogs state
    var cashierNameInput by remember { mutableStateOf("Cajero Central") }
    var initialBalanceInput by remember { mutableStateOf("50000") }
    var declaredCashInput by remember { mutableStateOf("") }
    var closingNotesInput by remember { mutableStateOf("") }

    var movementType by remember { mutableStateOf(MovementType.EGRESO) }
    var movementAmountInput by remember { mutableStateOf("") }
    var movementReasonInput by remember { mutableStateOf("") }

    // Dialog: Abrir Turno
    if (showOpenShiftDialog) {
        AlertDialog(
            onDismissRequest = { showOpenShiftDialog = false },
            title = { Text("Apertura de Turno de Caja", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Ingresa el fondo inicial de sencillo para el cambio en moneda chilena (CLP):", fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = cashierNameInput,
                        onValueChange = { cashierNameInput = it },
                        label = { Text("Nombre del Cajero") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = initialBalanceInput,
                        onValueChange = { initialBalanceInput = it },
                        label = { Text("Fondo Inicial en CLP (ej: 50000)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = initialBalanceInput.toIntOrNull() ?: 50000
                        viewModel.openCashShift(company.id, cashierNameInput, amount)
                        showOpenShiftDialog = false
                    }
                ) {
                    Text("Abrir Turno")
                }
            },
            dismissButton = {
                TextButton(onClick = { showOpenShiftDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Dialog: Cerrar Turno (Arqueo con detección de descuadre)
    if (showCloseShiftDialog && activeShift != null) {
        val shift = activeShift!!
        val calculatedSystemCash = movements.sumOf { it.amount }
        val declaredAmount = declaredCashInput.toIntOrNull() ?: 0
        val difference = declaredAmount - calculatedSystemCash

        AlertDialog(
            onDismissRequest = { showCloseShiftDialog = false },
            title = { Text("Cierre de Caja y Arqueo", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Cajero: ${shift.cashierName}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Efectivo calculado por sistema: ${ChileanUtils.formatClp(calculatedSystemCash)}",
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = declaredCashInput,
                        onValueChange = { declaredCashInput = it },
                        label = { Text("Efectivo Físico Contado (CLP) *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (declaredCashInput.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            color = if (difference == 0) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (difference == 0) {
                                    "✓ Caja cuadrada exactamente ($0 diferencia)"
                                } else if (difference > 0) {
                                    "⚠️ Sobrante de caja: +${ChileanUtils.formatClp(difference)}"
                                } else {
                                    "⚠️ Descuadre (Faltante): ${ChileanUtils.formatClp(difference)}"
                                },
                                color = if (difference == 0) Color(0xFF2E7D32) else Color(0xFFC62828),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = closingNotesInput,
                        onValueChange = { closingNotesInput = it },
                        label = { Text("Observaciones del cierre") },
                        maxLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val declared = declaredCashInput.toIntOrNull() ?: calculatedSystemCash
                        viewModel.closeCashShift(shift.id, declared, closingNotesInput)
                        showCloseShiftDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828))
                ) {
                    Text("Confirmar Cierre")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCloseShiftDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Dialog: Movimiento Manual de Efectivo (Ingreso / Egreso)
    if (showMovementDialog && activeShift != null) {
        AlertDialog(
            onDismissRequest = { showMovementDialog = false },
            title = { Text("Movimiento de Efectivo en Caja", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { movementType = MovementType.INGRESO },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (movementType == MovementType.INGRESO) Color(0xFF2E7D32) else Color.LightGray
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Ingreso")
                        }
                        Button(
                            onClick = { movementType = MovementType.EGRESO },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (movementType == MovementType.EGRESO) Color(0xFFC62828) else Color.LightGray
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Egreso")
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = movementAmountInput,
                        onValueChange = { movementAmountInput = it },
                        label = { Text("Monto en CLP (ej: 5000)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = movementReasonInput,
                        onValueChange = { movementReasonInput = it },
                        label = { Text("Motivo (ej: Compra de hielo, Sencillo)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = movementAmountInput.toIntOrNull() ?: 0
                        if (amount > 0 && movementReasonInput.isNotBlank()) {
                            viewModel.addCashMovement(activeShift!!.id, movementType, amount, movementReasonInput)
                            showMovementDialog = false
                            movementAmountInput = ""
                            movementReasonInput = ""
                        }
                    }
                ) {
                    Text("Registrar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showMovementDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("pos_cash_screen")
    ) {
        // POS Header
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.PointOfSale, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "CAJA Y POS — ${company.commercialName}",
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp
                        )
                    }
                    Text(
                        text = "Apertura, Cierres, Arqueo e Ingresos/Egresos",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        // Active Shift Card
        item {
            val shift = activeShift
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (shift != null) Color(0xFFF1F8E9) else Color(0xFFFFEBEE)
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (shift != null) Icons.Default.LockOpen else Icons.Default.Lock,
                                contentDescription = null,
                                tint = if (shift != null) Color(0xFF2E7D32) else Color(0xFFC62828)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (shift != null) "TURNO DE CAJA ABIERTO" else "CAJA CERRADA",
                                fontWeight = FontWeight.Black,
                                fontSize = 15.sp,
                                color = if (shift != null) Color(0xFF2E7D32) else Color(0xFFC62828)
                            )
                        }
                        if (shift != null) {
                            Text(
                                text = shift.cashierName,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.DarkGray
                            )
                        }
                    }

                    if (shift != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        val currentBalance = movements.sumOf { it.amount }
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                Text("Fondo Inicial:", fontSize = 11.sp, color = Color.Gray)
                                ChileanPriceText(amount = shift.initialBalance, fontSize = 14, fontWeight = FontWeight.Bold)
                            }
                            Column {
                                Text("Efectivo en Caja:", fontSize = 11.sp, color = Color.Gray)
                                ChileanPriceText(amount = currentBalance, fontSize = 16, fontWeight = FontWeight.Black, color = Color(0xFF1B5E20))
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { showMovementDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF455A64)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Movimiento (±)", fontSize = 12.sp)
                            }
                            Button(
                                onClick = { showCloseShiftDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Cerrar Turno", fontSize = 12.sp)
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "No hay turno activo para este local. Abre la caja para comenzar a cobrar ventas en efectivo y registrar movimientos.",
                            fontSize = 12.sp,
                            color = Color.DarkGray
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = { showOpenShiftDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Abrir Turno de Caja")
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Section: Cobro de Pedidos y Ventas
        item {
            Text(
                text = "Comandas y Registro de Ventas (${companyOrders.size})",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        items(companyOrders) { order ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(10.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Pedido #${order.orderNumber}",
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            StatusChip(status = order.status)
                        }
                        Text(
                            text = "${order.customerName} • ${order.paymentMethod.label}",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                        ChileanPriceText(amount = order.totalClp, fontSize = 14)
                    }

                    if (order.isPaid) {
                        Surface(
                            color = Color(0xFFE8F5E9),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("PAGADO", color = Color(0xFF2E7D32), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        Button(
                            onClick = {
                                viewModel.markOrderPaid(order.id, true, activeShift?.id)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text("Cobrar", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Section: Movimientos de Turno
        if (movements.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Historial de Movimientos de Caja (${movements.size})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            items(movements) { mov ->
                val timeFormat = SimpleDateFormat("HH:mm:ss", Locale("es", "CL"))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = mov.reason,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp
                            )
                            Text(
                                text = "${mov.movementType.label} • ${timeFormat.format(Date(mov.createdAt))}",
                                fontSize = 10.sp,
                                color = Color.Gray
                            )
                        }
                        Text(
                            text = (if (mov.amount >= 0) "+" else "") + ChileanUtils.formatClp(mov.amount),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (mov.amount >= 0) Color(0xFF2E7D32) else Color(0xFFC62828)
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}
