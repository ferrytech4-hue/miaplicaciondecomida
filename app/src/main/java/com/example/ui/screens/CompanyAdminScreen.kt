package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddBusiness
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CorporateFare
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CompanyEntity
import com.example.data.model.PlanType
import com.example.ui.components.ChileanPriceText
import com.example.ui.components.StoreQrDialog
import com.example.ui.theme.WhatsAppGreen
import com.example.ui.viewmodel.AppMode
import com.example.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompanyAdminScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val allCompanies by viewModel.allCompanies.collectAsState()
    val selectedCompany by viewModel.selectedCompany.collectAsState()

    var showRegisterModal by remember { mutableStateOf(false) }
    var qrCompanyToView by remember { mutableStateOf<CompanyEntity?>(null) }

    // Register Form State
    var newCommercialName by remember { mutableStateOf("") }
    var newLegalName by remember { mutableStateOf("") }
    var newRut by remember { mutableStateOf("") }
    var newSlug by remember { mutableStateOf("") }
    var newEmail by remember { mutableStateOf("") }
    var newPhone by remember { mutableStateOf("+569") }
    var newWhatsApp by remember { mutableStateOf("+569") }
    var newCategory by remember { mutableStateOf("Comida Rápida") }
    var newDeliveryFee by remember { mutableStateOf("1500") }

    if (qrCompanyToView != null) {
        StoreQrDialog(
            companyName = qrCompanyToView!!.commercialName,
            slug = qrCompanyToView!!.slug,
            onDismiss = { qrCompanyToView = null }
        )
    }

    if (showRegisterModal) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showRegisterModal = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .verticalScroll(rememberScrollState())
                    .testTag("register_company_modal")
            ) {
                Text(
                    text = "Registrar Nuevo Negocio Gastronómico",
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp
                )
                Text(
                    text = "Crea un nuevo local independiente con su propio menú, WhatsApp y aislamiento de datos.",
                    fontSize = 12.sp,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = newCommercialName,
                    onValueChange = {
                        newCommercialName = it
                        if (newSlug.isBlank()) {
                            newSlug = it.lowercase().replace(" ", "-").replace(Regex("[^a-z0-9-]"), "")
                        }
                    },
                    label = { Text("Nombre Comercial * (ej: Sanguchería La Esquina)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = newSlug,
                    onValueChange = { newSlug = it },
                    label = { Text("Enlace / Slug * (mipedidoexpress.cl/slug)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newLegalName,
                        onValueChange = { newLegalName = it },
                        label = { Text("Razón Social") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = newRut,
                        onValueChange = { newRut = it },
                        label = { Text("RUT Chileno") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newEmail,
                        onValueChange = { newEmail = it },
                        label = { Text("Correo Electrónico") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = newWhatsApp,
                        onValueChange = { newWhatsApp = it },
                        label = { Text("WhatsApp Pedidos (+569..)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newCategory,
                        onValueChange = { newCategory = it },
                        label = { Text("Categoría Comercial") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = newDeliveryFee,
                        onValueChange = { newDeliveryFee = it },
                        label = { Text("Tarifa Despacho (CLP)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (newCommercialName.isNotBlank() && newSlug.isNotBlank() && newWhatsApp.isNotBlank()) {
                            viewModel.registerCompany(
                                CompanyEntity(
                                    slug = newSlug.trim(),
                                    legalName = newLegalName.ifBlank { newCommercialName },
                                    rut = newRut.ifBlank { "76.000.000-1" },
                                    commercialName = newCommercialName.trim(),
                                    contactEmail = newEmail.ifBlank { "contacto@$newSlug.cl" },
                                    contactPhone = newPhone,
                                    whatsappNumber = newWhatsApp.trim(),
                                    logoUrl = "https://images.unsplash.com/photo-1555396273-367ea4eb4db5?w=400",
                                    bannerUrl = "https://images.unsplash.com/photo-1504674900247-0877df9cc836?w=1200",
                                    primaryColorHex = "#D32F2F",
                                    category = newCategory.ifBlank { "Comida Rápida" },
                                    deliveryFeeBase = newDeliveryFee.toIntOrNull() ?: 1500,
                                    isPublished = true,
                                    isOpenManual = true,
                                    subscriptionPlan = PlanType.PROFESIONAL
                                ),
                                onSuccess = { created ->
                                    Toast.makeText(context, "¡Empresa ${created.commercialName} creada exitosamente!", Toast.LENGTH_SHORT).show()
                                    showRegisterModal = false
                                }
                            )
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Guardar y Activar Empresa")
                }

                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("company_admin_screen")
    ) {
        // Header
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CorporateFare, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "MULTITENANT & LOCALES",
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp
                        )
                    }
                    Text(
                        text = "Aislamiento de datos y gestión de empresas",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }

                Button(
                    onClick = { showRegisterModal = true },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.AddBusiness, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Nuevo Local", fontSize = 12.sp)
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        // Active Tenant Banner
        item {
            val active = selectedCompany
            if (active != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                Text(
                                    text = "LOCAL ACTIVO EN GESTIÓN",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = active.commercialName,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Surface(
                                color = MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "Plan ${active.subscriptionPlan.name}",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Slug: mipedidoexpress.cl/${active.slug} • RUT: ${active.rut} • WhatsApp: ${active.whatsappNumber}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    viewModel.setMode(AppMode.STOREFRONT)
                                },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Ver Tienda Digital", fontSize = 12.sp)
                            }
                            Button(
                                onClick = { qrCompanyToView = active },
                                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color(0xFF455A64)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.QrCode, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Ver QR", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // List of all Registered Companies
        item {
            Text(
                text = "Todas las Empresas Registradas (${allCompanies.size})",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
            Text(
                text = "Toca cualquier empresa para cambiar el contexto y verificar el aislamiento de pedidos, cocina y caja.",
                fontSize = 11.sp,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        items(allCompanies) { comp ->
            val isCurrent = selectedCompany?.id == comp.id
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isCurrent) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(12.dp),
                border = if (isCurrent) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clickable { viewModel.selectCompany(comp) }
                    .testTag("company_item_${comp.slug}")
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = comp.commercialName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            if (isCurrent) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            }
                        }
                        Text(
                            text = "mipedidoexpress.cl/${comp.slug} • WhatsApp: ${comp.whatsappNumber}",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = if (comp.isPublished) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = if (comp.isPublished) "Catálogo: Publicado" else "Solo Tienda Directa",
                                    color = if (comp.isPublished) Color(0xFF2E7D32) else Color(0xFFC62828),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Button(
                        onClick = {
                            viewModel.selectCompany(comp)
                            Toast.makeText(context, "Contexto cambiado a: ${comp.commercialName}", Toast.LENGTH_SHORT).show()
                        },
                        colors = if (isCurrent) androidx.compose.material3.ButtonDefaults.outlinedButtonColors() else androidx.compose.material3.ButtonDefaults.buttonColors(),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text(if (isCurrent) "Activo" else "Seleccionar", fontSize = 11.sp)
                    }
                }
            }
        }

        // WhatsApp Business Cloud API Configuration Section
        item {
            Spacer(modifier = Modifier.height(20.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = WhatsAppGreen,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Settings, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Integración Oficial WhatsApp Business",
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "1. Modalidad Inicial (Activa): Generación de enlace directo 'https://wa.me/569...' con desglose formal en CLP, sin coste de mensajes por API de Meta.\n" +
                                "2. Modalidad Avanzada: Preparada para Webhook y WhatsApp Cloud API oficial de Meta para confirmaciones automáticas de estado.",
                        fontSize = 11.sp,
                        color = Color.DarkGray,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}
